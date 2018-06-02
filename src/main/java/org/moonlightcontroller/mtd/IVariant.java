package org.moonlightcontroller.mtd;


/**
 * Interface which represents a location in the network
 * It can be an Instance or a Segment
 *
 */
public interface IVariant {
	
	public interface Builder {
                public IVariant build();

                public IVariant setType(long type);

                public IVariant setVariant(long variant);
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
	ITypeIdentifier getVariant(long m);
}
