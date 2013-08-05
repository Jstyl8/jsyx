package com.jsyx;

import java.util.Arrays;
import java.util.Stack;

import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionHandle;

import com.jsyx.tipos.JValue;

/**
 * Frame de la JVM
 * <p>
 * Contiene la pila de operandos, variables locales, contador de programa,
 * metodo al que corresponde y frame padre.
 * </p>
 * <p>
 * Añadimos clase que llamo para usos posteriores
 * </p>
 * 
 * @author JZ
 * @version 05/12/2012
 */
public class StackFrame {


	Stack<JValue> operands;
	JValue[] locals;
	private int pc;
	private JMethod method;
	private StackFrame cf;
	public byte error;

	// added
	JClass clase;

	public StackFrame(StackFrame cf, JMethod meth, JClass clase) {
		this(cf, meth, clase, null);
	}
	/**
	 * Copy constructor
	 * @param old
	 */
	public StackFrame(StackFrame old){
		if (old.operands == null){
			this.operands = null;
		}
		else {
			this.operands = JValue.copyStackJValue(old.operands);
		}
		if (old.locals== null){
			this.locals = null;
		}
		else {
			this.locals = JValue.copyArrayJValue(old.locals);
		}
		this.pc = old.pc;
		this.error=old.error;
		if (old.cf == null){
			this.cf = null;
		}
		this.method = old.method.deepCopy();
		if (old.cf == null){
			this.cf = null;
		}
		else {
			this.cf = old.cf.deepCopy();		
		}
		if (old.clase == null){
			this.clase = null;
		}
		else {
			this.clase = old.clase.deepCopy(); 
		}
		
	}

	/**
	 * @param cf
	 *            anterior frame 
	 * @param meth
	 *            metodo a ejecutar
	 * @param clase
	 *            que llama
	 * @param thisRef
	 *            referencia al objeto que llama al metodo
	 */
	public StackFrame(StackFrame cf, JMethod meth, JClass clase, JValue thisRef) {
		int maxLocals=meth.getMaxLocals();
		if (thisRef != null) {
			locals = new JValue[maxLocals];
			if(maxLocals>0)locals[0] = thisRef;
		} else
			locals = new JValue[maxLocals];
		this.cf = cf;
		operands = new Stack<JValue>();
		pc = 0;
		error=0;
		method = meth;

		this.clase = clase;
	}

	public void push(JValue j) {
		operands.push(j);
	}

	@Override
	public String toString() {
		return "PC " + pc + " en metodo " + method.methodInfo.getName()
				+ " of " + clase.getName()
				+ System.getProperty("line.separator") + "Pila=" + operands 
				+ " Locals=" + Arrays.toString(locals);
	}

	public ConstantPool getConstantPool() {
		return clase.getConstantPool();
	}

	public InstructionHandle nextInstruction() {
		return method.get(pc);
	}

	public InstructionHandle[] getInstructions() {
		return method.getInstructions();
	}

	public Stack<JValue> getOperands() {
		return operands;
	}

	/**
	 * Metodo usado en el paso de parametros a llamadas a metodos
	 * 
	 * @param maxArgs
	 * 
	 * @return una copia de los parametros en sentido contrario
	 */
	public Stack<JValue> getParamsReverse(int maxArgs) {
		Stack<JValue> aux = (Stack<JValue>) operands.clone();
		Stack<JValue> ret = new Stack<JValue>();
		while (!aux.empty() && maxArgs > 0) {
			ret.push(aux.pop());
			maxArgs--;
		}
		return ret;
	}

	public JValue[] getLocals() {
		return locals;
	}

	public int getPC() {
		return pc;
	}

	public void setPC(int pc) {
		this.pc = pc;
	}

	public JMethod getMethod() {
		return method;
	}

	public void setMethod(JMethod method) {
		this.method = method;
	}

	public StackFrame getCf() {
		return cf;
	}

	public void setCf(StackFrame cf) {
		this.cf = cf;
	}

	public void incPC() {
		pc++;
	}

	public void incPC(int c) {
		pc += c;
	}

	public ConstantPoolGen getConstantPoolGen() {
		return clase.getConstantPoolGen();
	}

	public JValue pop() {
		return operands.pop();
	}

	public JValue peek() {
			return operands.peek();
	}

	public int size() {
		return operands.size();
	}

	public void clearParams(int maxArgs) {
		while (maxArgs > 0) {
			operands.pop();
			maxArgs--;
		}
	}

	public void clearParams() {
		operands.clear();
	}
	
	public StackFrame deepCopy() {
		return new StackFrame(this);
	}
	public boolean operandsEmtpy() {
		return operands.empty();
	}
	
}
