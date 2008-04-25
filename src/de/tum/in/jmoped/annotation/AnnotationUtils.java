package de.tum.in.jmoped.annotation;

import org.gjt.jclasslib.structures.AbstractStructureWithAttributes;
import org.gjt.jclasslib.structures.ClassFile;
import org.gjt.jclasslib.structures.FieldInfo;
import org.gjt.jclasslib.structures.InvalidByteCodeException;
import org.gjt.jclasslib.structures.MethodInfo;
import org.gjt.jclasslib.structures.attributes.RuntimeInvisibleAnnotationsAttribute;
import org.gjt.jclasslib.structures.constants.ConstantIntegerInfo;
import org.gjt.jclasslib.structures.constants.ConstantUtf8Info;
import org.gjt.jclasslib.structures.elementvalues.AnnotationElementValue;
import org.gjt.jclasslib.structures.elementvalues.ArrayElementValue;
import org.gjt.jclasslib.structures.elementvalues.ConstElementValue;
import org.gjt.jclasslib.structures.elementvalues.ElementValue;

import de.tum.in.jmoped.translator.Translator;

/**
 * Static annotation utils class that provides methods for looking up jmoped
 * specific annotations and for retrieving their values.
*
* @see {@link de.tum.in.jmoped.annotation.Bits}
* @see {@link de.tum.in.jmoped.annotation.FieldBits}
 */
public class AnnotationUtils {
	
	/**
	 * Returns the array of type <code>ElementValue</code> -- the data
	 * structure that represents annotations for parameters and local variables
	 * &#064;Bits.
	 * 
	 * @param methodInfo the method info
	 * @return the array of <code>ElementValue</code>, or null if there is no
	 *         annotation
	 * @throws InvalidByteCodeException
	 * @see getBit(ElementValue[], ClassFile, String, int)
	 * @see getBit(ElementValue, ClassFile)
	 */
	public static ElementValue[] getAnnotatedBits(MethodInfo methodInfo)
			throws InvalidByteCodeException {
		
		AnnotationElementValue[] annotations = getAnnotations(methodInfo);
		if (annotations == null) {
			return null;
		}
		
		AnnotationElementValue annotation;
		ClassFile classFile = methodInfo.getClassFile();
		annotation = getBitsAnnotation(annotations, classFile);
		if (annotation != null) {
			return getArrayElements(annotation);
		}
		
		annotation = getParameterBitsAnnotation(annotations, classFile);
		
		return (annotation != null) ? getArrayElements(annotation) : null;
	}
	
	public static ElementValue[] getAnnotatedRange(MethodInfo methodInfo) 
			throws InvalidByteCodeException {
		
		AnnotationElementValue[] annotations = getAnnotations(methodInfo);
		if (annotations == null) return null;
		
		AnnotationElementValue annotation;
		ClassFile classFile = methodInfo.getClassFile();
		annotation = getRangeAnnotation(annotations, classFile);
		if (annotation == null) return null;
		
		return getArrayElements(annotation);
	}
	
