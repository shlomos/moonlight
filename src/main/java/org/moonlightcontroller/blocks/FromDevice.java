package org.moonlightcontroller.blocks;

import java.util.Map;

import org.moonlightcontroller.processing.BlockClass;
import org.moonlightcontroller.processing.ProcessingBlock;
import org.moonlightcontroller.processing.NetworkStack;

public class FromDevice extends ProcessingBlock {

	private String devname;
	private boolean sniffer;
	private boolean promisc;
	private NetworkStack network_stack;

	public FromDevice(String id, String devname) {
		super(id);
		this.devname = devname;
		this.network_stack = NetworkStack.KERNEL;
		this.addPort();
	}

	public FromDevice(String id, String devname, NetworkStack net_stack) {
		super(id);
		this.devname = devname;
		this.network_stack = net_stack;
		fix_devname();
		this.addPort();
	}

	public FromDevice(String id, String devname, boolean sniffer, boolean promisc) {
		super(id);
		this.devname = devname;
		this.sniffer = sniffer;
		this.promisc = promisc;
		this.network_stack = NetworkStack.KERNEL;
		this.addPort();
	}

	public FromDevice(String id, String devname, boolean sniffer, boolean promisc, NetworkStack net_stack) throws IllegalArgumentException {
		super(id);
		this.devname = devname;
		this.network_stack = net_stack;
		this.sniffer = sniffer;
		if (this.sniffer && this.network_stack != NetworkStack.KERNEL) {
			throw new IllegalArgumentException("Cannot use sniffer with non-kernel device.");
		}
		this.promisc = promisc;
		fix_devname();
		this.addPort();
	}

	public void fix_devname() {
		if (this.network_stack == NetworkStack.NETMAP) {
			this.devname = "netmap:" + this.devname;
		}
	}

	@Override
	public String getBlockType() {
		switch(this.network_stack) {
			case KERNEL:
				return "FromDevice";
			case NETMAP:
				return "FromNetmapDevice";
			case DPDK:
				return "FromDPDKDevice";
			default:
				System.out.println("Unknown network stack: " + this.network_stack);
				return "FromDevice";
		}
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
		if (this.network_stack == NetworkStack.KERNEL) {
			config.put("sniffer", this.sniffer);
		}
		config.put("promisc", this.promisc);
	}

	@Override
	protected ProcessingBlock spawn(String id) {
		return new FromDevice(id, this.devname, this.sniffer, this.promisc, this.network_stack);
	}
}
