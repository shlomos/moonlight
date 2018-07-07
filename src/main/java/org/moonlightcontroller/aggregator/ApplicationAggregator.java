package org.moonlightcontroller.aggregator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Random;

import org.moonlightcontroller.aggregator.Tupple.Pair;
import org.moonlightcontroller.bal.BoxApplication;
import org.moonlightcontroller.mtd.IApplicationType;
import org.moonlightcontroller.registry.IApplicationRegistry;
import org.moonlightcontroller.blocks.Alert;
import org.moonlightcontroller.blocks.Discard;
import org.moonlightcontroller.blocks.FromDevice;
import org.moonlightcontroller.blocks.HeaderClassifier;
import org.moonlightcontroller.blocks.HeaderClassifier.HeaderClassifierRule;
import org.moonlightcontroller.blocks.IClassifierProcessingBlock;
import org.moonlightcontroller.blocks.IStaticProcessingBlock;
import org.moonlightcontroller.blocks.NetworkHeaderFieldsRewriter;
import org.moonlightcontroller.blocks.ToDevice;
import org.moonlightcontroller.events.EventManager;
import org.moonlightcontroller.events.InstanceAlertArgs;
import org.moonlightcontroller.exceptions.MergeException;
import org.moonlightcontroller.managers.models.messages.AlertMessage;
import org.moonlightcontroller.processing.BlockClass;
import org.moonlightcontroller.processing.Connector;
import org.moonlightcontroller.processing.IConnector;
import org.moonlightcontroller.processing.IProcessingBlock;
import org.moonlightcontroller.processing.IProcessingGraph;
import org.moonlightcontroller.processing.MutableProcessingGraph;
import org.moonlightcontroller.processing.ProcessingGraph;
import org.moonlightcontroller.topology.ILocationSpecifier;
import org.moonlightcontroller.topology.ITopologyManager;
import org.moonlightcontroller.topology.InstanceLocationSpecifier;
import org.moonlightcontroller.topology.TopologyManager;
import org.openboxprotocol.protocol.HeaderField;
import org.openboxprotocol.protocol.IStatement;
import org.openboxprotocol.protocol.OpenBoxHeaderMatch;
import org.openboxprotocol.protocol.Priority;
import org.openboxprotocol.types.EthType;
import org.openboxprotocol.types.IPv4Address;
import org.openboxprotocol.types.IpProto;
import org.openboxprotocol.types.TransportPort;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

/**
 * The class which is responsible for performing aggregation
 *
 */
public class ApplicationAggregator implements IApplicationAggregator {
	
	public static final boolean AGGREGATOR_DEBUG = false;
	
	/**
	 * Merges two processing graphs. This method assumes order of (a, b).
	 * 
	 * @param a First processing graph
	 * @param b Second processing graph
	 * @return A single, merged processing graph
	 */
	private static IProcessingGraph merge(IProcessingGraph a, IProcessingGraph b) {
		IProcessingGraph an = GraphUtils.normalizeToTree(a);
		IProcessingGraph bn = GraphUtils.normalizeToTree(b);
		
		if (AGGREGATOR_DEBUG) {
			System.out.println("Graph A normalized:");
			System.out.println(an);
			System.out.println("Graph B normalized:");
			System.out.println(bn);
		}
		
		IProcessingGraph unified = GraphUtils.concatGraphs(an, bn);
		if (AGGREGATOR_DEBUG) {
			System.out.println("Unified uncompressed graph:");
			System.out.println(unified);
		}
		
		MutableProcessingGraph unifiedMutable = new MutableProcessingGraph(unified);
		
		//compress(unifiedMutable, unified.getRoot(), null, 0, null, 0, new HashMap<>());
		compress(unifiedMutable);
		
		rewireClones(unifiedMutable);

		return unifiedMutable;
	}
	
