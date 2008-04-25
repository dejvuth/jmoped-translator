//package de.tum.in.jmoped.translator.stub;
//
//import de.tum.in.jmoped.translator.ModuleMaker;
//import de.tum.in.jmoped.translator.TranslatorUtils;
//
//public abstract class Stub implements ModuleMaker {
//	
////	/**
////	 * Method name followed by descriptor
////	 */
////	protected String method;
//	
//	/**
//	 * Name of the stub.
//	 */
//	protected String name;
//	
//	/**
//	 * The constructor.
//	 * 
//	 * @param className the class name.
//	 * @param method the method name and descriptor.
//	 */
//	public Stub(String className, String methodName, String methodDesc) {
////		this.method = method;
//		name = TranslatorUtils.formatName(className, methodName, methodDesc);
//	}
//
//	/**
//	 * Returns the formatted names of methods references by this stub.
//	 * 
//	 * @return the array of formatted names.
//	 */
//	public abstract String[] getReferences();
//	
//	public String getName() {
//		return name;
//	}
//	
//	/**
//	 * Gets the label at offset i of this stub.
//	 * 
//	 * @param i the offset.
//	 * @return the label at offset i.
//	 */
//	protected String getLabel(int i) {
//		return TranslatorUtils.formatName(name, i);
//	}
//}
