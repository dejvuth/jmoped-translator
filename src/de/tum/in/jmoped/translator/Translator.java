package de.tum.in.jmoped.translator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.gjt.jclasslib.bytecode.AbstractInstruction;
import org.gjt.jclasslib.bytecode.Opcodes;
import org.gjt.jclasslib.io.ByteCodeReader;
import org.gjt.jclasslib.structures.CPInfo;
import org.gjt.jclasslib.structures.ClassFile;
import org.gjt.jclasslib.structures.InvalidByteCodeException;
import org.gjt.jclasslib.structures.MethodInfo;
import org.gjt.jclasslib.structures.attributes.CodeAttribute;

import de.tum.in.jmoped.translator.stub.StubManager;
import de.tum.in.jmoped.underbone.LabelUtils;
import de.tum.in.jmoped.underbone.Module;
import de.tum.in.jmoped.underbone.RawArgument;
import de.tum.in.jmoped.underbone.Remopla;
import de.tum.in.jmoped.underbone.Variable;
import de.tum.in.wpds.Utils;

import static de.tum.in.jmoped.underbone.VariableType.*;

/**
 * A Java-bytecode-to-Remopla translator.
 * 
 * @author suwimont
 *
 */
public class Translator {

	/**
	 * The wrapper module. The analysis always starts here.
	 */
	MethodWrapper init;
	
	/**
	 * The search paths are where the classes will searched for.
	 */
	String[] searchPaths;
	
	/**
	 * Maps class name to its class translator.
	 */
	private HashMap<String, ClassTranslator> included = new HashMap<String, ClassTranslator>();
	
	/**
	 * The default number of bits of integers.
	 * Initialized by a call to {@link #translate(int, int, boolean, int, boolean)}.
	 */
	private int bits;
	
	/**
	 * If <code>false</code>, the produced Remopla code is to be executed
	 * by the virtual machine.
	 * Initialized by a call to {@link #translate(int, int, boolean, int, boolean)}.
	 */
	private boolean nondet;
	
	/**
	 * Determines the thread bound. One for the single-thread case.
	 * Initialized by a call to {@link #translate(int, int, boolean, int, boolean)}.
	 */
	private int tbound;
	
	/**
	 * Determines if whether to handle context-switches symbolically.
	 * Its content is meaningless is meaningless in the case of single-thread.
	 * Initialized by a call to {@link #translate(int, int, boolean, int, boolean)}.
	 */
	private boolean symbolic;
	
	/**
	 * The logger.
	 */
	private static Logger logger = Utils.getLogger(Translator.class);
	
	/**
	 * Verbosity level.
	 */
	private static int verbosity = 0;
	
	/**
	 * The constructor.
	 * 
	 * @param className the class name where the analysis starts.
	 * @param searchPaths the paths where class files are searched for.
	 * @param methodName the method name  where the analysis starts.
	 * @param methodDesc the method descriptor where the analysis starts.
	 * @throws InvalidByteCodeException
	 * @throws IOException
	 */
	public Translator(String className, String[] searchPaths,
			String methodName, String methodDesc) 
			throws InvalidByteCodeException, IOException {
		
		this.searchPaths = searchPaths;
		
		// Includes relevant methods
		className = className.replace('.', '/');
		includeAllMethodsFrom(className);
		logIncluded();
		
		// Creates a method wrapper
		init = new MethodWrapper(this, className, methodName, methodDesc);
	}
	
	/**
	 * Gets the stored search paths.
	 * 
	 * @return the search paths.
	 */
	public String[] getSearchPaths() {
		return searchPaths;
	}
	
	/**
	 * Gets the default number of bits of integers.
	 * 
	 * @return the default number of bits of integers.
	 */
	public int getBits() {
		return bits;
	}
	
	/**
	 * Returns <code>true</code> if the Remopla code is for the model checker.
	 * For the virtual machine, <code>false</code> is returned.
	 * 
	 * @return <code>true</code> if the Remopla code is for the model checker.
	 */
	public boolean nondeterministic() {
		return nondet;
	}
	