	/**
	 * Returns the annotated field bits if any, otherwise defaultBits is 
	 * returned. A field can be annotated in two ways:
	 * either by using &#064;Bits(String[]) or &#064;FieldBits(int).
	 * &#064;Bits has priority if both are annotated.
	 * 
	 * For example, the follwing snippet annotates field <code>foo</code>
	 * with 2 bits and field <code>bar</code> with one bit.
	 * <pre>
	 *    &#064;Bits({"2"})
	 *    int foo;
	 *    &#064;FieldBits(1)
	 *    int bar;
	 * </pre>
	 * 
	 * @param fieldInfo the field info
	 * @param defaultBits the default bits
	 * @return the number of bits
	 * @throws InvalidByteCodeException
	 */
	public static int getFieldBits(FieldInfo fieldInfo, int defaultBits)
			throws InvalidByteCodeException {
		
		ClassFile cf = fieldInfo.getClassFile();
		AnnotationElementValue[] annotations;
		AnnotationElementValue annotation;
		int index;
		
		annotations = getAnnotations(fieldInfo);
		
		// returns the default bits if there is no field annotation
		if (annotations == null) return defaultBits;
		
		// searches for annotaion of type @Bits(String[])
		annotation = getBitsAnnotation(annotations, cf);
		if (annotation != null) {
			
			ElementValue[] elemVals = getArrayElements(annotation);
			if (elemVals != null && elemVals.length > 0) {
				
				index = ((ConstElementValue) elemVals[0]).getConstValueIndex();
				ConstantUtf8Info info = cf.getConstantPoolUtf8Entry(index);
//				logger.debug("info.getString(): " + info.getString());
				
				try {
					return Integer.parseInt(info.getString());
				} catch (NumberFormatException e) {
				}
			}
		}
		
		// searches for annotaion of type @FieldBits(int)
		annotation = getFieldBitsAnnotation(annotations, cf);
		if (annotation != null) {
			
			ConstElementValue cnstElemVal = (ConstElementValue)
				annotation.getElementValuePairEntries()[0].getElementValue();
			index = ((ConstElementValue) cnstElemVal).getConstValueIndex();
			ConstantIntegerInfo info = (ConstantIntegerInfo)
				cf.getConstantPoolEntry(index, ConstantIntegerInfo.class);
			return info.getInt();
		}
		
		return defaultBits;
	}
	
	/**
	 * Returns the number of bits of elements of array <code>var</code>.
	 * Elements of an array are annotated with array name followed by [].
	 * If <code>var</code> is not explicitly annotated, the method tries
	 * to find if the method is annotated with &#064;ArrayBits(int),
	 * the default array annotaion. If nothing is found, zero is returned.
	 * 
	 * @param values the data structure used in annotations
	 * @param methodInfo the method info
	 * @param var the variable's name
	 * @return the number of bits, or 0 if the variable is not annotated
	 * @throws InvalidByteCodeException
	 */
	public static Integer getArrayBits(ElementValue[] values, 
			MethodInfo methodInfo, String var) {
		
		Integer bits;
		
		try {
			bits = getBits(values, methodInfo.getClassFile(), var + "[]", -1);
			if (bits != null) return bits;
		} catch (Exception e) {
		}
		
		return getDefaultArrayBits(methodInfo);
	}
	
	/**
	 * Returns the annotated value of &#064;ArrayBits(int) for methodInfo.
	 * &#064;ArrayBits(int) can be annotated either at method level or
	 * class level. The one at method level has the priority if both
	 * are annotated.
	 * Zero is returned if methodInfo is not annotated.
	 * 
	 * @param methodInfo the method info.
	 * @return the annotated bits.
	 */
	public static Integer getDefaultArrayBits(MethodInfo methodInfo) {
		
		Integer bits = getDefaultArrayBitsForAbstractStructure(methodInfo);
		if (bits != null) return bits;
		
		return getDefaultArrayBitsForAbstractStructure(methodInfo.getClassFile());
	}
	
