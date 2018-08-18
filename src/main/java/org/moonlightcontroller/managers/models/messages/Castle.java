package org.moonlightcontroller.managers.models.messages;

public class Castle extends Message {

	private long origin_dpid;
	private String block;
	
	// Default constructor to support Jersy
	public Castle() {}
	
	public Castle(int xid, long dpid, String block) {
		super(xid);
		this.origin_dpid = dpid;
		this.block = block;
	}
 	
	public long getOrigin_dpid() {
		return origin_dpid;
	}

	public String getBlock() {
		return block;
	}
}