	/**
	 * Returns a list of blocks that makes the path from 'from' until a non-static block is found, if such path exists, or null otherwise.
	 * Precondition: there are no branches in the requested path.
	 * 
	 * @param graph Graph to search path in
	 * @param from Start block
	 * @return list of blocks or null if no path exists
	 * @throws IllegalArgumentException when a branch is found
	 */
	private static List<IProcessingBlock> getStraightStaticsPath(IProcessingGraph graph, IProcessingBlock from) {
		IProcessingBlock current = from;
		List<IProcessingBlock> result = new ArrayList<>();
		
		while (current.getBlockClass() == BlockClass.BLOCK_CLASS_STATIC && graph.getOutDegree(current) == 1) {
			result.add(current);
			
			List<IProcessingBlock> successors = graph.getSuccessors(current);

			current = successors.get(0);
		}
		result.add(current);
		
		return result;
	}
		
	/**
	 * Clones a path from <code>from</code> to <code>to</code>, if such path exists.
	 * 
	 * @param graph Processing graph
	 * @param from Processing block to start cloning from
	 * @param to Processing block to stop cloning at (excluding)
	 * @param parent Parent of <code>from</code>
	 * @param parentPort Output port number of <code>parent</code> that is connected to <code>from</code>
	 * @param blocks A collection of blocks to be added to the graph (will be filled by this method)
	 * @param connectors A collection of connectors to be added to the graph (will be filled by this method)
	 * @param blocksToRemove A collection of blocks to be removed from the graph (will be filled by this method)
	 * @param connectorsToRemove A collection of connectors to be removed from the graph (will be filled by this method)
	 * @return If a path exists, this method returns the last block in the cloned path. Otherwise, it returns null.
	 */
	private static IProcessingBlock clonePath(IProcessingGraph graph,
			IProcessingBlock from, IProcessingBlock to, 
			IProcessingBlock parent, int parentPort,
			Collection<IProcessingBlock> blocks, Collection<IConnector> connectors,
			Set<IProcessingBlock> blocksToRemove, Set<IConnector> connectorsToRemove) {
		
		Set<IProcessingBlock> bta = new HashSet<>();
		Set<IConnector> cta = new HashSet<>();
		Set<IProcessingBlock> btr = new HashSet<>();
		Set<IConnector> ctr = new HashSet<>();
		
		IProcessingBlock result = null;
		
		while (true) {
			if (from == to) {
				result = parent;
				break;
			}
			
			List<IConnector> outs = graph.getOutgoingConnectors(from);
			if (outs.size() == 0) {
				// No path exists
				return null;
			} else if (outs.size() > 1) {
				//throw new IllegalArgumentException("Unexpected branch encountered");
				return null;
			}
			
			IProcessingBlock newCurrent = from.clone();
			bta.add(newCurrent);
			cta.add(new Connector.Builder()
					.setSourceBlock(parent)
					.setSourceOutputPort(parentPort)
					.setDestBlock(newCurrent).build());
			btr.add(from);
			ctr.add(outs.get(0));
			
			from = outs.get(0).getDestBlock();
			parent = newCurrent;
			parentPort = outs.get(0).getSourceOutputPort();
		}
		
		blocks.addAll(bta);
		connectors.addAll(cta);
		blocksToRemove.addAll(btr);
		connectorsToRemove.addAll(ctr);
		
		return result;
		//return clonePath(graph, outs.get(0).getDestBlock(), to, newCurrent, outs.get(0).getSourceOutputPort(), newCurrent, blocks, connectors, blocksToRemove, connectorsToRemove);
	}
	
