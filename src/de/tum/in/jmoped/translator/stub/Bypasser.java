package de.tum.in.jmoped.translator.stub;

import static de.tum.in.jmoped.underbone.ExprType.ARITH;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import de.tum.in.jmoped.translator.Translator;
import de.tum.in.jmoped.translator.TranslatorError;
import de.tum.in.jmoped.translator.TranslatorUtils;
import de.tum.in.jmoped.underbone.ExprSemiring;
import de.tum.in.jmoped.underbone.ExprType;
import de.tum.in.jmoped.underbone.Module;
import de.tum.in.jmoped.underbone.ExprSemiring.ArithType;
import de.tum.in.jmoped.underbone.ExprSemiring.CategoryType;

/**
 * Bypasses some method calls with hard-wired Remopla.
 * 
 * @author suwimont
 *
 */
public class Bypasser {

	/**
	 * Maps the class names to be bypassed.
	 * Keys are classes to be bypassed.
	 * If the corresponding set is empty then the whole class is bypassed;
	 * otherwise only the methods in the set are bypassed.
	 */
	private static HashMap<String, HashSet<String>> m 
			= new HashMap<String, HashSet<String>>();
	
	static {
		m.put("Stub", new HashSet<String>(1));
		
		HashSet<String> set = new HashSet<String>(2);
		set.add(TranslatorUtils.formatName("java/lang/Math", "random", "()V"));
		m.put("java/lang/Math", set);
	}
	
	public static boolean isBypassed(String[] called) {
		HashSet<String> set = m.get(called[0]);
		if (set == null) return false;
		if (set.isEmpty()) return true;
		return set.contains(called[0]);	
	}
	
	/**
	 * Bypasses the call.
	 * 
	 * @param module
	 * @param translator
	 * @param called
	 * @param label
	 * @param nextlabel
	 * @return <code>true</code> if the call is bypassed;
	 * 			or <code>false</code> otherwise.
	 */
	public static boolean bypass(Module module,
			Translator translator, String[] called,
			String label, String nextlabel) {
		
		if (!isBypassed(called)) return false;
		
		if (called[0].equals("Stub")) {
			invokestaticStub(module, translator, called, label, nextlabel);
		} else if (called[0].equals("java/lang/Math")) {
			invokestaticMath(module, translator, called, label, nextlabel);
		}
		
		return true;
	}
	
	private static void invokestaticStub(Module module,
			Translator translator, String[] called,
			String label, String nextlabel) {
		
		if (called[1].equals("nint")) {
			
			module.addRule(label, 
					new ExprSemiring(ARITH, ArithType.NDT, CategoryType.ONE), 
					nextlabel);
			return;
		}
		
		throw new TranslatorError("Unimplemented case: invokestatic " 
				+ Arrays.toString(called));
	}
	
	private static void invokestaticMath(Module module,
			Translator translator, String[] called,
			String label, String nextlabel) {
		
		if (called[1].equals("random")) {
			
			module.addRule(label, 
					new ExprSemiring(ExprType.PUSH, 
							new ExprSemiring.Value(CategoryType.TWO, 
									new Float(0), null, new Float(1))
					), nextlabel);
			return;
		}
		
		throw new TranslatorError("Unimplemented case: invokestatic " 
				+ Arrays.toString(called));
	}
}
