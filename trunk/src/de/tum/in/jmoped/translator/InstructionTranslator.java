package de.tum.in.jmoped.translator;

import java.util.HashSet;
import java.util.Set;

import org.gjt.jclasslib.bytecode.AbstractInstruction;
import org.gjt.jclasslib.bytecode.IncrementInstruction;
import org.gjt.jclasslib.bytecode.MultianewarrayInstruction;
import org.gjt.jclasslib.bytecode.Opcodes;
import org.gjt.jclasslib.structures.CPInfo;
import org.gjt.jclasslib.structures.constants.ConstantClassInfo;
import org.gjt.jclasslib.structures.constants.ConstantDoubleInfo;
import org.gjt.jclasslib.structures.constants.ConstantFloatInfo;
import org.gjt.jclasslib.structures.constants.ConstantIntegerInfo;
import org.gjt.jclasslib.structures.constants.ConstantLongInfo;
import org.gjt.jclasslib.structures.constants.ConstantStringInfo;
import org.gjt.jclasslib.structures.constants.ConstantUtf8Info;

import de.tum.in.jmoped.underbone.ExprSemiring.ArithType;
import de.tum.in.jmoped.underbone.ExprSemiring.CategoryType;
import de.tum.in.jmoped.underbone.ExprSemiring.If;
import de.tum.in.jmoped.underbone.ExprSemiring.JumpType;
import de.tum.in.jmoped.underbone.ExprSemiring.Local;
import de.tum.in.jmoped.underbone.ExprSemiring.Return;
import de.tum.in.jmoped.underbone.ExprSemiring.Unaryop;
import de.tum.in.jmoped.underbone.ExprSemiring.CompType;
import de.tum.in.jmoped.underbone.ExprSemiring;
import de.tum.in.jmoped.underbone.ExprType;

import static de.tum.in.jmoped.underbone.ExprType.*;

/**
 * A helper class that translates bytecode instructions to Remopla expressions.
 * 
 * @author suwimont
 *
 */
public class InstructionTranslator {
	