	/**
	 * Returns <code>true</code> if the Remopla code models a multithreaded program.
	 * 
	 * @return <code>true</code> if the Remopla code models a multithreaded program.
	 */
	public boolean multithreading() {
		return tbound > 1;
	}
	
	/**
	 * Determines the number of heap elements required for modelling multithreading programs.
	 * The method returns zero case of single-thread programs.
	 * 
	 * @return two in case of multithreaded programs; or zero otherwise.
	 */
	int getObjectBaseId() {
		return (multithreading()) ? 2 : 0;
	}
	
	/**
	 * Returns <code>true</code> if context-switches are symbolic.
	 * 
	 * @return <code>true</code> if context-switches are symbolic.
	 */
	public boolean symbolic() {
		return symbolic;
	}
	
	/**
	 * Translates to Remopla.
	 * 
	 * @param bits
	 * @param heapSize
	 * @param nondet
	 * @param tbound
	 * @return
	 * @throws InvalidByteCodeException 
	 */
	public Remopla translate(int bits, int heapSize, boolean nondet, int tbound, boolean symbolic)
			throws InvalidByteCodeException {

		log("bits: %d, heapSize: %d, nondet: %b, tbound: %d%n", 
				bits, heapSize, nondet, tbound);
		this.bits = bits;
		this.nondet = nondet;
		this.tbound = tbound;
		this.symbolic = symbolic;
		
		// Calculates heap size
		long[] heap = new long[heapSize];
		Arrays.fill(heap, (long) Math.pow(2, bits));
		long[] initheap = init.estimateHeapSizes(bits);
		if (initheap != null) {
			if (heapSize < initheap.length + 2) {
				String s = String.format(
						"The heap size is too small for the specified range(s). " +
						"A heap of size at least %d is required."
						, initheap.length + 2);
				throw new IllegalArgumentException(s);
			}
			System.arraycopy(initheap, 0, heap, 1, initheap.length);
		}
		log("Heap: %s%n", Arrays.toString(heap));
		
		// Global vars
		ArrayList<Variable> gv = new ArrayList<Variable>();
		for (ClassTranslator coll : included.values()) {
			
			/*
			 *  A boolean variable for each class, except the starting class,
			 *  that has static initializer.
			 */
			if (coll.containsClinit() && coll.getId() != 0) {
				gv.add(new Variable(BOOLEAN, coll.getName(), 0, true));
			}
			
			// Static field
			for (FieldTranslator field : coll.getStaticFields()) {
				
				// Bypasses final field
				if (field.isFinal()) continue;
				
				Variable var;
				if (field.isAssertionsDisabledField())
					var = new Variable(BOOLEAN, field.getName());
				else
					var = new Variable(INT, field.getName(), bits);
				var.setShared(true);
				gv.add(var);
			}
		}
		
		
		// Adds aux variables in case of multithreading
		if (multithreading()) {
			for (int i = 1; i <= tbound; i++) {
				gv.add(new Variable(INT, LabelUtils.formatSave(i), bits, true));
				gv.add(new Variable(BOOLEAN, LabelUtils.formatWaitFlag(i), 0, true));
				gv.add(new Variable(INT, LabelUtils.formatWaitFor(i), 0, true));
			}
		}
		
		// Creates list of module
		ArrayList<Module> modules = new ArrayList<Module>();
		modules.add(init.wrap(bits, nondet));
		for (ClassTranslator coll : included.values()) {
			for (ModuleMaker module : coll.getModuleMakers()) {
				modules.add(module.make(this));
			}
		}
		
		Remopla remopla = new Remopla(bits, heap, gv, modules, init.getName());
		log("%n=== Remopla ===%n");
		log("%s", remopla);
		log("%n===============%n");
		
		return remopla;
	}
	
	/**
	 * Returns the name of the class where the analysis starts.
	 * 
	 * @return the name of the class where the analysis starts.
	 */
	public String getInitClassName() {
		
		return init.getClassName();
	}
	
