package de.tum.in.jmoped.annotation;

/**
 * The annotation that specifies the number of bits of an instance field 
 * or method parameters under test.
 * 
 * @author suwimont
 */
public @interface Bits {

	String[] value();
}
