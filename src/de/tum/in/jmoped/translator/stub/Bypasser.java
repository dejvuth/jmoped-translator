package de.tum.in.jmoped.translator.stub;

import static de.tum.in.jmoped.underbone.ExprType.ARITH;
import static de.tum.in.jmoped.underbone.ExprType.HEAPLOAD;
import static de.tum.in.jmoped.underbone.ExprType.JUMP;
import static de.tum.in.jmoped.underbone.ExprType.NOTIFY;
import static de.tum.in.jmoped.underbone.ExprType.POPPUSH;
import static de.tum.in.jmoped.underbone.ExprType.WAITINVOKE;
import static de.tum.in.jmoped.underbone.ExprType.WAITRETURN;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import de.tum.in.jmoped.translator.ClassTranslator;
import de.tum.in.jmoped.translator.FieldTranslator;
import de.tum.in.jmoped.translator.MethodTranslator;
import de.tum.in.jmoped.translator.Translator;
import de.tum.in.jmoped.translator.TranslatorError;
import de.tum.in.jmoped.translator.TranslatorUtils;
import de.tum.in.jmoped.underbone.ExprSemiring;
import de.tum.in.jmoped.underbone.ExprType;
import de.tum.in.jmoped.underbone.Module;
import de.tum.in.jmoped.underbone.expr.Arith;
import de.tum.in.jmoped.underbone.expr.Category;
import de.tum.in.jmoped.underbone.expr.Condition;
import de.tum.in.jmoped.underbone.expr.Field;
import de.tum.in.jmoped.underbone.expr.If;
import de.tum.in.jmoped.underbone.expr.Invoke;
import de.tum.in.jmoped.underbone.expr.Jump;
import de.tum.in.jmoped.underbone.expr.NotifyType;
import de.tum.in.jmoped.underbone.expr.Poppush;
import de.tum.in.jmoped.underbone.expr.Value;

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
		
		HashSet<String> set = new HashSet<String>(3, 0.95f);
		set.add(TranslatorUtils.formatName("java/lang/Class", "getEnumConstants", "()[Ljava/lang/Object;"));
		set.add(TranslatorUtils.formatName("java/lang/Class", "getSuperclass", "()Ljava/lang/Class;"));
		m.put("java/lang/Class", set);
		
		set = new HashSet<String>(2);
		set.add(TranslatorUtils.formatName("java/lang/Math", "random", "()D"));
		m.put("java/lang/Math", set);
		
		set = new HashSet<String>(5, 0.95f);
		set.add(TranslatorUtils.formatName("java/lang/Object", "getClass", "()Ljava/lang/Class;"));
		set.add(TranslatorUtils.formatName("java/lang/Object", "wait", "()V"));
		set.add(TranslatorUtils.formatName("java/lang/Object", "notify", "()V"));
		set.add(TranslatorUtils.formatName("java/lang/Object", "notifyAll", "()V"));
		m.put("java/lang/Object", set);
	}
	
	/**
	 * Returns <code>true</code> if the method specified by <code>called</code>
	 * is bypassed.
	 * 
	 * @param called the method.
	 * @return <code>true</code> if bypassed.
	 */
	public static boolean isBypassed(String[] called) {
		HashSet<String> set = m.get(called[0]);
		if (set == null) return false;
		if (set.isEmpty()) return true;
		return set.contains(TranslatorUtils.formatName(called));	
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
		
		log("\tbypassing: %s%n", Arrays.toString(called));
		if (called[0].equals("Stub")) {
			bypassStub(module, translator, called, label, nextlabel);
		} else if (called[0].equals("java/lang/Class")) {
			bypassClass(module, translator, called, label, nextlabel);
		} else if (called[0].equals("java/lang/Math")) {
			bypassMath(module, translator, called, label, nextlabel);
		} else if (called[0].equals("java/lang/Object")) {
			bypassObject(module, translator, called, label, nextlabel);
		}
		
		return true;
	}
	
	private static void bypassStub(Module module,
			Translator translator, String[] called,
			String label, String nextlabel) {
		
		if (called[1].equals("nint")) {
			module.addRule(label, 
					new ExprSemiring(ARITH, new Arith(Arith.NDT, Category.ONE)), 
					nextlabel);
			return;
		}
		
		error(called);
	}
	
	private static void bypassClass(Module module,
			Translator translator, String[] called,
			String label, String nextlabel) {
		
		if (called[1].equals("getEnumConstants")) {
			// Pushes null if Enum was not included
			ClassTranslator e = translator.getClassTranslator("java/lang/Enum");
			if (e == null) {
				module.addRule(label, 
						new ExprSemiring(ExprType.PUSH, new Value(Category.ONE, 0)),
						nextlabel);
			}
			
			// Pushes null if no subclasses of Enum
			Set<ClassTranslator> set = e.getSubClasses();
			if (set.isEmpty()) {
				module.addRule(label, 
						new ExprSemiring(ExprType.PUSH, new Value(Category.ONE, 0)),
						nextlabel);
			}
			
			// Enumerates each subclass
			for (ClassTranslator ct : set) {
				String freshlabel0 = MethodTranslator.getFreshReturnLabel();
				module.addRule(label,
						new ExprSemiring(ExprType.IF, new If(If.IS, ct.getId())),
						freshlabel0);
				
				String freshlabel1 = MethodTranslator.getFreshReturnLabel();
				module.addRule(freshlabel0,
						new ExprSemiring(ExprType.CONSTLOAD, new Field(Category.ONE, FieldTranslator.formatName(ct.getName(), "ENUM$VALUES"))),
						freshlabel1);
				
				module.addRule(freshlabel1, new ExprSemiring(ExprType.JUMP, Jump.ONE), nextlabel);
			}
			return;
		} else if (called[1].equals("getSuperclass")) {
			Collection<ClassTranslator> all = translator.getClassTranslators();
			for (ClassTranslator ct : all) {
				String sname = ct.getSuperClassName();
				if (sname == null) continue;
				
				ClassTranslator sct = translator.getClassTranslator(sname);
				if (sct == null) continue;
				
				String freshlabel0 = MethodTranslator.getFreshReturnLabel();
				module.addRule(label,
						new ExprSemiring(ExprType.IF, new If(If.IS, ct.getId())),
						freshlabel0);
				
				String freshlabel1 = MethodTranslator.getFreshReturnLabel();
				module.addRule(freshlabel0,
						new ExprSemiring(ExprType.PUSH, new Value(Category.ONE, sct.getId())),
						freshlabel1);
				
				module.addRule(freshlabel1, new ExprSemiring(ExprType.JUMP, Jump.ONE), nextlabel);
			}
			return;
		}
		
		error(called);
	}
	
	private static void bypassMath(Module module,
			Translator translator, String[] called,
			String label, String nextlabel) {
		
		if (called[1].equals("random")) {
			module.addRule(label, 
					new ExprSemiring(ExprType.PUSH, 
							new Value(Category.TWO, 
									new Float(0), null, new Float(1))
					), nextlabel);
			return;
		}
		
		error(called);
	}
	
	private static void bypassObject(Module module,
			Translator translator, String[] called,
			String label, String nextlabel) {
		if (called[1].equals("getClass")) {
			module.addRule(label, new ExprSemiring(HEAPLOAD), nextlabel);
			return;
		}
		
		if (called[1].equals("wait")) {
			String freshlabel = MethodTranslator.getFreshReturnLabel();
			if (translator.multithreading()) {
				module.addSharedRule(label, new ExprSemiring(WAITINVOKE), freshlabel);
				module.addSharedRule(freshlabel, new ExprSemiring(WAITRETURN), nextlabel);
			} else {
				module.addRule(label, new ExprSemiring(POPPUSH, new Poppush(1, 0)), freshlabel);
				module.addRule(freshlabel, new ExprSemiring(JUMP, Jump.ONE), freshlabel);
			}
			return;
		}
		
		if (called[1].equals("notify")) {
			if (translator.multithreading()) {
				module.addSharedRule(label, 
						new ExprSemiring(NOTIFY, NotifyType.NOTIFY), 
						nextlabel);
			} else {
				module.addRule(label, new ExprSemiring(POPPUSH, new Poppush(1, 0)), nextlabel);
			}
			return;
		}
		
		if (called[1].equals("notifyAll")) {
			if (translator.multithreading()) {
				module.addSharedRule(label, 
						new ExprSemiring(NOTIFY, NotifyType.NOTIFYALL), 
						nextlabel);
			} else {
				module.addRule(label, new ExprSemiring(POPPUSH, new Poppush(1, 0)), nextlabel);
			}
			return;
		}
		
		error(called);
	}
	
	private static void error(String[] called) {
		throw new TranslatorError("Unimplemented case: invoke " 
				+ Arrays.toString(called));
	}
	
	private static void log(String msg, Object... args) {
		Translator.log(msg, args);
	}
}