	/**
	 * Translates the bytecode instruction specified by <code>ainst</code>.
	 * 
	 * @param translator the translator.
	 * @param cp the constant pool.
	 * @param ainst the bytecode instruction.
	 * @return the Remopla expression.
	 */
	public static ExprSemiring translate(Translator translator, CPInfo[] cp, 
			AbstractInstruction ainst) {
		
		switch (ainst.getOpcode()) {
		
		case Opcodes.OPCODE_AALOAD:
			return new ExprSemiring(ARRAYLOAD, CategoryType.ONE);
			
		case Opcodes.OPCODE_AASTORE:
			return new ExprSemiring(ARRAYSTORE, CategoryType.ONE);
		
		case Opcodes.OPCODE_ACONST_NULL:
			return new ExprSemiring(PUSH, new ExprSemiring.Value(CategoryType.ONE, 0));
		
		case Opcodes.OPCODE_ALOAD:
			return new ExprSemiring(LOAD, 
					new Local(CategoryType.ONE, TranslatorUtils.immediateByte(ainst)));
		
		case Opcodes.OPCODE_ALOAD_0:
		case Opcodes.OPCODE_ALOAD_1:
		case Opcodes.OPCODE_ALOAD_2:
		case Opcodes.OPCODE_ALOAD_3:
			return new ExprSemiring(LOAD, 
					new Local(CategoryType.ONE, ainst.getOpcode() - Opcodes.OPCODE_ALOAD_0));
			
		case Opcodes.OPCODE_ANEWARRAY:
			return multianewarray(translator, cp, 
					"[L" + TranslatorUtils.resolveClassName(cp, ainst) + ";", 1);
			
		case Opcodes.OPCODE_ARETURN:
			return new ExprSemiring(RETURN, 
					new Return(Return.Type.SOMETHING, CategoryType.ONE));
			
		case Opcodes.OPCODE_ARRAYLENGTH:
			return new ExprSemiring(ARRAYLENGTH);
			
		case Opcodes.OPCODE_ASTORE:
			return new ExprSemiring(STORE, 
					new Local(CategoryType.ONE, TranslatorUtils.immediateByte(ainst)));
		
		case Opcodes.OPCODE_ASTORE_0:
		case Opcodes.OPCODE_ASTORE_1:
		case Opcodes.OPCODE_ASTORE_2:
		case Opcodes.OPCODE_ASTORE_3:
			return new ExprSemiring(STORE, 
					new Local(CategoryType.ONE, ainst.getOpcode() - Opcodes.OPCODE_ASTORE_0));
			
		case Opcodes.OPCODE_ATHROW:
			return new ExprSemiring(RETURN, new Return(Return.Type.VOID));
			
		case Opcodes.OPCODE_BALOAD:
			return new ExprSemiring(ARRAYLOAD, CategoryType.ONE);
			
		case Opcodes.OPCODE_BASTORE:
			return new ExprSemiring(ARRAYSTORE, CategoryType.ONE);
			
		case Opcodes.OPCODE_BIPUSH:
			return bipush(ainst);
			
		case Opcodes.OPCODE_CALOAD:
			return new ExprSemiring(ARRAYLOAD, CategoryType.ONE);
			
		case Opcodes.OPCODE_CASTORE:
			return new ExprSemiring(ARRAYSTORE, CategoryType.ONE);
			
		case Opcodes.OPCODE_CHECKCAST:
			return checkcast(translator, cp, ainst);
			
		case Opcodes.OPCODE_D2F:
			return new ExprSemiring(UNARYOP, new Unaryop(Unaryop.Type.D2F));
			
		case Opcodes.OPCODE_D2I:
			return new ExprSemiring(UNARYOP, new Unaryop(Unaryop.Type.D2I));
			
		case Opcodes.OPCODE_D2L:
			return new ExprSemiring(UNARYOP, new Unaryop(Unaryop.Type.D2L));
			
		case Opcodes.OPCODE_DADD:
			return new ExprSemiring(ARITH, ArithType.FADD, CategoryType.TWO);
			
		case Opcodes.OPCODE_DALOAD:
			return new ExprSemiring(ARRAYLOAD, CategoryType.TWO);
			
		case Opcodes.OPCODE_DASTORE:
			return new ExprSemiring(ARRAYSTORE, CategoryType.TWO);
			
		case Opcodes.OPCODE_DCMPG:
			return new ExprSemiring(ARITH, ArithType.FCMPG, CategoryType.TWO);
			
		case Opcodes.OPCODE_DCMPL:
			return new ExprSemiring(ARITH, ArithType.FCMPL, CategoryType.TWO);
			
		case Opcodes.OPCODE_DCONST_0:
		case Opcodes.OPCODE_DCONST_1:
			return new ExprSemiring(PUSH, 
					new ExprSemiring.Value(CategoryType.TWO, (float) 
							(ainst.getOpcode() - Opcodes.OPCODE_DCONST_0)));
			
		case Opcodes.OPCODE_DDIV:
			return new ExprSemiring(ARITH, ArithType.FDIV, CategoryType.TWO);
				
		case Opcodes.OPCODE_DLOAD:
			return new ExprSemiring(LOAD, 
					new Local(CategoryType.TWO, TranslatorUtils.immediateByte(ainst)));
			
		case Opcodes.OPCODE_DLOAD_0:
		case Opcodes.OPCODE_DLOAD_1:
		case Opcodes.OPCODE_DLOAD_2:
		case Opcodes.OPCODE_DLOAD_3:
			return new ExprSemiring(LOAD, 
					new Local(CategoryType.TWO, ainst.getOpcode() - Opcodes.OPCODE_DLOAD_0));
			
		case Opcodes.OPCODE_DMUL:
			return new ExprSemiring(ARITH, ArithType.FMUL, CategoryType.TWO);
			
		case Opcodes.OPCODE_DNEG:
			return new ExprSemiring(UNARYOP, new Unaryop(Unaryop.Type.DNEG));
			
		case Opcodes.OPCODE_DREM:
			return new ExprSemiring(ARITH, ArithType.FREM, CategoryType.TWO);
			
		case Opcodes.OPCODE_DRETURN:
			return new ExprSemiring(RETURN, 
					new Return(Return.Type.SOMETHING, CategoryType.TWO));
			
		case Opcodes.OPCODE_DSTORE:
			return new ExprSemiring(STORE, 
					new Local(CategoryType.TWO, TranslatorUtils.immediateByte(ainst)));
			
		case Opcodes.OPCODE_DSTORE_0:
		case Opcodes.OPCODE_DSTORE_1:
		case Opcodes.OPCODE_DSTORE_2:
		case Opcodes.OPCODE_DSTORE_3:
			return new ExprSemiring(STORE, 
					new Local(CategoryType.TWO, ainst.getOpcode() - Opcodes.OPCODE_DSTORE_0));
			
		case Opcodes.OPCODE_DSUB:
			return new ExprSemiring(ARITH, ArithType.FSUB, CategoryType.TWO);
			
		case Opcodes.OPCODE_DUP:
			return new ExprSemiring(DUP, ExprSemiring.DupType.DUP);
			
		case Opcodes.OPCODE_DUP_X1:
			return new ExprSemiring(DUP, ExprSemiring.DupType.DUP_X1);
			
		case Opcodes.OPCODE_DUP_X2:
			return new ExprSemiring(DUP, ExprSemiring.DupType.DUP_X2);
			
		case Opcodes.OPCODE_DUP2:
			return new ExprSemiring(DUP, ExprSemiring.DupType.DUP2);
			
		case Opcodes.OPCODE_DUP2_X1:
			return new ExprSemiring(DUP, ExprSemiring.DupType.DUP2_X1);
			
		case Opcodes.OPCODE_DUP2_X2:
			return new ExprSemiring(DUP, ExprSemiring.DupType.DUP_X2);
			
		case Opcodes.OPCODE_F2D:
			return new ExprSemiring(UNARYOP, new Unaryop(Unaryop.Type.F2D));
			
		case Opcodes.OPCODE_F2I:
			return new ExprSemiring(UNARYOP, new Unaryop(Unaryop.Type.F2I));
			
		case Opcodes.OPCODE_F2L:
			return new ExprSemiring(UNARYOP, new Unaryop(Unaryop.Type.F2L));
			
		case Opcodes.OPCODE_FADD:
			return new ExprSemiring(ARITH, ArithType.FADD, CategoryType.ONE);
			
		case Opcodes.OPCODE_FALOAD:
			return new ExprSemiring(ARRAYLOAD, CategoryType.ONE);
			
		case Opcodes.OPCODE_FASTORE:
			return new ExprSemiring(ARRAYSTORE, CategoryType.ONE);
			
		case Opcodes.OPCODE_FCMPG:
			return new ExprSemiring(ARITH, ArithType.FCMPG, CategoryType.ONE);
			
		case Opcodes.OPCODE_FCMPL:
			return new ExprSemiring(ARITH, ArithType.FCMPL, CategoryType.ONE);
			
		case Opcodes.OPCODE_FCONST_0:
		case Opcodes.OPCODE_FCONST_1:
		case Opcodes.OPCODE_FCONST_2:
			return new ExprSemiring(PUSH, 
					new ExprSemiring.Value(CategoryType.ONE, (float) 
							(ainst.getOpcode() - Opcodes.OPCODE_FCONST_0)));
		
		case Opcodes.OPCODE_FDIV:
			return new ExprSemiring(ARITH, ArithType.FDIV, CategoryType.ONE);
				
		case Opcodes.OPCODE_FLOAD:
			return new ExprSemiring(LOAD, 
					new Local(CategoryType.ONE, TranslatorUtils.immediateByte(ainst)));
			
		case Opcodes.OPCODE_FLOAD_0:
		case Opcodes.OPCODE_FLOAD_1:
		case Opcodes.OPCODE_FLOAD_2:
		case Opcodes.OPCODE_FLOAD_3:
			return new ExprSemiring(LOAD, 
					new Local(CategoryType.ONE, ainst.getOpcode() - Opcodes.OPCODE_FLOAD_0));
			
		case Opcodes.OPCODE_FMUL:
			return new ExprSemiring(ARITH, ArithType.FMUL, CategoryType.ONE);
			
		case Opcodes.OPCODE_FNEG:
			return new ExprSemiring(UNARYOP, new Unaryop(Unaryop.Type.FNEG));
			
		case Opcodes.OPCODE_FREM:
			return new ExprSemiring(ARITH, ArithType.FREM, CategoryType.ONE);
			
		case Opcodes.OPCODE_FRETURN:
			return new ExprSemiring(RETURN, 
					new Return(Return.Type.SOMETHING, CategoryType.ONE));
			
		case Opcodes.OPCODE_FSTORE:
			return new ExprSemiring(STORE, 
					new Local(CategoryType.ONE, TranslatorUtils.immediateByte(ainst)));
			
		case Opcodes.OPCODE_FSTORE_0:
		case Opcodes.OPCODE_FSTORE_1:
		case Opcodes.OPCODE_FSTORE_2:
		case Opcodes.OPCODE_FSTORE_3:
			return new ExprSemiring(STORE, 
					new Local(CategoryType.ONE, ainst.getOpcode() - Opcodes.OPCODE_FSTORE_0));
			
		case Opcodes.OPCODE_FSUB:
			return new ExprSemiring(ARITH, ArithType.FSUB, CategoryType.ONE);
			
		case Opcodes.OPCODE_GETFIELD:
			return new ExprSemiring(FIELDLOAD, 
					TranslatorUtils.getReferencedName(cp, ainst));
			
		case Opcodes.OPCODE_GETSTATIC:
			return getstatic(cp, ainst);
			
		case Opcodes.OPCODE_GOTO:
		case Opcodes.OPCODE_GOTO_W:
			return new ExprSemiring(JUMP, JumpType.ONE);
			
		case Opcodes.OPCODE_I2B:
		case Opcodes.OPCODE_I2C:
			return new ExprSemiring(JUMP, JumpType.ONE);
			
		case Opcodes.OPCODE_I2D:
			return new ExprSemiring(UNARYOP, new Unaryop(Unaryop.Type.I2D));
			
		case Opcodes.OPCODE_I2F:
			return new ExprSemiring(UNARYOP, new Unaryop(Unaryop.Type.I2F));
			
		case Opcodes.OPCODE_I2L:
			return new ExprSemiring(UNARYOP, new Unaryop(Unaryop.Type.I2L));
			
		case Opcodes.OPCODE_I2S:
			return new ExprSemiring(JUMP, JumpType.ONE);
			
		case Opcodes.OPCODE_IADD:
			return new ExprSemiring(ARITH, ArithType.ADD, CategoryType.ONE);
			
		case Opcodes.OPCODE_IALOAD:
			return new ExprSemiring(ARRAYLOAD, CategoryType.ONE);
			
		case Opcodes.OPCODE_IAND:
			return new ExprSemiring(ARITH, ArithType.AND, CategoryType.ONE);
			
		case Opcodes.OPCODE_IASTORE:
			return new ExprSemiring(ARRAYSTORE, CategoryType.ONE);
			
		case Opcodes.OPCODE_ICONST_0:
		case Opcodes.OPCODE_ICONST_1:
		case Opcodes.OPCODE_ICONST_2:
		case Opcodes.OPCODE_ICONST_3:
		case Opcodes.OPCODE_ICONST_4:
		case Opcodes.OPCODE_ICONST_5:
			return new ExprSemiring(PUSH, 
					new ExprSemiring.Value(CategoryType.ONE, ainst.getOpcode() - Opcodes.OPCODE_ICONST_0));
			
		case Opcodes.OPCODE_ICONST_M1:
			return new ExprSemiring(PUSH, new ExprSemiring.Value(CategoryType.ONE, -1));
			
		case Opcodes.OPCODE_IDIV:
			return new ExprSemiring(ARITH, ArithType.DIV, CategoryType.ONE);
			
		case Opcodes.OPCODE_IF_ACMPEQ:
			return new ExprSemiring(IFCMP, CompType.EQ);
			
		case Opcodes.OPCODE_IF_ACMPNE:
			return new ExprSemiring(IFCMP, CompType.NE);
		
		case Opcodes.OPCODE_IF_ICMPEQ:
			return new ExprSemiring(IFCMP, CompType.EQ);
			
		case Opcodes.OPCODE_IF_ICMPGE:
			return new ExprSemiring(IFCMP, CompType.GE);
			
		case Opcodes.OPCODE_IF_ICMPGT:
			return new ExprSemiring(IFCMP, CompType.GT);
			
		case Opcodes.OPCODE_IF_ICMPLT:
			return new ExprSemiring(IFCMP, CompType.LT);
			
		case Opcodes.OPCODE_IF_ICMPLE:
			return new ExprSemiring(IFCMP, CompType.LE);
			
		case Opcodes.OPCODE_IF_ICMPNE:
			return new ExprSemiring(IFCMP, CompType.NE);
			
		case Opcodes.OPCODE_IFEQ:
			return new ExprSemiring(IF, new If(CompType.EQ));
			
		case Opcodes.OPCODE_IFGE:
			return new ExprSemiring(IF, new If(CompType.GE));
			
		case Opcodes.OPCODE_IFGT:
			return new ExprSemiring(IF, new If(CompType.GT));
			
		case Opcodes.OPCODE_IFLT:
			return new ExprSemiring(IF, new If(CompType.LT));
			
		case Opcodes.OPCODE_IFLE:
			return new ExprSemiring(IF, new If(CompType.LE));
			
		case Opcodes.OPCODE_IFNE:
			return new ExprSemiring(IF, new If(CompType.NE));
			
		case Opcodes.OPCODE_IFNONNULL:
			return new ExprSemiring(IF, new If(CompType.NE));
			
		case Opcodes.OPCODE_IFNULL:
			return new ExprSemiring(IF, new If(CompType.EQ));
			
		case Opcodes.OPCODE_IINC:
			return iinc(ainst);
		
		case Opcodes.OPCODE_ILOAD:
			return new ExprSemiring(LOAD, 
					new Local(CategoryType.ONE, TranslatorUtils.immediateByte(ainst)));
			
		case Opcodes.OPCODE_ILOAD_0:
		case Opcodes.OPCODE_ILOAD_1:
		case Opcodes.OPCODE_ILOAD_2:
		case Opcodes.OPCODE_ILOAD_3:
			return new ExprSemiring(LOAD, 
					new Local(CategoryType.ONE, ainst.getOpcode() - Opcodes.OPCODE_ILOAD_0));
			
		case Opcodes.OPCODE_IMUL:
			return new ExprSemiring(ARITH, ArithType.MUL, CategoryType.ONE);
			
		case Opcodes.OPCODE_INEG:
			return new ExprSemiring(UNARYOP, new Unaryop(Unaryop.Type.INEG));
			
		case Opcodes.OPCODE_INSTANCEOF:
			return instanceofInst(translator, cp, ainst);
			
		case Opcodes.OPCODE_INVOKEINTERFACE:
			return new ExprSemiring(ExprType.INVOKE, TranslatorUtils.getReferencedName(cp, ainst));
			
		case Opcodes.OPCODE_INVOKESPECIAL:
			return new ExprSemiring(ExprType.INVOKE, TranslatorUtils.getReferencedName(cp, ainst));	
			
		case Opcodes.OPCODE_INVOKESTATIC:
			return new ExprSemiring(ExprType.INVOKE, TranslatorUtils.getReferencedName(cp, ainst));
			
		case Opcodes.OPCODE_INVOKEVIRTUAL:
			return invokevirtualInst(cp, ainst);
			
		case Opcodes.OPCODE_IOR:
			return new ExprSemiring(ARITH, ArithType.OR, CategoryType.ONE);
			
		case Opcodes.OPCODE_IREM:
			return new ExprSemiring(ARITH, ArithType.REM, CategoryType.ONE);
			
		case Opcodes.OPCODE_IRETURN:
			return new ExprSemiring(ExprType.RETURN, 
					new Return(Return.Type.SOMETHING, CategoryType.ONE));
			
		case Opcodes.OPCODE_ISHL:
			return new ExprSemiring(ARITH, ArithType.SHL, CategoryType.ONE);
			
		case Opcodes.OPCODE_ISHR:
			return new ExprSemiring(ARITH, ArithType.SHR, CategoryType.ONE);
			
		case Opcodes.OPCODE_ISTORE:
			return new ExprSemiring(STORE, 
					new Local(CategoryType.ONE, TranslatorUtils.immediateByte(ainst)));
			
		case Opcodes.OPCODE_ISTORE_0:
		case Opcodes.OPCODE_ISTORE_1:
		case Opcodes.OPCODE_ISTORE_2:
		case Opcodes.OPCODE_ISTORE_3:
			return new ExprSemiring(STORE, 
					new Local(CategoryType.ONE, ainst.getOpcode() - Opcodes.OPCODE_ISTORE_0));
			
		case Opcodes.OPCODE_ISUB:
			return new ExprSemiring(ARITH, ArithType.SUB, CategoryType.ONE);
			
		case Opcodes.OPCODE_IUSHR:
			return new ExprSemiring(ARITH, ArithType.USHR, CategoryType.ONE);
			
		case Opcodes.OPCODE_IXOR:
			return new ExprSemiring(ARITH, ArithType.XOR, CategoryType.ONE);
			
		case Opcodes.OPCODE_JSR:
		case Opcodes.OPCODE_JSR_W:
			return new ExprSemiring(PUSH, JumpType.ONE);
			
		case Opcodes.OPCODE_L2D:
			return new ExprSemiring(UNARYOP, new Unaryop(Unaryop.Type.L2D));
			
		case Opcodes.OPCODE_L2F:
			return new ExprSemiring(UNARYOP, new Unaryop(Unaryop.Type.L2F));
			
		case Opcodes.OPCODE_L2I:
			return new ExprSemiring(UNARYOP, new Unaryop(Unaryop.Type.L2I));
			
		case Opcodes.OPCODE_LADD:
			return new ExprSemiring(ARITH, ArithType.ADD, CategoryType.TWO);
			
		case Opcodes.OPCODE_LALOAD:
			return new ExprSemiring(ARRAYLOAD, CategoryType.TWO);
			
		case Opcodes.OPCODE_LAND:
			return new ExprSemiring(ARITH, ArithType.AND, CategoryType.TWO);
			
		case Opcodes.OPCODE_LASTORE:
			return new ExprSemiring(ARRAYSTORE, CategoryType.TWO);
			
		case Opcodes.OPCODE_LCMP:
			return new ExprSemiring(ARITH, ArithType.CMP, CategoryType.TWO);
			
		case Opcodes.OPCODE_LCONST_0:
		case Opcodes.OPCODE_LCONST_1:
			return new ExprSemiring(PUSH, 
					new ExprSemiring.Value(CategoryType.TWO, ainst.getOpcode() - Opcodes.OPCODE_LCONST_0));
			
		case Opcodes.OPCODE_LDC:
			return ldcInst(translator, cp, ainst);
			
		case Opcodes.OPCODE_LDC_W:
			return ldc_wInst(translator, cp, ainst);
			
		case Opcodes.OPCODE_LDC2_W:
			return ldc_wInst(translator, cp, ainst);
			
		case Opcodes.OPCODE_LDIV:
			return new ExprSemiring(ARITH, ArithType.DIV, CategoryType.TWO);
			
		case Opcodes.OPCODE_LLOAD:
			return new ExprSemiring(LOAD, 
					new Local(CategoryType.TWO, TranslatorUtils.immediateByte(ainst)));
			
		case Opcodes.OPCODE_LLOAD_0:
		case Opcodes.OPCODE_LLOAD_1:
		case Opcodes.OPCODE_LLOAD_2:
		case Opcodes.OPCODE_LLOAD_3:
			return new ExprSemiring(LOAD, 
					new Local(CategoryType.TWO, ainst.getOpcode() - Opcodes.OPCODE_LLOAD_0));
			
		case Opcodes.OPCODE_LMUL:
			return new ExprSemiring(ARITH, ArithType.MUL, CategoryType.TWO);
			
		case Opcodes.OPCODE_LNEG:
			return new ExprSemiring(UNARYOP, new Unaryop(Unaryop.Type.LNEG));
			
		case Opcodes.OPCODE_LOOKUPSWITCH:
			return new ExprSemiring(JUMP, ainst);
			
		case Opcodes.OPCODE_LOR:
			return new ExprSemiring(ARITH, ArithType.OR, CategoryType.TWO);
			
		case Opcodes.OPCODE_LREM:
			return new ExprSemiring(ARITH, ArithType.REM, CategoryType.TWO);
			
		case Opcodes.OPCODE_LRETURN:
			return new ExprSemiring(RETURN, new Return(Return.Type.SOMETHING, CategoryType.TWO));
			
		case Opcodes.OPCODE_LSHL:
			return new ExprSemiring(ARITH, ArithType.SHL, CategoryType.TWO);
			
		case Opcodes.OPCODE_LSHR:
			return new ExprSemiring(ARITH, ArithType.SHR, CategoryType.TWO);
			
		case Opcodes.OPCODE_LSTORE:
			return new ExprSemiring(STORE, 
					new Local(CategoryType.TWO, TranslatorUtils.immediateByte(ainst)));
			
		case Opcodes.OPCODE_LSTORE_0:
		case Opcodes.OPCODE_LSTORE_1:
		case Opcodes.OPCODE_LSTORE_2:
		case Opcodes.OPCODE_LSTORE_3:
			return new ExprSemiring(STORE, 
					new Local(CategoryType.TWO, ainst.getOpcode() - Opcodes.OPCODE_LSTORE_0));
			
		case Opcodes.OPCODE_LSUB:
			return new ExprSemiring(ARITH, ArithType.SUB, CategoryType.TWO);
			
		case Opcodes.OPCODE_LUSHR:
			return new ExprSemiring(ARITH, ArithType.USHR, CategoryType.TWO);
			
		case Opcodes.OPCODE_LXOR:
			return new ExprSemiring(ARITH, ArithType.XOR, CategoryType.TWO);
			
		case Opcodes.OPCODE_MONITORENTER:
			return new ExprSemiring(MONITORENTER, 
					new ExprSemiring.Monitorenter(ExprSemiring.Monitorenter.Type.POP));
			
		case Opcodes.OPCODE_MONITOREXIT:
			return new ExprSemiring(MONITOREXIT);
			
		case Opcodes.OPCODE_MULTIANEWARRAY:
			return multianewarray(translator, cp, TranslatorUtils.resolveClassName(cp, ainst), ((MultianewarrayInstruction) ainst).getDimensions());
			
		case Opcodes.OPCODE_NEW:
			return newInst(cp, ainst);
			
		case Opcodes.OPCODE_NEWARRAY:
			return multianewarray(translator, cp, TranslatorUtils.getNewarrayType(ainst), 1);
			
		case Opcodes.OPCODE_POP:
			return new ExprSemiring(POPPUSH, new ExprSemiring.Poppush(1, 0));
			
		case Opcodes.OPCODE_POP2:
			return new ExprSemiring(POPPUSH, new ExprSemiring.Poppush(2, 0));
			
		case Opcodes.OPCODE_PUTFIELD:
			return new ExprSemiring(FIELDSTORE, 
					TranslatorUtils.getReferencedName(cp, ainst));	
			
		case Opcodes.OPCODE_PUTSTATIC:
			return new ExprSemiring(GLOBALSTORE, //FieldTranslator.formatName(
					TranslatorUtils.getReferencedName(cp, ainst));
			
		case Opcodes.OPCODE_RET:
			return new ExprSemiring(LOAD, 
					new Local(CategoryType.ONE, TranslatorUtils.immediateByte(ainst)));
			
		case Opcodes.OPCODE_RETURN:
			return new ExprSemiring(ExprType.RETURN, new Return(Return.Type.VOID));
			
		case Opcodes.OPCODE_SALOAD:
			return new ExprSemiring(ARRAYLOAD, CategoryType.ONE);
			
		case Opcodes.OPCODE_SASTORE:
			return new ExprSemiring(ARRAYSTORE, CategoryType.ONE);
			
		case Opcodes.OPCODE_SIPUSH:
			return sipush(ainst);
			
		case Opcodes.OPCODE_TABLESWITCH:
			return new ExprSemiring(JUMP, ainst);
			
		case Opcodes.OPCODE_WIDE:
			return new ExprSemiring(JUMP, JumpType.ONE);
		}
		
		throw new TranslatorError("Unsupported bytecode instruction: " 
				+ ainst.getOpcodeVerbose());
	}
	
