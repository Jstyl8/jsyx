package com.jsyx.tipos;

import org.apache.bcel.generic.Type;

import choco.Choco;
import choco.Options;
import choco.kernel.model.constraints.Constraint;
import choco.kernel.model.variables.real.RealVariable;

import com.jsyx.JConstraintSolver;
import com.jsyx.JVM;

/** Tipo float para JVM
 * @author JZ
 * @version 05/12/2012
 */
public class JFloat extends JValue {
	private float value;

	public JFloat(float val) {
		value = val;
		type = Type.INT;
		
	}
	
	private JFloat(JFloat old){
		this.value = old.value;
		type = Type.INT;
		this.isSymbol = old.isSymbol;
		this.name = old.name;
		this.constraintAsig = old.constraintAsig;
		this.setCopies(old.getCopies());
	}
	

	public JFloat() {
		this(0);
	}

	public float getValue() {
		return value;
	}

	public JFloat add(JFloat other) {
		float val = this.getValue() + other.getValue();
		return new JFloat(val);
	}

	/**
	 *  Add JFloat in Symbolic Execution
	 * @param other Other part of the sum
	 * @param s Name of the new JFloat
	 * @return JFloat
	 */
	public JFloat addSymb(JFloat other, String s){		
		float val = this.getValue() + other.getValue();
		if (isSymbol){
			JConstraintSolver cs = JConstraintSolver.getInstance();	
			
			//Creamos el nuevo JFloat a devolver
			JFloat jFloat = new JFloat(val);
			jFloat.setSymbol(false);
			jFloat.setName(s);
			
			
			RealVariable v1 = (RealVariable) cs.getVar(name);
			if (other.isSymbol()){
				RealVariable v2 = (RealVariable) cs.getVar(other.getName());
				
				RealVariable sum = Choco.makeRealVar(s,Double.MIN_VALUE,Double.MAX_VALUE, Options.V_BOUND); 
				cs.addVariable(sum);
				
				//A–adimos la constraint al modelo
				Constraint c = Choco.eq(sum,Choco.plus(v1,v2));
				JVM.addConstraint(c);
				cs.addConstraint(c);
				
				

			}
			else {
				RealVariable sum = Choco.makeRealVar(s,Double.MIN_VALUE,Double.MAX_VALUE, Options.V_BOUND);
				cs.addVariable(sum);
				
				//A–adimos la constraint al modelo
				Constraint c = Choco.eq(sum,Choco.plus(v1,Choco.constant(other.getValue())));
				JVM.addConstraint(c);
				cs.addConstraint(c);

				
			}
			return jFloat;
			
		}
		else if (other.isSymbol()){
			JConstraintSolver cs = JConstraintSolver.getInstance();	

			JFloat jInt = new JFloat(val);
			jInt.setSymbol(false);
			jInt.setName(s);
			
			RealVariable v = (RealVariable) cs.getVar(other.getName());
			
			RealVariable sum = Choco.makeRealVar(s,Double.MIN_VALUE,Double.MAX_VALUE, Options.V_BOUND);
			cs.addVariable(sum);
			
			Constraint c = Choco.eq(sum,Choco.plus(v,Choco.constant(value)));
			JVM.addConstraint(c);
			cs.addConstraint(c);
			
			return jInt;
			
		}
		else {
			return new JFloat(val);

		}
	}
	
	public JFloat mul(JFloat other) {
		float val = this.getValue() * other.getValue();
		return new JFloat(val);
	}

