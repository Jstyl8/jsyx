package com.jsyx;

import org.apache.bcel.classfile.CodeException;
import org.apache.bcel.classfile.ExceptionTable;
import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.Type;

/**
 * 
 * @author JZ
 */
public class JMethod {

	Method methodInfo;
	MethodGen methodGen;

	public JMethod(Method method, ClassGen classGen) {
		methodInfo = method;
		methodGen = new MethodGen(method, classGen.getClassName(),
				classGen.getConstantPool());
	}

	/**
	 * Copy Constructor
	 * 
	 * @param old
	 */
	private JMethod(JMethod old) {
		if (old.methodGen == null) {
			this.methodGen = null;
		} else {
			this.methodGen = (MethodGen) old.methodGen.clone();
		}
		if (old.methodInfo == null) {
			this.methodInfo = null;
		} else {
			this.methodInfo = old.methodInfo.copy(old.methodInfo
					.getConstantPool());
		}

	}

	public InstructionHandle get(int i) {
		return methodGen.getInstructionList().findHandle(i);
	}

	public int getMaxStack() {
		if (!isNative())
			return methodInfo.getCode().getMaxLocals();
		else {
			return 1;
		}
	}

	public String[] getArgumentsNames() {
		return methodGen.getArgumentNames();
	}

	public CodeException[] getExceptionTable() {
		return methodInfo.getCode().getExceptionTable();
	}

	public LineNumberTable getLineNumberTable() {
		return methodInfo.getLineNumberTable();
	}

	public Type[] getArgumentTypes() {
		return methodGen.getArgumentTypes();
	}

	public int getMaxLocals() {
		if (!isNative())
			return methodInfo.getCode().getMaxLocals();
		else {
			return getMaxArgs();
		}
	}

	// Dejamos length que devuelve el numero de parametros correcto,
	// tratamos caso double y long en llamadas a metodos de INVOKE
	public int getMaxArgs() {
		return methodInfo.getArgumentTypes().length;
	}

	public short[] getCode() {
		byte[] code = methodInfo.getCode().getCode();
		short[] codePlus = new short[code.length];
		// convert -opcodes to +opcodes
		for (int i = 0; i < code.length; i++) {
			codePlus[i] = (short) (code[i] < 0 ? code[i] + 256 : code[i]);
		}
		return codePlus;
	}

	public InstructionHandle[] getInstructions() {
		return methodGen.getInstructionList().getInstructionHandles();
	}

	@Override
	public String toString() {
		return methodInfo.toString() + "\n" + methodInfo.getCode();
	}

	public boolean isNative() {
		return methodInfo.isNative();
	}

	public String getFullNameAndSig() {
		return methodGen.getClassName() + "@" + methodInfo.getName()
				+ methodInfo.getSignature();

	}

	public JMethod deepCopy() {
		return new JMethod(this);
	}

	public String getBytecode() {
		return "Nombre: "+methodInfo.toString()+"\nSignature: "+methodInfo.getSignature()+"\n"+methodInfo.getCode().toString();
	}

	public boolean isStatic() {
		return methodInfo.isStatic();
	}
}