	private static ExprSemiring bipush(AbstractInstruction ainst) {
		
		int b = TranslatorUtils.immediateByte(ainst);
		if (b > 127) b -= 256;
		return new ExprSemiring(ExprType.PUSH, new ExprSemiring.Value(CategoryType.ONE, b));
	}
	
//	private static void fillSubclasses(Translator translator, Set<ClassTranslator> set,
//			ClassTranslator ct) {
//		
//		// Adds this id
//		set.add(ct);
//		
//		// Adds all sub ids
//		Set<ClassTranslator> subs = ct.getSubClasses();
//		if (subs == null) return;
//		for (ClassTranslator sub : subs) {
//			fillSubclasses(translator, set, sub);
//		}
//	}
	
//	private static Set<Integer> getCandidateTypes(Translator translator, 
//			CPInfo[] cp, AbstractInstruction ainst) {
//		
//		String className = TranslatorUtils.resolveClassName(cp, ainst);
//		ClassTranslator ct = translator.getClassTranslator(className);
//		log("\tclassName: %s%n", ct.getName());
//		
//		Set<Integer> set = new HashSet<Integer>();
//		
//		// Adds ids of all subs
//		Set<ClassTranslator> subs = ct.getDescendantClasses();
//		for (ClassTranslator sub : subs)
//			set.add(sub.getId());
//	
//		if (!ct.isArrayType()) {
//			// Adds ids of all implementers
//			for (ClassTranslator imp : translator.getImplementers(className))
//				set.add(imp.getId());
//		}
//		
//		// In case of array
//		int dim = TranslatorUtils.countDims(className);
//		if (dim > 0) {
//			ct = translator.getClassTranslator(TranslatorUtils.removeArrayPrefix(className));
//			if (ct != null) {
//				
//				// Gets all subs of the array internal
//				subs = ct.getDescendantClasses();
//				
//				// For each sub, adds the id of the array type of the sub
//				for (ClassTranslator sub : subs) {
//					ct = translator.getClassTranslator(
//							TranslatorUtils.insertArrayType(sub.getName(), dim));
//					if (ct != null)
//						set.add(ct.getId());
//				}
//			}
//		}
//		
//		return set;
//	}
	
