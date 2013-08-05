package com.jsyx;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Stack;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.CodeException;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantDouble;
import org.apache.bcel.classfile.ConstantFieldref;
import org.apache.bcel.classfile.ConstantFloat;
import org.apache.bcel.classfile.ConstantInteger;
import org.apache.bcel.classfile.ConstantLong;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.generic.*;

import choco.Choco;
import choco.Options;
import choco.cp.solver.CPSolver;
import choco.kernel.model.constraints.Constraint;
import choco.kernel.model.variables.Variable;
import choco.kernel.model.variables.integer.IntegerExpressionVariable;
import choco.kernel.model.variables.integer.IntegerVariable;
import choco.kernel.model.variables.real.RealConstantVariable;
import choco.kernel.model.variables.real.RealVariable;

import com.bcel.log.Log;
import com.jsyx.test.DEBUG;
import com.jsyx.test.Exec;
import com.jsyx.tipos.JArray;
import com.jsyx.tipos.JByte;
import com.jsyx.tipos.JChar;
import com.jsyx.tipos.JDouble;
import com.jsyx.tipos.JFloat;
import com.jsyx.tipos.JInteger;
import com.jsyx.tipos.JLong;
import com.jsyx.tipos.JObject;
import com.jsyx.tipos.JShort;
import com.jsyx.tipos.JValue;

/**
 * Clase que representa a la JVM
 * <p>
 * Contiene la memoria heap, area de clases y frame en ejecucion
 * </p>
 * 
 * @author JZ
 * @version 07/12/2012
 */
public class JVM implements Runnable {

	StackFrame topFrame;
	Heap heap;
	ClassArea classes;

	// exec
	// error movido a topframe para que se copie tambien(error=1 al
	// lanzar excepcion con getfield y la ejecucion se paraba)
	// private byte error;
	private TipoExec tipoExec;
	public static int limit = 5;
	private boolean symbolicExec = false;

	// Constraints
	// Array con todas las decisiones tomadas hasta el momento
	private static ArrayList<Constraint> constraints = new ArrayList<Constraint>();

	// Temp Symbols
	// S’mbolos temporales que se crean al usar expresiones como int X = N con N
	// simbolo — if ( n > m+5) m+5 seria temporal
	// con n y m simbolos
	private static final String TEMP = "Temp";
	private static int nTemp = 0;

	public JVM(StackFrame topFrame, Heap heap, ClassArea classes,
			boolean symbolicExec) {
		this.symbolicExec = symbolicExec;
		this.topFrame = topFrame;
		this.heap = heap;
		this.classes = classes;

	}

	public JVM(StackFrame topFrame, Heap heap, ClassArea classes) {
		this.topFrame = topFrame;
		this.heap = heap;
		this.classes = classes;

	}

	public void run() {
		switch (tipoExec) {
		case OPCODE:
			runOPCODE();
			break;
		case IHPC:
			runIHPC();
			break;
		}

	}

	public void runBT() {
		switch (tipoExec) {
		case OPCODE:
			runOPCODE();
			break;
		case IHPC:
			runIHPC_bt(0, -1, true);
			break;
		}

	}

	private void runOPCODE() {
		short[] code;
		short opcode;
		while (topFrame != null && topFrame.error == 0) {
			if (DEBUG.ON) {
				System.out.println("Estado " + topFrame);
			}
			if (topFrame.getMethod().isNative())
				ejecutarNative();
			else {
				// ejecucion acceso directo bytecode
				code = topFrame.getMethod().getCode();
				opcode = code[topFrame.getPC()];
				if (DEBUG.ON)
					System.out.println("[Executing "
							+ Constants.OPCODE_NAMES[opcode] + "]");
				ejecutarOPCODE(code, opcode, false);
			}
		}

	}

	private void runIHPC_bt(int level, int lastConstraint, boolean lastDecission) {
		InstructionHandle ih;
		Log l = null;

		if (DEBUG.LOG)
			l = Log.getInstance();
		while (topFrame != null && topFrame.error == 0 && level < limit) {

			if (DEBUG.ON) {
				System.out.println("Estado: " + topFrame);
			}
			if (topFrame.getMethod().isNative())
				ejecutarNative();
			else {
				ih = topFrame.nextInstruction();
				if (isBranchInst(ih.getInstruction())
						|| ih.getInstruction() instanceof GETFIELD
						|| ih.getInstruction() instanceof PUTFIELD) {
					// Compruebo si en el if existe algœn s’mbolo
					JValue top = topFrame.pop();
					if (top.isSymbol()
							|| (!topFrame.operandsEmtpy() && topFrame.peek()
									.isSymbol())) {
						topFrame.push(top);
						// Si el œltimo camino es false tomo el contrario
						if (!lastDecission) {
							if (DEBUG.ON) {
								System.out.println();
								System.out.println("Checking opposite path");
								System.out.println("[Executing "
										+ ih.getInstruction().getName() + "]");
							}
							ejecutarIH(ih, lastDecission);
							lastDecission = !lastDecission;
						} else {
							// Nuevo branch en el ‡rbol hago copia de todo lo
							// que cambia
							StackFrame parentFrame = topFrame;
							topFrame = parentFrame.deepCopy();
							Heap parentHeap = heap;
							heap = parentHeap.deepCopy();

							if (DEBUG.ON) {
								System.out.println("[Executing "
										+ ih.getInstruction().getName() + "]");
							}
							// Punto de control de constraints
							lastConstraint = constraints.size();
							ejecutarIH(ih, lastDecission);
							// Si no es hijo continuo
							if (topFrame.nextInstruction() != null) {
								runIHPC_bt(level + 1, lastConstraint, true);
							}
							// Reset Solver
							if (level - 1 < limit) {
								JConstraintSolver cs = JConstraintSolver
										.getInstance();
								cs.clearSolver();

								// eliminar constraints hasta el punto de
								// control
								for (int i = constraints.size() - 1; i >= lastConstraint; i--) {
									cs.removeConstraint(constraints.get(i));
									constraints.remove(i);
								}

							}
							// Reestablecemos estado
							lastDecission = !lastDecission;
							topFrame = parentFrame;
							heap = parentHeap;
						}

					}
					// Sino es s’mbolo ninguno ejecutamos normal
					else {
						if (DEBUG.ON) {
							System.out.println("[Executing "
									+ ih.getInstruction().getName() + "]");
						}
						topFrame.push(top);
						ejecutarIH(ih, false);
					}

				} else {
					// Sino es branch ejecucion normal
					if (DEBUG.ON) {
						System.out.println("[Executing "
								+ ih.getInstruction().getName() + "]");
					}
					ejecutarIH(ih, false);
				}

			}
		}
		// Nodo Raiz del ‡rbol
		if (level != limit && (topFrame == null || topFrame.error != 1)) {
			JConstraintSolver cs = JConstraintSolver.getInstance();
			CPSolver solver = (CPSolver) cs.getSolver();
			solver.read(cs.getModel());
			solver.solveAll();
			cs.printSolution();
		}
		// Alcanzado el limite de bœsqueda
		else {
			if (DEBUG.LOG) {
				l.writeNewPathLog();
				if (topFrame != null && topFrame.error != 0) {
					l.writeLog("Execution stopped - Exception found");

				} else {
					l.writeLog("Limit reached");
				}
			}
			if (DEBUG.ON) {
				if (topFrame != null && topFrame.error != 0) {
					System.out.println("Execution stopped - Exception found");
					System.out.println();
				} else {
					System.out.println("Limit reached");
					System.out.println();
				}
			}
		}

	}

	private void runIHPC() {
		InstructionHandle ih;
		while (topFrame != null && topFrame.error == 0) {
			if (DEBUG.ON) {
				System.out.println("Estado: " + topFrame);
			}
			if (topFrame.getMethod().isNative())
				ejecutarNative();
			else {
				// ejecucion accediendo instruccion pos bytecode
				ih = topFrame.nextInstruction();
				if (DEBUG.ON) {
					System.out.println("[Executing "
							+ ih.getInstruction().getName() + "]");
				}
				ejecutarIH(ih, false);
			}
		}
	}

	private int ejecutarNative() {
		JValue val = null;
		String sig = topFrame.getMethod().getFullNameAndSig();
		if (sig.equals(Natives.SIGS[0])) {
			val = Natives.fillInStackTrace(this);
		} else if (sig.equals(Natives.SIGS[1])) {
			val = Natives.print(this);
		} else if (sig.equals(Natives.SIGS[2])) {
			val = Natives.println(this);
		} else if (sig.equals(Natives.SIGS[3])) {
			val = Natives.getIntFromStr(this);
		}
		if (val != null) {
			topFrame.push(val);
			ejecutarXRETURN();
		} else
			ejecutarRETURN();
		return 0;
	}

	// no funciona bien del to por no usar PC
	// private void runIH() {
	// InstructionHandle[] iHandles;
	// while (topFrame != null && topFrame.error == 0) {
	// if (DEBUG.ON) {
	// System.out.println("Estado: " + topFrame);
	// }
	// // ejecucion recorriendo insts
	// iHandles = topFrame.getInstructions();
	// InstructionHandle ih = iHandles[topFrame.getPC()];
	// ejecutarIH(ih);
	// }
	//
	// }