	/**
	 * Clones a sub tree from the given <code>current</code> block, connecting the cloned sub tree to
	 * <code>parent</code> on output port number given at <code>parentPort</code>.
	 * 
	 * @param graph Processing graph
	 * @param current Block to start cloning at
	 * @param parent Parent block of <code>current</code>
	 * @param parentPort Output port number of <code>parent</code> that is connected to <code>current</code>
	 * @param blocks A collection of blocks to be added to the graph (will be filled by this method)
	 * @param connectors A collection of connectors to be added to the graph (will be filled by this method)
	 * @param blocksToRemove A collection of blocks to be removed from the graph (will be filled by this method)
	 * @param connectorsToRemove A collection of connectors to be removed from the graph (will be filled by this method)
	 */
	private static void cloneSubTree(IProcessingGraph graph,
			IProcessingBlock current, IProcessingBlock parent, int parentPort,
			Collection<IProcessingBlock> blocks, Collection<IConnector> connectors,
			Set<IProcessingBlock> blocksToRemove, Set<IConnector> connectorsToRemove) {
		IProcessingBlock newCurrent = current.clone();
		IConnector newConnector = new Connector.Builder()
				.setSourceBlock(parent)
				.setSourceOutputPort(parentPort)
				.setDestBlock(newCurrent)
				.build();
		blocksToRemove.add(current);
		blocks.add(newCurrent);
		connectors.add(newConnector);
		List<IConnector> outs = graph.getOutgoingConnectors(current);
		for (IConnector c: outs) {
			connectorsToRemove.add(c);
			cloneSubTree(graph, c.getDestBlock(), newCurrent, c.getSourceOutputPort(), blocks, connectors, blocksToRemove, connectorsToRemove);
		}
	}	
	
	//private static String _lastGraphStr = "";
	
