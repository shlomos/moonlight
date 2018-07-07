package org.moonlightcontroller.blocks;

import java.util.Map;

import org.moonlightcontroller.processing.BlockClass;
import org.moonlightcontroller.aggregator.UUIDGenerator;
import org.moonlightcontroller.exceptions.MergeException;
import org.moonlightcontroller.processing.ProcessingBlock;

public class SetTimestamp extends ProcessingBlock implements IStaticProcessingBlock {

	private String devname;
	private boolean sniffer;
	private boolean promisc;
	
	public SetTimestamp(String id) {
		super(id);
	}

	@Override
	public BlockClass getBlockClass() {
		return BlockClass.BLOCK_CLASS_STATIC;
	}

	@Override
	protected void putConfiguration(Map<String, Object> config) {
	}
	
	@Override
	protected ProcessingBlock spawn(String id) {
		return new SetTimestamp(id);
	}
	
	@Override
	public boolean canMergeWith(IStaticProcessingBlock other) {
		if (!(other instanceof SetTimestamp))
			return false;
		return true;
	}

	@Override
	public IStaticProcessingBlock mergeWith(IStaticProcessingBlock other) throws MergeException {
		if (other instanceof SetTimestamp) {
			SetTimestamp o = (SetTimestamp)other;
			return new SetTimestamp("MERGED##" + this.getId() + "##" + other.getId() + "##" + UUIDGenerator.getSystemInstance().getUUID().toString());
		} else {
			throw new MergeException("Cannot merge statics of different type");
		}
	}
}
