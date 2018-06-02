package org.moonlightcontroller.mtd;

public class ApplicationType implements IApplicationType {

	long type;
	
	/**
	 * @return the id of the location
	 */
	long getType() {
		return this.type;
	}
	
	/**
	 * Equality check
	 * @param m the type to find
	 * @return true if this.getType() == m
	 */
	boolean isMatch(long m) {
		return this.getType() == m;
	}