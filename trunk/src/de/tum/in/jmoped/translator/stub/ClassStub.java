//package de.tum.in.jmoped.translator.stub;
//
//import java.io.File;
//import java.io.IOException;
//import java.io.InputStream;
//import java.net.URL;
//
//import org.eclipse.core.runtime.FileLocator;
//import org.eclipse.core.runtime.Path;
//import org.eclipse.core.runtime.Platform;
//import org.gjt.jclasslib.io.ClassFileReader;
//import org.gjt.jclasslib.structures.InvalidByteCodeException;
//import org.osgi.framework.Bundle;
//
//import de.tum.in.jmoped.Activator;
//import de.tum.in.jmoped.translator.ModuleCollection;
//
//public class ClassStub extends ModuleCollection {
//	
//	/**
//	 * Name of the class stub.
//	 */
//	protected String className;
//	
//	private static Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);
//	
//	/**
//	 * The constructor.
//	 * 
//	 * @param id the id of this class stub.
//	 * @throws IOException 
//	 * @throws InvalidByteCodeException 
//	 */
//	public ClassStub(int id, String className) throws IOException, InvalidByteCodeException {
//		super(id);
//		this.className = className;
//		
//		String fileName = String.format(
//				"bin/de/tum/in/jmoped/translator/stub/%s.class", className)
//			.replace('/', File.separatorChar);
//		Path path = new Path(fileName);
//		URL url = FileLocator.find(bundle, path, null);
//		InputStream in = url.openStream();
//		cf = ClassFileReader.readFromInputStream(in);
//	}
//
//	@Override
//	public String getName() {
//		return className;
//	}
//	
////	/**
////	 * Includes the stub specified by name.
////	 * 
////	 * @param name the name of the stub.
////	 * @throws InvalidByteCodeException 
////	 * @throws IOException 
////	 */
////	public ModuleMaker include(String name) throws InvalidByteCodeException, IOException {
////		String methodName = TranslatorUtils.extractMethodName(name);
////		String methodDesc = TranslatorUtils.extractMethodDescriptor(name);
////		MethodInfo mi = cf.getMethod(methodName, methodDesc);
////		ModuleMaker module = new MethodTranslator(mi);
////		modules.put(name, module);
////		return module;
////	}
//}
