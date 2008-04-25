package de.tum.in.jmoped.translator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import org.gjt.jclasslib.structures.CPInfo;
import org.gjt.jclasslib.structures.ClassFile;
import org.gjt.jclasslib.structures.InvalidByteCodeException;
import org.gjt.jclasslib.structures.MethodInfo;
import org.gjt.jclasslib.structures.attributes.LocalVariableTableEntry;
import org.gjt.jclasslib.structures.elementvalues.ElementValue;

import de.tum.in.jmoped.annotation.AnnotationUtils;
import de.tum.in.jmoped.underbone.ExprSemiring.CompType;
import de.tum.in.jmoped.underbone.ExprSemiring;
import de.tum.in.jmoped.underbone.ExprType;
import de.tum.in.jmoped.underbone.LabelUtils;
import de.tum.in.jmoped.underbone.Module;

import static de.tum.in.jmoped.underbone.ExprType.*;
import static de.tum.in.jmoped.underbone.ExprSemiring.CompType.*;
import static de.tum.in.jmoped.underbone.ExprSemiring.ArithType.*;

/**
 * The <code>MethodWrapper</code> wraps the initial method by creating
 * a new method which calls the initial method with nondeterministic
 * argument values.
 * 
 * @author suwimont
 *
 */
public class MethodWrapper {

	private Translator translator;
	private String className;
	private String methodName;
	private String methodDesc;
	
	private MethodInfo mi;
	private String name;
	
	public final static String INIT = "init";
	private int initcount = 0;
	
	private HashMap<Integer, Integer> lv = new HashMap<Integer, Integer>();
	private HashMap<Integer, Integer> amax = new HashMap<Integer, Integer>();
	
	List<String> paramTypes;
	private HashMap<Integer, Range> ranges = new HashMap<Integer, Range>();
	private HashMap<Integer, Range> aranges = new HashMap<Integer, Range>();
	
	public MethodWrapper(Translator translator, String className, 
			String methodName, String methodDesc) 
			throws InvalidByteCodeException {
		
		log("*** Creating method wrapper for %s.%s%s ***%n", 
				className, methodName, methodDesc);
		
		methodDesc = methodDesc.replaceAll("QInteger;", "Ljava/lang/Integer;");
		
		String unsupported = methodDesc.replaceAll("Ljava/lang/Integer;", "");
		if (unsupported.contains("Q") || unsupported.contains("L")) {
			throw new TranslatorError("The initial method %s " +
					"must have parameters of primitive types.", methodDesc);
		}
		
		this.translator = translator;
		this.className = className;
		this.methodName = methodName;
		this.methodDesc = methodDesc;
		
		// Finds the method info
		ClassFile cf = translator.getClassTranslator(className).getClassFile();
		mi = cf.getMethod(methodName, methodDesc);
		if (mi == null) {
			List<String> params = LabelUtils.getParamTypes(methodDesc);
			MethodInfo[] methods = cf.getMethods();
			for (int i = 0; i < methods.length; i++) {
				
				// Continues if names not matched
				if (!methods[i].getName().equals(methodName))
					continue;
				
				// Continues if descriptors not matched
				List<String> candidates = LabelUtils.getParamTypes(
						methods[i].getDescriptor());
				if (!TranslatorUtils.matchParams(candidates, params))
					continue;
				
				mi = methods[i];
				break;
			}
			
			// Throws error if not found
			if (mi == null) {
				throw new TranslatorError("Method not found: %s.%s%s", 
						className, methodName, methodDesc);
			}
		}
		
		name = getFreshLabel();
		initRanges();
		
		log("*******************************************%n%n");
	}
	
	public String getClassName() {
		return className;
	}
	
	public String getMehtodName() {
		return methodName;
	}
	
	public String getName() {
		return name;
	}
	
	public boolean isStatic() {
		return TranslatorUtils.isStatic(mi.getAccessFlags());
	}
	