	private void ejecutarOPCODE(short[] code, short opcode, boolean lastDec) {
		short pos, parteA, parteB, cons, type;
		// segun opcode exec instruccion
		switch (opcode) {
		case Constants.NOP:
			ejecutarNOP();
			break;
		// /////////////// Stack Operations ////////////////
		// Instructions that push a constant onto the stack
		case Constants.ICONST_M1:
		case Constants.ICONST_0:
		case Constants.ICONST_1:
		case Constants.ICONST_2:
		case Constants.ICONST_3:
		case Constants.ICONST_4:
		case Constants.ICONST_5:
			ejecutarICONST(Integer.valueOf(opcode - Constants.ICONST_0));
			break;
		case Constants.ACONST_NULL:
			ejecutarACONST_NULL();
			break;
		case Constants.LCONST_0:// 9
		case Constants.LCONST_1:// 10
			ejecutarLCONST(Long.valueOf(opcode - Constants.LCONST_0));
			break;
		case Constants.FCONST_0:
		case Constants.FCONST_1:
			ejecutarFCONST(Float.valueOf(opcode - Constants.FCONST_0));
			break;
		case Constants.DCONST_0:
		case Constants.DCONST_1:
			ejecutarDCONST(Double.valueOf(opcode - Constants.DCONST_0));
			break;
		case Constants.BIPUSH:
			ejecutarBIPUSH((byte) code[topFrame.getPC() + 1]);
			break;
		case Constants.SIPUSH:
			parteA = code[topFrame.getPC() + 1];
			parteB = code[topFrame.getPC() + 2];
			ejecutarSIPUSH((short) getIndex(parteA, parteB));
			break;
		case Constants.LDC:// Push item from constant pool
			// Dudas
			pos = code[topFrame.getPC() + 1];
			ejecutarLDCBC(pos);
			break;
		case Constants.LDC2_W:
			// Dudas
			parteA = code[topFrame.getPC() + 1];
			parteB = code[topFrame.getPC() + 2];
			ejecutarLDC2_WBC(getIndex(parteA, parteB));
			break;
		// Instructions that load a local variable onto the stack
		case Constants.ILOAD:
			pos = code[topFrame.getPC() + 1];
			ejecutarILOAD((byte) pos);
			topFrame.incPC();// saltar index
			break;
		case Constants.ILOAD_0:
		case Constants.ILOAD_1:
		case Constants.ILOAD_2:
		case Constants.ILOAD_3:
			ejecutarILOAD((byte) (opcode - Constants.ILOAD_0));
			break;
		case Constants.LLOAD:
			pos = code[topFrame.getPC() + 1];
			ejecutarLLOAD((byte) pos);
			topFrame.incPC();// saltar index
			break;
		case Constants.LLOAD_0:
		case Constants.LLOAD_1:
		case Constants.LLOAD_2:
		case Constants.LLOAD_3:
			ejecutarLLOAD((byte) (opcode - Constants.LLOAD_0));
			break;
		case Constants.FLOAD:
			pos = code[topFrame.getPC() + 1];
			ejecutarFLOAD((byte) pos);
			topFrame.incPC();// saltar index
			break;
		case Constants.FLOAD_0:
		case Constants.FLOAD_1:
		case Constants.FLOAD_2:
		case Constants.FLOAD_3:
			ejecutarFLOAD((byte) (opcode - Constants.FLOAD_0));
			break;
		case Constants.DLOAD:
			pos = code[topFrame.getPC() + 1];
			ejecutarDLOAD((byte) pos);
			topFrame.incPC();// saltar index
			break;
		case Constants.DLOAD_0:
		case Constants.DLOAD_1:
		case Constants.DLOAD_2:
		case Constants.DLOAD_3:
			ejecutarDLOAD((byte) (opcode - Constants.DLOAD_0));
			break;
		case Constants.ALOAD:
			pos = code[topFrame.getPC() + 1];
			ejecutarALOAD((byte) pos);
			topFrame.incPC();// saltar index
			break;
		case Constants.ALOAD_0:
		case Constants.ALOAD_1:
		case Constants.ALOAD_2:
		case Constants.ALOAD_3:
			pos = code[topFrame.getPC() + 1];
			ejecutarALOAD((byte) (opcode - Constants.ALOAD_0));
			break;
		// arrays
		case Constants.IALOAD:
		case Constants.LALOAD:
		case Constants.FALOAD:
		case Constants.DALOAD:
		case Constants.CALOAD:
		case Constants.SALOAD:
		case Constants.BALOAD:
		case Constants.AALOAD:
			ejecutarXALOAD();
			break;
		// Instructions that store a value from the stack into a local variable
		case Constants.ASTORE:
			pos = code[topFrame.getPC() + 1];
			ejecutarASTORE(pos);
			topFrame.incPC();// saltar index
			break;
		case Constants.ASTORE_0:
		case Constants.ASTORE_1:
		case Constants.ASTORE_2:
		case Constants.ASTORE_3:
			ejecutarASTORE(opcode - Constants.ASTORE_0);
			break;
		case Constants.ISTORE:
			pos = code[topFrame.getPC() + 1];
			ejecutarISTORE(pos);
			topFrame.incPC();// saltar index
			break;
		case Constants.ISTORE_0:
		case Constants.ISTORE_1:
		case Constants.ISTORE_2:
		case Constants.ISTORE_3:
			ejecutarISTORE(opcode - Constants.ISTORE_0);
			break;
		case Constants.LSTORE:
			pos = code[topFrame.getPC() + 1];
			ejecutarLSTORE(pos);
			topFrame.incPC();// saltar index
			break;
		case Constants.LSTORE_0:
		case Constants.LSTORE_1:
		case Constants.LSTORE_2:
		case Constants.LSTORE_3:
			ejecutarLSTORE(opcode - Constants.LSTORE_0);
			break;
		case Constants.FSTORE:
			pos = code[topFrame.getPC() + 1];
			ejecutarFSTORE(pos);
			topFrame.incPC();// saltar index
			break;
		case Constants.FSTORE_0:
		case Constants.FSTORE_1:
		case Constants.FSTORE_2:
		case Constants.FSTORE_3:
			ejecutarFSTORE(opcode - Constants.FSTORE_0);
			break;
		case Constants.DSTORE:
			pos = code[topFrame.getPC() + 1];
			ejecutarDSTORE(pos);
			topFrame.incPC();// saltar index
			break;
		case Constants.DSTORE_0:
		case Constants.DSTORE_1:
		case Constants.DSTORE_2:
		case Constants.DSTORE_3:
			ejecutarDSTORE(opcode - Constants.DSTORE_0);
			break;
		case Constants.IASTORE:
		case Constants.LASTORE:
		case Constants.FASTORE:
		case Constants.DASTORE:
		case Constants.CASTORE:
		case Constants.SASTORE:
		case Constants.BASTORE:
		case Constants.AASTORE:
			ejecutarXASTORE();
			break;
		// Generic (typeless) stack operations
		case Constants.POP:
			ejecutarPOP();
			break;
		case Constants.POP2:
			ejecutarPOP2();
			break;
		case Constants.DUP:
			ejecutarDUP();
			break;
		// Integer arithmetic
		case Constants.IADD:
			ejecutarIADD();
			break;
		case Constants.LADD:
			ejecutarLADD();
			break;
		case Constants.FADD:
			ejecutarFADD();
			break;
		case Constants.DADD:
			ejecutarDADD();
			break;
		case Constants.ISUB:
			ejecutarISUB();
			break;
		case Constants.LSUB:
			ejecutarLSUB();
			break;
		case Constants.FSUB:
			ejecutarFSUB();
			break;
		case Constants.DSUB:
			ejecutarDSUB();
			break;
		case Constants.IMUL:
			ejecutarIMUL();
			break;
		case Constants.LMUL:
			ejecutarLMUL();
			break;
		case Constants.FMUL:
			ejecutarFMUL();
			break;
		case Constants.DMUL:
			ejecutarDMUL();
			break;
		case Constants.IDIV:
			ejecutarIDIV();
			break;
		case Constants.LDIV:
			ejecutarLDIV();
			break;
		case Constants.FDIV:
			ejecutarFDIV();
			break;
		case Constants.DDIV:
			ejecutarDDIV();
			break;
		case Constants.IREM:
			ejecutarIREM();
			break;
		case Constants.LREM:
			ejecutarLREM();
			break;
		case Constants.FREM:
			ejecutarFREM();
			break;
		case Constants.DREM:
			ejecutarDREM();
			break;
		case Constants.INEG:
			ejecutarINEG();
			break;
		case Constants.LNEG:
			ejecutarLNEG();
			break;
		case Constants.FNEG:
			ejecutarFNEG();
			break;
		case Constants.DNEG:
			ejecutarDNEG();
			break;
		case Constants.IINC:
			pos = code[topFrame.getPC() + 1];
			cons = code[topFrame.getPC() + 2];
			ejecutarIINC(pos, cons);
			break;
		// ///////////// Objects and Arrays ////////////
		// Instructions that deal with objects
		case Constants.NEW:
			parteA = code[topFrame.getPC() + 1];
			parteB = code[topFrame.getPC() + 2];
			ejecutarNEW(getIndex(parteA, parteB));
			break;
		case Constants.GETFIELD:
			parteA = code[topFrame.getPC() + 1];
			parteB = code[topFrame.getPC() + 2];
			ejecutarGETFIELD(getIndex(parteA, parteB), lastDec);
			break;
		case Constants.PUTFIELD:
			parteA = code[topFrame.getPC() + 1];
			parteB = code[topFrame.getPC() + 2];
			ejecutarPUTFIELD(getIndex(parteA, parteB), lastDec);
			break;
		case Constants.GETSTATIC:
			parteA = code[topFrame.getPC() + 1];
			parteB = code[topFrame.getPC() + 2];
			ejecutarGETSTATIC(getIndex(parteA, parteB));
			break;
		case Constants.PUTSTATIC:
			parteA = code[topFrame.getPC() + 1];
			parteB = code[topFrame.getPC() + 2];
			ejecutarPUTSTATIC(getIndex(parteA, parteB));
			break;
		// Instructions that deal with arrays
		case Constants.NEWARRAY:
			type = code[topFrame.getPC() + 1];
			ejecutarNEWARRAY(type);
			break;
		case Constants.ANEWARRAY:
			parteA = code[topFrame.getPC() + 1];
			parteB = code[topFrame.getPC() + 2];
			ejecutarANEWARRAY(getIndex(parteA, parteB));
			break;
		case Constants.ARRAYLENGTH:
			ejecutarARRAYLENGTH();
			break;
		// ////////////Exceptions ///////////////////////
		case Constants.ATHROW:
			ejecutarATHROW();
			break;
		// ////////////Control Flow /////////////////////
		// Conditional branch instructions
		case Constants.IFNONNULL:
			parteA = code[topFrame.getPC() + 1];
			parteB = code[topFrame.getPC() + 2];
			ejecutarIFNONNULL(getIndex(parteA, parteB), lastDec);
			break;
		case Constants.IFNULL:
			parteA = code[topFrame.getPC() + 1];
			parteB = code[topFrame.getPC() + 2];
			ejecutarIFNULL(getIndex(parteA, parteB), lastDec);
			break;
		case Constants.IFEQ:
			parteA = code[topFrame.getPC() + 1];
			parteB = code[topFrame.getPC() + 2];
			ejecutarIFEQ(getIndex(parteA, parteB), lastDec);
			break;
		case Constants.IFNE:
			parteA = code[topFrame.getPC() + 1];
			parteB = code[topFrame.getPC() + 2];
			ejecutarIFNE(getIndex(parteA, parteB), lastDec);
			break;
		case Constants.IFLT:
			parteA = code[topFrame.getPC() + 1];
			parteB = code[topFrame.getPC() + 2];
			ejecutarIFLT(getIndex(parteA, parteB), lastDec);
			break;
		case Constants.IFGE:
			parteA = code[topFrame.getPC() + 1];
			parteB = code[topFrame.getPC() + 2];
			ejecutarIFGE(getIndex(parteA, parteB), lastDec);
			break;
		case Constants.IFGT:
			parteA = code[topFrame.getPC() + 1];
			parteB = code[topFrame.getPC() + 2];
			ejecutarIFGT(getIndex(parteA, parteB), lastDec);
			break;
		case Constants.IFLE:
			parteA = code[topFrame.getPC() + 1];
			parteB = code[topFrame.getPC() + 2];
			ejecutarIFLE(getIndex(parteA, parteB), lastDec);
			break;
		case Constants.IF_ICMPEQ:
			parteA = code[topFrame.getPC() + 1];
			parteB = code[topFrame.getPC() + 2];
			ejecutarIF_ICMPEQ(getIndex(parteA, parteB), lastDec);
			break;
		case Constants.IF_ICMPNE:
			parteA = code[topFrame.getPC() + 1];
			parteB = code[topFrame.getPC() + 2];
			ejecutarIF_ICMPNE(getIndex(parteA, parteB), lastDec);
			break;
		case Constants.IF_ICMPLT:
			parteA = code[topFrame.getPC() + 1];
			parteB = code[topFrame.getPC() + 2];
			ejecutarIF_ICMPLT(getIndex(parteA, parteB), lastDec);
			break;
		case Constants.IF_ICMPGE:
			parteA = code[topFrame.getPC() + 1];
			parteB = code[topFrame.getPC() + 2];
			ejecutarIF_ICMPGE(getIndex(parteA, parteB), lastDec);
			break;
		case Constants.IF_ICMPGT:
			parteA = code[topFrame.getPC() + 1];
			parteB = code[topFrame.getPC() + 2];
			ejecutarIF_ICMPGT(getIndex(parteA, parteB), lastDec);
			break;
		case Constants.IF_ICMPLE:
			parteA = code[topFrame.getPC() + 1];
			parteB = code[topFrame.getPC() + 2];
			ejecutarIF_ICMPLE(getIndex(parteA, parteB), lastDec);
			break;
		case Constants.IF_ACMPEQ:
			parteA = code[topFrame.getPC() + 1];
			parteB = code[topFrame.getPC() + 2];
			ejecutarIF_ACMPEQ(getIndex(parteA, parteB));
			break;
		case Constants.IF_ACMPNE:
			parteA = code[topFrame.getPC() + 1];
			parteB = code[topFrame.getPC() + 2];
			ejecutarIF_ACMPNE(getIndex(parteA, parteB));
			break;
		case Constants.GOTO:
			parteA = code[topFrame.getPC() + 1];
			parteB = code[topFrame.getPC() + 2];
			ejecutarGOTO(getIndex(parteA, parteB));
			break;
		case Constants.LCMP:
			ejecutarLCMP();
			break;
		case Constants.DCMPL:
			ejecutarDCMPL();
			break;
		case Constants.DCMPG:
			ejecutarDCMPG();
			break;
		// ////////////////////// Method Invocation and Return ////////
		// Method return instructions
		case Constants.RETURN:
			ejecutarRETURN();
			break;
		case Constants.IRETURN:
			ejecutarIRETURN();
			break;
		case Constants.LRETURN:
			ejecutarLRETURN();
			break;
		case Constants.FRETURN:
			ejecutarFRETURN();
			break;
		case Constants.DRETURN:
			ejecutarDRETURN();
			break;
		case Constants.ARETURN:
			ejecutarARETURN();
			break;
		// Method invocation instructions
		case Constants.INVOKESPECIAL:
			ejecutarINVOKEBC(null, code);
			break;
		case Constants.INVOKESTATIC:
			ejecutarINVOKEBC(Constants.INVOKESTATIC, code);
			break;
		case Constants.INVOKEVIRTUAL:
			ejecutarINVOKEBC(null, code);
			break;
		// ETC
		default:
			error(topFrame, "opcode no reconocido");
			break;
		}

	}

