package org.openboxprotocol.protocol;

import java.util.LinkedList;
import java.util.List;

import org.moonlightcontroller.processing.IProcessingGraph;
import org.moonlightcontroller.topology.ILocationSpecifier;

public class Statement implements IStatement{

	private List<IProcessingGraph> graphs;
	private ILocationSpecifier location;
	
	@Override
	public ILocationSpecifier getLocation() {
		return location;
	}

	@Override
	public List<IProcessingGraph> getProcessingGraphs() {
		return graphs;
	}

	private Statement(Builder builder) {
		this.graphs = builder.graphs;
		this.location = builder.location;
	}
	
	public static class Builder implements IStatement.Builder {

		private List<IProcessingGraph> graphs;
		private ILocationSpecifier location;
		
		@Override
		public Builder setLocation(
				ILocationSpecifier locspec) {
			this.location = locspec;
			this.graphs = new LinkedList<>();
			return this;
		}

		@Override
		public Builder setProcessingGraph(IProcessingGraph graph) {
			this.graphs.add(graph);
			return this;
		}

		@Override
		public Builder removeProcessingGraph(IProcessingGraph graph) {
			this.graphs.remove(graph);
			return this;
		}

		@Override
		public IStatement build() {
			return new Statement(this);
		}
	}
}
