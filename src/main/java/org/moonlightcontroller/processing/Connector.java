package org.moonlightcontroller.processing;

public class Connector implements IConnector {

	private IProcessingBlock destinationBlock;
	private IProcessingBlock sourceBlock;
	private int sourcePort;
	private int destPort;
	
	private Connector() {
	}

	@Override
	public String getSourceBlockId() {
		return this.sourceBlock.getId();
	}

	@Override
	public int getSourceOutputPort() {
		return this.sourcePort;
	}

	@Override
	public int getDestInputPort() {
		return this.destPort;
	}

	@Override
	public String getDestinatinBlockId() {;
		return this.destinationBlock.getId();
	}

	@Override
	public IProcessingBlock getSourceBlock() {
		return this.sourceBlock;
	}

	@Override
	public IProcessingBlock getDestBlock() {
		return this.destinationBlock;
	}

	@Override
	public Builder createBuilder() {
		return new Builder(this);
	}
	
	@Override
	public Connector clone() {
		Connector other = new Connector();
		other.sourceBlock = this.sourceBlock;
		other.sourcePort = this.sourcePort;
		other.destPort = this.destPort;
		other.destinationBlock = this.destinationBlock;
		return other;
	}
	
	public static class Builder implements IConnector.Builder {

		private Connector conn;

		public Builder(Connector conn){
			this.conn = conn.clone();
		}
		
		public Builder(){
			this.conn = new Connector();
		}
		
		@Override
		public IConnector build() {
			return this.conn;
		}

		@Override
		public org.moonlightcontroller.processing.IConnector.Builder setSourceBlock(IProcessingBlock source) {
			this.conn.sourceBlock = source;
			return this;
		}

		@Override
		public org.moonlightcontroller.processing.IConnector.Builder setSourceOutputPort(int port) {
			this.conn.sourcePort = port;
			return this;
		}

		@Override
		public org.moonlightcontroller.processing.IConnector.Builder setDestInputPort(int port) {
			this.conn.destPort = port;
			return this;
		}

		@Override
		public org.moonlightcontroller.processing.IConnector.Builder setDestBlock(IProcessingBlock dest) {
			this.conn.destinationBlock = dest;
			return this;
		}
	}
}