	private void ejecutarIH(InstructionHandle ih, boolean lastDec) {

		Instruction i = ih.getInstruction();
		Number pos, inc;
		if (i instanceof NOP) {
			ejecutarNOP();
			// /////////////// Stack Operations ////////////////
			// Instructions that push a constant onto the stack
		} else if (i instanceof ICONST) {
			Number n = ((ICONST) i).getValue();
			ejecutarICONST(n.shortValue());
		} else if (i instanceof ACONST_NULL) {
			ejecutarACONST_NULL();
		} else if (i instanceof LCONST) {
			pos = ((LCONST) i).getValue();
			ejecutarLCONST(pos.longValue());
		} else if (i instanceof FCONST) {
			pos = ((FCONST) i).getValue();
			ejecutarFCONST(pos.floatValue());
		} else if (i instanceof DCONST) {
			pos = ((DCONST) i).getValue();
			ejecutarDCONST(pos.doubleValue());
		} else if (i instanceof BIPUSH) {
			pos = ((BIPUSH) i).getValue();
			ejecutarBIPUSH(pos.byteValue());
		} else if (i instanceof SIPUSH) {
			pos = ((SIPUSH) i).getValue();
			ejecutarSIPUSH(pos.shortValue());
		} else if (i instanceof LDC) {
			ejecutarLDC(i);
		} else if (i instanceof LDC2_W) {
			ejecutarLDC2_W((LDC2_W) i);
			// Instructions that load a local variable onto the stack
		} else if (i instanceof ILOAD) {
			pos = ((ILOAD) i).getIndex();
			ejecutarILOAD(pos.byteValue());
			if (i.getOpcode() == Constants.ILOAD)
				topFrame.incPC(1);
		} else if (i instanceof LLOAD) {
			pos = ((LLOAD) i).getIndex();
			ejecutarLLOAD(pos.byteValue());
			if (i.getOpcode() == Constants.LLOAD)
				topFrame.incPC();
		} else if (i instanceof FLOAD) {
			pos = ((FLOAD) i).getIndex();
			ejecutarFLOAD(pos.byteValue());
			if (i.getOpcode() == Constants.FLOAD)
				topFrame.incPC();
		} else if (i instanceof DLOAD) {
			pos = ((DLOAD) i).getIndex();
			ejecutarDLOAD(pos.byteValue());
			if (i.getOpcode() == Constants.DLOAD)
				topFrame.incPC();
		} else if (i instanceof ALOAD) {
			pos = ((ALOAD) i).getIndex();
			ejecutarALOAD(pos.byteValue());
			if (i.getOpcode() == Constants.ALOAD)
				topFrame.incPC();
		} else if (i instanceof IALOAD || i instanceof LALOAD
				|| i instanceof FALOAD || i instanceof DALOAD
				|| i instanceof CALOAD || i instanceof SALOAD
				|| i instanceof AALOAD || i instanceof BALOAD) {
			ejecutarXALOAD();
			// Instructions that store a value from the stack into a local
			// variable
		} else if (i instanceof ISTORE) {
			pos = ((ISTORE) i).getIndex();
			ejecutarISTORE(pos.shortValue());
			if (i.getOpcode() == Constants.ISTORE)
				topFrame.incPC();
		} else if (i instanceof LSTORE) {
			pos = ((LSTORE) i).getIndex();
			ejecutarLSTORE(pos.shortValue());
			if (i.getOpcode() == Constants.LSTORE)
				topFrame.incPC();
		} else if (i instanceof FSTORE) {
			pos = ((FSTORE) i).getIndex();
			ejecutarFSTORE(pos.shortValue());
			if (i.getOpcode() == Constants.FSTORE)
				topFrame.incPC();
		} else if (i instanceof DSTORE) {
			pos = ((DSTORE) i).getIndex();
			ejecutarDSTORE(pos.shortValue());
			if (i.getOpcode() == Constants.DSTORE)
				topFrame.incPC();
		} else if (i instanceof ASTORE) {
			pos = ((ASTORE) i).getIndex();
			ejecutarASTORE(pos.shortValue());
			if (i.getOpcode() == Constants.ASTORE)
				topFrame.incPC();
		} else if (i instanceof IASTORE || i instanceof LASTORE
				|| i instanceof FASTORE || i instanceof DASTORE
				|| i instanceof CASTORE || i instanceof SASTORE
				|| i instanceof BASTORE || i instanceof AASTORE) {
			ejecutarXASTORE();
			// Generic (typeless) stack operations
		} else if (i instanceof DUP) {
			ejecutarDUP();
		} else if (i instanceof POP) {
			ejecutarPOP();
			// Integer Arithmetic
		} else if (i instanceof IADD) {
			ejecutarIADD();
		} else if (i instanceof LADD) {
			ejecutarLADD();
		} else if (i instanceof FADD) {
			ejecutarFADD();
		} else if (i instanceof DADD) {
			ejecutarDADD();
		} else if (i instanceof ISUB) {
			ejecutarISUB();
		} else if (i instanceof LSUB) {
			ejecutarLSUB();
		} else if (i instanceof FSUB) {
			ejecutarFSUB();
		} else if (i instanceof DSUB) {
			ejecutarDSUB();
		} else if (i instanceof IMUL) {
			ejecutarIMUL();
		} else if (i instanceof LMUL) {
			ejecutarLMUL();
		} else if (i instanceof FMUL) {
			ejecutarFMUL();
		} else if (i instanceof DMUL) {
			ejecutarDMUL();
		} else if (i instanceof IDIV) {
			ejecutarIDIV();
		} else if (i instanceof LDIV) {
			ejecutarLDIV();
		} else if (i instanceof FDIV) {
			ejecutarFDIV();
		} else if (i instanceof DDIV) {
			ejecutarDDIV();
		} else if (i instanceof IREM) {
			ejecutarIREM();
		} else if (i instanceof LREM) {
			ejecutarLREM();
		} else if (i instanceof FREM) {
			ejecutarFREM();
		} else if (i instanceof DREM) {
			ejecutarDREM();
		} else if (i instanceof INEG) {
			ejecutarINEG();
		} else if (i instanceof LNEG) {
			ejecutarLNEG();
		} else if (i instanceof FNEG) {
			ejecutarFNEG();
		} else if (i instanceof DNEG) {
			ejecutarDNEG();
		} else if (i instanceof IINC) {
			pos = ((IINC) i).getIndex();
			inc = ((IINC) i).getIncrement();
			ejecutarIINC(pos.shortValue(), inc.shortValue());
			// ///////////// Objects and Arrays ////////////
			// Instructions that deal with objects
		} else if (i instanceof NEW) {
			pos = ((NEW) i).getIndex();
			ejecutarNEW(pos.shortValue());
		} else if (i instanceof GETFIELD) {
			pos = ((GETFIELD) i).getIndex();
			ejecutarGETFIELD(pos.shortValue(), lastDec);
		} else if (i instanceof PUTFIELD) {
			pos = ((PUTFIELD) i).getIndex();
			ejecutarPUTFIELD(pos.shortValue(), lastDec);
		} else if (i instanceof GETSTATIC) {
			pos = ((GETSTATIC) i).getIndex();
			ejecutarGETSTATIC(pos.shortValue());
		} else if (i instanceof PUTSTATIC) {
			pos = ((PUTSTATIC) i).getIndex();
			ejecutarPUTSTATIC(pos.shortValue());
			// Instructions that deal with arrays
		} else if (i instanceof NEWARRAY) {
			pos = ((NEWARRAY) i).getTypecode();
			ejecutarNEWARRAY(pos.shortValue());
		} else if (i instanceof ANEWARRAY) {
			pos = ((ANEWARRAY) i).getIndex();
			ejecutarANEWARRAY(pos.shortValue());
		} else if (i instanceof ARRAYLENGTH) {
			ejecutarARRAYLENGTH();
			// //////////Exceptions
		} else if (i instanceof ATHROW) {
			ejecutarATHROW();
		} else if (i instanceof IFNONNULL) {
			// ////////////Control Flow
			// Conditional branch instructions
			pos = ((IFNONNULL) i).getIndex();
			ejecutarIFNONNULL(pos.intValue(), lastDec);
		} else if (i instanceof IFNULL) {
			pos = ((IFNULL) i).getIndex();
			ejecutarIFNULL(pos.intValue(), lastDec);
		} else if (i instanceof IFEQ) {
			pos = ((IFEQ) i).getIndex();
			ejecutarIFEQ(pos.intValue(), lastDec);
		} else if (i instanceof IFNE) {
			pos = ((IFNE) i).getIndex();
			ejecutarIFNE(pos.intValue(), lastDec);
		} else if (i instanceof IFLT) {
			pos = ((IFLT) i).getIndex();
			ejecutarIFLT(pos.intValue(), lastDec);
		} else if (i instanceof IFGE) {
			pos = ((IFGE) i).getIndex();
			ejecutarIFGE(pos.intValue(), lastDec);
		} else if (i instanceof IFGT) {
			pos = ((IFGT) i).getIndex();
			ejecutarIFGT(pos.intValue(), lastDec);
		} else if (i instanceof IFLE) {
			pos = ((IFLE) i).getIndex();
			ejecutarIFLE(pos.intValue(), lastDec);
		} else if (i instanceof IF_ICMPEQ) {
			pos = ((IF_ICMPEQ) i).getIndex();
			ejecutarIF_ICMPEQ(pos.intValue(), lastDec);
		} else if (i instanceof IF_ICMPNE) {
			pos = ((IF_ICMPNE) i).getIndex();
			ejecutarIF_ICMPNE(pos.intValue(), lastDec);
		} else if (i instanceof IF_ICMPLT) {
			pos = ((IF_ICMPLT) i).getIndex();
			ejecutarIF_ICMPLT(pos.intValue(), lastDec);
		} else if (i instanceof IF_ICMPGE) {
			pos = ((IF_ICMPGE) i).getIndex();
			ejecutarIF_ICMPGE(pos.intValue(), lastDec);
		} else if (i instanceof IF_ICMPGT) {
			pos = ((IF_ICMPGT) i).getIndex();
			ejecutarIF_ICMPGT(pos.intValue(), lastDec);
		} else if (i instanceof IF_ICMPLE) {
			pos = ((IF_ICMPLE) i).getIndex();
			ejecutarIF_ICMPLE(pos.intValue(), lastDec);
		} else if (i instanceof IF_ACMPEQ) {
			pos = ((IF_ACMPEQ) i).getIndex();
			ejecutarIF_ACMPEQ(pos.intValue());
		} else if (i instanceof IF_ACMPNE) {
			pos = ((IF_ACMPNE) i).getIndex();
			ejecutarIF_ACMPNE(pos.intValue());
		} else if (i instanceof GOTO) {
			pos = ((GOTO) i).getIndex();
			ejecutarGOTO(pos.intValue());
		} else if (i instanceof LCMP) {
			ejecutarLCMP();
		} else if (i instanceof DCMPL) {
			ejecutarDCMPL();
		} else if (i instanceof DCMPG) {
			ejecutarDCMPG();
		} else if (i instanceof RETURN) {
			// //////////////////// Method Invocation and Return ////////
			// Method return instructions
			ejecutarRETURN();
		} else if (i instanceof IRETURN) {
			ejecutarIRETURN();
		} else if (i instanceof LRETURN) {
			ejecutarLRETURN();
		} else if (i instanceof FRETURN) {
			ejecutarFRETURN();
		} else if (i instanceof DRETURN) {
			ejecutarDRETURN();
		} else if (i instanceof ARETURN) {
			ejecutarARETURN();
			// Method invocation instructions
		} else if (i instanceof INVOKESPECIAL) {
			ejecutarINVOKE(i);
		} else if (i instanceof INVOKESTATIC) {
			ejecutarINVOKE(i);
		} else if (i instanceof INVOKEVIRTUAL) {
			ejecutarINVOKE(i);
		} else {
			// topFrame.error(topFrame, "Instruccion no reconocida");
		}
	}

	private void ejecutarATHROW() {
		JObject elaunch = (JObject) topFrame.pop();
		CodeException[] ces = topFrame.getMethod().getExceptionTable();
		ConstantPool cp = topFrame.getConstantPool();
		int pc = topFrame.getPC();
		for (int i = 0; i < ces.length; i++) {
			CodeException ce = ces[i];
			int type = ce.getCatchType();
			if (pc >= ce.getStartPC() && pc < ce.getEndPC()) {
				// si type 0 acepta cualquier excepcion
				if (type == 0) {
					topFrame.clearParams();
					topFrame.setPC(ce.getHandlerPC());
					topFrame.push(elaunch);
					return;
				} else {
					String nameClass = cp.getConstantString(type,
							Constants.CONSTANT_Class);
					if (elaunch.jClass.extiende(nameClass.replace("/", "."))) {
						topFrame.clearParams();
						topFrame.setPC(ce.getHandlerPC());
						topFrame.push(elaunch);
						return;
					}
				}
			}
		}
		// no se encontro handler, volver a situacion antes de llamada y
		// relanzar excepcion
		if (topFrame.getCf() != null) {
			setTopFrame(topFrame.getCf());
			topFrame.incPC(-3);
			topFrame.push(elaunch);
			ejecutarATHROW();
		} else {
			JObject string = (JObject) elaunch.getField("detailMessage")
					.getValue();
			String msg = "NULO";
			if (string.notNull()) {
				JArray arrmessage = (JArray) string.getField(0).getValue();
				msg = arrmessage.toStr();
			}
			error(topFrame, "No se encontro handler para " + elaunch
					+ " con mensaje: " + msg);
		}
	}

	private void ejecutarNOP() {
		topFrame.incPC();
	}

	private void ejecutarACONST_NULL() {
		JObject o = new JObject();
		topFrame.push(o);
		topFrame.incPC();
	}

	private void ejecutarICONST(int n) {
		topFrame.push(new JInteger(n));
		topFrame.incPC();
	}

	private void ejecutarLCONST(long n) {
		topFrame.push(new JLong(n));
		topFrame.incPC();
	}

	private void ejecutarFCONST(float f) {
		topFrame.push(new JFloat(f));
		topFrame.incPC();
	}

	private void ejecutarDCONST(double d) {
		topFrame.push(new JDouble(d));
		topFrame.incPC();
	}

	/**
	 * push a byte onto the stack as an integer value
	 * 
	 * @param b
	 */
	private void ejecutarBIPUSH(byte b) {
		// la especificacion de la instruccion mete un integer,
		// coge el byte y lo extiente a 32bit asi que esta bien con el integer
		// topFrame.push(new JByte(b));
		topFrame.push(new JInteger(b));
		topFrame.incPC(2);
	}

	private void ejecutarSIPUSH(short i) {
		topFrame.push(new JShort(i));
		topFrame.incPC(3);
	}

	private void ejecutarIINC(short pos, short inc) {
		JInteger jvalue = (JInteger) topFrame.locals[pos];
		if (symbolicExec && jvalue.isSymbol()) {
			String s = topFrame.locals[pos].getName().substring(0, 1)
					+ topFrame.locals[pos].getCopies();
			topFrame.locals[pos] = jvalue.incSymb(inc, s);
		} else {
			topFrame.locals[pos] = jvalue.add(new JInteger((byte) inc));
		}
		topFrame.incPC(3);
	}

	private void ejecutarLDC2_W(LDC2_W i) {
		ConstantPoolGen cpg = topFrame.getConstantPoolGen();
		Type t = i.getType(cpg);
		if (t == Type.LONG) {
			topFrame.push(new JLong(i.getValue(cpg).longValue()));
		} else if (t == Type.DOUBLE) {
			topFrame.push(new JDouble(i.getValue(cpg).doubleValue()));
		}
		topFrame.incPC(3);
	}

	private void ejecutarLDC2_WBC(int index) {
		ConstantPool cp = topFrame.getConstantPool();
		try {
			ConstantLong cl = (ConstantLong) cp.getConstant(index,
					Constants.CONSTANT_Long);
			// en dos partes
			// byte parteA= (byte) (cl.getBytes()&0xFF00);
			// byte parteB= (byte) (cl.getBytes()&0x00FF);
			// topFrame.push(new JInteger(parteB));
			// topFrame.push(new JInteger(parteA));
			topFrame.push(new JLong(cl.getBytes()));
		} catch (ClassFormatException e) {
			ConstantDouble cd = (ConstantDouble) cp.getConstant(index,
					Constants.CONSTANT_Double);
			topFrame.push(new JDouble(cd.getBytes()));
		}
		topFrame.incPC(3);
	}

