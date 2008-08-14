package de.tum.in.jmoped.translator;

import static de.tum.in.jmoped.underbone.ExprType.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.gjt.jclasslib.bytecode.AbstractInstruction;
import org.gjt.jclasslib.bytecode.LookupSwitchInstruction;
import org.gjt.jclasslib.bytecode.MatchOffsetPair;
import org.gjt.jclasslib.bytecode.Opcodes;
import org.gjt.jclasslib.bytecode.TableSwitchInstruction;
import org.gjt.jclasslib.io.ByteCodeReader;
import org.gjt.jclasslib.structures.AccessFlags;
import org.gjt.jclasslib.structures.AttributeInfo;
import org.gjt.jclasslib.structures.CPInfo;
import org.gjt.jclasslib.structures.InvalidByteCodeException;
import org.gjt.jclasslib.structures.MethodInfo;
import org.gjt.jclasslib.structures.attributes.CodeAttribute;
import org.gjt.jclasslib.structures.attributes.ExceptionTableEntry;
import org.gjt.jclasslib.structures.attributes.LineNumberTableAttribute;
import org.gjt.jclasslib.structures.attributes.LineNumberTableEntry;
import org.gjt.jclasslib.structures.constants.ConstantClassInfo;

import de.tum.in.jmoped.translator.stub.Bypasser;
import de.tum.in.jmoped.translator.stub.StubManager;
import de.tum.in.jmoped.underbone.ExprSemiring.ArithType;
import de.tum.in.jmoped.underbone.ExprSemiring.CategoryType;
import de.tum.in.jmoped.underbone.ExprSemiring.CompType;
import de.tum.in.jmoped.underbone.ExprSemiring.If;
import de.tum.in.jmoped.underbone.ExprSemiring.JumpType;
import de.tum.in.jmoped.underbone.ExprSemiring.Local;
import de.tum.in.jmoped.underbone.ExprSemiring.Poppush;
import de.tum.in.jmoped.underbone.ExprSemiring.Return;
import de.tum.in.jmoped.underbone.ExprSemiring.Value;
import de.tum.in.jmoped.underbone.ExprSemiring;
import de.tum.in.jmoped.underbone.LabelUtils;
import de.tum.in.jmoped.underbone.Remopla;
import de.tum.in.jmoped.underbone.ExprSemiring.Condition;
import de.tum.in.jmoped.underbone.ExprSemiring.Condition.ConditionType;
import de.tum.in.jmoped.underbone.ExprType;
import de.tum.in.jmoped.underbone.Module;
import de.tum.in.wpds.Rule;
import de.tum.in.wpds.Semiring;

/**
 * The translator that translates Java bytecodes method to Remopla modules.
 * 
 * @author suwimont
 *
 */
public class MethodTranslator implements ModuleMaker {
	
	/**
	 * The method.
	 */
	MethodInfo method;
	
	/**
	 * The module name.
	 */
	String name;
	
	/**
	 * The code attribute.
	 */
	CodeAttribute codeAttr;
	
	/**
	 * The list of Bytecode instructions.
	 */
	List<AbstractInstruction> ainstList;
	
	/**
	 * The line table.
	 */
	LineNumberTableEntry[] lineTable;
	
	/**
	 * The translated Remopla module.
	 */
	Module module;
	
	/**
	 * The constructor.
	 * 
	 * @param methodInfo the jClasslib's method info.
	 * @throws InvalidByteCodeException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public MethodTranslator(MethodInfo methodInfo) 
			throws InvalidByteCodeException, IOException {

		this.method = methodInfo;
		
		name = TranslatorUtils.formatName(
				StubManager.removeStub(methodInfo.getClassFile().getThisClassName()), 
				methodInfo.getName(), 
				StubManager.removeStub(methodInfo.getDescriptor()));
		
		// Finds the code attribute
		AttributeInfo ai = methodInfo.findAttribute(CodeAttribute.class);
		if (ai == null)
			throw new TranslatorError("Bytecodes for %s not found.", name);
		codeAttr = (CodeAttribute) ai;
		
		// Finds the bytecodes
		ainstList = (List<AbstractInstruction>) ByteCodeReader.readByteCode(codeAttr.getCode());
		
		// Finds the line table
		ai = codeAttr.findAttribute(LineNumberTableAttribute.class);
		if (ai != null)
			lineTable = ((LineNumberTableAttribute) ai).getLineNumberTable();
	}
	
	/**
	 * Gets the name of the module.
	 * 
	 * @return the name of the module
	 */
	public String getName() {
		
		return name;
	}
	
	/**
	 * Returns <code>true</code> if this method is a static initializer;
	 * or <code>false</code> otherwise.
	 * 
	 * @return <code>true</code> iff this method is a static initializer.
	 */
	public boolean isClinit() {
		
		return isClinitOf(TranslatorUtils.extractClassName(name));
	}
	
	/**
	 * Returns the string representing the formatted name of the static
	 * initializer of the class specified by className.
	 * 
	 * @param className the class name.
	 * @return the string representing the static initializer of className.
	 */
	public static String clinitOf(String className) {
		
		return TranslatorUtils.formatName(className, "<clinit>", "()V");
	}
	
	/**
	 * Returns <code>true</code> if the underlying method is the static
	 * initializer of the class specified by className, or <code>false</code>
	 * otherwise.
	 * 
	 * @param className the class name.
	 * @return <code>true</code> iff the underlying method is the static
	 * 		initializer of the class specified by className.
	 */
	private boolean isClinitOf(String className) {
		
		return clinitOf(className).equals(name);
	}
	
	/**
	 * Returns <code>true</code> if this method is synchronized; 
	 * or <code>false</code> otherwise.
	 * 
	 * @return <code>true</code> iff this method is synchronized.
	 */
	public boolean isSynchronized() {
		return (method.getAccessFlags() & AccessFlags.ACC_SYNCHRONIZED) != 0;
	}
	
	/**
	 * Returns <code>true</code> if this method is static; 
	 * or <code>false</code> otherwise.
	 * 
	 * @return <code>true</code> iff this method is static.
	 */
	public boolean isStatic() {
		return (method.getAccessFlags() & AccessFlags.ACC_STATIC) != 0;
	}
	
	/**
	 * Gets the the source line that corresponds to the bytecode instruction
	 * at <code>offset</code>. This is only possible when the source was
	 * compiled with in the debug mode. The method returns -1 if not found.
	 * 
	 * @param offset the bytecode offset.
	 * @return the source line.
	 */
	int getSourceLine(int offset) {
		
		if (lineTable == null)
			return -1;
		
		for (int i = 0; i < lineTable.length; i++) {
			
			if (offset >= lineTable[i].getStartPc()) {
				if (i == lineTable.length-1 || lineTable[i+1].getStartPc() > offset)
					return lineTable[i].getLineNumber();
			}
		}
		
		return -1;
	}
	
