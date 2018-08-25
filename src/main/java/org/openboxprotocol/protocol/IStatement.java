package org.openboxprotocol.protocol;

import java.util.List;

import org.moonlightcontroller.processing.IProcessingGraph;
import org.moonlightcontroller.topology.ILocationSpecifier;

public interface IStatement {

	public ILocationSpecifier getLocation();
	
	public List<IProcessingGraph> getProcessingGraphs();
	
	public interface Builder {
		public Builder setLocation(ILocationSpecifier locspec);
		
		public Builder setProcessingGraph(IProcessingGraph graph);

		public Builder removeProcessingGraph(IProcessingGraph graph);
		
		public IStatement build();
	}
}
