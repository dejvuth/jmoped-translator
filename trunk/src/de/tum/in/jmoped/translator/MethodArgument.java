package de.tum.in.jmoped.translator;

/**
 * A method argument.
 * 
 * @author suwimont
 *
 */
public class MethodArgument {

	/**
	 * The method name.
	 */
	private String methodName;
	
	/**
	 * The parameter types.
	 */
	private String[] types;
	
	/**
	 * The argument values.
	 */
	private Object[] values;
	
	/**
	 * The number of arguments filled so far.
	 */
	private int count = 0;
	
	/**
	 * The constructor.
	 * 
	 * @param methodName the method name.
	 * @param nargs the number of arguments.
	 */
	public MethodArgument(String methodName, int nargs) {
		this.methodName = methodName;
		types = new String[nargs];
		values = new Object[nargs];
	}
	
	/**
	 * Adds the parameter type and the argument value. 
	 * 
	 * @param type the parameter type.
	 * @param value the argument value.
	 */
	public void add(String type, Object value) {
		types[count] = type;
		values[count] = value;
		count++;
	}
	
	/**
	 * Interprets the JVM-base <code>type</code> (Table 4.2, JVM book) 
	 * into the corresponding Java-legal keyword.
	 * If the string <code>type</code> contains more than one type,
	 * only the first type is interpreted.
	 * 
	 * @param type the base type.
	 * @return the Java keyword type.
	 */
	public static String interpretType(String type) {
		switch (type.charAt(0)) {
		case 'B': return "byte";
		case 'C': return "char";
		case 'D': return "double";
		case 'F': return "float";
		case 'I': return "int";
		case 'J': return "long";
		case 'L': return type.substring(1, type.indexOf(';'))
							.replace('/', '.').replace('$', '.');
		case 'S': return "short";
		case 'Z': return "boolean";
		}
		return null;
	}
	
	private static final int TO_STRING = 0;
	private static final int TO_JAVA_STRING = 1;
	
	/**
	 * Returns the string representing the argument value at <code>index</code>.
	 * 
	 * @param index
	 * @param mode
	 * @return
	 */
	private String toString(int index, int mode) {
		
		// Returns raw value if not array
		if (types[index].charAt(0) != '[') 
			return values[index].toString();
		
		// Prepares the array's prefix and suffix, depending on mode.
		String prefix;
		String suffix;
		if (mode == TO_STRING) {
			prefix = "[";
			suffix = "]";
		} else {	// mode == TO_JAVA_STRING
			String type = interpretType(types[index].substring(1));
			prefix = String.format("new %s[] { ", type);
			suffix = " }";
		}
		
		// Returns array elements
		Number[] array = (Number[]) values[index];
		if (array.length == 0) return prefix + suffix;
		if (array.length == 1) return prefix + array[0] + suffix;
		
		StringBuilder out = new StringBuilder(prefix);
		out.append(array[0]);
		for (int i = 1; i < array.length; i++) {
			out.append(", ");
			out.append(array[i]);
		}
		out.append(suffix);
		
		return out.toString();
	}
	
	/**
	 * Returns the comma-separated list of argument values.
	 * 
	 * @param prefix
	 * @param suffix
	 * @return
	 */
	private String toString(int mode) {
		if (types == null) return null;
		if (types.length == 0) return "";
		if (types.length == 1) return toString(0, mode);
		
		StringBuilder out = new StringBuilder();
		out.append(toString(0, mode));
		for (int i = 1; i < types.length; i++) {
			out.append(", ");
			out.append(toString(i, mode));
		}
		
		return out.toString();
	}
	
	public String toJavaString() {
		return toString(TO_JAVA_STRING);
	}
	
	public String toString() {
		return String.format("%s(%s)", methodName, toString(TO_STRING));
	}
}
