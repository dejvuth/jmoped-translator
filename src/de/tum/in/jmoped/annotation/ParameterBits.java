package de.tum.in.jmoped.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
public @interface ParameterBits {

	int[] value();
}