	/**
	 * Returns the annotated value of &#064;ArrayBits(int) for struct. 
	 * Zero is returned if struct is not annotated.
	 * 
	 * @param struct meant to be either <code>ClassFile</code> or 
	 * 			<code>MethodInfo</code>.
	 * @return the annotated bits.
	 */
	private static Integer getDefaultArrayBitsForAbstractStructure(
			AbstractStructureWithAttributes struct) {
		
		try {
			ClassFile cf = struct.getClassFile();
			AnnotationElementValue[] annotations = getAnnotations(struct);
			AnnotationElementValue annotation = 
				getArrayBitsAnnotation(annotations, cf);	
			ConstElementValue value = (ConstElementValue)
				annotation.getElementValuePairEntries()[0].getElementValue();
			int index = ((ConstElementValue) value).getConstValueIndex();
			ConstantIntegerInfo info = (ConstantIntegerInfo)
				cf.getConstantPoolEntry(index, ConstantIntegerInfo.class);
			return info.getInt();
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * Returns the number of bits for a parameter.
	 * Zero is returned if the method is not annotated.
	 * 
	 * @param values the data structure used in annotations
	 * @param classFile the class file
	 * @param var the variable's name
	 * @param varIndex the variable index
	 * @return the number of bits, or 0 if the variable is not annotated
	 * @throws InvalidByteCodeException
	 */
	public static Integer getBits(ElementValue[] values, ClassFile classFile, 
			String var, int varIndex) {
		
		int index;
		ConstantUtf8Info info;
		String[] consts;
		
		if (values == null) {
			return null;
		}
		
		try {
			
			for (int i = 0; i < values.length; i++) {
				
				index = ((ConstElementValue) values[i]).getConstValueIndex();
				info = classFile.getConstantPoolUtf8Entry(index);
				Translator.log("i: %s, info.getString(): %s%n", i, info.getString());
				
				if (varIndex == i) {
					try {
						return new Integer(info.getString());
					} catch (NumberFormatException e) {
					}
				}
				
				consts = info.getString().split("[ :=]");
				for (int j = 0; j < consts.length; j++)
//					logger.debug("(" + j + ") " + consts[j]);
				
				if (consts == null || consts.length < 2)
					continue;
				
				if (consts[0].equals(var)) {
					try {
						return new Integer(consts[consts.length - 1]);
					} catch (NumberFormatException e) {
						continue;
					}
				}
			}
		} catch (InvalidByteCodeException e) {}
		
		try {
			return getInt(values[varIndex], classFile);
		} catch (ArrayIndexOutOfBoundsException e) {}
		
		return null;
	}
	
	private static Integer getRange(ElementValue[] values, ClassFile classFile, 
			String var, int varIndex, char left, char right) 
			throws InvalidByteCodeException {
		
		int index;
		ConstantUtf8Info info;
		String[] consts;
		
		if (values == null) return null;
		for (int i = 0; i < values.length; i++) {
				
			index = ((ConstElementValue) values[i]).getConstValueIndex();
			info = classFile.getConstantPoolUtf8Entry(index);
			
			if (varIndex == i) {
				try {
					String s = info.getString();
					return new Integer(s.substring(s.indexOf(left) + 1, s.indexOf(right)));
				} catch (Exception e) {
				}
			}
			
			consts = info.getString().split("[ :=]");
			if (consts == null || consts.length < 2)
				continue;
			
			if (consts[0].equals(var)) {
				try {
					String s = consts[consts.length - 1];
					return new Integer(s.substring(s.indexOf(left) + 1, s.indexOf(right)));
				} catch (Exception e) {
					continue;
				}
			}
		}
		
		return null;
	}
	
	public static Integer[] getMinMax(ElementValue[] values, ClassFile classFile, 
			String var, int varIndex) throws InvalidByteCodeException {
		
		Integer min = getRange(values, classFile, var, varIndex, '[', ',');
		if (min == null) return null;
		Integer max = getRange(values, classFile, var, varIndex, ',', ']');
		if (max == null) return null;
		return new Integer[] { min, max };
	}
	
	public static Integer[] getArrayMinMax(ElementValue[] values, 
			MethodInfo methodInfo, String var) throws InvalidByteCodeException {
		
		return getMinMax(values, methodInfo.getClassFile(), var + "[]", -1);
	}
	
	/**
	 * Returns the integer annotation of value. Zero is returned if annotation
	 * is not an integer.
	 * 
	 * @param value the annotation data structure
	 * @param classFile the class file
	 * @return the integer annotaion, or zero if annotation is not an integer
	 * @throws InvalidByteCodeException
	 */
	public static Integer getInt(ElementValue value, ClassFile classFile) {
		
		int index = ((ConstElementValue) value).getConstValueIndex();
		
		try {
			
			ConstantUtf8Info info = classFile.getConstantPoolUtf8Entry(index);
			try {
				return new Integer(info.toString());
			} catch (NumberFormatException e) {
				return null;
			}
		} catch (InvalidByteCodeException e) {}
		
		try {
			
			ConstantIntegerInfo info = (ConstantIntegerInfo)
				classFile.getConstantPoolEntry(index, ConstantIntegerInfo.class);
			return info.getInt();
		} catch (InvalidByteCodeException e) {
			
			return null;
		}
	}
	
	/*******************************************************************/
	/**                       PRIVATE METHODS                         **/
	/*******************************************************************/
	
	/**
	 * Private helper to retrieve the Annotaions attribute. 
	 * @param structure
	 * @return null if there is none
	 */
	private static AnnotationElementValue[] getAnnotations
		(AbstractStructureWithAttributes structure)
	{
		RuntimeInvisibleAnnotationsAttribute attr = 
			(RuntimeInvisibleAnnotationsAttribute)
			structure.findAttribute(RuntimeInvisibleAnnotationsAttribute.class);
	
		if (attr == null) {
			return null;
		}

		return attr.getRuntimeAnnotations();
	}

	/**
	 * Private helper method that returns ElementValue array.
	 * 
	 * @param annotation
	 * @return
	 */
	private static ElementValue[] getArrayElements(AnnotationElementValue annotation) {
		
		ArrayElementValue bitsArray = (ArrayElementValue)
			annotation.getElementValuePairEntries()[0].getElementValue();
		return bitsArray.getElementValueEntries();
	}
	
	/**
	 * Private helper method which looks for an annotation of type 
	 * <code>annotationType</code>.
	 * 
	 * @param annotations
	 * @param classFile
	 * @param annotationType
	 * @return
	 * @throws InvalidByteCodeException
	 */
	private static AnnotationElementValue getAnnotation
		(AnnotationElementValue[] annotations, ClassFile classFile, 
		 String annotationType)
		throws InvalidByteCodeException
	{
		for (AnnotationElementValue annotation : annotations) {
			int type = annotation.getTypeIndex();
			String typeName = classFile.getConstantPoolEntryName(type);
			if (typeName.equals(annotationType)) {
				return annotation;
			}
		}
		return null;	
	}
	
	private static AnnotationElementValue getArrayBitsAnnotation
		(AnnotationElementValue[] annotations, ClassFile classFile)
		throws InvalidByteCodeException
	{
		return getAnnotation(annotations, classFile,
				"Lde/tum/in/jmoped/annotation/ArrayBits;");
	}
	
	private static AnnotationElementValue getFieldBitsAnnotation
		(AnnotationElementValue[] annotations, ClassFile classFile)
		throws InvalidByteCodeException
	{
		return getAnnotation(annotations, classFile,
				"Lde/tum/in/jmoped/annotation/FieldBits;");
	}
	
	private static AnnotationElementValue getBitsAnnotation
		(AnnotationElementValue[] annotations, ClassFile classFile)
		throws InvalidByteCodeException
	{
		return getAnnotation(annotations, classFile, 
				"Lde/tum/in/jmoped/annotation/Bits;");
	}
	
	private static AnnotationElementValue getParameterBitsAnnotation
		(AnnotationElementValue[] annotations, ClassFile classFile)
		throws InvalidByteCodeException
	{
		return getAnnotation(annotations, classFile, 
				"Lde/tum/in/jmoped/annotation/ParameterBits;");
	}
	
	private static AnnotationElementValue getRangeAnnotation
		(AnnotationElementValue[] annotations, ClassFile classFile)
		throws InvalidByteCodeException
	{
		return getAnnotation(annotations, classFile, 
				"Lde/tum/in/jmoped/annotation/Range;");
	}
}

