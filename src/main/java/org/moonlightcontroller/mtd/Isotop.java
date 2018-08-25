package org.moonlightcontroller.mtd;
import java.util.Objects;

import org.moonlightcontroller.bal.BoxApplication;
import org.moonlightcontroller.processing.IProcessingGraph;


public class Isotop {

    public BoxApplication variant;
    public IProcessingGraph subVariant;

	public Isotop(BoxApplication variant, IProcessingGraph subVariant) {
		this.variant = variant;
		this.subVariant = subVariant;
	}

	@Override
	public boolean equals(Object obj) {
		Isotop other = (Isotop)obj;
		return other.variant == this.variant && other.subVariant == this.subVariant;
	}

	@Override
    public int hashCode() {
        return Objects.hash(variant, subVariant);
    }

	@Override
	public String toString() {
		return this.variant + " :: " + this.subVariant;
	}
}