	private String nextLabel(int i) {
		
		return TranslatorUtils.formatName(name, ainstList.get(i+1).getOffset());
	}
	
	/**
	 * Makes a Remopla module from this method.
	 */
	public Module make(Translator translator) {
		
		log("%n*** Making: %s ***%n", name);
		String label = TranslatorUtils.formatName(name, 0);
		module = new Module(name, 
				TranslatorUtils.doubleParams(isStatic(), method), 
				TranslatorUtils.isVoid(method), 
				codeAttr.getMaxStack(), 
				codeAttr.getMaxLocals());
		
		// If this is static initializer, 
		if (isClinit()) {
			
			/*
			 * Sets the boolean var to true, 
			 * if this is not the static init of the selected method.
			 */
			String thisName = TranslatorUtils.extractClassName(name);
			if (!translator.getInitClassName().equals(thisName)) {
				String next = getFreshReturnLabel();
				module.addRule(label, GLOBALPUSH, thisName, 1, next);
				label = next;
			}
			
			// Calls the super's static initializer (if any)
			String superName = translator.getClassTranslator(thisName).getSuperClassName();
			log("Super name: %s%n", superName);
			ClassTranslator superColl = translator.getClassTranslator(superName);
			if (superColl != null && superColl.containsClinit() 
					&& !translator.getInitClassName().equals(superName)) {
				
				// <p, name0> -> <p, clinit0 ret0> (INVOKE, 0, (global, EQ))
				String next = getFreshReturnLabel();
				module.addRule(label,
						new ExprSemiring(INVOKE, new ExprSemiring.Invoke(), new Condition(ConditionType.ZERO, superName)), 
						TranslatorUtils.formatName(clinitOf(superName), 0), 
						next);
				
				// <p, name0> -> <p, label> (ONE, (global, NE))
				module.addRule(label,
						new ExprSemiring(JUMP, JumpType.ONE, new Condition(ConditionType.ONE, superName)), 
						next);
				label = next;
			}
		}
		
		CPInfo[] cp = method.getClassFile().getConstantPool();
		int size = ainstList.size();
		for (int i = 0; i < size; i++) {
			
			AbstractInstruction ainst = ainstList.get(i);
			ExprSemiring d = InstructionTranslator.translate(translator, cp, ainst);
			if (i != 0)
				label = TranslatorUtils.formatName(name, ainst.getOffset());
			Translator.log("Making %s: %s%n", label, d);
			
			switch (ainst.getOpcode()) {
			
			case Opcodes.OPCODE_ATHROW:
				athrow(translator, d, label, ainst.getOffset(), cp);
				break;
			
			case Opcodes.OPCODE_AALOAD:
			case Opcodes.OPCODE_BALOAD:
			case Opcodes.OPCODE_IALOAD:
			case Opcodes.OPCODE_LALOAD:
			case Opcodes.OPCODE_SALOAD:
			case Opcodes.OPCODE_AASTORE:
			case Opcodes.OPCODE_BASTORE:
			case Opcodes.OPCODE_IASTORE:
			case Opcodes.OPCODE_LASTORE:
			case Opcodes.OPCODE_SASTORE:
				array(translator, d, label, nextLabel(i));
				break;
			
			case Opcodes.OPCODE_GETFIELD:
			case Opcodes.OPCODE_PUTFIELD:
				getputfield(translator, d, label, nextLabel(i));
				break;
			
			case Opcodes.OPCODE_GETSTATIC:
			case Opcodes.OPCODE_PUTSTATIC:
				getputstatic(translator, d, label, nextLabel(i));
				break;
			
			case Opcodes.OPCODE_GOTO:
			case Opcodes.OPCODE_GOTO_W:
				module.addRule(label, d, TranslatorUtils.branchTarget(name, ainst));
				break;
			
			case Opcodes.OPCODE_IF_ACMPEQ:
			case Opcodes.OPCODE_IF_ACMPNE:
			case Opcodes.OPCODE_IF_ICMPEQ:
			case Opcodes.OPCODE_IF_ICMPNE:
			case Opcodes.OPCODE_IF_ICMPLT:
			case Opcodes.OPCODE_IF_ICMPGE:
			case Opcodes.OPCODE_IF_ICMPGT:
			case Opcodes.OPCODE_IF_ICMPLE:
				module.addRule(label, d, TranslatorUtils.branchTarget(name, ainst));
				module.addRule(label, new ExprSemiring(IFCMP, negate(ainst.getOpcode())), nextLabel(i));
				break;
			
			case Opcodes.OPCODE_IFEQ:
			case Opcodes.OPCODE_IFNE:
			case Opcodes.OPCODE_IFLT:
			case Opcodes.OPCODE_IFGE:
			case Opcodes.OPCODE_IFGT:
			case Opcodes.OPCODE_IFLE:
			case Opcodes.OPCODE_IFNONNULL:
			case Opcodes.OPCODE_IFNULL:
				module.addRule(label, d, TranslatorUtils.branchTarget(name, ainst));
				module.addRule(label, 
						new ExprSemiring(IF, new If(negate(ainst.getOpcode()))), 
						nextLabel(i));
				break;
				
			case Opcodes.OPCODE_INSTANCEOF:
				instanceofExpr(d, label, nextLabel(i));
				break;
			
			case Opcodes.OPCODE_INVOKEINTERFACE:
				invokeinterface(translator, d, label, nextLabel(i), ainst.getOffset(), cp);
				break;
				
			case Opcodes.OPCODE_INVOKESPECIAL:
				invokespecial(translator, d, label, nextLabel(i));
				break;
				
			case Opcodes.OPCODE_INVOKESTATIC:
				invokestatic(translator, d, label, nextLabel(i), ainst.getOffset(), cp);
				break;
			
			case Opcodes.OPCODE_INVOKEVIRTUAL:
				if (d.type == PRINT) {
					module.addRule(label, d, nextLabel(i));
					break;
				}
				invokevirtual(translator, d, label, nextLabel(i), ainst.getOffset(), cp);
				break;
				
			case Opcodes.OPCODE_JSR:
			case Opcodes.OPCODE_JSR_W:
				jsr(d, label, nextLabel(i), ainst);
				break;
				
			case Opcodes.OPCODE_LDC:
			case Opcodes.OPCODE_LDC_W:
				// Replaces Integer.MAX_VALUE
				ExprSemiring.Value value = (ExprSemiring.Value) d.value;
				if (translator.nondeterministic() && value.isInteger()) {
					if (value.intValue() == Integer.MAX_VALUE)
						value.setValue((1 << (translator.getBits() - 1)) - 1);
					if (value.intValue() == Integer.MIN_VALUE)
						value.setValue(-(1 << (translator.getBits() - 1)));
				}
				module.addRule(label, d, nextLabel(i));
				break;
				
				
			case Opcodes.OPCODE_LOOKUPSWITCH:
				lookupswitch(translator, d, label, nextLabel(i));
				break;
				
			case Opcodes.OPCODE_MONITORENTER:
			case Opcodes.OPCODE_MONITOREXIT:
				if (translator.multithreading() && translator.nondeterministic())
					module.addSharedRule(label, d, nextLabel(i));
				else
					module.addRule(label, 
							new ExprSemiring(POPPUSH, new ExprSemiring.Poppush(1, 0)), 
							nextLabel(i));
				break;
				
			case Opcodes.OPCODE_NEW:
				if (d.type == JUMP) {
					String alabel = LabelUtils.formatAssertionName(nextLabel(i));
					module.addRule(label, d, alabel);
					module.addRule(alabel, ERROR, alabel);
					i += 3;
					break;
				}
				
				newExpr(translator, d, label, nextLabel(i));
				break;
				
			case Opcodes.OPCODE_ANEWARRAY:
			case Opcodes.OPCODE_MULTIANEWARRAY:
			case Opcodes.OPCODE_NEWARRAY:
				heapoverflow(label, NEWARRAY, d.value);
				module.addRule(label, d, nextLabel(i));
				break;
				
			case Opcodes.OPCODE_RET:
				ret(d, label);
				break;
			
			case Opcodes.OPCODE_RETURN:
			case Opcodes.OPCODE_ARETURN:
//			case Opcodes.OPCODE_ATHROW:
			case Opcodes.OPCODE_DRETURN:
			case Opcodes.OPCODE_FRETURN:
			case Opcodes.OPCODE_IRETURN:
			case Opcodes.OPCODE_LRETURN: {
//				if (!translator.multithreading() || !isSynchronized()) {
//					module.addRule(label, d);
//					break;
//				}
//				String label1 = getFreshReturnLabel();
//				String label2 = getFreshReturnLabel();
//				module.addRule(label, new ExprSemiring(LOAD, new Local(CategoryType.ONE, 0)), label1);
//				module.addSharedRule(label1, new ExprSemiring(MONITOREXIT), label2);
//				module.addRule(label2, d);
				returnExpr(translator, d, label);
				break;
			}
			
			case Opcodes.OPCODE_TABLESWITCH:
				tableswitch(translator, d, label, nextLabel(i));
				break;
				
			default:
				module.addRule(label, d, nextLabel(i));
			}
		}
		
		Translator.log("%n*****************%n", name);
		return module;
	}
	
