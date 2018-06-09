package org.moonlightcontroller.mtd;

public class ApplicationType implements IApplicationType {

	long type;
	
	/**
	 * @return the id of the location
	 */
	@Override
	public long getType() {
		return this.type;
	}
	
	/**
	 * Equality check
	 * @param m the type to find
	 * @return true if this.getType() == m
	 */
	@Override
	public boolean isMatch(long t) {
		return this.getType() == t;
	}
}