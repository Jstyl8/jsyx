package com.jsyx.tipos;

import org.apache.bcel.generic.Type;

import choco.Choco;
import choco.Options;
import choco.kernel.model.variables.integer.IntegerVariable;

import com.jsyx.JConstraintSolver;

/** Tipo byte para JVM
 * @author JZ
 * @version 05/12/2012
 */
public class JByte extends JValue {
	private byte value;

	public JByte(int val) {
		value = (byte) val;
		type = Type.INT;
	}
	
	/**
	 * Copy Contructor
	 * @param old 
	 */
	private JByte(JByte old){
		this.value = old.value;
		type = Type.INT;
		this.isSymbol = old.isSymbol;
		this.name = old.name;
	}
	public JByte() {
		this((byte) 0);
	}

	public byte getValue() {
		return value;
	}


	public JByte add(JByte other) {
		int val = this.getValue() + other.getValue();
		return new JByte(val);
	}
	
	public JByte addSymb(JByte other, String s){		
		if (isSymbol){
			JConstraintSolver cs = JConstraintSolver.getInstance();	
			
			JByte jByte = new JByte();
			jByte.setSymbol(false);
			jByte.setName(s);
			
			IntegerVariable v1 = (IntegerVariable) cs.getVar(name);
			if (other.isSymbol()){
				IntegerVariable v2 = (IntegerVariable) cs.getVar(other.getName());
				
				IntegerVariable sum = Choco.makeIntVar(s, Options.V_BOUND); 
				cs.addVariable(sum);
				cs.addConstraint(Choco.eq(sum,Choco.plus(v1,v2)));

			}
			else {
				IntegerVariable sum = Choco.makeIntVar(s, Options.V_BOUND); 
				cs.addVariable(sum);
				cs.addConstraint(Choco.eq(sum,Choco.plus(v1,Choco.constant(other.getValue()))));

				
			}
			return jByte;
			
		}
		else if (other.isSymbol()){
			JConstraintSolver cs = JConstraintSolver.getInstance();	

			JByte jByte = new JByte();
			jByte.setSymbol(false);
			jByte.setName(s);
			
			IntegerVariable v = (IntegerVariable) cs.getVar(other.getName());
			
			IntegerVariable sum = Choco.makeIntVar(s, Options.V_BOUND); 
			cs.addVariable(sum);
			cs.addConstraint(Choco.eq(sum,Choco.plus(v,Choco.constant(value))));
			
			return jByte;
			
		}
		else {
			byte val = (byte) (this.getValue() + other.getValue());
			return new JByte(val);

		}
	}

	public JByte mul(JByte other) {
		int val = this.getValue() * other.getValue();
		return new JByte(val);
	}

	public JByte sub(JByte other) {
		int val = this.getValue() - other.getValue();
		return new JByte(val);
	}

	public JByte div(JByte other) {
		int val = this.getValue() / other.getValue();
		return new JByte(val);
	}

	public JByte rem(JByte other) {
		int val = this.getValue() % other.getValue();
		return new JByte(val);
	}

	public JByte neg() {
		int val = -this.getValue();
		return new JByte(val);
	}

	public boolean equals(Object other) {
		if (other == null) {
			return false;
		}
		if (!this.getClass().equals(other.getClass())) {
			return false;
		}
		JByte other1 = (JByte) other;
		return this.getValue() == other1.getValue();
	}

	@Override
	public JValue deepCopy() {
		return new JByte(this);
	}
	
	@Override
	public String toString() {
		if (isSymbol()){
			return getName() + " = "+ value;
		}
		else{
			return "" + value;
		}
		
	}

	@Override
	public void setValue(JValue v) {
		if (v instanceof JByte){
			value = ((JByte) v).getValue();
		}
		
	}
	


}
