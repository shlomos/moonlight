package org.moonlightcontroller.blocks;

import java.util.Map;

import org.moonlightcontroller.processing.BlockClass;
import org.moonlightcontroller.processing.ProcessingBlock;
import org.moonlightcontroller.processing.NetworkStack;

public class ToDevice extends ProcessingBlock {

	private String devname;
	private NetworkStack network_stack;

	public ToDevice(String id, String devname){
		super(id);
		this.devname = devname;
		this.network_stack = NetworkStack.KERNEL;
	}

	public ToDevice(String id, String devname, NetworkStack net_stack){
		super(id);
		this.devname = devname;
		this.network_stack = net_stack;
		fix_devname();
	}

	public String getDevice() {
		return this.devname;
	}

	@Override
	public String getBlockType() {
		switch(this.network_stack) {
			case KERNEL:
				return "ToDevice";
			case NETMAP:
				return "ToNetmapDevice";
			case DPDK:
				return "ToDPDKDevice";
			default:
				System.out.println("Unknown network stack: " + this.network_stack);
				return "ToDevice";
		}
	}

	public void fix_devname() {
		if (this.network_stack == NetworkStack.NETMAP) {
			this.devname = "netmap:" + this.devname;
		}
	}

	@Override
	public BlockClass getBlockClass() {
		return BlockClass.BLOCK_CLASS_TERMINAL;
	}

	@Override
	protected void putConfiguration(Map<String, Object> config) {
		config.put("devname", devname);
	}

	@Override
	protected ProcessingBlock spawn(String id) {
		return new ToDevice(id, this.getDevice(), this.network_stack);
	}
}