	/**
	 * Compresses the lengths of paths in a normalized graph by merging classifiers and statics.
	 * 
	 * @param graph
	 */
	private static void compress(MutableProcessingGraph graph) {
		IProcessingBlock current;
		IProcessingBlock start;
		int startOutPort;
		
		String graphStr = graph.toString();
		
		boolean changed = false;
		
		start = null;
		
		Queue<IProcessingBlock> remaining = new LinkedList<>();
		Queue<Integer> remainingStartOutPort = new LinkedList<>();
		
		remaining.add(graph.getRoot());
		remainingStartOutPort.add(-1);
		
		while (remaining.size() > 0) {
			
			current = remaining.poll();
			startOutPort = remainingStartOutPort.poll();
			
			if (AGGREGATOR_DEBUG) {
				System.out.println("[DEBUG current: " + current.toShortString() + "]");
			}
			
			// The problem is merging classifiers, modifiers and shapers (denoted from now - dynamic).
			// Also, statics cannot be reordered across such dynamic blocks.
			// So on every path, we find such blocks and try to work out the ones in between them.
			if (current.getBlockClass() == BlockClass.BLOCK_CLASS_CLASSIFIER ||
					current.getBlockClass() == BlockClass.BLOCK_CLASS_MODIFIER ||
					current.getBlockClass() == BlockClass.BLOCK_CLASS_SHAPER) {
				if (start == null) {
					// This is the first dynamic block we see (at this point of time)
					start = current;
					for (IConnector c : graph.getOutgoingConnectors(current)) {
						if (!remaining.contains(c.getDestBlock())) {
							remaining.add(c.getDestBlock()); 
							remainingStartOutPort.add(c.getSourceOutputPort());
						}
					}
					continue;
				} else {
					// start is not null - we now have an end dynamic block
					IProcessingBlock end = current;
					List<IConnector> nextConnectors;
					
					//if (replacements.containsKey(start))
					//	start = replacements.get(start);
					
					Set<IProcessingBlock> lastBeforeEnd = new HashSet<>();

					IProcessingBlock newStart;

					// If both blocks are classifiers of the same type we can merge them
					if (start.getBlockClass() == BlockClass.BLOCK_CLASS_CLASSIFIER &&
							end.getBlockClass() == BlockClass.BLOCK_CLASS_CLASSIFIER &&
							start instanceof IClassifierProcessingBlock &&
							end instanceof IClassifierProcessingBlock &&
							((IClassifierProcessingBlock)start).canMergeWith((IClassifierProcessingBlock)end)) {
						// Merge classifiers
						Set<IProcessingBlock> blocksToRemove = new HashSet<>();
						Set<IConnector> connectorsToRemove = new HashSet<>();
						Set<IProcessingBlock> blocksToAdd = new HashSet<>();
						Set<IConnector> connectorsToAdd = new HashSet<>();

						List<Pair<Integer>> outPortSources = new ArrayList<>();
						
						try {
							IProcessingBlock merged = ((IClassifierProcessingBlock)start).mergeWith((IClassifierProcessingBlock)end, graph, outPortSources);
							
							if (merged instanceof HeaderClassifier) {
								printRules((HeaderClassifier)merged);
							}
							
							blocksToRemove.add(start);
							
							//replacements.put(start, merged);
							//replacements.put(end, merged);
							
							for (int i = 0; i < outPortSources.size(); i++) {
								// For each out port we have a pair (a, b). We should go over the path from 'start' out port a 
								// to 'end', and clone all blocks on the way except for 'end'. When reaching 'end', should take 
								// the sub-tree from 'end' out port b, clone it, and connect to 'end's parent.
								// On the way, we should mark all cloned blocks to remove.
								
								// Step 1: clone the path from 'start'[outPortSources[0]] to 'end' (excluding) and connect it from 'merged'[outPort]
								// (mark all cloned blocks to be removed)
								IProcessingBlock origin = graph.getOutgoingConnector(start, outPortSources.get(i).get(0)).getDestBlock();
								
								IProcessingBlock last = clonePath(graph, 
										origin, 
										end, 
										merged, 
										i, 
										blocksToAdd, connectorsToAdd, 
										blocksToRemove, connectorsToRemove);
								
								if (last != null) { // <- There was an actual such path
									if (outPortSources.get(i).get(0) == startOutPort) {
										// Marking 'last' for later use when looking for statics to merge
										lastBeforeEnd.add(last);
									}
									
									// Step 2: clone the subtree going out of 'end'[outPortSources[1]] and connect it from the last block of the path cloned above
									// (mark all cloned blocks to be removed)
									cloneSubTree(graph, 
											graph.getOutgoingConnector(end, outPortSources.get(i).get(1)).getDestBlock(), 
											last, (last == merged ? i : 0), 
											blocksToAdd, connectorsToAdd, 
											blocksToRemove, connectorsToRemove);
								} else {
									// Retain existing connectors
									connectorsToAdd.add(new Connector.Builder()
											.setSourceBlock(merged)
											.setSourceOutputPort(i)
											.setDestBlock(origin)
											.build());
								}
							}
							// Replace 'start' with 'merged'
							blocksToAdd.add(merged);
							blocksToRemove.add(start);
							graph.getIncomingConnectors(start).forEach(c -> connectorsToAdd.add(new Connector.Builder()
									.setSourceBlock(c.getSourceBlock())
									.setSourceOutputPort(c.getSourceOutputPort())
									.setDestBlock(merged)
									.build()));
							graph.getIncomingConnectors(start).forEach(c -> connectorsToRemove.add(c));
							
							blocksToRemove.add(end);
							
							List<IConnector> incoming = graph.getIncomingConnectors(end);
							incoming.forEach(c -> connectorsToRemove.add(c));

							newStart = merged;
							
							// Update graph
							blocksToRemove.forEach(b -> graph.removeBlock(b));
							connectorsToRemove.forEach(c -> graph.removeConnector(c));
							blocksToAdd.forEach(b -> graph.addBlock(b));
							connectorsToAdd.forEach(c -> graph.addConnector(c));
							
							//graph.getConnectors()
							//	.removeIf(c -> (!graph.getBlocks().contains(c.getSourceBlock()) || !graph.getBlocks().contains(c.getDestBlock()))); 

							String newGraphStr = graph.toString();
							if (!newGraphStr.equals(graphStr)) {
								changed = true;
								graphStr = newGraphStr;
							}
							
							if (AGGREGATOR_DEBUG) {
								if (changed) {
									System.out.println("Graph changed! New graph:\n" + newGraphStr);
								}
							}

						} catch (MergeException e) {
							// Cannot merge classifier
							newStart = start;
						}
					} else {
						// Cannot merge classifier
						newStart = start;
					}
					// For each connector going out from start (or merged), find statics to merge
					// until 'end'
					nextConnectors = graph.getOutgoingConnectors(newStart);

					Set<IProcessingBlock> blocksToRemove = new HashSet<>();
					Set<IConnector> connectorsToRemove = new HashSet<>();
					Set<IProcessingBlock> blocksToAdd = new HashSet<>();
					Set<IConnector> connectorsToAdd = new HashSet<>();
					
					for (IConnector next : nextConnectors) {
						// This must be done on the cloned blocks, can't do that on original graph
						// However, after the changes, 'end' does not exist in the graph anymore...
						// In fact this should be separated into two actions:
						// 1. Merge the classifiers, and mark the clones of the block that was before 'end'
						// 2. Search the paths from 'merged' to these marked clones for merging statics
						List<IProcessingBlock> path;
						path = getStraightStaticsPath(graph, next.getDestBlock());

						if (path == null) {
							continue;
						}

						for (int i = 0; i < path.size(); i++) {
							IProcessingBlock v1 = path.get(i);
							for (int j = i + 1; j < path.size() - 1; j++) {
								IProcessingBlock v2 = path.get(j);
								if (v1.getBlockType().equals(v2.getBlockType()) &&
										!blocksToRemove.contains(v1) &&
										!blocksToRemove.contains(v2) &&
										v1 instanceof IStaticProcessingBlock &&
										v2 instanceof IStaticProcessingBlock &&
										((IStaticProcessingBlock)v1).canMergeWith((IStaticProcessingBlock)v2)) {
									// Found statics to merge
									try {
										IStaticProcessingBlock merged = ((IStaticProcessingBlock)v1).mergeWith((IStaticProcessingBlock)v2);
										blocksToRemove.add(v1);
										blocksToRemove.add(v2);
										IConnector v1incoming = graph.getIncomingConnectors(v1).get(0);
										IConnector v1outgoing = graph.getOutgoingConnectors(v1).get(0);
										IConnector v2incoming = graph.getIncomingConnectors(v2).get(0);
										IConnector v2outgoing = graph.getOutgoingConnectors(v2).get(0);
										connectorsToAdd.add(new Connector.Builder()
												.setSourceBlock(v1incoming.getSourceBlock())
												.setSourceOutputPort(v1incoming.getSourceOutputPort())
												.setDestBlock(merged)
												.build());
										connectorsToAdd.add(new Connector.Builder()
												.setSourceBlock(merged)
												.setSourceOutputPort(0)
												.setDestBlock(v2outgoing.getDestBlock())
												.build());
										connectorsToRemove.add(v1incoming);
										connectorsToRemove.add(v1outgoing);
										connectorsToRemove.add(v2incoming);
										connectorsToRemove.add(v2outgoing);
										blocksToAdd.add(merged);
										//replacements.put(v1, merged);
										//replacements.put(v2, merged);
									} catch (MergeException e) {
										// Cannot actually merge these blocks
										// Do nothing
									}
								}
							}
						}
						
					}

					blocksToRemove.forEach(b -> graph.removeBlock(b));
					connectorsToRemove.forEach(c -> graph.removeConnector(c));
					blocksToAdd.forEach(b -> graph.addBlock(b));
					connectorsToAdd.forEach(c -> graph.addConnector(c));
					
					//graph.getConnectors()
					//	.removeIf(c -> (!graph.getBlocks().contains(c.getSourceBlock()) || !graph.getBlocks().contains(c.getDestBlock()))); 

					String newGraphStr = graph.toString();
					boolean rechanged = false;
					if (!newGraphStr.equals(graphStr)) {
						changed = true;
						rechanged = true;
						graphStr = newGraphStr;
					}
					
					if (AGGREGATOR_DEBUG) {
						if (rechanged) {
							System.out.println("Graph changed! New graph:\n" + newGraphStr);
						}
					}					
					
					// Restart compress if graph has changed
					if (changed) {
						compress(graph);
						return;
					}
				}
				
				
			} else {
				// Skip Terminals, Statics
				for (IConnector c : graph.getOutgoingConnectors(current)) {
					remaining.add(c.getDestBlock()); 
					remainingStartOutPort.add(c.getSourceOutputPort());
				}
				continue;
			}
			
		}
	}
	