	/**
	 * Estimates the heap sizes required by the method wrapper.
	 * The method only works with deterministic (1-dim) array lengths;
	 * otherwise <code>null</code> is returned.
	 * 
	 * @param bits the default number of bits
	 * @return the array of heap sizes required by this method wrapper
	 */
	public long[] estimateHeapSizes(int bits) {
		
		ArrayList<Long> sizes = new ArrayList<Long>();
		int i = 0;
		for (String param : paramTypes) {
			
			if (param.charAt(0) != '[' || param.charAt(1) == '[') continue;
			
			// Only deterministic array length
			Range range = ranges.get(i);
			if (range == null || range.min != range.max) 
				return null;
			checkRange(range, bits);
			int length = range.min;
			
			// Object type
			long defaultsize = (long) (1 << bits);
			sizes.add(defaultsize);
			
			// Array length
			sizes.add(new Long(2*(length + 1))); // + 1 because size tells no. of possibilities
			
			// Owner & counter
			if (translator.multithreading() && translator.symbolic()) {
				sizes.add(defaultsize);
				sizes.add(defaultsize);
			}
			
			// Array elements
			range = aranges.get(i);
			checkRange(range, bits);
			long size = (range == null) ? defaultsize : range.size();
			for (int j = 0; j < length; j++)
				sizes.add(size);
				
			i++;
		}
		
		// Converts List<Long> to long[]
		if (sizes.isEmpty()) return null;
		long[] out = new long[sizes.size()];
		i = 0;
		for (Long size : sizes) {
			out[i++] = size.longValue();
		}
		
		return out;
	}
	
	/**
	 * Initializes the local variables: paramTypes, ranges, aranges.
	 * 
	 * @throws InvalidByteCodeException
	 */
	private void initRanges() throws InvalidByteCodeException {
		
		log("Entering initRanges()%n");
		
//		ClassFile cf = translator.getClassTranslator(className).getClassFile();
//		MethodInfo mi = cf.getMethod(methodName, methodDesc);
		ClassFile cf = mi.getClassFile();
		LocalVariableTableEntry[] lvtEntries = TranslatorUtils.getLocalVariableTableEntries(mi);
		ElementValue[] annotatedBits = AnnotationUtils.getAnnotatedBits(mi);
		ElementValue[] annotatedRange = AnnotationUtils.getAnnotatedRange(mi);
		CPInfo[] cp = cf.getConstantPool();
		paramTypes = LabelUtils.getParamTypes(methodDesc);
		int i = -1;
		String paramName;
		Integer[] minmax;
		Integer bits;
		for (String param : paramTypes) {
			
			i++;
			log("(%d) param: %s%n", i, param);
			paramName = TranslatorUtils.getConstantUtf8(cp, lvtEntries[i].getNameIndex());
			minmax = AnnotationUtils.getMinMax(annotatedRange, cf, paramName, i);
			if (minmax != null) {
				ranges.put(i, new Range(minmax));
			} else {
				bits = AnnotationUtils.getBits(annotatedBits, cf, paramName, i);
				if (bits != null) {
					ranges.put(i, new Range(bits));
				} else {
					log("No annotaion found%n");
				}
			}
			
			// The following is for array parameter only
			if (param.charAt(0) != '[') {
				continue;
			}
			
			minmax = AnnotationUtils.getArrayMinMax(annotatedRange, mi, paramName);
			if (minmax != null) {
				aranges.put(i, new Range(minmax));
			} else {
				bits = AnnotationUtils.getArrayBits(annotatedBits, mi, paramName);
				if (bits != null) {
					aranges.put(i, new Range(bits));
				}
			}
		}
		
		log("ranges: %s%n", ranges);
		log("aranges: %s%n", aranges);
		log("Leaving initRanges()%n");
	}
	
