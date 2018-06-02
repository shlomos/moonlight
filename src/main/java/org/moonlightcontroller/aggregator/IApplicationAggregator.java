package org.moonlightcontroller.aggregator;

import java.util.List;

import org.moonlightcontroller.bal.BoxApplication;
import org.moonlightcontroller.managers.models.messages.Alert;
import org.moonlightcontroller.processing.IProcessingGraph;
import org.moonlightcontroller.topology.ILocationSpecifier;

/**
 * Interface for the application aggregator
 *
 */
public interface IApplicationAggregator {

	/**
	 * Performs aggregation for all applications in all locations
	 */
	void performAggregation();

	/**
	 * Performs aggregation for a location
	 */
	public void aggregateLocation(InstanceLocationSpecifier loc);

	/**
	 * Invalidate the graph for given location, forcing it to be reaggregated with new variants. 
	 */
	public void invalidateProcessingGraph(ILocationSpecifier loc);
	
	/**
	 * Set a registery object for apps
	 */
	public void setApplicationRegistry(IApplicationRegistry reg);

	/**
	 * after performAggregation is called this method returns a processing graph for each given location
	 * @param loc
	 * @return
	 */
	IProcessingGraph getProcessingGraph(ILocationSpecifier loc);
	
	/**
	 * Handles alerts coming from the control plane.
	 * It will know to which application the alert should be routed
	 * @param message
	 */
	void handleAlert(Alert message);
}