	/**
	 * Analyzes the label and gets the corresponding source line number.
	 * 
	 * @param label the Remopla label.
	 * @return the source line number.
	 * @see TranslatorUtils#formatName(String, String, String, int)
	 */
	public int getSourceLine(String label) {
		
		ClassTranslator coll = included.get(TranslatorUtils.extractClassName(label));
		ModuleMaker module = coll.getModuleMaker(LabelUtils.trimOffset(label));
		if (!(module instanceof MethodTranslator))
			return -1;
		return ((MethodTranslator) module).getSourceLine(LabelUtils.getOffset(label));
	}
	
	/**
	 * Returns all included class translators.
	 * 
	 * @return all included class translators.
	 */
	public Collection<ClassTranslator> getClassTranslators() {
		return included.values();
	}
	
	/**
	 * Returns the class translator of the class specified by className.
	 * The method also handles the case where the class is a stub.
	 * 
	 * @param className the class name.
	 * @return the module collection of the class
	 */
	public ClassTranslator getClassTranslator(String className) {
		
		if (className == null) return null;
		
		ClassTranslator ct = included.get(className);
		if (ct != null) return ct;
		
		return included.get(StubManager.removeStub(className));
	}
	
	/**
	 * Returns <code>true</code> if the class specified by className
	 * contains static initializer.
	 * 
	 * @param className the class name.
	 * @return <code>true</code> if the class contains static initializer;
	 * 		<code>false</code> otherwise.
	 */
	public boolean containsClinit(String className) {
		
		ClassTranslator coll = included.get(className);
		if (coll == null) return false;
		
		return coll.contains(MethodTranslator.clinitOf(className));
	}
	
	/**
	 * Returns <code>true</code> if the translator contains the method 
	 * specified by formattedName, or <code>false</code> otherwise.
	 * 
	 * @param formattedName the name of the method.
	 * @return <code>true</code> iff the translator contains the method 
	 * 		specified by formattedName.
	 */
	public boolean contains(String formattedName) {
		
		ClassTranslator coll = included.get(TranslatorUtils.extractClassName(formattedName));
		if (coll == null) return false;
		
		return coll.contains(formattedName);
	}
	
	private HashMap<String, HashSet<ClassTranslator>> implementers = 
		new HashMap<String, HashSet<ClassTranslator>>();
	
	private void fillSubInterfaces(HashSet<String> interfaces) {
		
		// Considers all interfaces
		for (ClassTranslator coll : included.values()) {
			
			// Bypasses non-interface class
			if (!coll.isInterface()) continue;
			
			// Gets interfaces
			String[] supers = coll.getInterfaces();
			if (supers == null) continue;
			
			// Adds the this interface to set if it extends the existing interfaces
			for (int i = 0; i < supers.length; i++) {
				if (interfaces.contains(supers[i])) {
					if (interfaces.add(coll.getName()))
						fillSubInterfaces(interfaces);	// Finds subs of sub
				}
			}
		}
	}
	
	public Set<ClassTranslator> getImplementers(String name) {
		
		// Returns the set if already created
		HashSet<ClassTranslator> classes = implementers.get(name);
		if (classes != null) return classes;
		
		// Creates new set
		classes = new HashSet<ClassTranslator>();
		implementers.put(name, classes);
		
		// Finds all sub-interfaces of this interface
		HashSet<String> subs = new HashSet<String>();
		subs.add(name);
		fillSubInterfaces(subs);
		
		// Searches in every included class
		for (ClassTranslator coll : included.values()) {
			
			// Bypasses interface
			if (coll.isInterface()) continue;
			
			// Gets interfaces this class implements
			String[] interfaces = coll.getInterfaces();
			if (interfaces == null) continue;
			
			// Adds the class to set if it implements the interface or any of its subs
			for (int i = 0; i < interfaces.length; i++) {
				if (subs.contains(interfaces[i])) {
					classes.add(coll);
				}
			}
		}
		
		return classes;
	}
	