	private void ejecutarXLOAD(byte pos) {
		topFrame.push(topFrame.locals[pos]);
		topFrame.incPC();
	}

	private void ejecutarILOAD(byte pos) {
		ejecutarXLOAD(pos);
	}

	private void ejecutarLLOAD(byte pos) {
		ejecutarXLOAD(pos);
	}

	private void ejecutarDLOAD(byte pos) {
		ejecutarXLOAD(pos);
	}

	private void ejecutarFLOAD(byte pos) {
		topFrame.push(topFrame.locals[pos]);
		topFrame.incPC();
	}

	private void ejecutarALOAD(byte pos) {
		topFrame.push(topFrame.locals[pos]);
		topFrame.incPC();
	}

	private void ejecutarLDCBC(short i) {
		JValue jValue = null;
		ConstantPool cp = topFrame.getConstantPool();
		Constant c = cp.getConstant(i);
		switch (c.getTag()) {
		case Constants.CONSTANT_Integer:
			ConstantInteger ci = (ConstantInteger) cp.getConstant(i + 1,
					Constants.CONSTANT_Integer);
			jValue = new JInteger(ci.getBytes());
			break;

		case Constants.CONSTANT_Float:
			ConstantFloat cf = (ConstantFloat) cp.getConstant(i + 1,
					Constants.CONSTANT_Float);
			jValue = new JFloat(cf.getBytes());
			break;
		case Constants.CONSTANT_String:
			String s = cp.getConstantString(i, Constants.CONSTANT_String);
			jValue = heap.createStringObject(s, classes);
			break;
		}
		topFrame.push(jValue);
		topFrame.incPC(2);
	}

	private void ejecutarLDC(Instruction i) {
		LDC ldc = (LDC) i;
		ConstantPoolGen cpg = topFrame.getConstantPoolGen();
		Type type = ldc.getType(cpg);
		JValue jValue = null;
		if (type == Type.INT) {
			Integer n = (Integer) ldc.getValue(cpg);
			jValue = new JInteger(n);
		} else if (type == Type.FLOAT) {
			Float f = (Float) ldc.getValue(cpg);
			jValue = new JFloat(f);
		} else if (type == Type.STRING) {
			String f = (String) ldc.getValue(cpg);
			jValue = heap.createStringObject(f, classes);
		}
		topFrame.push(jValue);
		topFrame.incPC(2);
	}

	private void ejecutarXSTORE(int pos) {
		JValue val = topFrame.operands.pop();
		topFrame.locals[pos] = val;
		topFrame.incPC();
	}

	/**
	 * Symbolic Execution Store
	 * 
	 * @param pos
	 */
	private void ejecutarXSTORESymbInt(int pos) {
		JValue local = topFrame.locals[pos];
		JValue operand = topFrame.operands.pop();

		if (local == null) {
			if (operand.isSymbol()) {
				// Copiamos el contenido de Operand en local
				local = operand.deepCopy();
				nTemp++;
				String newName = TEMP + Integer.toString(nTemp);
				String oldName = local.getName();
				local.setName(newName);
				local.setConstraintAsig(null);

				// TODO Limites

				JConstraintSolver cs = JConstraintSolver.getInstance();

				IntegerVariable v1 = (IntegerVariable) cs.getVar(oldName);

				// Creamos nueva variable para el Temp nuevo
				IntegerVariable v2 = Choco.makeIntVar(newName, -10, 10,
						Options.V_ENUM);
				cs.addVariable(v2);

				// A–adimos constraint de igualdad
				Constraint c = Choco.eq(v1, v2);
				cs.addConstraint(c);
				local.setConstraintAsig(c);
				constraints.add(c);

				topFrame.locals[pos] = local;
				topFrame.incPC();
			} else {
				topFrame.locals[pos] = operand;
				topFrame.incPC();
			}
		} else {
			if (local.isSymbol()) {

				JConstraintSolver cs = JConstraintSolver.getInstance();
				Constraint c = null;
				IntegerVariable v1 = (IntegerVariable) cs.getVar(local
						.getName());
				if (operand.isSymbol()) {

					Constraint constraint = operand.getConstraintAsig();

					// Si la œltima asignacion no es null tenemos que comprobar
					// si alguno de los operandos de la asignaci—n
					// es el mismo y as’ crear una copia de este
					if (constraint != null) {
						Variable[] aVars = constraint.getVariables();
						ArrayList<Variable> varList = getVar(aVars);
						for (Variable var : varList) {
							if (var.equals(v1)) {
								JValue jValue = (JValue) local.deepCopy();
								jValue.setName(local.getName().substring(0, 5)
										+ "_" + local.getCopies());
								jValue.setCopies(local.getCopies() + 1);

								v1 = Choco.makeIntVar(jValue.getName(),
										Options.V_BOUND);
								cs.addVariable(v1);
								topFrame.locals[pos] = jValue;
							}
						}
					}

					constraint = operand.getConstraintNegation();

					if (constraint != null) {
						Variable[] aVars = constraint.getVariables();
						ArrayList<Variable> varList = getVar(aVars);
						for (Variable var : varList) {

							if (var.getName().substring(0, 1)
									.equals(local.getName().substring(0, 1))) {

								// operand.setName(var.getName());
								// operand.setCopies(local.getCopies()+1);

								topFrame.locals[pos] = operand;
								topFrame.incPC();
								return;
							}
						}
					}

					// A–adimos la contraint de la asignacion (eq) al modelo.
					IntegerVariable v2 = (IntegerVariable) cs.getVar(operand
							.getName());
					c = Choco.eq(v1, v2);
					cs.addConstraint(c);
					local.setConstraintAsig(c);
					constraints.add(c);
				} else {
					c = local.getConstraintAsig();
					// Eliminamos la œltima asignaci—n del modelo
					if (c != null) {
						cs.removeConstraint(c);
						local.setConstraintAsig(null);
					}
					addConstraintSymbolInt(local, operand);
				}
				topFrame.locals[pos].setValue(operand);
				topFrame.incPC();
			} else {
				// Si ninguno de los dos es s’mbolo ejecutar normal
				topFrame.locals[pos] = operand;
				topFrame.incPC();

			}
		}
	}

	// TODO update
	private void ejecutarXSTORESymbReal(int pos) {
		JValue local = topFrame.locals[pos];
		JValue operand = topFrame.operands.pop();

		if (local == null) {
			if (operand.isSymbol()) {
				local = topFrame.operands.pop().deepCopy();
				nTemp++;
				String newName = TEMP + Integer.toString(nTemp);
				String oldName = local.getName();
				local.setName(newName);
				local.setConstraintAsig(null);
				// TODO Limites

				JConstraintSolver cs = JConstraintSolver.getInstance();

				RealVariable v1 = (RealVariable) cs.getVar(oldName);
				RealVariable v2 = Choco.makeRealVar(newName, -100, 100,
						Options.V_ENUM);

				cs.addVariable(v2);

				Constraint c = Choco.eq(v1, v2);
				cs.addConstraint(c);
				local.setConstraintAsig(c);

				topFrame.incPC();
			} else {
				local = operand;
				topFrame.incPC();
			}
		} else {
			if (local.isSymbol()) {
				JConstraintSolver cs = JConstraintSolver.getInstance();
				Constraint c = local.getConstraintAsig();
				if (c == null) {
					cs.removeConstraint(c);
					local.setConstraintAsig(null);
				}

				RealVariable v1 = (RealVariable) cs.getVar(local.getName());
				if (operand.isSymbol()) {
					RealVariable v2 = (RealVariable) cs.getVar(operand
							.getName());
					c = Choco.eq(v1, v2);
					cs.addConstraint(c);
					local.setConstraintAsig(c);

				} else {
					addConstraintSymbolReal(local, operand);
				}
				topFrame.locals[pos] = local;
				topFrame.incPC();
			} else {
				local = operand;
				topFrame.incPC();

			}
		}

	}

	// TODO
	private void ejecutarASTORE(int pos) {
		ejecutarXSTORE(pos);
	}

	private void ejecutarISTORE(int pos) {
		if (symbolicExec) {
			ejecutarXSTORESymbInt(pos);
		} else {
			ejecutarXSTORE(pos);
		}

	}

	// TODO
	private void ejecutarLSTORE(int pos) {
		if (symbolicExec) {
			ejecutarXSTORESymbInt(pos);
		} else {
			ejecutarXSTORE(pos);
		}
	}

	private void ejecutarFSTORE(int pos) {
		if (symbolicExec) {
			ejecutarXSTORESymbReal(pos);
		} else {
			ejecutarXSTORE(pos);
		}
	}

	private void ejecutarDSTORE(int pos) {
		if (symbolicExec) {
			ejecutarXSTORESymbReal(pos);
		} else {
			ejecutarXSTORE(pos);
		}
	}

	private void ejecutarXASTORE() {
		JValue val = topFrame.pop();
		JInteger index = (JInteger) topFrame.pop();
		JArray arrayRef = (JArray) topFrame.pop();
		if (symbolicExec) {
			// TODO aastore otros tipos
			if (arrayRef.type == Type.INT || arrayRef.type == Type.LONG
					|| arrayRef.type == Type.SHORT
					|| arrayRef.type == Type.BYTE)
				ejecutarXASTORESymbInt(arrayRef.get(index.getValue()), val,
						arrayRef, index.getValue());
			else if (arrayRef.type == Type.FLOAT
					|| arrayRef.type == Type.DOUBLE)
				ejecutarXASTORESymbReal(arrayRef.get(index.getValue()), val,
						arrayRef, index.getValue());
			else{
				arrayRef.setAt(index.getValue(), val);
				topFrame.incPC();
			}
		} else {
			arrayRef.setAt(index.getValue(), val);
			topFrame.incPC();
		}

	}

	private void ejecutarXASTORESymbReal(JValue local, JValue operand,
			JArray arrayRef, int pos) {
		if (local == null) {
			if (operand.isSymbol()) {
				// Copiamos el contenido de Operand en local
				local = operand.deepCopy();
				nTemp++;
				String newName = TEMP + Integer.toString(nTemp);
				String oldName = local.getName();
				local.setName(newName);
				local.setConstraintAsig(null);

				// TODO Limites

				JConstraintSolver cs = JConstraintSolver.getInstance();

				RealVariable v1 = (RealVariable) cs.getVar(oldName);

				// Creamos nueva variable para el Temp nuevo
				RealVariable v2 = Choco.makeRealVar(newName, -100, 100,
						Options.V_ENUM);
				cs.addVariable(v2);

				// A–adimos constraint de igualdad
				Constraint c = Choco.eq(v1, v2);
				cs.addConstraint(c);
				local.setConstraintAsig(c);
				constraints.add(c);

				arrayRef.setAt(pos, local);
				topFrame.incPC();
			} else {
				arrayRef.setAt(pos, operand);
				topFrame.incPC();
			}
		} else {
			if (local.isSymbol()) {

				JConstraintSolver cs = JConstraintSolver.getInstance();
				Constraint c = null;
				RealVariable v1 = (RealVariable) cs.getVar(local.getName());
				if (operand.isSymbol()) {

					Constraint constraint = operand.getConstraintAsig();

					// Si la œltima asignacion no es null tenemos que comprobar
					// si alguno de los operandos de la asignaci—n
					// es el mismo y as’ crear una copia de este
					if (constraint != null) {
						Variable[] aVars = constraint.getVariables();
						ArrayList<Variable> varList = getVar(aVars);
						for (Variable var : varList) {
							if (var.equals(v1)) {
								JValue jValue = (JValue) local.deepCopy();
								jValue.setName(local.getName().substring(0, 5)
										+ "_" + local.getCopies());
								jValue.setCopies(local.getCopies() + 1);

								v1 = Choco.makeRealVar(jValue.getName(), -100,
										100, Options.V_ENUM);
								cs.addVariable(v1);
								arrayRef.setAt(pos, jValue);
							}
						}
					}

					constraint = operand.getConstraintNegation();

					if (constraint != null) {
						Variable[] aVars = constraint.getVariables();
						ArrayList<Variable> varList = getVar(aVars);
						for (Variable var : varList) {

							if (var.getName().substring(0, 1)
									.equals(local.getName().substring(0, 1))) {

								// operand.setName(var.getName());
								// operand.setCopies(local.getCopies()+1);
								arrayRef.setAt(pos, operand);
								topFrame.incPC();
								return;
							}
						}
					}

					// A–adimos la contraint de la asignacion (eq) al modelo.
					RealVariable v2 = (RealVariable) cs.getVar(operand
							.getName());
					c = Choco.eq(v1, v2);
					cs.addConstraint(c);
					local.setConstraintAsig(c);
					constraints.add(c);
				} else {
					c = local.getConstraintAsig();
					// Eliminamos la œltima asignaci—n del modelo
					if (c != null) {
						cs.removeConstraint(c);
						local.setConstraintAsig(null);
					}
					addConstraintSymbolReal(local, operand);
				}
				arrayRef.setRawAt(pos, operand);
				topFrame.incPC();
			}
		}
	}