	private static ExprSemiring checkcast(Translator translator, CPInfo[] cp, 
			AbstractInstruction ainst) {

		// Gets all candidates
		Set<Integer> set = translator.getCastableIds(
				TranslatorUtils.resolveClassName(cp, ainst));
		
		// Null is ok, always included
		set.add(0);
		
		ExprSemiring.Condition cond = new ExprSemiring.Condition(
				ExprSemiring.Condition.ConditionType.CONTAINS, 
				set);
		return new ExprSemiring(JUMP, JumpType.ONE, cond);
	}
	
	private static ExprSemiring getstatic(CPInfo[] cp, AbstractInstruction ainst) {
		
		String[] ref = TranslatorUtils.getReferencedName(cp, ainst);
		
//		if (ref[1].equals("$assertionsDisabled"))
//			return new ExprSemiring(ExprType.PUSH, 0);
		
		return new ExprSemiring(GLOBALLOAD, ref);
	}
	
	private static ExprSemiring iinc(AbstractInstruction ainst) {
		
		IncrementInstruction iinst = (IncrementInstruction) ainst;
		int c = iinst.getIncrementConst();
		if (iinst.isWide()) {
			if (c > 32767) c -= 65536;
		} else {
			if (c > 127) c -= 256;
		}
		return new ExprSemiring(ExprType.INC, 
				new ExprSemiring.Inc(iinst.getImmediateByte(), c));
	}
	
