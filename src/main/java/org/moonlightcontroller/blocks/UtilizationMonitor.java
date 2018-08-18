package org.moonlightcontroller.blocks;

import org.moonlightcontroller.processing.IProcessingBlock;
import org.moonlightcontroller.processing.ProcessingBlock;
import org.moonlightcontroller.exceptions.MergeException;
import org.moonlightcontroller.processing.BlockClass;
import java.util.Map;

public class UtilizationMonitor extends ProcessingBlock implements IStaticProcessingBlock {
	private int window;
	private double proc_threshold;
	IProcessingBlock protected_block;

	public UtilizationMonitor(String id) {
		super(id);
	}
	
	public UtilizationMonitor(String id, IProcessingBlock protected_block, int window, double thresh) {
		super(id);
		this.window = window;
		this.proc_threshold = thresh;
		this.protected_block = protected_block;
	}

	public int getWindow() {
		return window;
	}

	public double getProcessingThreshold() {
		return proc_threshold;
	}

	@Override
	public boolean canMergeWith(IStaticProcessingBlock other) {
		return false;
	}

	@Override
	public IStaticProcessingBlock mergeWith(IStaticProcessingBlock other) throws MergeException {
		return null;
	}

	@Override
	public BlockClass getBlockClass() {
		return BlockClass.BLOCK_CLASS_CLASSIFIER;
	}

	@Override
	protected void putConfiguration(Map<String, Object> config) {
		config.put("window", this.window);
		config.put("proc_threshold", this.proc_threshold);
		config.put("block", this.protected_block.getId());
	}

	@Override
	protected ProcessingBlock spawn(String id) {
		return new UtilizationMonitor(id, this.protected_block, this.window, this.proc_threshold);
	}

}