	private void ejecutarXASTORESymbInt(JValue local, JValue operand,
			JArray arrayRef, int pos) {
		if (local == null) {
			if (operand.isSymbol()) {
				// Copiamos el contenido de Operand en local
				local = operand.deepCopy();
				nTemp++;
				String newName = TEMP + Integer.toString(nTemp);
				String oldName = local.getName();
				local.setName(newName);
				local.setConstraintAsig(null);

				// TODO Limites

				JConstraintSolver cs = JConstraintSolver.getInstance();

				IntegerVariable v1 = (IntegerVariable) cs.getVar(oldName);

				// Creamos nueva variable para el Temp nuevo
				IntegerVariable v2 = Choco.makeIntVar(newName, -10, 10,
						Options.V_ENUM);
				cs.addVariable(v2);

				// A–adimos constraint de igualdad
				Constraint c = Choco.eq(v1, v2);
				cs.addConstraint(c);
				local.setConstraintAsig(c);
				constraints.add(c);

				arrayRef.setAt(pos, local);
				topFrame.incPC();
			} else {
				arrayRef.setAt(pos, operand);
				topFrame.incPC();
			}
		} else {
			if (local.isSymbol()) {

				JConstraintSolver cs = JConstraintSolver.getInstance();
				Constraint c = null;
				IntegerVariable v1 = (IntegerVariable) cs.getVar(local
						.getName());
				if (operand.isSymbol()) {

					Constraint constraint = operand.getConstraintAsig();

					// Si la œltima asignacion no es null tenemos que comprobar
					// si alguno de los operandos de la asignaci—n
					// es el mismo y as’ crear una copia de este
					if (constraint != null) {
						Variable[] aVars = constraint.getVariables();
						ArrayList<Variable> varList = getVar(aVars);
						for (Variable var : varList) {
							if (var.equals(v1)) {
								JValue jValue = (JValue) local.deepCopy();
								jValue.setName(local.getName().substring(0, 5)
										+ "_" + local.getCopies());
								jValue.setCopies(local.getCopies() + 1);

								v1 = Choco.makeIntVar(jValue.getName(),
										Options.V_BOUND);
								cs.addVariable(v1);
								arrayRef.setAt(pos, jValue);
							}
						}
					}

					constraint = operand.getConstraintNegation();

					if (constraint != null) {
						Variable[] aVars = constraint.getVariables();
						ArrayList<Variable> varList = getVar(aVars);
						for (Variable var : varList) {

							if (var.getName().substring(0, 1)
									.equals(local.getName().substring(0, 1))) {

								// operand.setName(var.getName());
								// operand.setCopies(local.getCopies()+1);
								arrayRef.setAt(pos, operand);
								topFrame.incPC();
								return;
							}
						}
					}

					// A–adimos la contraint de la asignacion (eq) al modelo.
					IntegerVariable v2 = (IntegerVariable) cs.getVar(operand
							.getName());
					c = Choco.eq(v1, v2);
					cs.addConstraint(c);
					local.setConstraintAsig(c);
					constraints.add(c);
				} else {
					c = local.getConstraintAsig();
					// Eliminamos la œltima asignaci—n del modelo
					if (c != null) {
						cs.removeConstraint(c);
						local.setConstraintAsig(null);
					}
					addConstraintSymbolInt(local, operand);
				}
				arrayRef.setRawAt(pos, operand);
				topFrame.incPC();
			}
		}
	}

	private void ejecutarXALOAD() {
		// TODO simbolica
		JInteger index = (JInteger) topFrame.pop();
		JArray arrayRef = (JArray) topFrame.pop();
		topFrame.push(arrayRef.get(index.getValue()));
		topFrame.incPC();
	}

	private void ejecutarPOP() {
		topFrame.pop();
		topFrame.incPC();
	}

	private void ejecutarPOP2() {
		JValue v = topFrame.peek();
		if (v.type == Type.DOUBLE || v.type == Type.LONG) {
			topFrame.pop();
		} else {
			topFrame.pop();
			topFrame.pop();
		}
		topFrame.incPC();
	}

	private void ejecutarDUP() {
		topFrame.push(topFrame.peek());
		topFrame.incPC();
	}

	private void ejecutarIADD() {
		JInteger op1 = (JInteger) topFrame.operands.pop();
		JInteger op2 = (JInteger) topFrame.operands.pop();

		if (symbolicExec) {
			if (op1.isSymbol() || op2.isSymbol()) {
				nTemp++;
			}
			topFrame.push(op1.addSymb(op2, TEMP + Integer.toString(nTemp)));
		} else {

			topFrame.push(op1.add(op2));
		}
		topFrame.incPC();
	}

	private void ejecutarLADD() {
		JLong op1 = (JLong) topFrame.operands.pop();
		JLong op2 = (JLong) topFrame.operands.pop();
		topFrame.push(op1.add(op2));
		topFrame.incPC();
	}

	private void ejecutarFADD() {
		JFloat op1 = (JFloat) topFrame.operands.pop();
		JFloat op2 = (JFloat) topFrame.operands.pop();
		topFrame.push(op1.add(op2));
		topFrame.incPC();
	}

	private void ejecutarDADD() {
		JDouble op1 = (JDouble) topFrame.operands.pop();
		JDouble op2 = (JDouble) topFrame.operands.pop();
		topFrame.push(op1.add(op2));
		topFrame.incPC();
	}

	private void ejecutarISUB() {
		JInteger op1 = (JInteger) topFrame.operands.pop();
		JInteger op2 = (JInteger) topFrame.operands.pop();
		if (symbolicExec) {
			if (op1.isSymbol() || op2.isSymbol()) {
				nTemp++;
			}
			topFrame.push(op2.subSymb(op1, TEMP + Integer.toString(nTemp)));
		} else {
			topFrame.push(op2.sub(op1));
		}
		topFrame.incPC();
	}

	private void ejecutarLSUB() {
		JLong op1 = (JLong) topFrame.operands.pop();
		JLong op2 = (JLong) topFrame.operands.pop();
		topFrame.push(op1.sub(op2));
		topFrame.incPC();
	}

	private void ejecutarFSUB() {
		JFloat op1 = (JFloat) topFrame.operands.pop();
		JFloat op2 = (JFloat) topFrame.operands.pop();
		topFrame.push(op1.sub(op2));
		topFrame.incPC();
	}

	private void ejecutarDSUB() {
		JDouble op1 = (JDouble) topFrame.operands.pop();
		JDouble op2 = (JDouble) topFrame.operands.pop();
		topFrame.push(op1.sub(op2));
		topFrame.incPC();
	}

	private void ejecutarIMUL() {
		JInteger op1 = (JInteger) topFrame.operands.pop();
		JInteger op2 = (JInteger) topFrame.operands.pop();
		if (symbolicExec) {
			if (op1.isSymbol() || op2.isSymbol()) {
				nTemp++;
			}
			topFrame.push(op1.mulSymb(op2, TEMP + Integer.toString(nTemp)));
		} else {
			topFrame.push(op1.mul(op2));
		}
		topFrame.incPC();
	}

	private void ejecutarLMUL() {
		JLong op1 = (JLong) topFrame.operands.pop();
		JLong op2 = (JLong) topFrame.operands.pop();
		topFrame.push(op1.mul(op2));
		topFrame.incPC();
	}

	private void ejecutarFMUL() {
		JFloat op1 = (JFloat) topFrame.operands.pop();
		JFloat op2 = (JFloat) topFrame.operands.pop();
		topFrame.push(op1.mul(op2));
		topFrame.incPC();
	}

	private void ejecutarDMUL() {
		JDouble op1 = (JDouble) topFrame.operands.pop();
		JDouble op2 = (JDouble) topFrame.operands.pop();
		topFrame.push(op1.mul(op2));
		topFrame.incPC();
	}

	private void ejecutarIDIV() {
		JInteger op1 = (JInteger) topFrame.operands.pop();
		JInteger op2 = (JInteger) topFrame.operands.pop();
		if (symbolicExec) {
			if (op1.isSymbol() || op2.isSymbol()) {
				nTemp++;
			}
			topFrame.push(op1.divSymb(op2, TEMP + Integer.toString(nTemp)));
		} else {
			topFrame.push(op1.div(op2));
		}
		topFrame.incPC();
	}

	private void ejecutarLDIV() {
		JLong op1 = (JLong) topFrame.operands.pop();
		JLong op2 = (JLong) topFrame.operands.pop();
		topFrame.push(op1.div(op2));
		topFrame.incPC();
	}

	private void ejecutarFDIV() {
		JFloat op1 = (JFloat) topFrame.operands.pop();
		JFloat op2 = (JFloat) topFrame.operands.pop();
		topFrame.push(op1.div(op2));
		topFrame.incPC();
	}

	private void ejecutarDDIV() {
		JDouble op1 = (JDouble) topFrame.operands.pop();
		JDouble op2 = (JDouble) topFrame.operands.pop();
		topFrame.push(op1.div(op2));
		topFrame.incPC();
	}

	private void ejecutarIREM() {
		JInteger op1 = (JInteger) topFrame.operands.pop();
		JInteger op2 = (JInteger) topFrame.operands.pop();

		if (symbolicExec) {
			if (op1.isSymbol() || op2.isSymbol()) {
				nTemp++;
			}
			topFrame.push(op1.remSymb(op2, TEMP + Integer.toString(nTemp)));
		} else {
			topFrame.push(op1.rem(op2));
		}
		topFrame.incPC();
	}

	private void ejecutarLREM() {
		JLong op1 = (JLong) topFrame.operands.pop();
		JLong op2 = (JLong) topFrame.operands.pop();
		topFrame.push(op1.rem(op2));
		topFrame.incPC();
	}

	private void ejecutarFREM() {
		JFloat op1 = (JFloat) topFrame.operands.pop();
		JFloat op2 = (JFloat) topFrame.operands.pop();
		topFrame.push(op1.rem(op2));
		topFrame.incPC();
	}

	private void ejecutarDREM() {
		JDouble op1 = (JDouble) topFrame.operands.pop();
		JDouble op2 = (JDouble) topFrame.operands.pop();
		topFrame.push(op1.rem(op2));
		topFrame.incPC();
	}

	private void ejecutarINEG() {
		JInteger op1 = (JInteger) topFrame.operands.pop();
		if (symbolicExec) {
			if (!op1.isSymbol()) {
				nTemp++;
			}
			topFrame.push(op1.negSymb(TEMP + Integer.toString(nTemp)));
		} else {
			topFrame.push(op1.neg());
		}
		topFrame.incPC();
	}

	private void ejecutarLNEG() {
		JLong op1 = (JLong) topFrame.operands.pop();
		topFrame.push(op1.neg());
		topFrame.incPC();
	}

	private void ejecutarFNEG() {
		JFloat op1 = (JFloat) topFrame.operands.pop();
		topFrame.push(op1.neg());
		topFrame.incPC();
	}

	private void ejecutarDNEG() {
		JDouble op1 = (JDouble) topFrame.operands.pop();
		topFrame.push(op1.neg());
		topFrame.incPC();
	}

	// TODO AC..
	private void ejecutarIF_ACMPEQ(int index) {
		JObject val2 = (JObject) topFrame.pop();
		JObject val1 = (JObject) topFrame.pop();
		if (val1.heapPtr == val2.heapPtr) {
			topFrame.incPC(index);
		} else {
			topFrame.incPC(3);
		}
	}

	private void ejecutarIF_ACMPNE(int index) {
		JObject val2 = (JObject) topFrame.pop();
		JObject val1 = (JObject) topFrame.pop();
		if (val1.heapPtr != val2.heapPtr) {
			topFrame.incPC(index);
		} else {
			topFrame.incPC(3);
		}
	}

	private void ejecutarIFNONNULL(int index, boolean lastDec) {
		JObject val = (JObject) topFrame.pop();
		if (lastDec && val.isSymbol()) {
			if (DEBUG.ON) {
				System.out.println("[CONDITION = TRUE]");
			}
			JConstraintSolver cs = JConstraintSolver.getInstance();
			// Constraint c = Choco.FALSE;
			// constraints.add(c);
			// cs.addConstraint(c);

			topFrame.incPC(index);

		} else if (val.isSymbol()) {
			if (DEBUG.ON) {
				System.out.println("[CONDITION = FALSE]");
			}
			JConstraintSolver cs = JConstraintSolver.getInstance();

			topFrame.incPC(3);

		} else if (val.notNull()) {
			topFrame.incPC(index);
		} else {
			topFrame.incPC(3);
		}
	}

	private void ejecutarIFNULL(int index, boolean lastDec) {
		JObject val = (JObject) topFrame.pop();
		if (lastDec && val.isSymbol()) {
			if (DEBUG.ON) {
				System.out.println("[CONDITION = TRUE]");
			}
			JConstraintSolver cs = JConstraintSolver.getInstance();
			// Constraint c = Choco.FALSE;
			// constraints.add(c);
			// cs.addConstraint(c);
			topFrame.incPC(index);

		} else if (val.isSymbol()) {
			if (DEBUG.ON) {
				System.out.println("[CONDITION = FALSE]");
			}
			JConstraintSolver cs = JConstraintSolver.getInstance();

			topFrame.incPC(3);

		} else if (!val.notNull()) {
			topFrame.incPC(index);
		} else {
			topFrame.incPC(3);
		}
	}

	private void ejecutarIFEQ(int index, boolean lastDec) {
		JInteger val = (JInteger) topFrame.pop();
		if (lastDec && val.isSymbol()) {
			if (DEBUG.ON) {
				System.out.println("[CONDITION = TRUE]");
			}
			JConstraintSolver cs = JConstraintSolver.getInstance();
			IntegerVariable v = (IntegerVariable) cs.getVar(val.getName());

			Constraint c = Choco.eq(v, 0);
			constraints.add(c);
			cs.addConstraint(c);

			topFrame.incPC(index);

		} else if (val.isSymbol()) {
			if (DEBUG.ON) {
				System.out.println("[CONDITION = FALSE]");
			}
			JConstraintSolver cs = JConstraintSolver.getInstance();
			IntegerVariable v = (IntegerVariable) cs.getVar(val.getName());

			Constraint c = Choco.neq(v, 0);
			constraints.add(c);
			cs.addConstraint(c);

			topFrame.incPC(3);

		} else if (val.getValue() == 0) {
			topFrame.incPC(index);
		} else {
			topFrame.incPC(3);
		}

	}