	public MethodArgument[] getMethodArguments(List<RawArgument> raws, List<Float> floats) {
		
		if (raws == null) return null;
		
		List<String> types = init.paramTypes;
		int nargs = types.size();
		log("nargs: %d%n", nargs);
		
		MethodArgument[] args = new MethodArgument[raws.size()];
		String methodName = init.getClassName() + "." + init.getMehtodName();
		
		// For each possible argument
		int i = 0;
		for (RawArgument raw : raws) {
			log("raw: %s%n", raw);
			MethodArgument arg = new MethodArgument(methodName, nargs);
			
			// For each parameter type
			int j = (init.isStatic()) ? 0 : 1;
			for (String type : types) {
				
				Object argj = raw.getLocalVariable(j);
				log("type: %s, j: %d, argj: %s%n", type, j, argj);
				
				switch (type.charAt(0)) {
				case 'D':
					if (floats == null) arg.add(type, argj);
					else arg.add(type, floats.get((Integer) argj));
					j++;
					break;
					
				case 'F':
					if (floats == null) arg.add(type, argj);
					else arg.add(type, floats.get((Integer) argj));
					break;
					
				case 'J':
					arg.add(type, argj);
					j++;
					break;
					
				case '[': {
					int ptr = (Integer) argj;
					int length = (Integer) raw.getHeapElement(ptr);
					log("length: %d%n", length);
					
					Number[] array = new Number[length];
					for (int k = 0; k < array.length; k++)
						array[k] = (Number) raw.getHeapElement(ptr + k + 1);
					arg.add(type, array);
					break;
				}
				default:
					arg.add(type, argj);
				}
				j++;
			}
			log("arg: %s%n%n", arg);
			args[i++] = arg;
		}
		
		return args;
	}
	
	/**
	 * MOVEME
	 * 
	 * @param s
	 * @param separator
	 * @return
	 */
	public static String toString(int[] s, String separator) {
		
		if (s == null) return null;
		if (s.length == 0) return "";
		if (s.length == 1) return Integer.toString(s[0]);
		
		StringBuilder out = new StringBuilder();
		out.append(s[0]);
		for (int i = 1; i < s.length; i++) {
			out.append(separator);
			out.append(s[i]);
		}
		
		return out.toString();
	}
	
	/**
	 * MOVEME
	 * 
	 * @param s
	 * @param separator
	 * @return
	 */
	public static String toString(String[] s, String separator) {
		
		if (s == null) return null;
		if (s.length == 0) return "";
		if (s.length == 1) return s[0];
		
		StringBuilder out = new StringBuilder(s[0]);
		for (int i = 1; i < s.length; i++) {
			out.append(separator);
			out.append(s[i]);
		}
		
		return out.toString();
	}
	
	/**
	 * Sets the verbosity level.
	 * 
	 * @param level the verbosity level.
	 */
	public static void setVerbosity(int level) {
		verbosity = level;
	}
	
	/**
	 * Logs translator information.
	 * 
	 * @param msg
	 * @param args
	 */
	public static void log(String msg, Object... args) {
		log(2, msg, args);
	}
	
	/**
	 * Logs translator information.
	 * 
	 * @param msg
	 * @param args
	 */
	public static void info(String msg, Object... args) {
		log(1, msg, args);
	}
	
	private static void log(int threshold, String msg, Object... args) {
		if (verbosity >= threshold)
			logger.fine(String.format(msg, args));
	}
	
	/**
	 * Includes every method in the classes that are statically reachable
	 * from the the class specified by <code>className</code>.
	 */
	private void includeAllMethodsFrom(String className) 
			throws IOException, InvalidByteCodeException {
		
		includeAllReachableClasses(className);
		updateSubClasses();
		updateInstanceFields();
		info("Included %d classes%n", included.size());
	}
	
