package org.moonlightcontroller.blocks;

import java.util.Map;

import org.moonlightcontroller.processing.BlockClass;
import org.moonlightcontroller.processing.ProcessingBlock;

public class ToDevice extends ProcessingBlock {

	private String devname;
	private boolean netmap;

	public ToDevice(String id, String devname){
		super(id);
		this.devname = devname;
		this.netmap = false;
	}

	public ToDevice(String id, String devname, boolean netmap){
		super(id);
		this.devname = devname;
		this.netmap = netmap;
	}

	public String getDevice() {
		return this.devname;
	}

	@Override
	public String getBlockType() {
		return this.netmap ? "ToDevice" : "ToNetmapDevice"; 
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
		return new ToDevice(id, this.getDevice(), this.netmap);
	}
}
