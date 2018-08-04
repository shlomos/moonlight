package org.moonlightcontroller.mtd;
import java.util.Objects;


public class ApplicationType implements IApplicationType {

	long type;

	public ApplicationType(long type) {
		this.type = type;
	}
	
	/**
	 * @return the id of the location
	 */
	@Override
	public long getType() {
		return this.type;
	}

	@Override
	public boolean equals(Object obj) {
		ApplicationType other = (ApplicationType)obj;
		return other.type == this.type;
	}

	@Override
    public int hashCode() {
        return Objects.hash(type);
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

	@Override
	public String toString() {
		return ""+this.type;
	}
}