	private static ExprSemiring instanceofInst(Translator translator, CPInfo[] cp, 
			AbstractInstruction ainst) {

		// Gets all candidates
		Set<Integer> set = translator.getCastableIds(
				TranslatorUtils.resolveClassName(cp, ainst));
		
		return new ExprSemiring(UNARYOP, new Unaryop(Unaryop.Type.CONTAINS, set));
	}
	
	private static ExprSemiring invokevirtualInst(CPInfo[] cp, AbstractInstruction ainst) {
		
		String[] ref = TranslatorUtils.getReferencedName(cp, ainst);
		if (ref[0].equals("java/io/PrintStream")) {
			if (ref[1].equals("print") || ref[1].equals("println")) {
				
				// Finds out type
				ExprSemiring.Print.Type type = null;
				if (ref[2].matches("\\((I|J)\\)V"))
					type = ExprSemiring.Print.Type.INTEGER;
				else if (ref[2].matches("\\((F|D)\\)V"))
					type = ExprSemiring.Print.Type.FLOAT;
				else if (ref[2].equals("(C)V"))
					type = ExprSemiring.Print.Type.CHARACTER;
				else if (ref[2].equals("(Ljava/lang/String;)V"))
					type = ExprSemiring.Print.Type.STRING;
				
				// Not print, if type not supported
				if (type != null) {
					return new ExprSemiring(PRINT, 
							new ExprSemiring.Print(type, ref[1].equals("println")));
				}
			}
		}
		
		return new ExprSemiring(INVOKE, ref);
	}
	
