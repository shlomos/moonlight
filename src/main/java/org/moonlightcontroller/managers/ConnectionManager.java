package org.moonlightcontroller.managers;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.moonlightcontroller.aggregator.ApplicationAggregator;
import org.moonlightcontroller.aggregator.IApplicationAggregator;
import org.moonlightcontroller.aggregator.Origin;
import org.moonlightcontroller.bal.BoxApplication;
import org.moonlightcontroller.blocks.CustomBlock;
import org.moonlightcontroller.blocks.ObiType;
import org.moonlightcontroller.events.EventManager;
import org.moonlightcontroller.events.InstanceCastleArgs;
import org.moonlightcontroller.managers.models.ConnectionInstance;
import org.moonlightcontroller.managers.models.IRequestSender;
import org.moonlightcontroller.managers.models.NullRequestSender;
import org.moonlightcontroller.managers.models.messages.AddCustomModuleRequest;
import org.moonlightcontroller.managers.models.messages.AddCustomModuleResponse;
import org.moonlightcontroller.managers.models.messages.Alert;
import org.moonlightcontroller.managers.models.messages.Castle;
import org.moonlightcontroller.managers.models.messages.Error;
import org.moonlightcontroller.managers.models.messages.Hello;
import org.moonlightcontroller.managers.models.messages.IMessage;
import org.moonlightcontroller.managers.models.messages.KeepAlive;
import org.moonlightcontroller.managers.models.messages.ListCapabilitiesResponse;
import org.moonlightcontroller.managers.models.messages.SetProcessingGraphRequest;
import org.moonlightcontroller.managers.models.messages.SetProcessingGraphResponse;
import org.moonlightcontroller.processing.IConnector;
import org.moonlightcontroller.processing.IProcessingBlock;
import org.moonlightcontroller.processing.IProcessingGraph;
import org.moonlightcontroller.processing.JsonBlock;
import org.moonlightcontroller.processing.JsonConnector;
import org.moonlightcontroller.southbound.server.SouthboundApi;
import org.moonlightcontroller.topology.ILocationSpecifier;
import org.moonlightcontroller.topology.InstanceLocationSpecifier;
import org.moonlightcontroller.topology.TopologyManager;
import org.openboxprotocol.exceptions.InstanceNotAvailableException;


/**
 * Connection Manager is the class which holds connections to all active OBIs
 * It can send them messages and receives messages from them.
 */
public class ConnectionManager implements ISouthboundClient {

	private final static Logger LOG = Logger.getLogger(ConnectionManager.class.getName());
	private final static long CASTLE_GRACE_TIME = 5000;

	Map<InstanceLocationSpecifier, Map<String, Long>> castle_timers;
	Map<InstanceLocationSpecifier, ConnectionInstance> instancesMapping;
	Map<Integer, IMessage> messagesMapping;
	Map<Integer, IRequestSender> requestSendersMapping;

	private static ConnectionManager instance;

	private ConnectionManager () {
		castle_timers = new ConcurrentHashMap<>();
		instancesMapping = new ConcurrentHashMap<>();
		messagesMapping = new ConcurrentHashMap<>();
		requestSendersMapping = new ConcurrentHashMap<>();
	}

	public synchronized static ConnectionManager getInstance() {
		if (instance == null) {
			instance = new ConnectionManager();
		}

		return instance;
	}

	public Response handleKeepaliveRequest(KeepAlive message) {
		messagesMapping.put(message.getXid(), message);
		return handleKeepaliveRequest(getInstanceLocationSpecifier(message.getDpid()), message.getXid());
	}