	/**
	 * Groups all clones of the same block and changes connectors to point to only one of the instances,
	 * discarding other clones used to normalize the graph into a tree or to duplicate parts of the graph
	 * during the compression process.
	 * 
	 * @param graph
	 */
	private static void rewireClones(MutableProcessingGraph graph) {
		graph.clean();
		
		List<IProcessingBlock> blocks = graph.getBlocks();
		
		List<IProcessingBlock> clones = blocks.stream().filter(b -> b.isClone()).collect(Collectors.toList());
		
		Map<IProcessingBlock, Set<IProcessingBlock>> sets = new HashMap<IProcessingBlock, Set<IProcessingBlock>>();
		
		for (IProcessingBlock clone : clones) {
			if (!sets.containsKey(clone.getOriginalInstance())) {
				sets.put(clone.getOriginalInstance(), new HashSet<>());
			}
			sets.get(clone.getOriginalInstance()).add(clone);
		}
	
		for (Set<IProcessingBlock> s : sets.values()) {
			IProcessingBlock leader;
			IProcessingBlock rep = s.iterator().next();
			if (blocks.contains(rep.getOriginalInstance())) {
				leader = rep.getOriginalInstance();
			} else {
				leader = rep;
			}
			
			for (IProcessingBlock clone : s) {
				if (clone != leader) {
					List<IConnector> ins = graph.getIncomingConnectors(clone);
					ins.forEach(c -> graph.addConnector(new Connector.Builder()
							.setSourceBlock(c.getSourceBlock())
							.setSourceOutputPort(c.getSourceOutputPort())
							.setDestBlock(leader)
							.build()));
					ins.forEach(c -> graph.removeConnector(c));
				}
			}
		}
		
		graph.clean();
	}
	
