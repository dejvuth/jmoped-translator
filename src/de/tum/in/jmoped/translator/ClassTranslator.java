package de.tum.in.jmoped.translator;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import org.gjt.jclasslib.structures.AccessFlags;
import org.gjt.jclasslib.structures.ClassFile;
import org.gjt.jclasslib.structures.FieldInfo;
import org.gjt.jclasslib.structures.InvalidByteCodeException;
import org.gjt.jclasslib.structures.constants.ConstantClassInfo;

import de.tum.in.jmoped.translator.stub.StubManager;

/**
 * A class translator.
 * 
 * @author suwimont
 *
 */
public class ClassTranslator {

	/**
	 * Every collection has a unique id starting from one.
	 */
	private int id;
	
	/**
	 * The class name.
	 */
	private String name;
	
	/**
	 * The class file of this class.
	 */
	private ClassFile cf;
	
	/**
	 * For non-static fields: maps field name to field translator
	 */
	protected HashMap<String, FieldTranslator> instanceFields 
			= new HashMap<String, FieldTranslator>();
	
	/**
	 * Set of static fields.
	 */
	protected HashMap<String, FieldTranslator> staticFields 
			= new HashMap<String, FieldTranslator>();
	
	/**
	 * Maps formatted names to module makers.
	 */
	protected HashMap<String, ModuleMaker> modules = new HashMap<String, ModuleMaker>();
	
	/**
	 * Super class name.
	 */
	protected String superClass;
	
	/**
	 * Set of subclasses.
	 */
	protected HashSet<ClassTranslator> subClasses;
	
	/**
	 * Implementing interfaces.
	 */
	protected String[] interfaces;
	
	public ClassTranslator(int id, String name) {
		this.id = id;
		this.name = name;
		this.superClass = "java/lang/Object";
	}
	
	/**
	 * The constructor.
	 * 
	 * @param id the id of this collection.
	 */
	public ClassTranslator(int id, ClassFile cf) throws InvalidByteCodeException {
		this.id = id;
		this.cf = cf;
		
		// Name
		name = StubManager.removeStub(cf.getThisClassName());
		
		// Fields
		FieldInfo[] fi = cf.getFields();
		for (int i = 0; i < fi.length; i++) {
			
			// Creates field translator
			Translator.log("\tfield: %s%n", fi[i].getName());
			FieldTranslator field = new FieldTranslator(fi[i]);
			if (field.isStatic()) {
				staticFields.put(field.getName(), field);
			} else {
				instanceFields.put(field.getName(), field);
			}
		}
		
		// Super class
		superClass = StubManager.removeStub(cf.getSuperClassName());
		
		// Interfaces
		interfaces = getInterfaces(cf);
	}
	
	/**
	 * Returns the interface names that this class implements.
	 * 
	 * @param cf the class file.
	 * @return the array of interface names.
	 * @throws InvalidByteCodeException
	 */
	private String[] getInterfaces(ClassFile cf) throws InvalidByteCodeException {
		
		int[] indices = cf.getInterfaces();
		if (indices == null) return null;
		
		String[] interfaces = new String[indices.length];
		for (int i = 0; i < indices.length; i++) {
			ConstantClassInfo info = (ConstantClassInfo) 
					cf.getConstantPoolEntry(indices[i], ConstantClassInfo.class);
			interfaces[i] = StubManager.removeStub(info.getName());
		}
		
		return interfaces;
	}
	
	/**
	 * Returns the id of this collection. Each method collection has a unique
	 * id. The starting class must have id one.
	 * 
	 * @return the id of this collection.
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * Returns the underlying class file of this class translator. 
	 * 
	 * @return the class file.
	 */
	public ClassFile getClassFile() {
		return cf;
	}
	
	public boolean isArrayType() {
		return cf == null;
	}
	
	/**
	 * Returns the name of this collection.
	 * 
	 * @return the name of this collection.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Returns <code>true</code> if this class in an interface.
	 * 
	 * @return <code>true</code> if this class in an interface.
	 */
	public boolean isInterface() {
		// Returns false in case of array type
		if (cf == null) return false;
		
		// Looks at the interface flag
		return (cf.getAccessFlags() & AccessFlags.ACC_INTERFACE) != 0;
	}
	
	/**
	 * Adds new module maker. 
	 * 
	 * @param module the module maker.
	 */
	public void add(ModuleMaker module) {
		modules.put(module.getName(), module);
	}
	
	/**
	 * Returns <code>true</code> if the method specified by formattedName
	 * is included.
	 * 
	 * @param forammtedName the formatted name of method.
	 * @return <code>true</code> if the method is included.
	 */
	public boolean contains(String formattedName) {
		return modules.containsKey(formattedName);
	}
	
	public boolean contains(String className, String methodName, String methodDesc) {
		return contains(TranslatorUtils.formatName(className, methodName, methodDesc));
	}
	
	/**
	 * Returns <code>true</code> if the collection contains a static initializer.
	 * 
	 * @return <code>true</code> if the collection contains a static initializer.
	 */
	public boolean containsClinit() {
		return contains(TranslatorUtils.formatName(getName(), "<clinit>", "()V"));
	}
	
//	/**
//	 * Returns the map of instance fields of this collection.
//	 * 
//	 * @return the map of instance field translators.
//	 * @see #staticFields.
//	 */
//	HashMap<String, FieldTranslator> getInstanceFieldMap() {
//		return instanceFields;
//	}
	
	/**
	 * Returns the set of static fields of this collection.
	 * 
	 * @return the set of static field translators.
	 * @see #staticFields.
	 */
	public Collection<FieldTranslator> getStaticFields() {
		return staticFields.values();
	}
	
	/**
	 * Returns the module makers of this collection.
	 * 
	 * @return the module makers.
	 * @see #modules.
	 */
	public Collection<ModuleMaker> getModuleMakers() {
		return modules.values();
	}
	
	/**
	 * Gets the module maker specified by name.
	 * 
	 * @param name the name of the module maker.
	 * @return the module maker.
	 */
	public ModuleMaker getModuleMaker(String name) {
		return modules.get(name);
	}
	
	public ModuleMaker getModuleMaker(String methodName, String methodDesc) {
		String name = TranslatorUtils.formatName(getName(), methodName, methodDesc);
		return getModuleMaker(name);
	}
	
	/**
	 * Gets the super class of this class; or <code>null</code> if this class
	 * is an instance of <code>java/lang/Object</code>.
	 * 
	 * @return the super class.
	 */
	public String getSuperClassName() {
		return superClass;
	}
	
	public void setSubClasses(HashSet<ClassTranslator> subClasses) {
		this.subClasses = subClasses;
	}
	
	/**
	 * Gets all known subclasses. The method returns <code>null</code> if
	 * there is none.
	 * 
	 * @return the set of subclasses.
	 */
	public HashSet<ClassTranslator> getSubClasses() {
		return subClasses;
	}
	
	public String[] getInterfaces() {
		return interfaces;
	}
	
	/**
	 * Returns the size of this collection.
	 * Size is defined as the number of non-static fields in the collection.
	 * 
	 * @return the size of this collection.
	 */
	public int size() {
		return instanceFields.size();
	}
	
	/**
	 * Return the field translator of the field specified by name.
	 * 
	 * @param name the field's name.
	 * @return the field translator.
	 */
	public FieldTranslator getInstanceFieldTranslator(String name) {
		return instanceFields.get(name);
	}
	
	public FieldTranslator getStaticFieldTranslator(String name) {
		return staticFields.get(name);
	}
	
	public String toString() {
		return getName();
	}
}
