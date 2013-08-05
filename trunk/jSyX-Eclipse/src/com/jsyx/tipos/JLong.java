package com.jsyx.tipos;

import org.apache.bcel.generic.Type;

import choco.Choco;
import choco.Options;
import choco.kernel.model.constraints.Constraint;
import choco.kernel.model.variables.integer.IntegerVariable;

import com.jsyx.JConstraintSolver;
import com.jsyx.JVM;

/** Tipo long para JVM
 * @author JZ
 * @version 18/03/2013
 */
public class JLong extends JValue {
	private long value;

	public JLong(long val) {
		value = val;
		type = Type.LONG;
	}

	private JLong(JLong old){
		this.value = old.value;
		type = Type.LONG;
		this.isSymbol = old.isSymbol;
		this.name = old.name;
		this.constraintAsig = old.constraintAsig;
		this.setCopies(old.getCopies());
	}
	public JLong() {
		this(0);
	}

	public long getValue() {
		return value;
	}

	public JLong add(JLong other) {
		long val = this.getValue() + other.getValue();
		return new JLong(val);
	}

	/**
	 *  Add JLong in Symbolic Execution
	 * @param other Other part of the sum
	 * @param s Name of the new JLong
	 * @return JLong
	 */
	public JLong addSymb(JLong other, String s){		
		long val = this.getValue() + other.getValue();
		if (isSymbol){
			JConstraintSolver cs = JConstraintSolver.getInstance();	
			
			//Creamos el nuevo JLong a devolver
			JLong jInt = new JLong(val);
			jInt.setSymbol(false);
			jInt.setName(s);
			
			
			IntegerVariable v1 = (IntegerVariable) cs.getVar(name);
			if (other.isSymbol()){
				IntegerVariable v2 = (IntegerVariable) cs.getVar(other.getName());
				
				IntegerVariable sum = Choco.makeIntVar(s, Options.V_BOUND); 
				cs.addVariable(sum);
				
				//A–adimos la constraint al modelo
				Constraint c = Choco.eq(sum,Choco.plus(v1,v2));
				JVM.addConstraint(c);
				cs.addConstraint(c);
				
				

			}
			else {
				IntegerVariable sum = Choco.makeIntVar(s, Options.V_BOUND); 
				cs.addVariable(sum);
				
				//A–adimos la constraint al modelo
				Constraint c = Choco.eq(sum,Choco.plus(v1,Choco.constant((int)other.getValue())));
				JVM.addConstraint(c);
				cs.addConstraint(c);

				
			}
			return jInt;
			
		}
		else if (other.isSymbol()){
			JConstraintSolver cs = JConstraintSolver.getInstance();	

			JLong jInt = new JLong(val);
			jInt.setSymbol(false);
			jInt.setName(s);
			
			IntegerVariable v = (IntegerVariable) cs.getVar(other.getName());
			
			IntegerVariable sum = Choco.makeIntVar(s, Options.V_BOUND); 
			cs.addVariable(sum);
			
			Constraint c = Choco.eq(sum,Choco.plus(v,Choco.constant((int)value)));
			JVM.addConstraint(c);
			cs.addConstraint(c);
			
			return jInt;
			
		}
		else {
			return new JLong(val);

		}
	}

	public JLong mul(JLong other) {
		long val = this.getValue() * other.getValue();
		return new JLong(val);
	}
	
