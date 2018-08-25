package org.moonlightcontroller.bal;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.text.Segment;

import org.moonlightcontroller.events.IAlertListener;
import org.moonlightcontroller.events.IHandleClient;
import org.moonlightcontroller.events.IInstanceDownListener;
import org.moonlightcontroller.events.IInstanceUpListener;
import org.moonlightcontroller.processing.IProcessingGraph;
import org.moonlightcontroller.topology.IApplicationTopology;
import org.moonlightcontroller.topology.ILocationSpecifier;
import org.moonlightcontroller.topology.InstanceLocationSpecifier;
import org.moonlightcontroller.topology.TopologyManager;
import org.moonlightcontroller.mtd.IApplicationType;
import org.openboxprotocol.protocol.IStatement;
import org.openboxprotocol.protocol.Priority;

/**
 * Abstract class for all OpenBox Applications to inherit.
 * It supplies the minimal functionality for inheriting classes to create OpenBox Applications.
 * OpenBox Applications must inherit this class in order to be able to register with the registry. 
 */
public abstract class BoxApplication {
	
	protected String name;
	protected IApplicationType type;
	
	private Priority priority;
	private Map<ILocationSpecifier, IStatement> statements;
	private IAlertListener alertListener;
	private IInstanceDownListener instanceDownListener;
	private IInstanceUpListener instanceUpListener;
	
	public BoxApplication(String name) {
		this(name, Priority.MEDIUM);
	}
	
	public BoxApplication(String name, Priority priority) {
		this.name = name;
		this.priority = priority;
		this.statements = new HashMap<>();
	}
	
	/**
	 * @return the name of the application
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @return the type of the application
	 */
	public IApplicationType getType() {
		return this.type;
	}

	public void setType(IApplicationType type) {
		this.type = type;
	}
	
	/**
	 * @return the priority of the application
	 */
	public Priority getPriority() {
		return this.priority;
	}
			
	/**
	 * @return the statements of the application
	 */
	public Collection<IStatement> getStatements() {
		return this.statements.values();
	}
	
	/**
	 * Fetches the 'lowest' processing graph that exists for the given location
	 * E.g., if 'loc' is an OBI under segment s1.1 which is in segment s1, and there is no graph
	 * for the OBI location specifier, but there are graphs for s1.1 and for s1, then this method
	 * should return the graph for s1.1
	 */
	public List<IProcessingGraph> getProcessingGraphs(ILocationSpecifier loc) {
		ILocationSpecifier lowest = null;
		for (ILocationSpecifier si: statements.keySet()) {
			if (si instanceof InstanceLocationSpecifier && si.equals(loc)) {
				lowest = si;
				break;
			}
			if (si.findChild(loc.getId()) != null) {
				if (lowest.findChild(si.getId()) != null) {
					lowest = si;
				}
			}
		}
		if (lowest != null) {
			return statements.get(lowest).getProcessingGraphs();
		} else {
			return null;
		}
	}
	
	/**
	 * A handler for the Application Started event. 
	 * Whenever the application starts, the controller will call this method.
	 * Inheriting classes can override this, to get a notification on application starting.
	 * @param top The topology of the network
	 * @param handles A client for accessing read/write handles
	 */
	public void handleAppStart(IApplicationTopology top, IHandleClient handles) {
	}
	
	/**
	 * A handler for the Application Stop event. 
	 * Whenever the application stops, the controller will call this method.
	 * Inheriting classes can override this, to get a notification on application stopping.
	 */
	public void handleAppStop(){
	}
	
	/**
	 * A handler for the Application error event. 
	 * Whenever the application encounters an error, the controller will call this method.
	 * Inheriting classes can override this, to get a notification on application error.
	 */
	public void handleError(){
	}
	
	
	public IAlertListener getAlertListener(){
		return this.alertListener;
	}
	
	public IInstanceDownListener getInstanceDownListener(){
		return this.instanceDownListener;
	}

	public IInstanceUpListener getInstanceUpListener(){
		return this.instanceUpListener;
	}

	protected void setStatements(Collection<IStatement> statements) {
		for (IStatement st: statements) {
			this.statements.put(st.getLocation(), st);
		}
	}

	protected void setAlertListener(IAlertListener al){
		this.alertListener = al;
	}
	
	protected void setInstanceDowntListener(IInstanceDownListener dl){
		this.instanceDownListener = dl;
	}
	
	protected void setInstanceUpListener(IInstanceUpListener ul){
		this.instanceUpListener = ul;
	}
}
