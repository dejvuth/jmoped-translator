package de.tum.in.jmoped.translator.stub;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;

import org.gjt.jclasslib.io.ClassFileReader;
import org.gjt.jclasslib.structures.ClassFile;
import org.gjt.jclasslib.structures.InvalidByteCodeException;

import de.tum.in.jmoped.SearchUtils;
import de.tum.in.jmoped.translator.ClassTranslator;
import de.tum.in.jmoped.translator.TranslatorUtils;

public class StubManager {
	
	private static HashSet<String> stubs = new HashSet<String>();
	private static String[] classpaths = System.getProperty("java.class.path")
			.split(System.getProperty("path.separator"));
	
	static {
		stubs.add("de/tum/in/jmoped/underbone/BDDSemiring");
		stubs.add("de/tum/in/jmoped/underbone/ExprSemiring");
		stubs.add("de/tum/in/jmoped/underbone/Variable");
		stubs.add("de/tum/in/jmoped/underbone/VarManager");
		
		stubs.add("java/io/PrintStream");
		
		stubs.add("java/lang/AssertionError");
		stubs.add("java/lang/Character");
		stubs.add("java/lang/Class");
		stubs.add("java/lang/Enum");
		stubs.add("java/lang/Error");
		stubs.add("java/lang/Float");
		stubs.add("java/lang/Double");
		stubs.add("java/lang/IllegalArgumentException");
		stubs.add("java/lang/Integer");
		stubs.add("java/lang/InterruptedException");
		stubs.add("java/lang/Long");
		stubs.add("java/lang/Number");
		stubs.add("java/lang/RuntimeException");
		stubs.add("java/lang/String");
		stubs.add("java/lang/StringBuilder");
		stubs.add("java/lang/System");
		stubs.add("java/lang/Thread");
		stubs.add("java/lang/Throwable");
		
		stubs.add("java/math/BigInteger");
		
		stubs.add("java/util/Random");
		
		stubs.add("net/sf/javabdd/BDD");
		stubs.add("net/sf/javabdd/BDDDomain");
		stubs.add("net/sf/javabdd/BDDFactory");
		stubs.add("net/sf/javabdd/BDDFactory$Node");
		stubs.add("net/sf/javabdd/BDDVarSet");
		
		stubs.add("org/junit/Assert");
	}
	
	/**
	 * Returns <code>true</code> if there is a stub for the class
	 * specified by <code>className</code>; or <code>false</code> otherwise.
	 * 
	 * @param className the class name.
	 * @return <code>true</code> iff there is a stub for the class
	 * 			specified by <code>className</code>.
	 */
	public static boolean hasStub(String className) {
		return stubs.contains(className);
	}
	
//	private static String stubPath = null;
	
	/**
	 * Creates a class file for the stub specified by <code>className</code>.
	 * 
	 * @param className the class name of the stub.
	 * @return the class file.
	 * @throws InvalidByteCodeException
	 * @throws IOException
	 */
	public static ClassFile findClassFile(String className) 
			throws InvalidByteCodeException, IOException {
		
		URL url = null;
		try {
			url = SearchUtils.findURL(className);
		} catch (NoClassDefFoundError e) {
			return null;
		}
		if (url != null) 
			return ClassFileReader.readFromInputStream(url.openStream());
		
//		// Looks at each classpaths
//		String fileName = String.format("%s.class", className)
//				.replace('/', File.separatorChar);
//		if (stubPath == null) {
//			for (String cp : classpaths) {
//				File file = new File(cp, fileName);
//				if (file.exists()) {
//					stubPath = cp;
//					return ClassFileReader.readFromFile(file);
//				}
//			}
//		} else {
//			return ClassFileReader.readFromFile(new File(stubPath, fileName));
//		}
		
		// Not found
		return null;
	}
	
	/**
	 * Creates class translator for the class specified by className.
	 * 
	 * @param id
	 * @param className
	 * @return
	 * @throws IOException
	 * @throws InvalidByteCodeException
	 */
	public static ClassTranslator createClassStub(int id, String className) 
			throws IOException, InvalidByteCodeException {
		
		String prefixClassName = prefixName(className);
		ClassFile cf = findClassFile(prefixClassName);
		if (cf == null) {
			cf = TranslatorUtils.findClassFile(prefixClassName, 
					new String[] { "jmoped/bin" });
			if (cf == null) return null;
		}
		return new ClassTranslator(id, cf);
	}
	
	private final static String PREFIX = "de/tum/in/jmoped/translator/stub/";
	
	/**
	 * Creates a stub name by prefixing the className.
	 * 
	 * @param className the class name.
	 * @return the stub name.
	 */
	public static String prefixName(String className) {
		if (className.startsWith(PREFIX)) return className;
		return String.format("%s%s", PREFIX, className);
	}
	
	/**
	 * Extracts the class name from stub. 
	 * The method does nothing if the argument is not a stub.
	 * 
	 * @param stub the stub name.
	 * @return the class name of the stub.
	 */
	public static String removeStub(String stub) {
		if (stub == null) return null;
		return stub.replace(PREFIX, "");
	}
}
