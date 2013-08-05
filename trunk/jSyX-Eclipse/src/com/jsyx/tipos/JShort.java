package com.jsyx.tipos;

import org.apache.bcel.generic.Type;

import choco.Choco;
import choco.Options;
import choco.kernel.model.variables.integer.IntegerVariable;

import com.jsyx.JConstraintSolver;


/** Tipo short para JVM
 * @author JZ
 * @version 05/12/2012
 */
public class JShort extends JValue {
	private short value;

	public JShort(int val) {
		value = (short) val;
		type = Type.INT;
	}
	
	private JShort(JShort old){
		this.value = old.value;
		type = Type.INT;
		this.isSymbol = old.isSymbol;
		this.name = old.name;
	}

	public JShort() {
		this(0);
	}

	public int getValue() {
		return value;
	}

	public JShort add(JShort other) {
		int val = this.getValue() + other.getValue();
		return new JShort(val);
	}
	
	public JShort addSymb(JShort other, String s){		
		if (isSymbol){
			JConstraintSolver cs = JConstraintSolver.getInstance();	
			
			JShort jShort = new JShort();
			jShort.setSymbol(false);
			jShort.setName(s);
			
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
			return jShort;
			
		}
		else if (other.isSymbol()){
			JConstraintSolver cs = JConstraintSolver.getInstance();	

			JShort jShort = new JShort();
			jShort.setSymbol(false);
			jShort.setName(s);
			
			IntegerVariable v = (IntegerVariable) cs.getVar(other.getName());
			
			IntegerVariable sum = Choco.makeIntVar(s, Options.V_BOUND); 
			cs.addVariable(sum);
			cs.addConstraint(Choco.eq(sum,Choco.plus(v,Choco.constant(value))));
			
			return jShort;
			
		}
		else {
			short val = (short) (this.getValue() + other.getValue());
			return new JShort(val);

		}
	}

	public JShort mul(JShort other) {
		int val = this.getValue() * other.getValue();
		return new JShort(val);
	}

	public JShort sub(JShort other) {
		int val = this.getValue() - other.getValue();
		return new JShort(val);
	}

	public JShort div(JShort other) {
		int val = this.getValue() / other.getValue();
		return new JShort(val);
	}

	public JShort rem(JShort other) {
		int val = this.getValue() % other.getValue();
		return new JShort(val);
	}

	public JShort neg() {
		int val = -this.getValue();
		return new JShort(val);
	}

	public boolean equals(Object other) {
		if (other == null) {
			return false;
		}
		if (!this.getClass().equals(other.getClass())) {
			return false;
		}
		JShort other1 = (JShort) other;
		return this.getValue() == other1.getValue();
	}

	@Override
	public JValue deepCopy() {
		return new JShort(this);
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
		if (v instanceof JShort){
			value = (short) ((JShort) v).getValue();
		}
		
	}

}