	private void ejecutarIFNE(int index, boolean lastDec) {
		JInteger val = (JInteger) topFrame.pop();
		if (lastDec && val.isSymbol()) {
			if (DEBUG.ON) {
				System.out.println("[CONDITION = TRUE]");
			}
			JConstraintSolver cs = JConstraintSolver.getInstance();
			IntegerVariable v = (IntegerVariable) cs.getVar(val.getName());

			Constraint c = Choco.neq(v, 0);
			constraints.add(c);
			cs.addConstraint(c);

			topFrame.incPC(index);

		} else if (val.isSymbol()) {
			if (DEBUG.ON) {
				System.out.println("[CONDITION = FALSE]");
			}
			JConstraintSolver cs = JConstraintSolver.getInstance();
			IntegerVariable v = (IntegerVariable) cs.getVar(val.getName());

			Constraint c = Choco.eq(v, 0);
			constraints.add(c);
			cs.addConstraint(c);

			topFrame.incPC(3);
		} else if (val.getValue() != 0) {
			topFrame.incPC(index);
		} else {
			topFrame.incPC(3);
		}

	}

	private void ejecutarIFLT(int index, boolean lastDec) {
		JInteger val = (JInteger) topFrame.pop();
		if (lastDec && val.isSymbol()) {
			if (DEBUG.ON) {
				System.out.println("[CONDITION = TRUE]");
			}
			JConstraintSolver cs = JConstraintSolver.getInstance();
			IntegerVariable v = (IntegerVariable) cs.getVar(val.getName());

			Constraint c = Choco.lt(v, 0);
			constraints.add(c);
			cs.addConstraint(c);

			topFrame.incPC(index);

		} else if (val.isSymbol()) {
			if (DEBUG.ON) {
				System.out.println("[CONDITION = FALSE]");
			}
			JConstraintSolver cs = JConstraintSolver.getInstance();
			IntegerVariable v = (IntegerVariable) cs.getVar(val.getName());

			Constraint c = Choco.geq(v, 0);
			constraints.add(c);
			cs.addConstraint(c);

			topFrame.incPC(3);
		} else if (val.getValue() < 0) {
			topFrame.incPC(index);
		} else {
			topFrame.incPC(3);
		}
	}

	private void ejecutarIFGE(int index, boolean lastDec) {
		JInteger val = (JInteger) topFrame.pop();
		if (lastDec && val.isSymbol()) {
			if (DEBUG.ON) {
				System.out.println("[CONDITION = TRUE]");
			}
			JConstraintSolver cs = JConstraintSolver.getInstance();
			IntegerVariable v = (IntegerVariable) cs.getVar(val.getName());

			Constraint c = Choco.geq(v, 0);
			constraints.add(c);
			cs.addConstraint(c);

			topFrame.incPC(index);

		} else if (val.isSymbol()) {
			if (DEBUG.ON) {
				System.out.println("[CONDITION = FALSE]");
			}
			JConstraintSolver cs = JConstraintSolver.getInstance();
			IntegerVariable v = (IntegerVariable) cs.getVar(val.getName());

			Constraint c = Choco.lt(v, 0);
			constraints.add(c);
			cs.addConstraint(c);

			topFrame.incPC(3);
		} else if (val.getValue() >= 0) {
			topFrame.incPC(index);
		} else {
			topFrame.incPC(3);
		}
	}

	private void ejecutarIFGT(int index, boolean lastDec) {
		JInteger val = (JInteger) topFrame.pop();
		if (lastDec && val.isSymbol()) {
			if (DEBUG.ON) {
				System.out.println("[CONDITION = TRUE]");
			}
			JConstraintSolver cs = JConstraintSolver.getInstance();
			IntegerVariable v = (IntegerVariable) cs.getVar(val.getName());

			Constraint c = Choco.gt(v, 0);
			constraints.add(c);
			cs.addConstraint(c);

			topFrame.incPC(index);

		} else if (val.isSymbol()) {
			if (DEBUG.ON) {
				System.out.println("[CONDITION = FALSE]");
			}
			JConstraintSolver cs = JConstraintSolver.getInstance();
			IntegerVariable v = (IntegerVariable) cs.getVar(val.getName());

			Constraint c = Choco.leq(v, 0);
			constraints.add(c);
			cs.addConstraint(c);

			topFrame.incPC(3);
		} else if (val.getValue() > 0) {
			topFrame.incPC(index);
		} else {
			topFrame.incPC(3);
		}

	}

	private void ejecutarIFLE(int index, boolean lastDec) {
		JInteger val = (JInteger) topFrame.pop();
		if (lastDec && val.isSymbol()) {
			if (DEBUG.ON) {
				System.out.println("[CONDITION = TRUE]");
			}
			JConstraintSolver cs = JConstraintSolver.getInstance();
			IntegerVariable v = (IntegerVariable) cs.getVar(val.getName());

			Constraint c = Choco.leq(v, 0);
			constraints.add(c);
			cs.addConstraint(c);

			topFrame.incPC(index);

		} else if (val.isSymbol()) {
			if (DEBUG.ON) {
				System.out.println("[CONDITION = FALSE]");
			}
			JConstraintSolver cs = JConstraintSolver.getInstance();
			IntegerVariable v = (IntegerVariable) cs.getVar(val.getName());

			Constraint c = Choco.gt(v, 0);
			constraints.add(c);
			cs.addConstraint(c);

			topFrame.incPC(3);
		} else if (val.getValue() <= 0) {
			topFrame.incPC(index);
		} else {
			topFrame.incPC(3);
		}

	}

	private void ejecutarIF_ICMPEQ(int index, boolean lastDec) {
		JInteger val2 = (JInteger) topFrame.pop();
		JInteger val1 = (JInteger) topFrame.pop();

		if (lastDec && val1.isSymbol()) {
			if (DEBUG.ON) {
				System.out.println("[CONDITION = TRUE]");
			}
			JConstraintSolver cs = JConstraintSolver.getInstance();
			IntegerVariable v1 = (IntegerVariable) cs.getVar(val1.getName());
			Constraint c;

			if (val2.isSymbol()) {
				IntegerVariable v2 = (IntegerVariable) cs
						.getVar(val2.getName());
				c = Choco.eq(v1, v2);
			} else {
				c = Choco.eq(v1, val2.getValue());
			}

			constraints.add(c);
			cs.addConstraint(c);

			topFrame.incPC(index);

		} else if (val1.isSymbol()) {
			if (DEBUG.ON) {
				System.out.println("[CONDITION = FALSE]");
			}
			JConstraintSolver cs = JConstraintSolver.getInstance();
			IntegerVariable v1 = (IntegerVariable) cs.getVar(val1.getName());
			Constraint c;

			if (val2.isSymbol()) {
				IntegerVariable v2 = (IntegerVariable) cs
						.getVar(val2.getName());
				c = Choco.neq(v1, v2);
			} else {
				c = Choco.neq(v1, val2.getValue());
			}
			constraints.add(c);
			cs.addConstraint(c);

			topFrame.incPC(3);
		} else if (val1.getValue() == val2.getValue()) {
			topFrame.incPC(index);
		} else {
			topFrame.incPC(3);
		}

	}

	private void ejecutarIF_ICMPNE(int index, boolean lastDec) {
		JInteger val2 = (JInteger) topFrame.pop();
		JInteger val1 = (JInteger) topFrame.pop();

		if (lastDec && val1.isSymbol()) {
			if (DEBUG.ON) {
				System.out.println("[CONDITION = TRUE]");
			}
			JConstraintSolver cs = JConstraintSolver.getInstance();
			IntegerVariable v1 = (IntegerVariable) cs.getVar(val1.getName());
			Constraint c;

			if (val2.isSymbol()) {
				IntegerVariable v2 = (IntegerVariable) cs
						.getVar(val2.getName());
				c = Choco.neq(v1, v2);
			} else {
				c = Choco.neq(v1, val2.getValue());
			}

			constraints.add(c);
			cs.addConstraint(c);

			topFrame.incPC(index);

		} else if (val1.isSymbol()) {
			if (DEBUG.ON) {
				System.out.println("[CONDITION = FALSE]");
			}
			JConstraintSolver cs = JConstraintSolver.getInstance();
			IntegerVariable v1 = (IntegerVariable) cs.getVar(val1.getName());
			Constraint c;

			if (val2.isSymbol()) {
				IntegerVariable v2 = (IntegerVariable) cs
						.getVar(val2.getName());
				c = Choco.eq(v1, v2);
			} else {
				c = Choco.eq(v1, val2.getValue());
			}
			constraints.add(c);
			cs.addConstraint(c);

			topFrame.incPC(3);
		} else if (val1.getValue() != val2.getValue()) {
			topFrame.incPC(index);
		} else {
			topFrame.incPC(3);
		}
	}

	private void ejecutarIF_ICMPLT(int index, boolean lastDec) {
		JInteger val2 = (JInteger) topFrame.pop();
		JInteger val1 = (JInteger) topFrame.pop();

		if (lastDec && val1.isSymbol()) {
			if (DEBUG.ON) {
				System.out.println("[CONDITION = TRUE]");
			}
			JConstraintSolver cs = JConstraintSolver.getInstance();
			IntegerVariable v1 = (IntegerVariable) cs.getVar(val1.getName());
			Constraint c;

			if (val2.isSymbol()) {
				IntegerVariable v2 = (IntegerVariable) cs
						.getVar(val2.getName());
				c = Choco.lt(v1, v2);
			} else {
				c = Choco.lt(v1, val2.getValue());
			}

			constraints.add(c);
			cs.addConstraint(c);

			topFrame.incPC(index);

		} else if (val1.isSymbol()) {
			if (DEBUG.ON) {
				System.out.println("[CONDITION = FALSE]");
			}
			JConstraintSolver cs = JConstraintSolver.getInstance();
			IntegerVariable v1 = (IntegerVariable) cs.getVar(val1.getName());
			Constraint c;

			if (val2.isSymbol()) {
				IntegerVariable v2 = (IntegerVariable) cs
						.getVar(val2.getName());
				c = Choco.geq(v1, v2);
			} else {
				c = Choco.geq(v1, val2.getValue());
			}
			constraints.add(c);
			cs.addConstraint(c);

			topFrame.incPC(3);
		} else if (val1.getValue() < val2.getValue()) {
			topFrame.incPC(index);
		} else {
			topFrame.incPC(3);
		}
	}

	private void ejecutarIF_ICMPGE(int index, boolean lastDec) {
		JInteger val2 = (JInteger) topFrame.pop();
		JInteger val1 = (JInteger) topFrame.pop();

		if (lastDec && val1.isSymbol()) {
			if (DEBUG.ON) {
				System.out.println("[CONDITION = TRUE]");
			}
			JConstraintSolver cs = JConstraintSolver.getInstance();
			IntegerVariable v1 = (IntegerVariable) cs.getVar(val1.getName());
			Constraint c;

			if (val2.isSymbol()) {
				IntegerVariable v2 = (IntegerVariable) cs
						.getVar(val2.getName());
				c = Choco.geq(v1, v2);
			} else {
				c = Choco.geq(v1, val2.getValue());
			}

			constraints.add(c);
			cs.addConstraint(c);

			topFrame.incPC(index);

		} else if (val1.isSymbol()) {
			if (DEBUG.ON) {
				System.out.println("[CONDITION = FALSE]");
			}
			JConstraintSolver cs = JConstraintSolver.getInstance();
			IntegerVariable v1 = (IntegerVariable) cs.getVar(val1.getName());
			Constraint c;

			if (val2.isSymbol()) {
				IntegerVariable v2 = (IntegerVariable) cs
						.getVar(val2.getName());
				c = Choco.lt(v1, v2);
			} else {
				c = Choco.lt(v1, val2.getValue());
			}
			constraints.add(c);
			cs.addConstraint(c);

			topFrame.incPC(3);
		} else if (val1.getValue() >= val2.getValue()) {
			topFrame.incPC(index);
		} else {
			topFrame.incPC(3);
		}
	}

	private void ejecutarIF_ICMPGT(int index, boolean lastDec) {
		JInteger val2 = (JInteger) topFrame.pop();
		JInteger val1 = (JInteger) topFrame.pop();

		if (lastDec && val1.isSymbol()) {
			if (DEBUG.ON) {
				System.out.println("[CONDITION = TRUE]");
			}
			JConstraintSolver cs = JConstraintSolver.getInstance();
			IntegerVariable v1 = (IntegerVariable) cs.getVar(val1.getName());
			Constraint c;

			if (val2.isSymbol()) {
				IntegerVariable v2 = (IntegerVariable) cs
						.getVar(val2.getName());
				c = Choco.gt(v1, v2);
			} else {
				c = Choco.gt(v1, val2.getValue());
			}

			constraints.add(c);
			cs.addConstraint(c);

			topFrame.incPC(index);

		} else if (val1.isSymbol()) {
			if (DEBUG.ON) {
				System.out.println("[CONDITION = FALSE]");
			}
			JConstraintSolver cs = JConstraintSolver.getInstance();
			IntegerVariable v1 = (IntegerVariable) cs.getVar(val1.getName());
			Constraint c;

			if (val2.isSymbol()) {
				IntegerVariable v2 = (IntegerVariable) cs
						.getVar(val2.getName());
				c = Choco.leq(v1, v2);
			} else {
				c = Choco.leq(v1, val2.getValue());
			}
			constraints.add(c);
			cs.addConstraint(c);

			topFrame.incPC(3);
		} else if (val1.getValue() > val2.getValue()) {
			topFrame.incPC(index);
		} else {
			topFrame.incPC(3);
		}
	}