	public JFloat mulSymb(JFloat other, String s){		
		float val = this.getValue() * other.getValue();
		if (isSymbol){
			JConstraintSolver cs = JConstraintSolver.getInstance();	
			
			JFloat jInt = new JFloat(val);
			jInt.setSymbol(false);
			jInt.setName(s);
			
			
			RealVariable v1 = (RealVariable) cs.getVar(name);
			if (other.isSymbol()){
				RealVariable v2 = (RealVariable) cs.getVar(other.getName());
				
				RealVariable mul = Choco.makeRealVar(s,Double.MIN_VALUE,Double.MAX_VALUE, Options.V_BOUND);
				cs.addVariable(mul);
				Constraint c = Choco.eq(mul,Choco.mult(v1,v2));
				JVM.addConstraint(c);
				cs.addConstraint(c);
				jInt.constraintAsig = c;

			}
			else {
				RealVariable mul = Choco.makeRealVar(s,Double.MIN_VALUE,Double.MAX_VALUE, Options.V_BOUND);
				cs.addVariable(mul);
				Constraint c = Choco.eq(mul,Choco.mult(v1,Choco.constant(other.getValue())));
				JVM.addConstraint(c);
				cs.addConstraint(c);
				jInt.constraintAsig = c;

				
			}
			return jInt;
			
		}
		else if (other.isSymbol()){
			JConstraintSolver cs = JConstraintSolver.getInstance();	

			JFloat jInt = new JFloat(val);
			jInt.setSymbol(false);
			jInt.setName(s);
			
			RealVariable v = (RealVariable) cs.getVar(other.getName());
			
			RealVariable mul = Choco.makeRealVar(s,Double.MIN_VALUE,Double.MAX_VALUE, Options.V_BOUND);
			cs.addVariable(mul);

			Constraint c = Choco.eq(mul,Choco.mult(v,Choco.constant(value)));
			JVM.addConstraint(c);
			cs.addConstraint(c);
			jInt.constraintAsig = c;
			
			return jInt;
			
		}
		else {
			return new JFloat(val);

		}
	}
	
	public JFloat sub(JFloat other) {
		float val = this.getValue() - other.getValue();
		return new JFloat(val);
	}

	public JFloat subSymb(JFloat other, String s){		
		float val = this.getValue() + other.getValue();
		if (isSymbol){
			JConstraintSolver cs = JConstraintSolver.getInstance();	
			
			JFloat jInt = new JFloat(val);
			jInt.setSymbol(false);
			jInt.setName(s);
			
			
			RealVariable v1 = (RealVariable) cs.getVar(name);
			if (other.isSymbol()){
				RealVariable v2 = (RealVariable) cs.getVar(other.getName());
				
				RealVariable sub = Choco.makeRealVar(s,Double.MIN_VALUE,Double.MAX_VALUE, Options.V_BOUND);
				cs.addVariable(sub);
				
				Constraint c = Choco.eq(sub,Choco.minus(v2,v1));
				JVM.addConstraint(c);
				cs.addConstraint(c);
				
				

			}
			else {
				RealVariable sub = Choco.makeRealVar(s,Double.MIN_VALUE,Double.MAX_VALUE, Options.V_BOUND);
				cs.addVariable(sub);
				
				Constraint c = Choco.eq(sub,Choco.minus(v1,Choco.constant(other.getValue())));
				JVM.addConstraint(c);
				cs.addConstraint(c);

				
			}
			return jInt;
			
		}
		else if (other.isSymbol()){
			JConstraintSolver cs = JConstraintSolver.getInstance();	

			JFloat jInt = new JFloat(val);
			jInt.setSymbol(false);
			jInt.setName(s);
			
			RealVariable v = (RealVariable) cs.getVar(other.getName());
			
			RealVariable sub = Choco.makeRealVar(s,Double.MIN_VALUE,Double.MAX_VALUE, Options.V_BOUND);
			cs.addVariable(sub);
			
			Constraint c = Choco.eq(sub,Choco.minus(v,Choco.constant(value)));
			JVM.addConstraint(c);
			cs.addConstraint(c);
			
			return jInt;
			
		}
		else {
			return new JFloat(val);

		}
	}
	
	public JFloat div(JFloat other) {
		float val = this.getValue() / other.getValue();
		return new JFloat(val);
	}

	
	public JFloat rem(JFloat other) {
		float val = this.getValue() % other.getValue();
		return new JFloat(val);
	}

	
	public JFloat neg() {
		float val = -this.getValue();
		return new JFloat(val);
	}

	
	private void setValue(float v) {
		value = v;
		
	}
	
	public boolean equals(Object other) {
		if (other == null) {
			return false;
		}
		if (!this.getClass().equals(other.getClass())) {
			return false;
		}
		JFloat other1 = (JFloat) other;
		return this.getValue() == other1.getValue();
	}

	@Override
	public JValue deepCopy() {
		return new JFloat(this);
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
		if (v instanceof JFloat){
			value = ((JFloat) v).getValue();
		}
		
	}

}
