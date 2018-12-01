package org.moonlightcontroller.blocks;

import java.util.Map;

import org.moonlightcontroller.processing.BlockClass;
import org.moonlightcontroller.processing.ProcessingBlock;

public class ToHost extends ProcessingBlock {

	private String devname;

	public ToHost(String id, String devname){
		super(id);
		this.devname = devname;
	}

	public String getDevice() {
		return this.devname;
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
		return new ToHost(id, this.getDevice());
	}
}