	private void ejecutarIF_ICMPLE(int index, boolean lastDec) {
		JInteger val2 = (JInteger) topFrame.pop();
		JInteger val1 = (JInteger) topFrame.pop();

		if (lastDec && val1.isSymbol()) {
			if (DEBUG.ON) {
				System.out.println("[CONDITION = TRUE]");
			}
			JConstraintSolver cs = JConstraintSolver.getInstance();
			IntegerVariable v1 = (IntegerVariable) cs.getVar(val1.getName());
			Constraint c;

			if (val2.isSymbol()) {
				IntegerVariable v2 = (IntegerVariable) cs
						.getVar(val2.getName());
				c = Choco.leq(v1, v2);
			} else {
				c = Choco.leq(v1, val2.getValue());
			}

			constraints.add(c);
			cs.addConstraint(c);

			topFrame.incPC(index);

		} else if (val1.isSymbol()) {
			if (DEBUG.ON) {
				System.out.println("[CONDITION = FALSE]");
			}
			JConstraintSolver cs = JConstraintSolver.getInstance();
			IntegerVariable v1 = (IntegerVariable) cs.getVar(val1.getName());
			Constraint c;

			if (val2.isSymbol()) {
				IntegerVariable v2 = (IntegerVariable) cs
						.getVar(val2.getName());
				c = Choco.gt(v1, v2);
			} else {
				c = Choco.gt(v1, val2.getValue());
			}
			constraints.add(c);
			cs.addConstraint(c);

			topFrame.incPC(3);
		} else if (val1.getValue() <= val2.getValue()) {
			topFrame.incPC(index);
		} else {
			topFrame.incPC(3);
		}

	}

	private void ejecutarLCMP() {
		JLong op1 = (JLong) topFrame.pop();
		JLong op2 = (JLong) topFrame.pop();
		if (op2.getValue() > op1.getValue())
			topFrame.push(new JInteger(1));
		else if (op2.getValue() < op1.getValue())
			topFrame.push(new JInteger(-1));
		else
			topFrame.push(new JInteger(0));
		topFrame.incPC();
	}

	private void ejecutarDCMPL() {
		JDouble op1 = (JDouble) topFrame.pop();
		JDouble op2 = (JDouble) topFrame.pop();
		if (op2.getValue() > op1.getValue())
			topFrame.push(new JInteger(1));
		else if (op2.getValue() < op1.getValue())
			topFrame.push(new JInteger(-1));
		else
			topFrame.push(new JInteger(0));
		topFrame.incPC();
	}

	private void ejecutarDCMPG() {
		ejecutarDCMPL();
	}

	private void ejecutarGOTO(int index) {
		topFrame.incPC(index);
	}

	private void ejecutarNEW(int index) {
		JObject object;
		ConstantPool cp = topFrame.getConstantPool();
		ConstantClass cc = (ConstantClass) cp.getConstant(index,
				Constants.CONSTANT_Class);
		String className = cc.getBytes(cp);
		JClass jClass = classes.loadClass(className);
		object = heap.createObject(jClass);
		if (symbolicExec) {
			if (!object.isException()) {
				object.setSymbol(true);
				for (int j = 0; j < object.jFields.length; j++) {
					// no usamos valor devuelto para evitar sobreescribir el
					// valor del field si existe desde la clase (static)
					String namepro = object.getName() + "."
							+ object.jFields[j].getName();
					Exec.newJValues(object.jFields[j].fieldInfo.getType(),
							namepro);
					object.jFields[j].getValue().setSymbol(true);
					object.jFields[j].getValue().setName(namepro);
				}
			}
		}
		topFrame.push(object);
		topFrame.incPC(3);
	}

	private void ejecutarGETFIELD(int index, boolean lastDec) {
		JObject o = (JObject) topFrame.pop();
		// comun para todos los casos
		ConstantPool cp = topFrame.getConstantPool();
		ConstantFieldref cfr = (ConstantFieldref) cp.getConstant(index,
				Constants.CONSTANT_Fieldref);
		ConstantNameAndType cnat = (ConstantNameAndType) cp.getConstant(
				cfr.getNameAndTypeIndex(), Constants.CONSTANT_NameAndType);
		String fieldName = cnat.getName(cp);

		if (lastDec && o.isSymbol()) {
			if (DEBUG.ON) {
				System.out.println("[CONDITION = NULL]");
			}
			// TODO constraint a usar?¿
			// A lo mejor tenemos que crearnos una nueva Constraint en Choco
			// pero ahora con dar false creo que bastar‡ (4.5.2 en la
			// documentaci—n)
			JConstraintSolver cs = JConstraintSolver.getInstance();
			Constraint c = Choco.FALSE;
			constraints.add(c);
			cs.addConstraint(c);
			createThrowable(o);
			ejecutarATHROW();
		} else if (o.isSymbol()) {
			if (DEBUG.ON) {
				System.out.println("[CONDITION = NOTNULL]");
			}
			JValue val = o.getField(fieldName).getValue();
			// createConstraintGetField(val);
			topFrame.push(val);
			topFrame.incPC(3);
		} else {
			topFrame.push(o.getField(fieldName).getValue());
			topFrame.incPC(3);
		}
	}

	private void ejecutarPUTFIELD(int index, boolean lastDec) {
		JValue val = topFrame.pop();
		JObject o = (JObject) topFrame.pop();
		ConstantPool cp = topFrame.getConstantPool();
		ConstantFieldref cfr = (ConstantFieldref) cp.getConstant(index,
				Constants.CONSTANT_Fieldref);
		ConstantNameAndType cnat = (ConstantNameAndType) cp.getConstant(
				cfr.getNameAndTypeIndex(), Constants.CONSTANT_NameAndType);
		String fieldName = cnat.getName(cp);
		if (lastDec && o.isSymbol()) {
			if (DEBUG.ON) {
				System.out.println("[CONDITION = NULL]");
			}
			// TODO constraint a usar?¿
			Constraint c = Choco.FALSE;
			constraints.add(c);
			JConstraintSolver cs = JConstraintSolver.getInstance();
			cs.addConstraint(c);

			createThrowable(o);
			ejecutarATHROW();
		} else if (o.isSymbol()) {
			if (DEBUG.ON) {
				System.out.println("[CONDITION = NOTNULL]");
			}
			JConstraintSolver cs = JConstraintSolver.getInstance();
			Constraint c = null;
			JValue local = o.getField(fieldName).getValue();
			Variable v1, v2;
			v1 = generateVariable(cs, local);
			if (val.isSymbol()) {
				Constraint lastAsig = val.getConstraintAsig();
				if (lastAsig != null) {
					Variable[] aVars = lastAsig.getVariables();
					ArrayList<Variable> varList = getVar(aVars);
					for (Variable var : varList) {
						if (var.equals(v1)) {
							JValue jValue = (JValue) local.deepCopy();
							jValue.setName(local.getName().substring(0, 5)
									+ "_" + local.getCopies());
							jValue.setCopies(local.getCopies() + 1);
							if (local instanceof JInteger)
								v1 = Choco.makeIntVar(jValue.getName(),
										Options.V_BOUND);
							else if (local instanceof JFloat)
								v1 = Choco.makeRealVar(jValue.getName(),
										Double.MIN_VALUE, Double.MAX_VALUE,
										Options.V_BOUND);
							cs.addVariable(v1);
							o.setFieldValue(fieldName, jValue);
						}
					}
				}

				// TODO lo de constraint negation?¿

				if (local instanceof JInteger) {
					v2 = (IntegerVariable) cs.getVar(val.getName());
					c = Choco.eq((IntegerVariable) v1, (IntegerVariable) v2);
				} else if (local instanceof JFloat) {
					v2 = (RealVariable) cs.getVar(val.getName());
					c = Choco.eq((RealVariable) v1, (RealVariable) v2);
				}
				cs.addConstraint(c);
				local.setConstraintAsig(c);
				constraints.add(c);
			} else {
				c = local.getConstraintAsig();
				if (c != null) {
					cs.removeConstraint(c);
					local.setConstraintAsig(null);
				}
				if (local instanceof JFloat || local instanceof JDouble)
					addConstraintSymbolReal(local, val);
				else
					addConstraintSymbolInt(local, val);
			}
			// para solo mod el valor del JValue
			o.getField(fieldName).getValue().setValue(val);
			// o.setFieldValue(fieldName, val);
			topFrame.incPC(3);
		} else {
			o.setFieldValue(fieldName, val);
			topFrame.incPC(3);
		}
	}

	private Variable generateVariable(JConstraintSolver cs, JValue local) {
		Variable v1;
		if (local instanceof JInteger)
			v1 = (IntegerVariable) cs.getVar(local.getName());
		else if (local instanceof JFloat)
			v1 = (RealVariable) cs.getVar(local.getName());
		else {
			v1 = null;
		}
		return v1;
	}

	private void ejecutarGETSTATIC(int index) {
		ConstantPool cp = topFrame.getConstantPool();
		ConstantFieldref cfr = (ConstantFieldref) cp.getConstant(index,
				Constants.CONSTANT_Fieldref);
		ConstantNameAndType cnat = (ConstantNameAndType) cp.getConstant(
				cfr.getNameAndTypeIndex(), Constants.CONSTANT_NameAndType);
		String fieldName = cnat.getName(cp);
		String className = cfr.getClass(cp);
		// si debo ejecutar clinit, lo ejecuto pero no avanzo en ejecucion
		// de JVM, para k en la siguiente pasada se ejecute correctamente
		// donde se quedo
		if (classes.checkCLINIT(className)) {
			classes.addClass(prepararCLINIT(className));
		} else {
			JClass otra = classes.loadClass(className);
			JValue val = otra.getField(fieldName).getValue();
			if (symbolicExec) {
				otra.convertToSymbol();
				createConstraintGetField(val);
			}
			topFrame.push(val);
			topFrame.incPC(3);
		}
	}

	private void ejecutarPUTSTATIC(int index) {
		JValue val = topFrame.pop();
		ConstantPool cp = topFrame.getConstantPool();
		ConstantFieldref cfr = (ConstantFieldref) cp.getConstant(index,
				Constants.CONSTANT_Fieldref);
		ConstantNameAndType cnat = (ConstantNameAndType) cp.getConstant(
				cfr.getNameAndTypeIndex(), Constants.CONSTANT_NameAndType);
		String fieldName = cnat.getName(cp);
		String className = cfr.getClass(cp);
		if (classes.checkCLINIT(className)) {
			classes.addClass(prepararCLINIT(className));
		} else {
			JClass otra = classes.loadClass(className);
			if (symbolicExec) {
				otra.convertToSymbol();
				JConstraintSolver cs = JConstraintSolver.getInstance();
				Constraint c = null;
				JValue local = otra.getField(fieldName).getValue();
				Variable v1, v2;
				v1 = generateVariable(cs, local);
				if (val.isSymbol()) {
					Constraint lastAsig = val.getConstraintAsig();
					if (lastAsig != null) {
						Variable[] aVars = lastAsig.getVariables();
						ArrayList<Variable> varList = getVar(aVars);
						for (Variable var : varList) {
							if (var.equals(v1)) {
								JValue jValue = (JValue) local.deepCopy();
								jValue.setName(local.getName().substring(0, 5)
										+ "_" + local.getCopies());
								jValue.setCopies(local.getCopies() + 1);
								if (local instanceof JInteger)
									v1 = Choco.makeIntVar(jValue.getName(),
											Options.V_BOUND);
								else if (local instanceof JFloat)
									v1 = Choco.makeRealVar(jValue.getName(),
											Double.MIN_VALUE, Double.MAX_VALUE,
											Options.V_BOUND);
								cs.addVariable(v1);
								otra.setFieldValue(fieldName, jValue);
							}
						}
					}
					// TODO lo de constraint negation?¿
					if (local instanceof JInteger) {
						v2 = (IntegerVariable) cs.getVar(val.getName());
						c = Choco
								.eq((IntegerVariable) v1, (IntegerVariable) v2);
					} else if (local instanceof JFloat) {
						v2 = (RealVariable) cs.getVar(val.getName());
						c = Choco.eq((RealVariable) v1, (RealVariable) v2);
					}
					cs.addConstraint(c);
					local.setConstraintAsig(c);
					constraints.add(c);
				} else {
					c = local.getConstraintAsig();
					if (c != null) {
						cs.removeConstraint(c);
						local.setConstraintAsig(null);
					}
					if (local instanceof JFloat || local instanceof JDouble)
						addConstraintSymbolReal(local, val);
					else
						addConstraintSymbolInt(local, val);
				}
				// para modificar solo el valor del JValue
				otra.getField(fieldName).getValue().setValue(val);
				// otra.setFieldValue(fieldName, val);
				topFrame.incPC(3);
			} else {
				otra.setFieldValue(fieldName, val);
				topFrame.incPC(3);
			}
		}
	}

	/**
	 * Prepara la JVM para ejecutar el metodo clinit de una clase className
	 * 
	 * @param className
	 *            el nombre de la clase
	 * @return el JClass de la clase
	 */
	private JClass prepararCLINIT(String className) {
		JClass otra = classes.getClass(className);
		JMethod clinit = otra.getCLINIT();
		StackFrame clinitFrame = new StackFrame(topFrame, clinit, otra);
		setTopFrame(clinitFrame);
		return otra;
	}

	private void ejecutarARRAYLENGTH() {
		JArray array = (JArray) topFrame.pop();
		topFrame.push(new JInteger(array.nElems));
		topFrame.incPC();
	}

	private void ejecutarNEWARRAY(short type) {
		JInteger nElems = (JInteger) topFrame.pop();
		JArray array = heap.createNewArray(type, nElems.getValue());
		if (symbolicExec) {
			array.setSymbol(true);
			for (int j = 0; j < array.data.length; j++) {
				String namepro = array.getName() + "[" + j + "]";
				array.data[j] = Exec.newJValues(array.type, namepro);
				array.data[j].setSymbol(true);
				array.data[j].setName(namepro);
			}
		}
		topFrame.push(array);
		topFrame.incPC(2);
	}

