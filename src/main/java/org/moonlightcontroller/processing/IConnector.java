package org.moonlightcontroller.processing;


public interface IConnector {
	public IProcessingBlock getSourceBlock();
	public IProcessingBlock getDestBlock();
	public int getSourceOutputPort();
	public int getDestInputPort();

	// Legacy
	String getSourceBlockId();
	String getDestinatinBlockId();
	Builder createBuilder();
	
	public interface Builder {
		
		public Builder setSourceBlock(IProcessingBlock source);
		public Builder setSourceOutputPort(int port);
		public Builder setDestBlock(IProcessingBlock dest);
		public Builder setDestInputPort(int port);
		public IConnector build();
	}
}
