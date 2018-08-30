package org.moonlightcontroller.events;

import org.moonlightcontroller.managers.models.messages.Castle;
import org.moonlightcontroller.processing.IProcessingBlock;
import org.moonlightcontroller.topology.InstanceLocationSpecifier;

/**
 * Arguments for the castle event
 */
public class InstanceCastleArgs {

	private InstanceLocationSpecifier instance;
	private Castle castle;
	private IProcessingBlock block;

	public InstanceCastleArgs(InstanceLocationSpecifier loc, Castle castle, IProcessingBlock bl) {
		this.instance = loc;
		this.castle = castle;
		this.block = bl;
	}

	public InstanceLocationSpecifier getInstance() {
		return instance;
	}

	public Castle getCastle() {
		return castle;
	}

	public IProcessingBlock getBlock() {
		return block;
	}
}