	private static ExprSemiring ldcInst(Translator translator, CPInfo[] cp, int i, AbstractInstruction ainst) {
		
		CPInfo ce = cp[i];
		
		switch (ce.getTag()) {
		
		case CPInfo.CONSTANT_INTEGER:
			return new ExprSemiring(PUSH, 
					new ExprSemiring.Value(CategoryType.ONE, ((ConstantIntegerInfo) ce).getInt()));
			
		case CPInfo.CONSTANT_LONG:
			return new ExprSemiring(PUSH, 
					new ExprSemiring.Value(CategoryType.TWO, (int) ((ConstantLongInfo) ce).getLong()));
				
		case CPInfo.CONSTANT_FLOAT:
			return new ExprSemiring(PUSH, 
					new ExprSemiring.Value(CategoryType.ONE, ((ConstantFloatInfo) ce).getFloat()));
			
		case CPInfo.CONSTANT_DOUBLE:
			return new ExprSemiring(PUSH, 
					new ExprSemiring.Value(CategoryType.TWO, ((ConstantDoubleInfo) ce).getDouble()));
			
		case CPInfo.CONSTANT_CLASS: {
			int index = ((ConstantClassInfo) ce).getNameIndex();
			String string = ((ConstantUtf8Info) cp[index]).getString();
			ClassTranslator ct = translator.getClassTranslator(string);
			if (ct == null)	// FIXME
				return new ExprSemiring(PUSH, new ExprSemiring.Value(1));
			
			return new ExprSemiring(PUSH, new ExprSemiring.Value(CategoryType.ONE, ct.getId()));
		}
			
		// TODO
		default:	//case CPInfo.CONSTANT_STRING:
			int index = -1;
			if (ce instanceof ConstantStringInfo) {
				index = ((ConstantStringInfo) ce).getStringIndex();
				
			} 
//			else if (ce instanceof ConstantClassInfo) {
//				index = ((ConstantClassInfo) ce).getNameIndex();
//			}
			
			if (index == -1)
				throw new TranslatorError("Cannot handle class: %s", ce.getClass());
			
			String string = ((ConstantUtf8Info) cp[index]).getString();
			return new ExprSemiring(PUSH, new ExprSemiring.Value(CategoryType.ONE, string));
		}
	}
	