	/**
	 * Includes all classes that are reachable from the class specified by
	 * <code>className</code>.
	 * 
	 * @param className
	 * @throws IOException
	 * @throws InvalidByteCodeException
	 */
	@SuppressWarnings("unchecked")
	private void includeAllReachableClasses(String className) 
			throws IOException, InvalidByteCodeException {
		
		if (className == null) 
			return;
		
		// Removes stub prefix
		log("Including class: %s%n", className);
		String extractedClassName = StubManager.removeStub(className);
		
		// Returns if already included
		if (included.containsKey(extractedClassName))
			return;
		
		// Includes array types
		if (extractedClassName.startsWith("[")) {
			included.put(extractedClassName, new ClassTranslator(included.size() + 1, extractedClassName));
			if (extractedClassName.charAt(1) == '[')
				includeAllReachableClasses(extractedClassName.substring(1));
//			includeAllReachableClasses(TranslatorUtils.removeArrayPrefix(extractedClassName));
			return;
		}
		
		// Includes stub first, if any
		ClassTranslator collection;
		if (StubManager.hasStub(extractedClassName)) {
			collection = StubManager.createClassStub(included.size() + 1, className);
			log("\tstub: %s%n", collection.getName());
		} 
		
		// Skips if ignored
		else if (isIgnored(className)) {
			log("Ignored: %s%n%n", className);
			return;
		} 
		
		// Includes the class
		else {
			collection = new ClassTranslator(included.size() + 1, 
					TranslatorUtils.findClassFile(className, searchPaths));
		}
		included.put(extractedClassName, collection);
		
		// Interfaces
		String[] interfaces = collection.getInterfaces();
		if (interfaces != null) {
			for (int i = 0; i < interfaces.length; i++) {
				// Recursively includes for each interface
				log("\tinterface: %s%n", interfaces[i]);
				includeAllReachableClasses(interfaces[i]);
			}
		}
		
		// Super class
		log("\tsuper: %s%n", collection.getSuperClassName());
		includeAllReachableClasses(collection.getSuperClassName());
		
		// Goes through all methods
		ClassFile cf = collection.getClassFile();
		MethodInfo[] methods = cf.getMethods();
		if (methods == null) return;
		for (int i = 0; i < methods.length; i++) {
			
			// Includes all classes in parameters
			log("\tmethod: %s%s%n", methods[i].getName(), methods[i].getDescriptor());
			List<String> params = LabelUtils.getParamTypes(methods[i].getDescriptor());
			for (String param : params) {
				if (param.startsWith("[")) {
					includeAllReachableClasses(param);
					continue;
				}
				if (param.startsWith("L")) {
					includeAllReachableClasses(param.substring(1, param.length() - 1));
					continue;
				}
			}
			
			CodeAttribute code = (CodeAttribute) methods[i].findAttribute(CodeAttribute.class);
			if (code == null) return;
			
			ModuleMaker module = new MethodTranslator(methods[i]);
			collection.add(module);
			
			CPInfo[] cp = cf.getConstantPool();
			List<AbstractInstruction> ainstList = 
				(List<AbstractInstruction>) ByteCodeReader.readByteCode(code.getCode());
			for (AbstractInstruction ainst : ainstList) {
				
				String ref = null;
				switch (ainst.getOpcode()) {
				
				case Opcodes.OPCODE_GETSTATIC:
				case Opcodes.OPCODE_PUTSTATIC:
				case Opcodes.OPCODE_INVOKEINTERFACE: 	
				case Opcodes.OPCODE_INVOKESPECIAL:
				case Opcodes.OPCODE_INVOKESTATIC:
				case Opcodes.OPCODE_INVOKEVIRTUAL:
					String[] refs = TranslatorUtils.getReferencedName(cp, ainst);
//					ref = TranslatorUtils.removeArrayPrefix(refs[0]);
					ref = refs[0];
					
				case Opcodes.OPCODE_ANEWARRAY:
					if (ref == null)
						ref = "[L" + TranslatorUtils.resolveClassName(cp, ainst) + ";";
					
				case Opcodes.OPCODE_CHECKCAST:
				case Opcodes.OPCODE_MULTIANEWARRAY:
				case Opcodes.OPCODE_NEW:
					if (ref == null) {
						ref = TranslatorUtils.resolveClassName(cp, ainst);
//						ref = TranslatorUtils.removeArrayPrefix(ref);
					}
					
				case Opcodes.OPCODE_NEWARRAY:
					if (ref == null) {
						ref = TranslatorUtils.getNewarrayType(ainst);
					}
					log("\t\tref: %s (from %s)%n", ref, className);
					includeAllReachableClasses(ref);
					break;
				}
			}
		}
		log("%n");
	}
	