	public static void main(String[] args) throws Exception {
		
		HeaderClassifierRule[] firewallRules = {
				new HeaderClassifierRule.Builder()
					.setHeaderMatch(
						new OpenBoxHeaderMatch.Builder()
							.setExact(HeaderField.ETH_TYPE, EthType.IPv4)
							.setExact(HeaderField.IP_PROTO, IpProto.TCP)
							.setExact(HeaderField.TCP_DST, TransportPort.of(80))
							.build())
					.setPriority(Priority.CRITICAL)
					.setOrder(0)
					.build(),
				new HeaderClassifierRule.Builder()
					.setHeaderMatch(
						new OpenBoxHeaderMatch.Builder()
							.setExact(HeaderField.ETH_TYPE, EthType.IPv4)
							.setMasked(HeaderField.IPV4_SRC, IPv4Address.of("202.101.0.0"), IPv4Address.of("255.255.0.0"))
							.build())
					.setPriority(Priority.MEDIUM)
					.setOrder(1)
					.build(),
				new HeaderClassifierRule.Builder()
					.setHeaderMatch(new OpenBoxHeaderMatch.Builder().build())
					.setPriority(Priority.DEFAULT)
					.setOrder(2)
					.build()
						
		};
		
		IProcessingBlock[] firewallBlocks = {
				new FromDevice("FromDevice-Firewall", ""),
				new HeaderClassifier("HeaderClassifier-Firewall", ImmutableList.copyOf(firewallRules), Priority.CRITICAL, false),
				new Discard("Discard-Firewall"),
				new Alert("Alert-Firewall", "FIREWALL ALERT"),
				new ToDevice("ToDevice-Firewall", ""),
		};
		IConnector[] firewallConnectors = {
				new Connector.Builder().setSourceBlock(firewallBlocks[0]).setSourceOutputPort(0).setDestBlock(firewallBlocks[1]).build(),
				new Connector.Builder().setSourceBlock(firewallBlocks[1]).setSourceOutputPort(0).setDestBlock(firewallBlocks[2]).build(),
				new Connector.Builder().setSourceBlock(firewallBlocks[1]).setSourceOutputPort(1).setDestBlock(firewallBlocks[3]).build(),
				new Connector.Builder().setSourceBlock(firewallBlocks[1]).setSourceOutputPort(2).setDestBlock(firewallBlocks[4]).build(),
				new Connector.Builder().setSourceBlock(firewallBlocks[3]).setSourceOutputPort(0).setDestBlock(firewallBlocks[4]).build(),
		};
		
		HeaderClassifierRule[] lbRules = {
				new HeaderClassifierRule.Builder()
					.setHeaderMatch(
						new OpenBoxHeaderMatch.Builder()
							.setExact(HeaderField.ETH_TYPE, EthType.IPv4)
							.setExact(HeaderField.IPV4_DST, IPv4Address.of("10.0.0.1"))
							.build())
						.setPriority(Priority.VERY_HIGH)
						.setOrder(0)
						.build(),
				new HeaderClassifierRule.Builder()
					.setHeaderMatch(new OpenBoxHeaderMatch.Builder().build())
					.setPriority(Priority.DEFAULT)
					.setOrder(1)
					.build()
		};
		
		IProcessingBlock[] lbBlocks = {
				new FromDevice("FromDevice-LB", "", true, true),
				new HeaderClassifier("HeaderClassifier-LB", ImmutableList.copyOf(lbRules), Priority.MEDIUM, false),
				new Alert("Alert-LB", "LOAD BALANCER ALERT"),
				new NetworkHeaderFieldsRewriter("NetworkHeaderFieldsRewriter-LB"),
				new ToDevice("ToDevice-LB", ""),
		};
		IConnector[] lbConnectors = {
				new Connector.Builder().setSourceBlock(lbBlocks[0]).setSourceOutputPort(0).setDestBlock(lbBlocks[1]).build(),
				new Connector.Builder().setSourceBlock(lbBlocks[1]).setSourceOutputPort(0).setDestBlock(lbBlocks[2]).build(),
				new Connector.Builder().setSourceBlock(lbBlocks[1]).setSourceOutputPort(1).setDestBlock(lbBlocks[4]).build(),
				new Connector.Builder().setSourceBlock(lbBlocks[2]).setSourceOutputPort(0).setDestBlock(lbBlocks[3]).build(),
				new Connector.Builder().setSourceBlock(lbBlocks[3]).setSourceOutputPort(0).setDestBlock(lbBlocks[4]).build(),
		};
		
		IProcessingGraph a = new ProcessingGraph.Builder()
			.setBlocks(ImmutableList.copyOf(firewallBlocks))
			.setConnectors(ImmutableList.copyOf(firewallConnectors))
			.setRoot(firewallBlocks[0])
			.build();
		
		System.out.println("Input graph A:");
		System.out.println(a);
		
		IProcessingGraph b = new ProcessingGraph.Builder()
			.setBlocks(ImmutableList.copyOf(lbBlocks))
			.setConnectors(ImmutableList.copyOf(lbConnectors))
			.setRoot(lbBlocks[0])
			.build();
		
		System.out.println("Input graph B:");
		System.out.println(b);
		
		IProcessingGraph merged = merge(a, b);
		
		System.out.println("Result graph:");
		System.out.println(merged);
		
		System.out.println();
		
		for (IProcessingBlock block : merged.getBlocks()) {
			if (block instanceof HeaderClassifier) {
				HeaderClassifier classifier = (HeaderClassifier)block;
				printRules(classifier);
			}
		}
		
		System.out.println();
		System.out.println("Total number of blocks: " + merged.getBlocks().size());
	}
	