	public Response handleCastleRequest(Castle message) {
		messagesMapping.put(message.getXid(), message);
		long dpid = message.getOrigin_dpid();
		IApplicationAggregator aggr = ApplicationAggregator.getInstance();
		String block = message.getBlock();
		InstanceLocationSpecifier loc = getInstanceLocationSpecifier(dpid);
		/* Handle multiple castles hack */
		boolean ignore = false;
		if (!castle_timers.containsKey(loc)) {
			castle_timers.put(loc, new ConcurrentHashMap<>());
		} else {
			if (castle_timers.get(loc).containsKey(block)) {
				ignore = castle_timers.get(loc).get(block) < System.currentTimeMillis() - CASTLE_GRACE_TIME;
			}
		}
		if (ignore) {
			return okResponse();
		}
		castle_timers.get(loc).put(block, System.currentTimeMillis());
		/*
		 * TODO: Now we can be specific to this app, and the block. We might only randomize IT.
		 * BoxApplication app = aggr.getOrigin(loc, block).getApp();
		 */
		Origin origin = aggr.getOrigin(loc, block);
		if (origin!=null) {
			System.out.println("casteling message with good origin!");
			//aggr.printOrigins();
			EventManager.getInstance().HandleCastle(
					origin.getApp().getName(),
					new InstanceCastleArgs((InstanceLocationSpecifier)loc, message, origin.getBlock()));
			aggr.invalidateProcessingGraph(loc);
			return sendSetProcessingGraphRequest(loc);
		}
		System.out.println("casteling message with bad origin!");
		return okResponse();
	}

	private InstanceLocationSpecifier getInstanceLocationSpecifier(long dpid) {
			return new InstanceLocationSpecifier(dpid);
	}

	private Response handleKeepaliveRequest(InstanceLocationSpecifier instanceLocationSpecifier, int xid) {
		ConnectionInstance data = instancesMapping.get(instanceLocationSpecifier);
		if (data != null) {
			data.updateKeepAlive();
			return okResponse();
		}

		return internalErrorResponse();
	}

	public List<InstanceLocationSpecifier> getAliveInstances(ILocationSpecifier loc) {
		return TopologyManager.getInstance().getSubInstances(loc).stream()
				.filter(item -> isAlive(item)).collect(Collectors.toList());
	}

	public List<InstanceLocationSpecifier> getAliveInstances() {
		return getAliveInstances(TopologyManager.getInstance().getSegment());
	}

	private boolean isAlive(InstanceLocationSpecifier item) {
		ConnectionInstance data = instancesMapping.get(item);
		return data.getKeepAliveDate()
				.isAfter(LocalDateTime.now().minusSeconds(data.getKeepAliveInterval()));
	}

	public Response sendSetProcessingGraphRequest(InstanceLocationSpecifier loc) {
		try {
			IProcessingGraph processingGraph = ApplicationAggregator.getInstance().getProcessingGraph(loc);
			List<JsonBlock> blocks = new ArrayList<>();
			List<JsonConnector> connectors = new ArrayList<>();
			if (processingGraph != null){
				List<IProcessingBlock> custom = processingGraph.getBlocks().stream()
						.filter(b -> b instanceof CustomBlock)
						.collect(Collectors.toList());
				if (custom.size() > 0){
					this.sendCustomBlocks(custom, loc, ObiType.ClickObi);
				}
				blocks = translateBlocks(processingGraph.getBlocks());
				connectors = translateConnectors(processingGraph.getConnectors());
			}

			ConnectionInstance inst = instancesMapping.get(loc);
			if (inst != null) {
				SetProcessingGraphRequest processMessage = new SetProcessingGraphRequest(0, instancesMapping.get(loc).getDpid(), null, blocks, connectors);
				sendMessage(loc, processMessage, new NullRequestSender());
				return okResponse();
			} else {
				LOG.warning("No OBI at location: " + loc);
			}
			return null;

		} catch (Exception e) {
			LOG.warning("Error occured while sending SetProcessingGraph request" + e.toString());
			e.printStackTrace();
			return internalErrorResponse();
		}
	}

	public Response handleHelloRequest(String ip, Hello message) {
		int xid = message.getXid();
		messagesMapping.put(xid, message);
		long dpid = message.getDpid();
		InstanceLocationSpecifier key = getInstanceLocationSpecifier(dpid);

		ConnectionInstance value = (new ConnectionInstance.Builder())
				.setIp(ip)
				.setDpid(dpid)
				.setVersion(message.getVersion())
				.setCapabilities(message.getCapabilities())
				.build();
		instancesMapping.put(key, value);
		return sendSetProcessingGraphRequest(key);
	}

