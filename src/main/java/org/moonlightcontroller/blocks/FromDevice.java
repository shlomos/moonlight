package org.moonlightcontroller.blocks;

import java.util.Map;

import org.moonlightcontroller.processing.BlockClass;
import org.moonlightcontroller.processing.ProcessingBlock;

public class FromDevice extends ProcessingBlock {

	private String devname;
	private boolean sniffer;
	private boolean promisc;
	private boolean netmap;
	
	public FromDevice(String id, String devname) {
		super(id);
		this.devname = devname;
		this.netmap = false;
		this.addPort();
	}

	public FromDevice(String id, String devname, Boolean netmap) {
		super(id);
		this.devname = devname;
		this.netmap = netmap;
		fix_devname();
		this.addPort();
	}

	public FromDevice(String id, String devname, boolean sniffer, boolean promisc) {
		super(id);
		this.devname = devname;
		this.sniffer = sniffer;
		this.promisc = promisc;
		this.netmap = false;
		this.addPort();
	}

	public FromDevice(String id, String devname, boolean sniffer, boolean promisc, boolean netmap) throws IllegalArgumentException {
		super(id);
		this.devname = devname;
		this.netmap = netmap;
		this.sniffer = sniffer;
		if (this.sniffer && this.netmap) {
			throw new IllegalArgumentException("Cannot use sniffer with netmap device.");
		}
		this.promisc = promisc;
		fix_devname();
		this.addPort();
	}

	public void fix_devname() {
		if (this.netmap) {
			this.devname = "netmap:" + this.devname;
		}
	}
	
	@Override
	public String getBlockType() {
		return this.netmap ? "FromNetmapDevice" : "FromDevice"; 
	}

	public String getDevname() {
		return devname;
	}

	public boolean isSniffer() {
		return sniffer;
	}

	public boolean isPromisc() {
		return promisc;
	}	

	@Override
	public BlockClass getBlockClass() {
		return BlockClass.BLOCK_CLASS_TERMINAL;
	}

	@Override
	protected void putConfiguration(Map<String, Object> config) {
		config.put("devname", this.devname);
		if (!this.netmap) {
			config.put("sniffer", this.sniffer);
		}
		config.put("promisc", this.promisc);
	}
	
	@Override
	protected ProcessingBlock spawn(String id) {
		return new FromDevice(id, this.devname, this.sniffer, this.promisc, this.netmap);
	}
}