	private static void printRules(HeaderClassifier classifier) {
		int i = 0;
		System.out.println("Rules for classifier " + classifier.getId() + ":");
		for (HeaderClassifierRule rule: classifier.getRules()) {
			System.out.println(String.format("%s --> [%d]", rule.getMatch(), i++));
		}
	}

	private static ApplicationAggregator _instance;
	
	private IApplicationRegistry registry;
	private Map<ILocationSpecifier, IProcessingGraph> aggregated;
	private Map<ILocationSpecifier, Map<String, Origin>> origins;
	
	private ApplicationAggregator() {
		this.registry = null;
		this.aggregated = new HashMap<>();
		this.origins = new HashMap<>();
	}
	
	public static IApplicationAggregator getInstance() {
		if (_instance == null) {
			synchronized (ApplicationAggregator.class) {
				if (_instance == null) {
					_instance = new ApplicationAggregator();
				}
			}
		}
		return _instance;
	}
	
	@Override
	public void setApplicationRegistry(IApplicationRegistry reg) {
		this.registry = reg;
	}
	
	@Override
	public void performAggregation() {
		ITopologyManager topology = TopologyManager.getInstance(); 
		for (InstanceLocationSpecifier loc : topology.getAllEndpoints()) {
			aggregateLocation(loc);
		}
	}