	private void sendCustomBlocks(
			List<IProcessingBlock> custom, 
			InstanceLocationSpecifier loc, 
			ObiType obitype) throws InstanceNotAvailableException, IOException {
		
		if (obitype == null){
			obitype = ObiType.ClickObi;
		}
		for (IProcessingBlock block : custom) {
			CustomBlock customblock = (CustomBlock)block;
			AddCustomModuleRequest req = new AddCustomModuleRequest(
					0,
					customblock.getModuleName(), 
					customblock.getModuleContent(), 
					customblock.getTranslationObject(obitype));
			this.sendMessage(loc, req, new NullRequestSender());
		}
	}

	private List<JsonConnector> translateConnectors(List<IConnector> connectors) {
		return connectors.stream().map(connector -> translateConnector(connector)).collect(Collectors.toList());
	}

	private JsonConnector translateConnector(IConnector connector) {
		return new JsonConnector(connector.getSourceBlockId(), 
				connector.getSourceOutputPort(), 
				connector.getDestinatinBlockId(),
				connector.getDestInputPort());
	}


	private List<JsonBlock> translateBlocks(List<IProcessingBlock> blocks) {
		return blocks.stream().map(block -> translateBlock(block)).collect(Collectors.toList());
	}

	private JsonBlock translateBlock(IProcessingBlock block) {
		return new JsonBlock(block.getBlockType(), block.getId(), block.getConfiguration());
	}

	public Response handleSetProcessingGraphResponse(SetProcessingGraphResponse message) {
		IMessage originMessage = messagesMapping.get(message.getXid());

		if (originMessage == null) {
			return internalErrorResponse();
		}

		if (originMessage instanceof SetProcessingGraphRequest) {
			long dpid = ((SetProcessingGraphRequest)originMessage).getDpid();
			InstanceLocationSpecifier loc = getInstanceLocationSpecifier(dpid);
			ConnectionInstance instance = instancesMapping.get(loc);
			instance.setProcessingGraphConfiged(true);
			return okResponse();
		}

		return badRequestResponse();
	}

	private Response badRequestResponse() {
		return Response.status(Status.BAD_REQUEST).build();
	}
	
	private Response internalErrorResponse() {
		return Response.status(Status.INTERNAL_SERVER_ERROR).build();
	}

	private Response okResponse() {
		return Response.status(Status.OK).build();
	}

	@Override
	public void sendMessage(ILocationSpecifier loc, IMessage message, IRequestSender requestSender) throws InstanceNotAvailableException {
		ConnectionInstance connectionInstance = instancesMapping.get(loc);
		if (connectionInstance == null) {
			throw new InstanceNotAvailableException();
		}
		int xid = XidGenerator.generateXid();
		message.setXid(xid);
		messagesMapping.put(xid, message);
		connectionInstance.sendRequest(message, requestSender);
		requestSendersMapping.put(xid, requestSender);

	}

	public Response handleResponse(IMessage message) {
		IRequestSender iRequestSender = requestSendersMapping.get(message.getXid());
		iRequestSender.onSuccess(message);
		return okResponse();
	}

	public Response handleErrorMessage(Error message) {
		IRequestSender iRequestSender = requestSendersMapping.get(message.getXid());
		if (iRequestSender != null){
			iRequestSender.onFailure(message);			
		}
		return okResponse();
	}

	public Response handleListCapabilitiesResponse(ListCapabilitiesResponse message) {
		// see read response + update ListCapabilities in connectionInstansce
		// TODO: Dana this code is not thread safe - commenting out
		/*
		try {
			int xid = message.getXid();
			ConnectionInstance connectionInstance = instancesMapping.get(xid);
			connectionInstance.setCapabilities(message.getCapabilities());
		} catch (NullPointerException e) {
			return badRequestResponse();
		}
		*/
		
		return handleResponse(message);
	}



	public Response handleAlert(Alert message) {
		ApplicationAggregator.getInstance().handleAlert(message);
		return okResponse();
	}

	public Response handleAddCustomModuleResponse(AddCustomModuleResponse message) {
		IRequestSender iRequestSender = requestSendersMapping.get(message.getXid());
		iRequestSender.onSuccess(message);
		return okResponse();
	}
}
