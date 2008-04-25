package de.tum.in.jmoped.translator.stub.java.lang;

import de.tum.in.jmoped.translator.stub.java.io.PrintStream;

public class System {

	public static final PrintStream err = null;
	public static final PrintStream out = null;
	
	public static void arraycopy(Object src, int srcPos, 
			Object dest, int destPos, int length) {
		
		Object[] a = (Object[]) src;
		Object[] b = (Object[]) dest;
		
		for (int i = 0; i < length; i++)
			b[destPos + i] = a[srcPos + i];
	}
}