	private IProcessingGraph findGraphForLocation(BoxApplication app, ILocationSpecifier loc) {
		ITopologyManager topology = TopologyManager.getInstance();
		Collection<IStatement> statements = app.getStatements();
		for(IStatement stmt: statements) {
			ILocationSpecifier stmtLoc = stmt.getLocation();
			for (InstanceLocationSpecifier i: topology.getSubInstances(stmtLoc)) {
				if (i.equals(loc)) {
					return stmt.getProcessingGraph();
				}
			}
		}
		return null;
	}

	@Override
	public void aggregateLocation(ILocationSpecifier loc) {
		synchronized (this) {
			// Make sure we removed previous aggregated graph. 
			this.aggregated.remove(loc);
			// TODO: The following should be done for each OBI location specifier
			for (IApplicationType app_type : registry.getApplicationTypes()) {
				List<BoxApplication> variants = registry.getApplicationVariants(app_type);
				HashMap<BoxApplication, IProcessingGraph> plausibleVariants = new HashMap<>();
				Map<String, Origin> origins = new HashMap<>();
				Random rand = new Random();
				BoxApplication variant;
				IProcessingGraph graph;

				for (BoxApplication app: variants) {
					graph = findGraphForLocation(app, loc);
					if (graph != null) {
						plausibleVariants.put(app, graph);
					}
				}

				Object[] keys = plausibleVariants.keySet().toArray();
				if (keys.length == 0) {
					return;
				} else if (keys.length == 1) {
					variant = (BoxApplication)keys[0];
					graph = plausibleVariants.get(variant);
				} else {
					// TODO: get next pick if the same variant is chosen as the current.
					int pick = rand.nextInt(keys.length);
					variant = (BoxApplication)keys[pick];
					graph = plausibleVariants.get(variant);
				}
				graph.getBlocks()
						.stream()
						.filter(b -> b.getBlockType().equals("Alert"))
						.forEach(b -> origins.put(b.getId(), new Origin(variant, b, b.getId())));
				this.origins.put(loc, origins);

				IProcessingGraph prev = this.aggregated.get(loc);
				if (graph == null){
					continue;
				}
				if (prev != null){
					graph = merge(prev, graph);
				}
				this.aggregated.put(loc, graph);
			}
		}
	}

	@Override
	public IProcessingGraph getProcessingGraph(ILocationSpecifier loc) {
		IProcessingGraph graph = this.aggregated.get(loc);
		if (graph == null) {
			aggregateLocation(loc);
			graph = this.aggregated.get(loc);
		}
		return graph;
	}

	@Override
	public void invalidateProcessingGraph(ILocationSpecifier loc) {
		this.aggregated.remove(loc);
	}
	
	@Override
	public void handleAlert(org.moonlightcontroller.managers.models.messages.Alert message) {
		ILocationSpecifier loc = TopologyManager.getInstance().resolve(message.getOrigin_dpid());
		Map<String, Origin> origins = this.origins.get(loc);
		if (origins != null){
			for (AlertMessage alert : message.getMessages()) {
				Origin origin = origins.get(alert.getOrigin_block());
				if (origin != null){
					EventManager.getInstance().HandleAlert(
							origin.app.getName(), 
							new InstanceAlertArgs((InstanceLocationSpecifier)loc, message, origin.block));					
				}
			}
		}
	}
	
	private static class Origin {
		private BoxApplication app;
		private IProcessingBlock block;
		private String id;
		
		public Origin(BoxApplication app, IProcessingBlock block, String id) {
			this.app = app;
			this.block = block;
			this.id = id;
		}
	}
}
