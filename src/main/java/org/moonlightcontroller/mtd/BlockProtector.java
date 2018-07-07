package org.moonlightcontroller.mtd;

import java.util.List;
import java.util.ArrayList;

import com.google.common.collect.ImmutableList;

import org.moonlightcontroller.blocks.SetTimestamp;
import org.moonlightcontroller.blocks.UtilizationMonitor;
import org.moonlightcontroller.processing.Connector;
import org.moonlightcontroller.processing.IConnector;
import org.moonlightcontroller.processing.IProcessingBlock;
import org.moonlightcontroller.processing.IProcessingGraph;
import org.moonlightcontroller.processing.ProcessingGraph;

public class BlockProtector {

	private static BlockProtector _instance;

	private BlockProtector() {
	}

	public static BlockProtector getInstance() {
		if (_instance == null) {
			synchronized (BlockProtector.class) {
				if (_instance == null) {
					_instance = new BlockProtector();
				}
			}
		}
		return _instance;
	}

	public IProcessingGraph getProtectedSubGraph(IProcessingBlock block, int num_outputs, int window, double proc_threshold) {
		List<IConnector> connectors = new ArrayList<>();
		List<IProcessingBlock> blocks = new ArrayList<>();
		SetTimestamp entry_stamp = new SetTimestamp("SetTimestamp_" + block.getId());
		UtilizationMonitor guard = new UtilizationMonitor("Guard_" + block.getId(), window, proc_threshold);
		// TODO: a discard block can be wired to the guard if heavy packets should be dropped 
		// instead of alerting the OBC formtd casteling.
		blocks.addAll(ImmutableList.of(entry_stamp, block, guard));
		connectors.addAll(ImmutableList.of(
			new Connector.Builder().setSourceBlock(entry_stamp).setSourceOutputPort(0).setDestBlock(block).build()
		));
		for (int i = 0; i < num_outputs; i++) {
			connectors.add(new Connector.Builder().setSourceBlock(block).setSourceOutputPort(i).setDestBlock(guard).setDestInputPort(i).build());
		}
		return new ProcessingGraph.Builder().setBlocks(blocks).setConnectors(connectors).setRoot(entry_stamp).build();
	}
}