	private static void checkRange(Range range, int bits) {
		
		if (range == null || range.valid(bits)) return;
		
		String t = (range.bits == -1) 
				? String.format("range [%d,%d]", range.min, range.max) 
				: String.format("bits %d", range.bits);
		String s = String.format("The specified %s " +
				"is too large for integers with %d bits.", 
				t, bits);
		throw new IllegalArgumentException(s);
	}
	
	/**
	 * Wraps the selected module where the analysis should start with
	 * non-deterministic arguments.
	 * 
	 * 
	 * @param bits
	 * @param nondet
	 * @return
	 * @throws InvalidByteCodeException
	 */
	public Module wrap(int bits, boolean nondet) throws InvalidByteCodeException {
		
		int lvcount = 0;
		int i = 0;
		String from = name, to;
		Stack<String> flabels = new Stack<String>();
		Stack<String> blabels = new Stack<String>();
		ArrayList<Integer> maxs = new ArrayList<Integer>();
		ExprSemiring d;
		
		Module init = new Module(name, new boolean[0], true, paramTypes.size(), 1);
		
		// TODO Calls static initializers of parameters?
		
		// Calls static initializer (if any)
		if (translator.containsClinit(className)) {
			to = getFreshLabel();
			init.addRule(from, new ExprSemiring(INVOKE, new ExprSemiring.Invoke()), 
					TranslatorUtils.formatName(MethodTranslator.getClinitName(className), 0), 
					to);
			from = to;
		}
		
		// Creates "this" if the starting method is not static
		ClassTranslator ct = translator.getClassTranslator(className);
		if (!isStatic()) {
			
			// Ensures one more stack depth for "NEW"
			init.ensureMaxStack(paramTypes.size() + 1);
			
			to = getFreshLabel();
			d = new ExprSemiring(
					NEW, 
					new ExprSemiring.New(
							ct.getId(), 
							ct.size() + translator.getObjectBaseId()));
			init.addRule(from, d, to);
			from = to;
		}
		
		for (String param : paramTypes) {
			
			lv.put(i, lvcount++);
			
			// Handles java/lang/Integer as parameters
			if (param.equals("Ljava/lang/Integer;")) {
				
				// Ensures the stack depth to three
				init.ensureMaxStack(3);
				
				to = getFreshLabel();
				ClassTranslator ict = translator.getClassTranslator("java/lang/Integer");
				if (ict == null) {
					System.err.println("Fatal error: java/lang/Integer not found");
				}
				d = new ExprSemiring(
						ExprType.NEW, 
						new ExprSemiring.New(
								ict.getId(), 
								translator.getObjectBaseId() + ict.size()));
				init.addRule(from, d, to);
				
				from = to; to = getFreshLabel();
				d = new ExprSemiring(DUP, ExprSemiring.DupType.DUP);
				init.addRule(from, d, to);
				
				from = to;
			}
			
			to = getFreshLabel();
			Range range = ranges.get(i);
			checkRange(range, bits);
			if (nondet) {
				// Pushes non-deterministic value
				if (range == null) {
					d = new ExprSemiring(PUSH, new ExprSemiring.Value());
				} else {
					int min = range.min;
					if (min < 0 && param.charAt(0) == '[') min = 0;
					d = new ExprSemiring(PUSH, new ExprSemiring.Value(min, 1, range.max));
				}
				init.addRule(from, d, to);
			} else {
				// Remembers the maximum number of this parameter
				int min, max;
				if (range != null) {
					min = range.min;
					max = range.max + 1;
				}
				else {
					min = (int) -Math.pow(2, bits-1);
					if (min == -1) min = 0;
					max = (int) Math.pow(2, bits-1);
					if (max == 1) max = 2;
				}
				maxs.add(max);
				
				// Pushes the minimum number
				if (min < 0 && param.charAt(0) == '[') min = 0;
				d = new ExprSemiring(ExprType.PUSH, new ExprSemiring.Value(min));
				init.addRule(from, d, to);
				
				// Updates labels
				from = to; to = getFreshLabel();
				
				// Stores the minimum number to variable i
				d = new ExprSemiring(ExprType.STORE, lv.get(i));
				init.addRule(from, d, to);
				
				// Jumps forwards and remembers the jumped label
				from = to; to = getFreshLabel();
				flabels.push(to);
				init.addRule(from, ONE, to);
				
				// Updates labels and remembers backwards jump label 
				// (the next one after goto)
				to = getFreshLabel();
				blabels.push(to);
			}
			
			// Calls initializer of Integer
			if (param.equals("Ljava/lang/Integer;")) {
				from = to; to = getFreshLabel();
				d = new ExprSemiring(INVOKE, 
						new ExprSemiring.Invoke(false, new boolean[] { false, false }));
				init.addRule(from, d, "java/lang/Integer.<init>(I)V0", to);
			}
				
			// Continues if the parameter is not array
			if (param.charAt(0) != '[') {
				from = to;
				i++;
				continue;
			}
						
			// Gets array bits
			range = aranges.get(i);
			checkRange(range, bits);
			
			if (nondet) {
				// Updates labels
				from = to; to = getFreshLabel();
				
				// Creates array
				ExprSemiring.Value v;
				if (range != null) {
					v = new ExprSemiring.Value(range.min, 1, range.max);
					d = new ExprSemiring(NEWARRAY, new ExprSemiring.Newarray(v));
				} else {
					v = new ExprSemiring.Value();
					d = new ExprSemiring(NEWARRAY, new ExprSemiring.Newarray(v));
				}
				init.addRule(from, d, to);
			} else {
				// We need 3 more local variables for each array
				lvcount += 3;
				
				// Gets max value of array elements
				int max;
				if (range != null) {
					max = range.max;
				} else {
					max = (int) Math.pow(2, bits-1) - 1;
					if (max == 0) max = 1;
				}
				amax.put(i, max);
				
				from = to; to = getFreshLabel();
				init.addRule(from, HEAPRESET, to);
				
				from = to; to = getFreshLabel();
				init.addRule(from, LOAD, lv.get(i), to);
				
				// Pushes the min value of array elements
				from = to; to = getFreshLabel();
				int min;
				if (range != null) {
					min = range.min;
				} else {
					min = (int) -Math.pow(2, bits-1);
					if (min == -1) min = 0;
				}
				init.addRule(from, 
						NEWARRAY, new ExprSemiring.Newarray(new ExprSemiring.Value(min)), 
						to);
					
				from = to; to = getFreshLabel();
				init.addRule(from, STORE, lv.get(i)+1, to);
				
				from = to; to = getFreshLabel();
				init.addRule(from, PUSH, new ExprSemiring.Value(1), to);
				
				from = to; to = getFreshLabel();
				init.addRule(from, STORE, lv.get(i)+2, to);
				
				from = to; to = getFreshLabel();
				init.addRule(from, ONE, to);
				flabels.push(to);
				
				to = getFreshLabel();
				blabels.push(to);
			}
			from = to;
			i++;
		}
		to = getFreshLabel();
		if (!nondet) {
			// Pushes the current argument values
			i = 0;
			for (String param : paramTypes) {
				int v = lv.get(i++);
				if (param.charAt(0) != '[') {
					init.addRule(from, LOAD, v, to);
					from = to; to = getFreshLabel();
					continue;
				}
				
				v++;
				init.addRule(from, LOAD, v, to);
				from = to; to = getFreshLabel();
			}
		}
		
		if (!nondet) {
			init.addRule(from, HEAPSAVE, to);
			from = to; to = getFreshLabel();
		}
		
		// Invokes the method
		init.addRule(from, 
				new ExprSemiring(INVOKE, new ExprSemiring.Invoke(isStatic(), TranslatorUtils.doubleParams(isStatic(), methodDesc), true)), 
				TranslatorUtils.formatName(className, methodName, methodDesc, 0),
				to);
		if (nondet) return init;
		
		from = to; to = getFreshLabel();
		init.addRule(from, HEAPRESTORE, to);
		
		//TODO Pushes "this" back
		if (!isStatic()) {
			from = to; to = getFreshLabel();
			init.addRule(from, PUSH, new ExprSemiring.Value(1), to);
		}
		
		for (i = paramTypes.size()-1; i >= 0; i--) {
			
			if (paramTypes.get(i).charAt(0) == '[') {
				
				from = to; to = getFreshLabel();
				init.addRule(from, LOAD, lv.get(i)+1, to);
				
				from = to; to = getFreshLabel();
				init.addRule(from, ARRAYLENGTH, to);
				
				from = to; to = getFreshLabel();
				String jump = to;
				init.addRule(from, IF, new ExprSemiring.If(NE), jump);
				to = getFreshLabel();
				init.addRule(from, IF, new ExprSemiring.If(EQ), to);
				
				from = to; to = getFreshLabel();
				init.addRule(from, PUSH, new ExprSemiring.Value(0), to);
				
				from = to; to = jump;
				init.addRule(from, STORE, lv.get(i)+2, to);
				
				from = jump; to = getFreshLabel();
				init.addRule(from, LOAD, lv.get(i)+1, to);
				
				from = to; to = getFreshLabel();
				init.addRule(from, ARRAYLENGTH, to);
				
				from = to; to = getFreshLabel();
				init.addRule(from, PUSH, new ExprSemiring.Value(1), to);
				
				from = to; to = getFreshLabel();
				init.addRule(from, ARITH, SUB, to);
				
				from = to; to = getFreshLabel();
				init.addRule(from, STORE, lv.get(i)+3, to);
				
				from = to; to = getFreshLabel();
				String l64 = to;
				init.addRule(from, ONE, to);
				
				from = getFreshLabel(); to = getFreshLabel();
				String l33 = from;
				init.addRule(from, LOAD, lv.get(i)+1, to);
				
				from = to; to = getFreshLabel();
				init.addRule(from , LOAD, lv.get(i)+3, to);
				
				from = to; to = getFreshLabel();
				init.addRule(from, ARRAYLOAD, to);
				
				from = to; to = getFreshLabel();
				init.addRule(from, PUSH, new ExprSemiring.Value(amax.get(i)), to);
				
				from = to; to = getFreshLabel();
				String l51 = to;
				init.addRule(from, IFCMP, GE, to);
				to = getFreshLabel();
				init.addRule(from, IFCMP, LT, to);
				
				from = to; to = getFreshLabel();
				init.addRule(from, LOAD, lv.get(i)+1, to);
				
				from = to; to = getFreshLabel();
				init.addRule(from , LOAD, lv.get(i)+3, to);
				
				from = to; to = getFreshLabel();
				init.addRule(from , DUP, ExprSemiring.DupType.DUP2, to);
				
				from = to; to = getFreshLabel();
				init.addRule(from, ARRAYLOAD, to);
				
				from = to; to = getFreshLabel();
				init.addRule(from, PUSH, new ExprSemiring.Value(1), to);
				
				from = to; to = getFreshLabel();
				init.addRule(from, ARITH, ADD, to);
				
				from = to; to = getFreshLabel();
				init.addRule(from, ARRAYSTORE, to);
				
				from = to; to = flabels.peek();
				init.addRule(from, ONE, to);
				
				from = l51; to = getFreshLabel();
				init.addRule(from, LOAD, lv.get(i)+1, to);
				
				from = to; to = getFreshLabel();
				init.addRule(from , LOAD, lv.get(i)+3, to);
				
				from = to; to = getFreshLabel();
				init.addRule(from, PUSH, new ExprSemiring.Value(0), to);
				
				from = to; to = getFreshLabel();
				init.addRule(from, ARRAYSTORE, to);
				
				from = to; to = getFreshLabel();
				init.addRule(from , LOAD, lv.get(i)+3, to);
				
				from = to; to = getFreshLabel();
				jump = to;
				init.addRule(from, IF, new ExprSemiring.If(NE), jump);
				to = getFreshLabel();
				init.addRule(from, IF, new ExprSemiring.If(EQ), to);
				
				from = to; to = getFreshLabel();
				init.addRule(from, PUSH, new ExprSemiring.Value(0), to);
				
				from = to; to = jump;
				init.addRule(from, STORE, lv.get(i)+2, to);
				
				from = jump; to = l64;
				init.addRule(from, INC, new ExprSemiring.Inc(lv.get(i)+3, -1), to);
				
				from = l64; to = getFreshLabel();
				init.addRule(from , LOAD, lv.get(i)+3, to);
				
				from = to; to = flabels.pop();
				init.addRule(from, IF, new ExprSemiring.If(GE), l33);
				init.addRule(from, IF, new ExprSemiring.If(LT), to);
				
				from = to; to = getFreshLabel();
				init.addRule(from , LOAD, lv.get(i)+2, to);
				
				from = to; to = getFreshLabel();
				init.addRule(from, IF, new ExprSemiring.If(NE), blabels.pop());
				init.addRule(from, IF, new ExprSemiring.If(EQ), to);
			}
			
			// Increments the variable i
			from = to; to = flabels.pop();
			d = new ExprSemiring(ExprType.INC, new ExprSemiring.Inc(lv.get(i), 1));
			init.addRule(from, d, to);
			
			// Pushes the variable i
			from = to; to = getFreshLabel();
			d = new ExprSemiring(ExprType.LOAD, lv.get(i));
			init.addRule(from, d, to);
			
			// Pushes the maximum value of parameter i
			from = to; to = getFreshLabel();
			d = new ExprSemiring(ExprType.PUSH, new ExprSemiring.Value(maxs.get(i)));
			init.addRule(from, d, to);
			
			// Jumps back if the variable i is less than the maximum value
			from = to; to = blabels.pop();
			d = new ExprSemiring(ExprType.IFCMP, CompType.LT);
			init.addRule(from, d, to);
			
			// Falls through if the variable i is not less than the maximum value
			to = getFreshLabel();
			d = new ExprSemiring(ExprType.IFCMP, CompType.GE);
			init.addRule(from, d, to);
		}
		
		init.setMaxLocals(lvcount);
		int paramnum = paramTypes.size();
		if (nondet) {
			init.setMaxStack(paramnum);
		} else {
			if (paramnum < 4) init.setMaxStack(4);
			else init.setMaxStack(paramnum);
		}
		return init;
	}
	
	private String getFreshLabel() {
		
		return INIT + initcount++;
	}
	
	private static void log(String msg, Object... args) {
		
		Translator.log(msg, args);
	}
	
	private class Range {
		
		int bits = -1;
		int min;
		int max;
		
		Range(Integer[] minmax) {
			
			this.min = minmax[0];
			this.max = minmax[1];
		}
		
		Range(int bits) {
			
			this.bits = bits;
			if (bits == 1) {
				this.min = 0;
				this.max = 1;
			} else {
				this.min = (int) -Math.pow(2, bits-1);
				this.max = (int) (Math.pow(2, bits-1) - 1);
			}
		}
		
		/**
		 * max - min + 1
		 * 
		 * @return
		 */
		long size() {
			
			return max - min + 1;
		}
		
		/**
		 * Returns <code>true</code> if [-2^(b-1), 2^(b-1)-1] resides in
		 * this range.
		 * 
		 * @param b
		 * @return
		 */
		boolean valid(int b) {
			
			if (bits > -1) return b >= bits;
			
			if (min < (int) -Math.pow(2, b-1)) return false;
			return max <= (int) (Math.pow(2, b-1) - 1);
		}
		
		public String toString() {
			return String.format("[%d,%d]", min, max);
		}
	}
}
