package de.tum.in.jmoped.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * The annotation that specifies the number of bits of an instance field.
 * 
 * @author suwimont
 */
@Target(ElementType.FIELD)
public @interface FieldBits {

	int value();
}