	private static ExprSemiring ldcInst(Translator translator, CPInfo[] cp, AbstractInstruction ainst) {
		
		return ldcInst(translator, cp, TranslatorUtils.immediateByte(ainst), ainst);
	}
	
	private static ExprSemiring ldc_wInst(Translator translator, CPInfo[] cp, AbstractInstruction ainst) {
		
		return ldcInst(translator, cp, TranslatorUtils.immediateShort(ainst), ainst);
	}
	
	private static ExprSemiring multianewarray(Translator translator, CPInfo[] cp, 
			String className, int dim) {
		
		int[] types = new int[dim];
		for (int i = 0; i < dim; i++) {
			types[i] = translator.getClassTranslator(className).getId();
			className = className.substring(1);
		}
			
		return new ExprSemiring(NEWARRAY, 
				new ExprSemiring.Newarray(new ExprSemiring.Value(CategoryType.ONE, 0), dim, types));
	}
	
	private static ExprSemiring newInst(CPInfo[] cp, AbstractInstruction ainst) {
		
		String className = TranslatorUtils.resolveClassName(cp, ainst);
		
		// Bypasses the AssertionError class
		if (className.equals("java/lang/AssertionError"))
			return new ExprSemiring(JUMP, JumpType.ONE);
		
		return new ExprSemiring(ExprType.NEW, className);
	}
	
	private static ExprSemiring sipush(AbstractInstruction ainst) {
		
		int s = TranslatorUtils.immediateShort(ainst);
		if (s > 32767) s -= 65536;
		return new ExprSemiring(ExprType.PUSH, new ExprSemiring.Value(CategoryType.ONE, s));
	}
	
	private static void log(String msg, Object... args) {
		Translator.log(msg, args);
	}
}