	private void athrow(Translator translator, ExprSemiring d,
			String label, int offset, CPInfo[] cp) {
		
		// Gets the exception table
		ExceptionTableEntry[] etable = codeAttr.getExceptionTable();
		if (etable == null) {
			// Propagates exceptions 
			String label1 = getFreshReturnLabel();
			module.addRule(label, new ExprSemiring(
					GLOBALSTORE, new ExprSemiring.Field(CategoryType.ONE, Remopla.e)),
					label1);
			
			returnExpr(translator, d, label1);
			return;
		}
		
		Set<Integer> handled = new HashSet<Integer>();
		for (ExceptionTableEntry e : etable) {
			
			// Continues if e is not in the scope
			if (offset < e.getStartPc() || offset >= e.getEndPc())
				continue;
			
			// Finds the catch class
			ConstantClassInfo cci = (ConstantClassInfo) cp[e.getCatchType()];
			if (cci == null) {
				log("ConstantClassInfo at entry %d not found.%n", e.getCatchType());
				continue;
			}
			
			// Gets all candidates
			Set<Integer> candidates = null;
			try {
				candidates = translator.getCastableIds(
						StubManager.removeStub(cci.getName()));
			} catch (InvalidByteCodeException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			// Adds a rule
			candidates.removeAll(handled);
			Condition cond = new Condition(
					ConditionType.CONTAINS, candidates);
			module.addRule(label, new ExprSemiring(JUMP, JumpType.THROW, cond), 
					TranslatorUtils.formatName(name, e.getHandlerPc()));
			
			// Updates handled
			handled.addAll(candidates);	
		}
		
		// Propagates exceptions if not handled
		handled.add(0);
		Condition cond = new Condition(ConditionType.NOTCONTAINS, handled);
		String label1 = getFreshReturnLabel();
		module.addRule(label, new ExprSemiring(JUMP, JumpType.ONE, cond), label1);
		
		String label2 = getFreshReturnLabel();
		module.addRule(label1, new ExprSemiring(
				GLOBALSTORE, new ExprSemiring.Field(CategoryType.ONE, Remopla.e)),
				label2);
		
		returnExpr(translator, d, label2);
	}
	
	private void array(Translator translator, ExprSemiring d,
			String label, String nextlabel) {
		
		int category = ((ExprSemiring.CategoryType) d.value).intValue();
//		int depth = (d.type == ExprType.ARRAYLOAD) ? category : category + 1;
		int depth = (d.type == ExprType.ARRAYLOAD) ? 1 : category + 1;
		npe(label, depth);
		
		String error = LabelUtils.formatIoobName(label);
		module.addSharedRule(label, 
				new ExprSemiring(IOOB, new ExprSemiring.Npe(depth)), 
				error);
		module.addRule(error, ERROR, error);
		
		module.addSharedRule(label, d, nextlabel);
	}
	
	private void poppushfield(ExprType type, int cat, String label, String nextlabel) {
		module.addSharedRule(label, 
				new ExprSemiring(POPPUSH, 
						new Poppush(
						(type == ExprType.FIELDLOAD) ? 1 : 1 + cat, 
						(type == ExprType.FIELDLOAD) ? cat : 0)), 
				nextlabel);
	}
	
	/**
	 * Handles GETFIELD and PUTFIELD.
	 * 
	 * @param translator
	 * @param d
	 * @param label
	 * @param nextlabel
	 */
	private void getputfield(Translator translator, ExprSemiring d,
			String label, String nextlabel) {
		
		String[] ref = (String[]) d.value;
		Translator.log("\tref: %s%n", Arrays.toString(ref));
		String eref0 = StubManager.removeStub(ref[0]);
		CategoryType cat = TranslatorUtils.getCategory(ref[2]);
		
		// Adds goto NPE
		npe(label, (d.type == ExprType.FIELDLOAD) ? 0 : cat.intValue());
		
		// If the class is ignored, pushes nondeterministically
		ClassTranslator coll = translator.getClassTranslator(eref0);
		if (coll == null) {
//			module.addSharedRule(label, 
//					new ExprSemiring(PUSH, new ExprSemiring.Value(CategoryType.ONE, 1)), 
//					nextlabel);
			poppushfield(d.type, cat.intValue(), label, nextlabel);
			return;
		}
		
		String fieldName = resolveFieldName(coll, ref[1], translator);//FieldTranslator.formatName(coll.getName(), ref[1]);
		HashSet<ClassTranslator> supers = getSuperClassesUntil(eref0, coll, translator);
//		ClassTranslator sup = findSuperClassHavingField(coll, ref[1], translator);
		HashSet<ClassTranslator> subs = coll.getSubClasses();
		boolean hasCond = supers.size() > 1 || subs != null;
		int baseid = translator.getObjectBaseId();
		
		
		// Super classes and "this"
		FieldTranslator field;
		boolean added = false;
		for (ClassTranslator superColl : supers) {
			log("\tsuperColl.getName(): %s%n", superColl.getName());
			field = superColl.getInstanceFieldTranslator(fieldName);
			if (field == null) {
				continue;
			}
			Condition cond = null;
			if (hasCond)
				cond = new Condition(ConditionType.CONTAINS, setOf(superColl.getId()));
			module.addSharedRule(label, 
					new ExprSemiring(d.type, 
							new ExprSemiring.Field(cat, baseid + field.getId()), 
							cond), 
					nextlabel);
			added = true;
		}
		
		// No fields found
		if (subs == null) {
			if (!added) {
				poppushfield(d.type, cat.intValue(), label, nextlabel);
			}
			return;
		}
		
		// Subclasses
		for (ClassTranslator subColl : subs) {
			field = subColl.getInstanceFieldTranslator(fieldName);
			if (field == null) continue;
			Condition cond = new Condition(ConditionType.CONTAINS, setOf(subColl.getId()));
			module.addSharedRule(label, 
					new ExprSemiring(d.type, 
							new ExprSemiring.Field(cat, baseid + field.getId()), 
							cond), 
					nextlabel);
			added = true;
		}
		
		if (!added)
			poppushfield(d.type, cat.intValue(), label, nextlabel);
	}
	
	/**
	 * Collects this class and iteratively all its parent classes until
	 * the class specified by <code>className</code> is reached.
	 * 
	 * @param className
	 * @param thisColl
	 * @param translator
	 * @return
	 */
	private HashSet<ClassTranslator> getSuperClassesUntil(String className,
			ClassTranslator thisColl, Translator translator) {
		
		HashSet<ClassTranslator> set = new HashSet<ClassTranslator>();
		String superName = thisColl.getName();
		set.add(thisColl);
		while (!superName.equals(className)) {
			superName = thisColl.getSuperClassName();
			ClassTranslator superColl = translator.getClassTranslator(superName);
			set.add(superColl);
			thisColl = superColl;
		}
		return set;
	}
	
	private void poppushstatic(ExprType type, int cat, String label, String nextlabel) {
		module.addSharedRule(label, 
				new ExprSemiring(POPPUSH, 
						new Poppush(
						(type == ExprType.GLOBALLOAD) ? 0 : cat, 
						(type == ExprType.GLOBALLOAD) ? cat : 0)), 
				nextlabel);
	}
	
	/**
	 * Handles GETSTATIC and PUTSTATIC.
	 * 
	 * @param translator
	 * @param d
	 * @param label
	 * @param nextlabel
	 */
	private void getputstatic(Translator translator, ExprSemiring d, 
			String label, String nextlabel) {
		
		String[] ref = (String[]) d.value;
		log("\tref: %s%n", Arrays.toString(ref));
		ClassTranslator coll = translator.getClassTranslator(ref[0]);
		
		// If the class is ignored, pushes nondeterministically
		CategoryType cat = TranslatorUtils.getCategory(ref[2]);
		if (coll == null) {
//			module.addRule(label, 
//					new ExprSemiring(PUSH, new ExprSemiring.Value(CategoryType.ONE)), 
//					nextlabel);
			poppushstatic(d.type, cat.intValue(), label, nextlabel);
			return;
		}
		
		String name = FieldTranslator.formatName(coll.getName(), ref[1]);
		FieldTranslator field = coll.getStaticFieldTranslator(name);
		if (field == null) {
			poppushstatic(d.type, cat.intValue(), label, nextlabel);
			return;
		}
		
		if (field.isFinal()) {
			if (d.type == GLOBALLOAD) {
				d.type = CONSTLOAD;
			} else { // d.type == GLOBALSTORE
				d.type = CONSTSTORE;
			}
		}
		
		d.value = new ExprSemiring.Field(cat, name);

		/*
		 * Invokes static initializer if:
		 * 	(1) translator included it,
		 *  (2) the field does not belong to the starting class (because
		 *  	it's already included), and
		 *  (3) the initializer is not this method.
		 */
		boolean cond1 = coll.containsClinit();
		boolean cond2 = !translator.getInitClassName().equals(coll.getName());
		boolean cond3 = !isClinitOf(coll.getName());
		log("\tcond1: %b, cond2: %b, cond3: %b%n", cond1, cond2, cond3);
		if (cond1 && cond2 && cond3) {
			
			// <p, label> -> <p, clinit0 ret0> (INVOKE, 0, (global, EQ))
			String ret0 = getFreshReturnLabel();
			module.addRule(label, 
					new ExprSemiring(INVOKE, new ExprSemiring.Invoke(), new Condition(ConditionType.ZERO, coll.getName())), 
					TranslatorUtils.formatName(coll.getName(), "<clinit>", "()V", 0), 
					ret0);
			
			// <p, ret0> -> <p, nextlabel> (GLOBALLOAD/GLOBALSTORE, var)
			Rule r = new Rule(new ExprSemiring(d.type, d.value), 
					Remopla.p, ret0, Remopla.p, nextlabel);
			if (!field.isFinal()) r.setGlobal(true);
			module.addRule(r);
			
			d.aux = new Condition(ConditionType.ONE, coll.getName());
		}
		
		// <p, label> -> <p, nextlabel> (GLOBALLOAD/GLOBALSTORE, var, (global, NE))
		Rule r = new Rule(d, Remopla.p, label, Remopla.p, nextlabel);
		if (!field.isFinal()) r.setGlobal(true);
		module.addRule(r);
	}
	
	private void instanceofExpr(ExprSemiring d, String label, String nextlabel) {
		
		String freshlabel = getFreshReturnLabel();
		module.addSharedRule(label, 
				new ExprSemiring(ExprType.FIELDLOAD, 
						new ExprSemiring.Field(CategoryType.ONE, 0)), 
				freshlabel);
		module.addRule(freshlabel, d, nextlabel);
	}
	
	private boolean invoke(Translator translator, String[] called, 
			String label, String nextlabel, ClassTranslator coll, int id, boolean cond) {
		
		// The method might not exist, if the class is abstract
		ModuleMaker maker = coll.getModuleMaker(called[1], called[2]);
		if (maker == null) {
			return false;
		}
		
		int nargs = TranslatorUtils.countParams(called[2]) + 1;
		if (translator.multithreading() && maker.isSynchronized()) {
			String label1 = getFreshReturnLabel();
			module.addSharedRule(label, 
					new ExprSemiring(MONITORENTER, 
							new ExprSemiring.Monitorenter(
									ExprSemiring.Monitorenter.Type.TOUCH, nargs)), 
					label1);
			label = label1;
		}
		
		// Creates new semiring (with invoke condition if cond is true)
		ExprSemiring newd = new ExprSemiring(INVOKE, 
				new ExprSemiring.Invoke(false, nargs));
		if (cond) {
			newd.aux  = new Condition(ConditionType.CONTAINS, setOf(id));
		}
		
		// Invokes the method
		String fname = TranslatorUtils.formatName(maker.getName(), 0);
		if (TranslatorUtils.isVoid(called[2])) {
			
			// <p, label> -> <p, fname nextlabel> (INVOKE, d.value, (id, ZERO))
			module.addRule(label, newd, fname, nextlabel);
		} else {
			String freshLabel = getFreshReturnLabel();
			
			// <p, label> -> <p, fname freshlabel> (INVOKE, d.value, (id, ZERO))
			module.addRule(label, newd, fname, freshLabel);
			
			// <p, freshlabel> -> <p, nextlabel> (GETRETURN)
			module.addRule(freshLabel, 
					GETRETURN, TranslatorUtils.getReturnCategory(called[2]), 
					nextlabel);
		}
		
		return true;
	}
	
	private void handleException(Translator translator, String label, 
			String nextlabel, int offset, CPInfo[] cp) {
		
		String n2 = getFreshReturnLabel();
		String n3 = getFreshReturnLabel();
		
		// FIXME the ordering is important for toMoped()
		
		// [Exception] GLOBALLOAD pushes the status variable (e)
		module.addRule(label, new ExprSemiring(GLOBALLOAD, 
				new ExprSemiring.Field(CategoryType.ONE, Remopla.e)), n2);
		
		// [Exception] Continues if the status is zero
		Condition cond = new Condition(ConditionType.CONTAINS, 
				new HashSet<Integer>(Arrays.asList(0)));
		module.addRule(n2, new ExprSemiring(JUMP, JumpType.ONE, cond), n3);
		
		// [Exception] THROW if the status non-zero
		athrow(translator, new ExprSemiring(RETURN, new Return(Return.Type.VOID)), n2, offset, cp);
		
		// [Exception] Pops the status variable
		module.addRule(n3, new ExprSemiring(POPPUSH, new Poppush(1, 0)), nextlabel);
	}
	
	/**
	 * Adds rules to module for invokeinterface instruction.
	 * 
	 * @param translator the translator.
	 * @param called the called method.
	 * @param label the current label.
	 * @param nextlabel the next label.
	 */
	private void invokeinterface(Translator translator, ExprSemiring d, 
			String label, String nextlabel, int offset, CPInfo[] cp) {
		
		// Adds goto NPE
		String[] called = (String[]) d.value;
		Translator.log("\tinvokeinterface: %s%n", Arrays.toString(called));
		npe(label, TranslatorUtils.countParams(called[2]));
		
		// Starts new thread
		Set<ClassTranslator> implementers = translator.getImplementers(called[0]);
		if (newThread(translator, called, label, nextlabel, implementers))
			return;
		
		// Invokes all possible implementers
		String freshlabel = getFreshReturnLabel();
		for (ClassTranslator imp : implementers) {
			log("\timplementer: %s%n", imp);
			invoke(translator, called, label, freshlabel, imp, imp.getId(), true);
		}
		
		handleException(translator, freshlabel, nextlabel, offset, cp);
	}
	
	private boolean newThread(Translator translator, String[] called, 
			String label, String nextlabel, Set<ClassTranslator> implementers) {
		
		if (!TranslatorUtils.nameEquals(called, "java/lang/Runnable", "run", "()V"))
			return false;
		
		ClassTranslator coll = translator.getClassTranslator("java/lang/Thread");
		HashSet<ClassTranslator> subs = new HashSet<ClassTranslator>();
		fillSubClassesHavingMethod(subs, coll, "run", "()V", translator);
		for (ClassTranslator sub : subs) {
			dynamic(translator, called, label, nextlabel, sub, sub.getId());
		}
		for (ClassTranslator imp : implementers) {
			dynamic(translator, called, label, nextlabel, imp, imp.getId());
		}
		
		return true;
	}
	
	private void invokestatic(Translator translator, ExprSemiring d, 
			String label, String nextlabel, int offset, CPInfo[] cp) {
		
		String[] called = (String[]) d.value;
		Translator.log("\tcalled: %s%n", Arrays.toString(called));
		
		// Bypasses the class org/junit/Assert
		if (called[0].equals("org/junit/Assert")) {
			invokestaticAssert(translator, called, label, nextlabel);
			return;
		}
		
//		// Bypasses the class de/tum/in/jmoped/translator/stub/Stub
//		if (called[0].equals("Stub")) {
//			invokestaticStub(translator, called, label, nextlabel);
//			return;
//		}
		
		if (Bypasser.bypass(module, translator, called, label, nextlabel))
			return;
		
		// Bypasses if the translator doesn't include the class
		ClassTranslator coll = translator.getClassTranslator(called[0]);
		if (coll == null) {
			poppush(label, called[2], true, nextlabel);
			return;
		}
		
		String fname = TranslatorUtils.formatName(coll.getName(), called[1], called[2]);
		Translator.log("\tformatted name: %s%n", fname);
		ModuleMaker maker = coll.getModuleMaker(fname);
		
		// Bypasses if translator doesn't include the method
		if (maker == null) {
			poppush(label, called[2], true, nextlabel);
			return;
		}
		
		int nargs = TranslatorUtils.countParams(called[2]);
		boolean isVoid = TranslatorUtils.isVoid(called[2]);
		
		/*
		 * Invokes static initializer if:
		 * 	(1) translator included it,
		 *  (2) the to-call class is not the starting class (because
		 *  	it's already included), and
		 *  (3) the initializer is not this method.
		 */
		boolean cond1 = coll.containsClinit();
		boolean cond2 = !translator.getInitClassName().equals(coll.getName());
		boolean cond3 = !isClinitOf(coll.getName());
		log("\tcond1: %b, cond2: %b, cond3: %b%n", cond1, cond2, cond3);
		boolean clinit = cond1 && cond2 && cond3;
		
		// Invokes static initializer (if any)
		String ret0 = null;
		ExprSemiring d1 = null;
		if (clinit) {
			// <p, label> -> <p, clinit0 ret0> (INVOKE, 0, (global, EQ))
			ret0 = getFreshReturnLabel();
			Semiring newd = new ExprSemiring(INVOKE, new ExprSemiring.Invoke(), 
					new Condition(ConditionType.ZERO, called[0]));
			module.addRule(label, 
					newd,
					TranslatorUtils.formatName(clinitOf(called[0]), 0), 
					ret0);
			
			d.aux = new Condition(ConditionType.ONE, called[0]);
			d1 = new ExprSemiring(INVOKE, 
					new ExprSemiring.Invoke(true, nargs));
		}
		
		// Invokes the method
		d.value = new ExprSemiring.Invoke(true, nargs);
		fname = TranslatorUtils.formatName(fname, 0);
		String freshlabel = getFreshReturnLabel();
		if (isVoid) {
			if (clinit) {
				// <p, ret0> -> <p, fname nextlabel> (INVOKE, d.value)
				module.addRule(ret0, d1, fname, nextlabel);
			}
			
			// <p, label> -> <p, fname nextlabel> (INVOKE, d.value, (global, ONE))
			module.addRule(label, d, fname, freshlabel);
			
			handleException(translator, freshlabel, nextlabel, offset, cp);
		} else {
			if (clinit) {
				// <p, ret0> -> <p, fname freshlabel> (INVOKE, d.value)
				module.addRule(ret0, d1, fname, freshlabel);
			}
			
			// <p, label> -> <p, fname freshlabel> (INVOKE, d.value, (global, ONE))
			module.addRule(label, d, fname, freshlabel);
			
			String freshlabel2 = getFreshReturnLabel();
			handleException(translator, freshlabel, freshlabel2, offset, cp);
			
			// <p, freshlabel> -> <p, nextlabel> (GETRETURN)
			module.addRule(freshlabel2, 
					GETRETURN, TranslatorUtils.getReturnCategory(called[2]), 
					nextlabel);
		}
	}
	
	private void invokestaticAssert(Translator translator, String[] called,
			String label, String nextlabel) {
		
		if (called[1].equals("assertEquals")) {
			if (called[2].equals("(Ljava/lang/Object;Ljava/lang/Object;)V")) {
				
				String[] equals = new String[] {
						"java/lang/Object", "equals", "(Ljava/lang/Object;)Z"};
				
				ClassTranslator ct = translator.getClassTranslator(equals[0]);
				HashSet<ClassTranslator> subs = new HashSet<ClassTranslator>();
				fillSubClassesHavingMethod(subs, ct, equals[1], equals[2], translator);
				String freshlabel = getFreshReturnLabel();
				
				// Invokes all possible sub classes
				for (ClassTranslator sub : subs) {
					invoke(translator, equals, label, freshlabel, sub, sub.getId(), true);
				}
				
				addAssertRules(freshlabel, CompType.NE, nextlabel, label, CompType.EQ);
				return;
			}
		}
		
		if (called[1].equals("assertFalse")) {
			addAssertRules(label, CompType.EQ, nextlabel, label, CompType.NE);
			return;
		}
		
		if (called[1].equals("assertTrue")) {
			addAssertRules(label, CompType.NE, nextlabel, label, CompType.EQ);
			return;
		}
		
		throw new TranslatorError("Unimplemented case: invokestatic " 
				+ Arrays.toString(called));
	}
	
	private void invokestaticStub(Translator translator, String[] called,
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
	
	private void addAssertRules(String fromlabel, CompType truebranch, 
			String nextlabel, String badlabel, CompType falsebranch) {
		
		// True branch
		module.addRule(fromlabel, 
				new ExprSemiring(IF, new If(truebranch)), 
				nextlabel);
		
		// False branch
		String alabel = LabelUtils.formatAssertionName(badlabel);
		module.addRule(badlabel, 
				new ExprSemiring(IF, new If(falsebranch)), 
				alabel);
		module.addRule(alabel, ERROR, alabel);
	}
	
	private void invokespecial(Translator translator, ExprSemiring d, 
			String label, String nextlabel) {
		
		String[] called = (String[]) d.value;
		log("\tinvokespecial: %s%n", Arrays.toString(called));
		
		// Bypasses if the translator doesn't include the class
		ClassTranslator coll = translator.getClassTranslator(called[0]);
		if (coll == null) {
			poppush(label, called[2], false, nextlabel);
			return;
		}
		
		if (!invoke(translator, called, label, nextlabel, coll, coll.getId(), false))
			poppush(label, called[2], false, nextlabel);
	}
	
	private void dynamic(Translator translator, String[] called, 
			String label, String nextlabel, ClassTranslator coll, int id) {
		
		String fname = TranslatorUtils.formatName(coll.getName(), "run", "()V", 0);
		
		// Creates new semiring (with condition CONTAINS)
		ExprSemiring newd = new ExprSemiring(DYNAMIC, 
				new ExprSemiring.Invoke(false, 1), 
				new Condition(ConditionType.CONTAINS, setOf(id)));
		
		// <p, label> -> <p, nextlabel> |> <p, fname> (DYNAMIC, null, (CONTAINS, id))
		module.addDynamicRule(label, newd, nextlabel, fname);
	}
	
	private boolean handleObjectMethods(Translator translator, String[] called, 
			String label, String nextlabel) {
		
		if (!called[0].equals("java/lang/Object")) return false;
		
		if (called[1].equals("getClass")) {
			module.addRule(label, new ExprSemiring(HEAPLOAD), nextlabel);
			return true;
		}
		
		if (called[1].equals("wait")) {
			String freshlabel = getFreshReturnLabel();
			if (translator.multithreading()) {
				module.addSharedRule(label, new ExprSemiring(WAITINVOKE), freshlabel);
				module.addSharedRule(freshlabel, new ExprSemiring(WAITRETURN), nextlabel);
			} else {
				module.addRule(label, new ExprSemiring(POPPUSH, new ExprSemiring.Poppush(1, 0)), freshlabel);
				module.addRule(freshlabel, new ExprSemiring(JUMP, JumpType.ONE), freshlabel);
			}
			return true;
		}
		
		if (called[1].equals("notify")) {
			if (translator.multithreading()) {
				module.addSharedRule(label, 
						new ExprSemiring(NOTIFY, ExprSemiring.NotifyType.NOTIFY), 
						nextlabel);
			} else {
				module.addRule(label, new ExprSemiring(POPPUSH, new ExprSemiring.Poppush(1, 0)), nextlabel);
			}
			return true;
		}
		
		if (called[1].equals("notifyAll")) {
			if (translator.multithreading()) {
				module.addSharedRule(label, 
						new ExprSemiring(NOTIFY, ExprSemiring.NotifyType.NOTIFYALL), 
						nextlabel);
			} else {
				module.addRule(label, new ExprSemiring(POPPUSH, new ExprSemiring.Poppush(1, 0)), nextlabel);
			}
			return true;
		}
		
		return false;
	}
	
	private void poppush(String label, String desc, boolean stc, String nextlabel) {
		
		log("\tpoppush(%s, %s, %b, %s)%n", label, desc, stc, nextlabel);
		module.addRule(label, POPPUSH, new ExprSemiring.Poppush(
				TranslatorUtils.countParams(desc) + ((stc) ? 0 : 1), 
				TranslatorUtils.getReturnCategory(desc).intValue()), 
				nextlabel);
	}
	
	/**
	 * Adds rules to module for invokevirtual instruction.
	 * (1) For abstract method: includes subclasses only.
	 * (2) For non-abstract mehtod:
	 * 		(2.1) if called has the method: includes subclasses only.
	 * 		(2.2) if not: includes its direct super class that has the method
	 * 				and also its subclasses.
	 * 
	 * @param translator the translator.
	 * @param called the called method.
	 * @param label the current label.
	 * @param nextlabel the next label.
	 */
	private void invokevirtual(Translator translator, ExprSemiring d, 
			String label, String nextlabel, int offset, CPInfo[] cp) {
		
		String[] called = (String[]) d.value;
		Translator.log("\tinvokevirtual: %s%n", Arrays.toString(called));
		
		// Adds goto NPE
		npe(label, TranslatorUtils.countParams(called[2]));
		
		// Handles methods of Object (if it is the case)
		if (handleObjectMethods(translator, called, label, nextlabel)) return;
		
		/* 
		 * Finds the first super class that has the method, in case the called
		 * collection does not have one. If cannot find one, it means that
		 * the called method is abstract. In this case, we look at its
		 * subclasses only.
		 */
		
		// Bypasses, if the class is ignored
		ClassTranslator coll = translator.getClassTranslator(called[0]);
		if (coll == null) {
			poppush(label, called[2], false, nextlabel);
			return;
		}
		
		ModuleMaker maker = coll.getModuleMaker(called[1], called[2]);
		ClassTranslator superColl = null;
		if (maker == null) {
			superColl = findSuperClassHavingMethod(coll, called[1], called[2], translator);
		}
		
		// Bypasses if translator doesn't include the method
		HashSet<ClassTranslator> subs = new HashSet<ClassTranslator>();
		fillSubClassesHavingMethod(subs, coll, called[1], called[2], translator);
		if (superColl == null && subs.isEmpty()) {
			poppush(label, called[2], false, nextlabel);
			return;
		}
		
		// cond is true if there are more than one possibilities
		boolean cond = subs.size() + ((superColl != null) ? 1 : 0) > 1;
		
		// Invokes the first super class that has the method if this class does't have it
		String freshlabel = getFreshReturnLabel();
		if (superColl != null) {
			invoke(translator, called, label, freshlabel, superColl, coll.getId(), cond);
		}
		
		// Invokes all possible sub classes
		for (ClassTranslator subColl : subs) {
			invoke(translator, called, label, freshlabel, subColl, subColl.getId(), cond);
		}
		
		handleException(translator, freshlabel, nextlabel, offset, cp);
	}
	
	private void newExpr(Translator translator, ExprSemiring d, 
			String label, String nextlabel) {
		
		String className = (String) d.value;
		ClassTranslator coll = translator.getClassTranslator(className);
		
		// Bypasses, if the class is ignored
		if (coll == null) {
			module.addRule(label, PUSH, new ExprSemiring.Value(CategoryType.ONE), nextlabel);
			return;
		}
		
		/*
		 * Invokes static initializer if:
		 * 	(1) translator included it,
		 *  (2) the to-call class is not the starting class (because
		 *  	it's already included), and
		 *  (3) the initializer is not this method.
		 */
		boolean cond1 = coll.containsClinit();
		boolean cond2 = !translator.getInitClassName().equals(coll.getName());
		boolean cond3 = !isClinitOf(coll.getName());
		log("\tcond1: %b, cond2: %b, cond3: %b%n", cond1, cond2, cond3);
		boolean clinit = cond1 && cond2 && cond3;
		
		
		int id = coll.getId();
		int size = translator.getObjectBaseId() + coll.size();
		d.value = new ExprSemiring.New(id, size);
		
		// Heap overflow
		heapoverflow(label, NEW, d.value);
		
		// In case of NOT including static initializer
		if (!clinit) {
			
			// <p, label> -> <p, nextlabel> (NEW, (id, size))
			module.addSharedRule(label, d, nextlabel);
			return;
		}
		
		// In case of including static initializer
		
		// <p, label> -> <p, clinit0 ret0> (INVOKE, 0, (ZERO, className))
		String ret0 = getFreshReturnLabel();
		Semiring newd = new ExprSemiring(INVOKE, new ExprSemiring.Invoke(), 
				new Condition(ConditionType.ZERO, className));
		module.addRule(label, 
				newd,
				TranslatorUtils.formatName(clinitOf(className), 0), 
				ret0);
		
		// <p, ret0> -> <p, nextlabel> (NEW, (id, size))
		newd = new ExprSemiring(NEW, new ExprSemiring.New(id, size));
		module.addRule(ret0, newd, nextlabel);
		
		// <p, label> -> <p, nextlabel> (NEW, (id, size), (ONE, className))
		newd = new ExprSemiring(NEW, 
				new ExprSemiring.New(id, size),
				new Condition(ConditionType.ONE, className));
		module.addRule(label, newd, nextlabel);
	}
	
	private void heapoverflow(String label, ExprType type, Object value) {
		String holabel = LabelUtils.formatHeapOverflowName(label);
		module.addSharedRule(label, 
				new ExprSemiring(HEAPOVERFLOW, type, value), 
				holabel);
		module.addRule(holabel, ERROR, holabel);
	}
	
	private void npe(String label, int depth) {
		
		String npelabel = LabelUtils.formatNpeName(label);
		module.addSharedRule(label, 
				new ExprSemiring(NPE, new ExprSemiring.Npe(depth)), 
				npelabel);
		module.addRule(npelabel, ERROR, npelabel);
	}
	
	/**
	 * Jump table for bytecode JSR and RET
	 */
	private ArrayList<String> jsrtable = null;
	
	private void jsr(ExprSemiring d, String label, String nextlabel, 
			AbstractInstruction ainst) {
		
		// Initializes table (if necessary)
		if (jsrtable == null) jsrtable = new ArrayList<String>();
		
		// Pushes nextlabel's index and saves it in the jump table
		String freshlabel = getFreshReturnLabel();
		module.addRule(label, 
				new ExprSemiring(PUSH, new Value(jsrtable.size())), 
				freshlabel);
		jsrtable.add(nextlabel);
		
		// Jumps
		module.addRule(freshlabel, d, TranslatorUtils.branchTarget(name, ainst));
	}
	
	private void ret(ExprSemiring d, String label) {
		
		// Makes sure that the jump table is initialized
		if (jsrtable == null)
			throw new TranslatorError("Unexpected error while translating a finally block");
		
		// Pushes the return address index
		String freshlabel = getFreshReturnLabel();
		module.addRule(label, d, freshlabel);
		
		// Jumps to return address
		for (int i = 0; i < jsrtable.size(); i++) {
			module.addRule(freshlabel, 
					new ExprSemiring(IF, new If(i)), 
					jsrtable.get(i));
		}
	}
	
	@SuppressWarnings("unchecked")
	private void lookupswitch(Translator translator, ExprSemiring d, 
			String label, String nextlabel) {
		
		LookupSwitchInstruction inst = (LookupSwitchInstruction) d.value;
		int offset = inst.getOffset();
		Set<Integer> set = new HashSet<Integer>();
		
		List<MatchOffsetPair> pairs = inst.getMatchOffsetPairs();
		for (MatchOffsetPair pair : pairs) {
			
			module.addRule(label, 
					new ExprSemiring(IF, new ExprSemiring.If(pair.getMatch())),
					TranslatorUtils.formatName(name, offset + pair.getOffset()));
			set.add(pair.getMatch());
		}
		
		// Default case
		module.addRule(label,
				new ExprSemiring(IF, new ExprSemiring.If(set)),
				TranslatorUtils.formatName(name, offset + inst.getDefaultOffset()));
	}
	
	private void tableswitch(Translator translator, ExprSemiring d, 
			String label, String nextlabel) {
		
		TableSwitchInstruction inst = (TableSwitchInstruction) d.value;
		int offset = inst.getOffset();
		int[] jumpOffsets = inst.getJumpOffsets();
		int lowByte = inst.getLowByte();
		int highByte = inst.getHighByte();
		
		for (int i = 0; i <= highByte - lowByte; i++) {
			
			module.addRule(label, 
					new ExprSemiring(IF, new ExprSemiring.If(lowByte + i)),
					TranslatorUtils.formatName(name, offset + jumpOffsets[i]));
		}
		
		// Default case
		module.addRule(label,
				new ExprSemiring(IF, new ExprSemiring.If(lowByte, highByte)),
				TranslatorUtils.formatName(name, offset + inst.getDefaultOffset()));
	}
	
	private void returnExpr(Translator translator, ExprSemiring d, 
			String label) {
		if (!translator.multithreading() || !isSynchronized()) {
			module.addRule(label, d);
			return;
		}
		String label1 = getFreshReturnLabel();
		String label2 = getFreshReturnLabel();
		module.addRule(label, new ExprSemiring(LOAD, new Local(CategoryType.ONE, 0)), label1);
		module.addSharedRule(label1, new ExprSemiring(MONITOREXIT), label2);
		module.addRule(label2, d);
		return;
	}
	
	/**
	 * Recursively goes up in the object hierarchy and returns the first class
	 * that has the field specified by fieldName.
	 * 
	 * @param coll the class where the search starts.
	 * @param fieldName the field name.
	 * @param translator the translator.
	 * @return the resolved field name.
	 */
	private String resolveFieldName(ClassTranslator coll, 
			String fieldName, Translator translator) {
		
		String candidate = TranslatorUtils.formatName(coll.getName(), fieldName);
		FieldTranslator field = coll.getInstanceFieldTranslator(candidate);
		if (field != null) return candidate;
		
		String superName = coll.getSuperClassName();
		if (superName == null) return null;
		
		ClassTranslator superColl = translator.getClassTranslator(superName);
		if (superColl == null) return null;
		
		return resolveFieldName(superColl, fieldName, translator);
	}
	
	/**
	 * Recursively goes up in the object hierarchy and returns the first class
	 * that has the method specified by methodName and methodDesc.
	 * 
	 * @param coll
	 * @param methodName
	 * @param methodDesc
	 * @param translator
	 * @return
	 */
	private ClassTranslator findSuperClassHavingMethod(ClassTranslator coll, 
			String methodName, String methodDesc, Translator translator) {
		
		String superName = coll.getSuperClassName();
		if (superName == null) return null;
		
		ClassTranslator superColl = translator.getClassTranslator(superName);
		if (superColl == null) return null;
		
		if (superColl.contains(superColl.getName(), methodName, methodDesc))
			return superColl;
		
		return findSuperClassHavingMethod(superColl, methodName, methodDesc, translator);
	}
	
	/**
	 * Fills in the set all subclasses (including itself) that have the method
	 * specified by methodName and methodDesc.
	 * 
	 * @param set
	 * @param coll
	 * @param methodName
	 * @param methodDesc
	 * @param translator
	 */
	private void fillSubClassesHavingMethod(HashSet<ClassTranslator> set, 
			ClassTranslator coll, String methodName, String methodDesc, Translator translator) {
			
		if (coll.contains(coll.getName(), methodName, methodDesc))
			set.add(coll);
		
		HashSet<ClassTranslator> subs = coll.getSubClasses();
		if (subs == null) return;
		for (ClassTranslator sub : subs) {
			fillSubClassesHavingMethod(set, sub, methodName, methodDesc, translator);
		}
	}
	
	private static Set<Integer> setOf(int value) {
		return new HashSet<Integer>(Arrays.asList(value));
	}
	
	/**
	 * Negates opcode to its opposite comparison type.
	 * 
	 * @param opcode the opcode.
	 * @return the opposite comparison type.
	 */
	private CompType negate(int opcode) {
		
		switch (opcode) {
		case Opcodes.OPCODE_IF_ACMPEQ: 
		case Opcodes.OPCODE_IF_ICMPEQ: 
		case Opcodes.OPCODE_IFEQ:
		case Opcodes.OPCODE_IFNULL:
			return CompType.NE;
			
		case Opcodes.OPCODE_IF_ACMPNE:
		case Opcodes.OPCODE_IF_ICMPNE:
		case Opcodes.OPCODE_IFNE:
		case Opcodes.OPCODE_IFNONNULL:
			return CompType.EQ;
			
		case Opcodes.OPCODE_IF_ICMPLT:
		case Opcodes.OPCODE_IFLT:
			return CompType.GE;
			
		case Opcodes.OPCODE_IF_ICMPGE:
		case Opcodes.OPCODE_IFGE:
			return CompType.LT;
			
		case Opcodes.OPCODE_IF_ICMPGT:
		case Opcodes.OPCODE_IFGT:
			return CompType.LE;
			
		case Opcodes.OPCODE_IF_ICMPLE:
		case Opcodes.OPCODE_IFLE:
			return CompType.GT;
		}
		
		throw new IllegalArgumentException("Illegal opcode: " + opcode);
	}
	
	public static String getClinitName(String className) {
		
		return TranslatorUtils.formatName(className, "<clinit>", "()V");
	}
	
	private static void log(String msg, Object... args) {
		
		Translator.log(msg, args);
	}
	
	private final static String RET = "ret";
	private static int retcount = 0;
	private String getFreshReturnLabel() {
		return RET + retcount++;
	}
}