	/**
	 * Updates subclass information for each class.
	 * 
	 * @set {@link ClassTranslator#setSubClasses(HashSet)}
	 */
	private void updateSubClasses() {
		
		// Collects subclass information
		log("updateSubClasses%n");
		HashMap<String, HashSet<ClassTranslator>> subs = 
				new HashMap<String, HashSet<ClassTranslator>>();
		for (ClassTranslator ct : included.values()) {
			
			// Bypasses if no super class
			String superName = ct.getSuperClassName();
			if (superName == null) continue;
			
			// Adds this collection to the set of subclasses of its super
			HashSet<ClassTranslator> set = subs.get(superName);
			if (set == null) {
				set = new HashSet<ClassTranslator>();
				subs.put(superName, set);
			}
			set.add(ct);
		}
		
		// Sets subclass information
		for (ClassTranslator ct : included.values()) {
			ct.setSubClasses(subs.get(ct.getName()));
		}
	}
	
	/**
	 * Updates instance fields for each class:
	 * (i) Copies all fields from its parent classes.
	 * (ii) Assigns a unique id to each field. The id begins with one in case
	 * of non-multithreading; and two in multithreading case.
	 */
	private void updateInstanceFields() {
		
		HashSet<ClassTranslator> updated 
				= new HashSet<ClassTranslator>((int) (1.4*included.size()));
		for (ClassTranslator ct : included.values()) {
			HashMap<String, FieldTranslator> fields = getSuperInstanceFields(updated, ct);
			int id = 1;
			for (FieldTranslator field : fields.values()) {
				field.setId(id++);
			}
		}
	}
	
	/**
	 * Recursively collects all instance fields of this class 
	 * and its parent classes.
	 * 
	 * @param updated the set of class that already collected.
	 * @param ct the class translator where the fields to be collected.
	 * @return the field map.
	 */
	private HashMap<String, FieldTranslator> getSuperInstanceFields(
			HashSet<ClassTranslator> updated, ClassTranslator ct) {
		
		if (updated.contains(ct)) return ct.instanceFields;
		
		updated.add(ct);
		HashMap<String, FieldTranslator> thisFields = ct.instanceFields;
		String superName = ct.getSuperClassName();
		ClassTranslator superct = getClassTranslator(superName);
		if (superct == null) return thisFields;
		
		HashMap<String, FieldTranslator> superFields 
				= getSuperInstanceFields(updated, superct);
		for (Map.Entry<String, FieldTranslator> superEntry : superFields.entrySet()) {
			thisFields.put(superEntry.getKey(), (FieldTranslator) superEntry.getValue().clone());
		}
		return thisFields;
	}
	
	private boolean ignoreJavaLibrary = false;
	
	public void setIgnoreJavaLibrary(boolean ignore) {
		ignoreJavaLibrary = ignore;
	}
	
	private boolean isIgnored(String className) {
		return isPackageIgnored(className);
	}
	
	private static final String[] ignoredPackages = new String[] { 
		"java/io", "org/gjt/jclasslib", "sun"/*, "de/tum/in/jmoped/translator"*/ };
	
//	static {
//		ignoredPackages = new HashSet<String>();
//		ignoredPackages.add("java/awt");
//		ignoredPackages.add("java/io");
//	}
	
	private boolean isPackageIgnored(String className) {
		
		if (className.equals("java/lang/Object"))
			return false;
		
		for (int i = 0; i < ignoredPackages.length; i++) {
			if (className.startsWith(ignoredPackages[i])) 
				return true;
		}
		return false;
		
//		int index = className.lastIndexOf('/');
//		if (index == -1) return false;
//		String packageName = className.substring(0, index);
//		return ignoredPackages.contains(packageName);
	}
	