	public JLong mulSymb(JLong other, String s){		
		long val = this.getValue() * other.getValue();
		if (isSymbol){
			JConstraintSolver cs = JConstraintSolver.getInstance();	
			
			JLong jInt = new JLong(val);
			jInt.setSymbol(false);
			jInt.setName(s);
			
			
			IntegerVariable v1 = (IntegerVariable) cs.getVar(name);
			if (other.isSymbol()){
				IntegerVariable v2 = (IntegerVariable) cs.getVar(other.getName());
				
				IntegerVariable mul = Choco.makeIntVar(s, Options.V_BOUND); 
				cs.addVariable(mul);
				Constraint c = Choco.eq(mul,Choco.mult(v1,v2));
				JVM.addConstraint(c);
				cs.addConstraint(c);
				jInt.constraintAsig = c;

			}
			else {
				IntegerVariable mul = Choco.makeIntVar(s, Options.V_BOUND); 
				cs.addVariable(mul);
				Constraint c = Choco.eq(mul,Choco.mult(v1,Choco.constant((int)other.getValue())));
				JVM.addConstraint(c);
				cs.addConstraint(c);
				jInt.constraintAsig = c;

				
			}
			return jInt;
			
		}
		else if (other.isSymbol()){
			JConstraintSolver cs = JConstraintSolver.getInstance();	

			JLong jInt = new JLong(val);
			jInt.setSymbol(false);
			jInt.setName(s);
			
			IntegerVariable v = (IntegerVariable) cs.getVar(other.getName());
			
			IntegerVariable mul = Choco.makeIntVar(s, Options.V_BOUND); 
			cs.addVariable(mul);

			Constraint c = Choco.eq(mul,Choco.mult(v,Choco.constant((int)value)));
			JVM.addConstraint(c);
			cs.addConstraint(c);
			jInt.constraintAsig = c;
			
			return jInt;
			
		}
		else {
			return new JLong(val);

		}
	}


	public JLong sub(JLong other) {
		long val = this.getValue() - other.getValue();
		return new JLong(val);
	}
	
	public JLong subSymb(JLong other, String s){		
		long val = this.getValue() + other.getValue();
		if (isSymbol){
			JConstraintSolver cs = JConstraintSolver.getInstance();	
			
			JLong jInt = new JLong(val);
			jInt.setSymbol(false);
			jInt.setName(s);
			
			
			IntegerVariable v1 = (IntegerVariable) cs.getVar(name);
			if (other.isSymbol()){
				IntegerVariable v2 = (IntegerVariable) cs.getVar(other.getName());
				
				IntegerVariable sub = Choco.makeIntVar(s, Options.V_BOUND); 
				cs.addVariable(sub);
				
				Constraint c = Choco.eq(sub,Choco.minus(v2,v1));
				JVM.addConstraint(c);
				cs.addConstraint(c);
				
				

			}
			else {
				IntegerVariable sub = Choco.makeIntVar(s, Options.V_BOUND); 
				cs.addVariable(sub);
				
				Constraint c = Choco.eq(sub,Choco.minus(v1,Choco.constant((int)other.getValue())));
				JVM.addConstraint(c);
				cs.addConstraint(c);

				
			}
			return jInt;
			
		}
		else if (other.isSymbol()){
			JConstraintSolver cs = JConstraintSolver.getInstance();	

			JLong jInt = new JLong(val);
			jInt.setSymbol(false);
			jInt.setName(s);
			
			IntegerVariable v = (IntegerVariable) cs.getVar(other.getName());
			
			IntegerVariable sub = Choco.makeIntVar(s, Options.V_BOUND); 
			cs.addVariable(sub);
			
			Constraint c = Choco.eq(sub,Choco.minus(v,Choco.constant((int)value)));
			JVM.addConstraint(c);
			cs.addConstraint(c);
			
			return jInt;
			
		}
		else {
			return new JLong(val);

		}
	}

	public JLong div(JLong other) {
		long val = this.getValue() / other.getValue();
		return new JLong(val);
	}

	public JLong rem(JLong other) {
		long val = this.getValue() % other.getValue();
		return new JLong(val);
	}

	public JLong neg() {
		long val = -this.getValue();
		return new JLong(val);
	}

	public boolean equals(Object other) {
		if (other == null) {
			return false;
		}
		if (!this.getClass().equals(other.getClass())) {
			return false;
		}
		JLong other1 = (JLong) other;
		return this.getValue() == other1.getValue();
	}

	@Override
	public JValue deepCopy() {
		return new JLong(this);
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

	private void setValue(long v) {
		value = v;
	}
	
	@Override
	public void setValue(JValue v) {
		if (v instanceof JLong){
			value = ((JLong) v).getValue();
		}
		
	}

}
