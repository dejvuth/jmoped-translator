package de.tum.in.jmoped.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * The annotation that specifies the number of bits of method parameters under test.
 * 
 * @author suwimont
 */
@Target(ElementType.METHOD)
public @interface ParameterBits {

	int[] value();
}