	private void logIncluded() {
		
		log("#included: %s%n", included.size());
		log("included:  %s%n", included);
		log("%n*** Collections ***%n");
		for (ClassTranslator coll : included.values()) {
			log("collection %d: %s, super: %s, subs: %s%n", coll.getId(), 
					coll.getName(), coll.getSuperClassName(), coll.getSubClasses());
			for (Map.Entry<String, FieldTranslator> entry : coll.staticFields.entrySet()) {
				log("\tstatic field: %s (%s)%n", entry.getKey(), entry.getValue());
			}
			for (Map.Entry<String, FieldTranslator> entry : coll.instanceFields.entrySet()) {
				log("\tfield: %s (%s)%n", entry.getKey(), entry.getValue());
			}
			for (ModuleMaker module : coll.getModuleMakers())
				if (module != null)
					log("\tmodule: %s%n", module.getName());
		}
		log("*******************%n%n");
	}
	
	/**
	 * Returns the string representation of this translator.
	 * 
	 * @return the string representation of this translator.
	 */
	public String toString() {
		
		StringBuilder s = new StringBuilder();
		s.append(String.format("#included: %s%n", included.size()));
		s.append(String.format("included:  %s%n", included));
		s.append(String.format("%n*** Collections ***%n"));
		for (ClassTranslator coll : included.values()) {
			s.append(String.format("collection %d: %s, super: %s, subs: %s%n", coll.getId(), 
					coll.getName(), coll.getSuperClassName(), coll.getSubClasses()));
			for (Map.Entry<String, FieldTranslator> entry : coll.staticFields.entrySet()) {
				s.append(String.format("\tstatic field: %s (%s)%n", entry.getKey(), entry.getValue()));
			}
			for (Map.Entry<String, FieldTranslator> entry : coll.instanceFields.entrySet()) {
				s.append(String.format("\tfield: %s (%s)%n", entry.getKey(), entry.getValue()));
			}
			for (ModuleMaker module : coll.getModuleMakers())
				if (module != null)
					s.append(String.format("\tmodule: %s%n", module.getName()));
		}
		s.append(String.format("*******************%n%n"));
		
		return s.toString();
	}
	
	/**
	 * Prints the translator usage.
	 */
	private static void usage() {
		String newline = System.getProperty("line.separator");
		System.err.println(newline 
				+ "Java-bytecode-to-Remopla translator" + newline + newline
				+ "Parameters: [package/]class.method bits heapsize [path1 ...]" + newline + newline
				+ "Examples: \"sort/Quicksort.test([I)V\" 4 15 \"/home/suwimont/examples/bin\"" + newline
		);
		System.exit(1);
	}
	
	/**
	 * Starts the translator from command line.
	 * 
	 * @param args the arguments: [method, bits, heapsize, searchpaths]
	 */
	public static void main(String[] args) {
		// Needs at least 3 arguments
		if (args.length < 3) {
			usage();
		}
		
		// From the third argument are search paths
		ArrayList<String> paths = new ArrayList<String>();
		for (int i = 3; i < args.length; i++)
			paths.add(args[i]);
		
		// Gets the classpath from the file name classpath (if any)
		File cp = new File("classpath");
		if (cp.exists()) {
			try {
				BufferedReader reader = new BufferedReader(new FileReader(cp));
				String line;
				while ((line = reader.readLine()) != null) {
					String[] split = line.split(File.pathSeparator);
					for (String s : split)
						paths.add(s);
				}
			} catch (Exception e) {
				// Falls through
			}
		}
		
		try {
			// Decodes the method name
			int dot = args[0].lastIndexOf('.');
			int left = args[0].indexOf('(');
			if (dot < 0 || left < 0 || dot >= left)
				usage();
			
			// Constructs a translator
			String className = args[0].substring(0, dot);
			String methodName = args[0].substring(dot + 1, left);
			String methodDesc = args[0].substring(left);
			Translator translator = new Translator(className, 
					paths.toArray(new String[paths.size()]), 
					methodName, methodDesc);
			
			// Translate to Remopla
			int bits = Integer.parseInt(args[1]);
			int heapSize = Integer.parseInt(args[2]);
			Remopla remopla = translator.translate(bits, heapSize, true, 1, false);
			System.out.println(remopla.toMoped());
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}