	private void ejecutarANEWARRAY(int index) {

		JInteger nElems = (JInteger) topFrame.pop();
		ConstantPool cp = topFrame.getConstantPool();
		ConstantClass cc = (ConstantClass) cp.getConstant(index,
				Constants.CONSTANT_Class);
		String className = cc.getBytes(cp);
		JClass jClass = classes.loadClass(className);
		JArray array = heap.createNewObjectArray(jClass, nElems.getValue());
		if (symbolicExec) {
			array.setSymbol(true);
			int length = array.getnElems();
			JObject jo;
			for (int j = 0; j < length; j++) {
				jo = (JObject) array.get(j);
				jo.setSymbol(true);
				jo.setName(array.getName() + "[" + jo.getName() + "]");
				for (int k = 0; k < jo.jFields.length; k++) {
					String namepro = jo.getName() + "."
							+ jo.jFields[k].getName();
					Exec.newJValues(jo.jFields[k].fieldInfo.getType(), namepro);
					jo.jFields[k].getValue().setSymbol(true);
					jo.jFields[k].getValue().setName(namepro);
				}
			}
		}
		topFrame.push(array);
		topFrame.incPC(3);
	}

	// Ejecucion Instrucciones
	private void ejecutarINVOKEBC(Short stat, short[] code) {

		// get clase y method info a partir de BC

		ConstantPool cp = topFrame.getConstantPool();
		ConstantMethodref methodIndex = (ConstantMethodref) cp
				.getConstant(
						getIndex(code[topFrame.getPC() + 1],
								code[topFrame.getPC() + 2]),
						Constants.CONSTANT_Methodref);

		// get clase
		// modo largo, siguiendo cadena referencias
		// ConstantClass classIndex =(ConstantClass)
		// cp.getConstant(methodIndex.getClassIndex(),Constants.CONSTANT_Class);
		// ConstantString stringClassIndex=(ConstantString)
		// cp.getConstant(classIndex.getNameIndex(),Constants.CONSTANT_String);
		// ConstantUtf8 utf8ClassIndex=(ConstantUtf8)
		// cp.getConstant(stringClassIndex.getStringIndex(),Constants.CONSTANT_Utf8);
		// String className =utf8ClassIndex.toString();

		// USANDO ATAJO
		String className = methodIndex.getClass(cp);

		// get method
		ConstantNameAndType nameAndTypeIndex = (ConstantNameAndType) cp
				.getConstant(methodIndex.getNameAndTypeIndex());
		// v2 ConstantNameAndType nameAndTypeIndex=(ConstantNameAndType)
		// cp.getConstant(methodIndex.getNameAndTypeIndex(),Constants.CONSTANT_NameAndType);
		String methodName = nameAndTypeIndex.getName(cp);
		String signature = nameAndTypeIndex.getSignature(cp);

		JClass jClass = classes.loadClass(className);
		JMethod jMethod = jClass.getMethod(methodName, signature);

		// maximo argumentos
		int maxArgs = jMethod.getMaxArgs() + 1;

		// si es estatico disminuyo params
		if (stat != null)
			maxArgs--;
		// get pila en orden para parametros
		Stack<JValue> stack = topFrame.getParamsReverse(maxArgs);
		topFrame.clearParams(maxArgs);

		StackFrame newTopFrame;
		int desp = 0;
		if (stat != null) {
			newTopFrame = new StackFrame(topFrame, jMethod, jClass);
		} else {
			// si no es estatico, meto referencia en locals[0]
			desp = 1;
			JObject ref = (JObject) stack.pop();
			newTopFrame = new StackFrame(topFrame, jMethod, jClass, ref);
		}
		// paso parametros tras que se ha creado la estructura
		for (int j = 0; j < maxArgs - desp && !stack.empty(); j++) {
			JValue val = stack.pop();
			// si no es el primero y el anterior ocupa 2 espacios, lo desplazo
			if (j != 0
					&& (newTopFrame.getLocals()[j - 1].type == Type.LONG || newTopFrame
							.getLocals()[j - 1].type == Type.DOUBLE)) {
				newTopFrame.getLocals()[j + desp + 1] = val;
			} else
				newTopFrame.getLocals()[j + desp] = val;
		}
		topFrame.incPC(3);
		setTopFrame(newTopFrame);
	}

	private void ejecutarINVOKE(Instruction ins) {

		// cast to invokeinstrucction
		InvokeInstruction i = (InvokeInstruction) ins;
		// get clase y method info
		ConstantPoolGen cgp = topFrame.getConstantPoolGen();
		ObjectType clase = (ObjectType) i.getReferenceType(cgp);
		String methodName = i.getMethodName(cgp);
		String signature = i.getSignature(cgp);
		JClass jClass = classes.loadClass(clase.getClassName());
		JMethod jMethod = jClass.getMethod(methodName, signature);

		// maximo argumentos
		int maxArgs = jMethod.getMaxArgs() + 1;

		// si es estatico disminuyo params
		if (i instanceof INVOKESTATIC)
			maxArgs--;
		// get pila en orden para parametros
		Stack<JValue> stack = topFrame.getParamsReverse(maxArgs);
		topFrame.clearParams(maxArgs);

		StackFrame newTopFrame;
		int desp = 0;
		if (i instanceof INVOKESTATIC) {
			newTopFrame = new StackFrame(topFrame, jMethod, jClass);
		} else {
			// si no es estatico, meto referencia en locals[0]
			desp = 1;
			JObject ref = (JObject) stack.pop();
			newTopFrame = new StackFrame(topFrame, jMethod, jClass, ref);
		}
		// paso parametros tras que se ha creado la estructura
		for (int j = 0; j < maxArgs - desp && !stack.empty(); j++) {
			// si no es el primero y el anterior es long/double lo desplazo
			JValue val = stack.pop();
			if (j != 0
					&& (newTopFrame.getLocals()[j - 1].type == Type.LONG || newTopFrame
							.getLocals()[j - 1].type == Type.DOUBLE)) {
				newTopFrame.getLocals()[j + desp + 1] = val;
			} else
				newTopFrame.getLocals()[j + desp] = val;
		}
		topFrame.incPC(3);
		setTopFrame(newTopFrame);
	}

	private void ejecutarRETURN() {
		setTopFrame(topFrame.getCf());
	}

	private void ejecutarIRETURN() {
		ejecutarXRETURN();
	}

	private void ejecutarLRETURN() {
		ejecutarXRETURN();
	}

	private void ejecutarFRETURN() {
		ejecutarXRETURN();
	}

	private void ejecutarDRETURN() {
		ejecutarXRETURN();
	}

	private void ejecutarARETURN() {
		ejecutarXRETURN();
	}

	private void ejecutarXRETURN() {
		if (topFrame.size() != 1)
			error(topFrame, "Tamaño de pila para return distinto de 1");
		JValue val = topFrame.pop();
		StackFrame previous = topFrame.getCf();
		if (symbolicExec && previous == null) {

			if (DEBUG.LOG || DEBUG.USER) {
				Log l = Log.getInstance();
				l.saveOutput(val.toString());
			}

			setTopFrame(previous);

		} else {
			if (previous == null) {
				error(topFrame, "No hay frame previo para return");
			} else {
				previous.push(val);
				setTopFrame(previous);
			}
		}
	}

	/**
	 * Construye un indice de 16 bits separado en 2 bytes
	 * 
	 * @param subi1
	 *            primera parte indice
	 * @param subi2
	 *            segunda parte indice
	 * @return el indice completo
	 */
	private int getIndex(short subi1, short subi2) {
		ByteBuffer bb = ByteBuffer.allocate(2);
		bb.order(ByteOrder.BIG_ENDIAN);
		bb.put((byte) subi1);
		bb.put((byte) subi2);
		short index = bb.getShort(0);
		// byte bsubi1 = (byte) subi1;
		// byte bsubi2 = (byte) subi2;
		//
		// int index = (bsubi1 << 8) | bsubi2;

		return index;
	}

	private void error(StackFrame topFrame, String msg) {
		topFrame.error = 1;
		if (DEBUG.ON) {
			System.err.println(topFrame);
			System.err.println(msg);
		}
	}

	public void setTipoExec(TipoExec tipoExec) {
		this.tipoExec = tipoExec;
	}

	public StackFrame getTopFrame() {
		return topFrame;
	}

	public void setTopFrame(StackFrame topFrame) {
		this.topFrame = topFrame;
	}

	public Heap getHeap() {
		return heap;
	}

	public ClassArea getClasses() {
		return classes;
	}

	private boolean isBranchInst(Instruction i) {
		// ( (i instanceof IF_ICMPGE) ||
		// (i instanceof IF_ACMPEQ) ||
		// (i instanceof IF_ICMPEQ) ||
		// (i instanceof IF_ACMPNE) ||
		// (i instanceof IF_ICMPNE) ||
		// (i instanceof IF_ICMPGT) ||
		// (i instanceof IF_ICMPLE) ||
		// (i instanceof IF_ICMPLT) ||
		// (i instanceof IFEQ) ||
		// (i instanceof IFGE) ||
		// (i instanceof IFGT) ||
		// (i instanceof IFLE) ||
		// (i instanceof IFLT) ||
		// (i instanceof IFNE) ||
		// (i instanceof IFNONNULL) ||
		// (i instanceof IFNULL) )
		if (i instanceof IfInstruction) {
			return true;
		} else
			return false;
	}

	private void addConstraintSymbolInt(JValue v1, JValue v2) {
		JConstraintSolver cs = JConstraintSolver.getInstance();
		IntegerVariable var = (IntegerVariable) cs.getVar(v1.getName());
		Constraint c = null;
		if (v2 instanceof JByte) {
			JByte val = (JByte) v2;
			c = Choco.eq(var, val.getValue());
		} else if (v2 instanceof JChar) {
			JChar val = (JChar) v2;
			c = Choco.eq(var, val.getValue());

		} else if (v2 instanceof JInteger) {
			JInteger val = (JInteger) v2;
			c = Choco.eq(var, val.getValue());
			/*
			 * } //TODO loong else if (v2 instanceof JLong) { JLong val =
			 * (JLong) v2; cs.addConstraint(Choco.eq(v1,
			 * (Integer)val.getValue()));
			 */
		} else if (v2 instanceof JShort) {
			JShort val = (JShort) v2;
			c = Choco.eq(var, val.getValue());
		}

		if (c != null) {
			cs.addConstraint(c);
			v1.setConstraintAsig(c);
			constraints.add(c);
		}

	}

	private void addConstraintSymbolReal(JValue v1, JValue v2) {
		JConstraintSolver cs = JConstraintSolver.getInstance();
		RealConstantVariable var = (RealConstantVariable) cs.getVar(v1
				.getName());
		if (v2 instanceof JDouble) {
			JDouble val = (JDouble) v2;
			Constraint c = Choco.eq(var, val.getValue());
			cs.addConstraint(c);
			v1.setConstraintAsig(c);
		} else if (v2 instanceof JFloat) {
			JFloat val = (JFloat) v2;
			Constraint c = Choco.eq(var, val.getValue());
			cs.addConstraint(c);
			v1.setConstraintAsig(c);
		}
	}

	public static void addConstraint(Constraint c) {
		constraints.add(c);
	}

	public static ArrayList<Variable> getVar(Variable[] vars) {

		ArrayList<Variable> res = new ArrayList<Variable>();
		for (Variable v : vars) {
			if (v instanceof IntegerVariable) {
				res.add(v);
			} else if (v instanceof IntegerExpressionVariable) {
				res.addAll(getVar(v.getVariables()));
			}

		}
		return res;
	}

	/**
	 * Crea un objeto previo a la ejecucion
	 * 
	 * @param className
	 *            de objeto a crear
	 * @return el objeto creado
	 */
	public JValue ejecutarPreNEW(String className) {
		JClass jClass = classes.loadClass(className);
		JObject object = heap.createObject(jClass);
		return object;
	}

	/**
	 * Crea un array previo a la ejecucion
	 * 
	 * @param type
	 *            base a crear
	 * @param nel
	 *            numero de elementos
	 * @return el array creado
	 */
	public JValue ejecutarPreNEWARRAY(short type, int nel) {
		JArray array = heap.createNewArray(type, nel);
		return array;
	}

	/**
	 * 
	 * @param className
	 *            de objeto a crear
	 * @param size
	 *            del array
	 * @return
	 */
	public JValue ejecutarPreNEWOBJECTARRAY(String className, int size) {
		JClass jClass = classes.loadClass(className);
		JArray array = heap.createNewObjectArray(jClass, size);
		return array;
	}

	/**
	 * Transform signature en nombre clase
	 * 
	 * @param signature
	 *            Lcom/bcel/test/A;
	 * @return com.bcel.test.A
	 */
	public String getNameClassFromSignature(String signature) {
		String name = signature.replace("/", ".");
		return name.substring(1, name.length() - 1);
	}

	/**
	 * Crea un throwable con un mensaje y lo pone en la cima de la pila con un
	 * mensaje
	 * 
	 * @param o
	 *            el objeto nulo
	 */
	private void createThrowable(JObject o) {
		JClass throwableClass = classes.loadClass("java.lang.Throwable");
		JObject throwable = heap.createObject(throwableClass);
		JObject message = heap.createStringObject(o.getName() + " is NULL",
				classes);
		throwable.setFieldValue("detailMessage", message);
		topFrame.push(throwable);
	}

	/**
	 * Crea y añade la constraint dependiendo del tipo de val
	 * 
	 * @param val
	 *            el JValue del field
	 */
	private void createConstraintGetField(JValue val) {
		JConstraintSolver cs = JConstraintSolver.getInstance();
		Constraint c;

		if (val instanceof JInteger) {
			IntegerVariable iv = (IntegerVariable) cs.getVar(val.getName());
			JInteger jint = (JInteger) val;
			c = Choco.eq(iv, jint.getValue());
		} else if (val instanceof JFloat) {
			RealVariable rv = (RealVariable) cs.getVar(val.getName());
			JFloat jfloat = (JFloat) val;
			c = Choco.eq(rv, jfloat.getValue());
		} else {
			// si no es de otro tipo, para posibles ampliaciones
			c = null;
		}
		constraints.add(c);
		cs.addConstraint(c);
	}

}
