package org.moonlightcontroller.mtd;


/**
 * Interface which represents a location in the network
 * It can be an Instance or a Segment
 *
 */
public interface IApplicationType {
	
	public interface Builder {
		public IApplicationType build();
	}
	
	/**
	 * @return the id of the location
	 */
	long getType();
	
	/**
	 * support searching the topology tree
	 * @param m the type to find
	 * @return true if this.getType() == m
	 */
	boolean isMatch(long m);
	
	/**
	 * Finds the child of this ILocationSpecifier which has id equals to m
	 * @param m
	 * @return
	 */
	IApplicationType getVariant(long m);
}
