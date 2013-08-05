package com.jsyx.tipos;

import org.apache.bcel.generic.Type;

import choco.Choco;
import choco.Options;
import choco.kernel.model.constraints.Constraint;
import choco.kernel.model.variables.integer.IntegerVariable;

import com.jsyx.JConstraintSolver;
import com.jsyx.JVM;

/** Tipo int para JVM
 * @author JZ
 * @version 05/12/2012
 */
public class JInteger extends JValue {
	private int value;

	public JInteger(int val) {
		value = val;
		type = Type.INT;
	}
	
	private JInteger(JInteger old){
		this.value = old.value;
		type = Type.INT;
		this.isSymbol = old.isSymbol;
		this.name = old.name;
		this.constraintAsig = old.constraintAsig;
		this.setCopies(old.getCopies());
		//this.constraintNegation = old.constraintNegation;
	}

	public JInteger() {
		this(0);
	}

	public int getValue() {
		return value;
	}

	public JInteger add(JInteger other) {
		int val = this.getValue() + other.getValue();
		return new JInteger(val);
	}
	
	/**
	 *  Add JInteger in Symbolic Execution
	 * @param other Other part of the sum
	 * @param s Name of the new JInteger
	 * @return JInteger
	 */
	public JInteger addSymb(JInteger other, String s){		
		int val = this.getValue() + other.getValue();
		if (isSymbol){
			JConstraintSolver cs = JConstraintSolver.getInstance();	
			
			//Creamos el nuevo JInteger a devolver
			JInteger jInt = new JInteger(val);
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
				
				//A–adimos la constraint al modeloi
				Constraint c = Choco.eq(sum,Choco.plus(v1,Choco.constant(other.getValue())));
				JVM.addConstraint(c);
				cs.addConstraint(c);

				
			}
			return jInt;
			
		}
		else if (other.isSymbol()){
			JConstraintSolver cs = JConstraintSolver.getInstance();	

			JInteger jInt = new JInteger(val);
			jInt.setSymbol(false);
			jInt.setName(s);
			
			jInt.constraintNegation = other.constraintNegation;
			
			IntegerVariable v = (IntegerVariable) cs.getVar(other.getName());
			
			IntegerVariable sum = Choco.makeIntVar(s, Options.V_BOUND); 
			cs.addVariable(sum);
			
			Constraint c = Choco.eq(sum,Choco.plus(v,Choco.constant(value)));
			JVM.addConstraint(c);
			cs.addConstraint(c);
			
			return jInt;
			
		}
		else {
			return new JInteger(val);

		}
	}

	public JInteger mul(JInteger other) {
		int val = this.getValue() * other.getValue();
		return new JInteger(val);
	}
	
	public JInteger mulSymb(JInteger other, String s){		
		int val = this.getValue() * other.getValue();
		if (isSymbol){
			JConstraintSolver cs = JConstraintSolver.getInstance();	
			
			JInteger jInt = new JInteger(val);
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
				Constraint c = Choco.eq(mul,Choco.mult(v1,Choco.constant(other.getValue())));
				JVM.addConstraint(c);
				cs.addConstraint(c);
				jInt.constraintAsig = c;

				
			}
			return jInt;
			
		}
		else if (other.isSymbol()){
			JConstraintSolver cs = JConstraintSolver.getInstance();	

			JInteger jInt = new JInteger(val);
			jInt.setSymbol(false);
			jInt.setName(s);
			
			IntegerVariable v = (IntegerVariable) cs.getVar(other.getName());
			
			IntegerVariable mul = Choco.makeIntVar(s, Options.V_BOUND); 
			cs.addVariable(mul);

			Constraint c = Choco.eq(mul,Choco.mult(v,Choco.constant(value)));
			JVM.addConstraint(c);
			cs.addConstraint(c);
			jInt.constraintAsig = c;
			
			return jInt;
			
		}
		else {
			return new JInteger(val);

		}
	}


	public JInteger sub(JInteger other) {
		int val = this.getValue() - other.getValue();
		return new JInteger(val);
	}
	
	public JInteger subSymb(JInteger other, String s){		
		int val = this.getValue() - other.getValue();
		if (isSymbol){
			JConstraintSolver cs = JConstraintSolver.getInstance();	
			
			JInteger jInt = new JInteger(val);
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
				
				Constraint c = Choco.eq(sub,Choco.minus(v1,Choco.constant(other.getValue())));
				JVM.addConstraint(c);
				cs.addConstraint(c);

				
			}
			return jInt;
			
		}
		else if (other.isSymbol()){
			JConstraintSolver cs = JConstraintSolver.getInstance();	

			JInteger jInt = new JInteger(val);
			jInt.setSymbol(false);
			jInt.setName(s);
			
			IntegerVariable v = (IntegerVariable) cs.getVar(other.getName());
			
			IntegerVariable sub = Choco.makeIntVar(s, Options.V_BOUND); 
			cs.addVariable(sub);
			
			Constraint c = Choco.eq(sub,Choco.minus(v,Choco.constant(value)));
			JVM.addConstraint(c);
			cs.addConstraint(c);
			
			return jInt;
			
		}
		else {
			return new JInteger(val);

		}
	}

	public JInteger div(JInteger other) {
		int val = this.getValue() / other.getValue();
		return new JInteger(val);
	}
	
	public JInteger divSymb(JInteger other, String s){	
		int val = 0;
		if (other.getValue() != 0){
			val = this.getValue() / other.getValue();	
		}
		
		if (isSymbol){
			JConstraintSolver cs = JConstraintSolver.getInstance();	
			
			JInteger jInt = new JInteger(val);
			jInt.setSymbol(false);
			jInt.setName(s);
			
			IntegerVariable v1 = (IntegerVariable) cs.getVar(name);
			if (other.isSymbol()){
				IntegerVariable v2 = (IntegerVariable) cs.getVar(other.getName());
				
				IntegerVariable div = Choco.makeIntVar(s, Options.V_BOUND); 
				cs.addVariable(div);
				cs.addConstraint(Choco.eq(div,Choco.div(v2,v1)));

			}
			else {
				IntegerVariable div = Choco.makeIntVar(s, Options.V_BOUND); 
				cs.addVariable(div);
				cs.addConstraint(Choco.eq(div,Choco.div(v1,Choco.constant(other.getValue()))));

				
			}
			return jInt;
			
		}
		else if (other.isSymbol()){
			JConstraintSolver cs = JConstraintSolver.getInstance();	

			JInteger jInt = new JInteger(val);
			jInt.setSymbol(false);
			jInt.setName(s);
			
			IntegerVariable v = (IntegerVariable) cs.getVar(other.getName());
			
			IntegerVariable div = Choco.makeIntVar(s, Options.V_BOUND); 
			cs.addVariable(div);
			cs.addConstraint(Choco.eq(div,Choco.div(v,Choco.constant(value))));
			
			return jInt;
			
		}
		else {
			return new JInteger(val);

		}
	}

	public JInteger rem(JInteger other) {
		int val = this.getValue() % other.getValue();
		return new JInteger(val);
	}
	
	public JInteger remSymb(JInteger other, String s){
		int val = this.getValue() % other.getValue();
		if (isSymbol){
			JConstraintSolver cs = JConstraintSolver.getInstance();	
			
			JInteger jInt = new JInteger(val);
			jInt.setSymbol(false);
			jInt.setName(s);
			
			IntegerVariable v1 = (IntegerVariable) cs.getVar(name);
			if (other.isSymbol()){
				IntegerVariable v2 = (IntegerVariable) cs.getVar(other.getName());
				
				IntegerVariable rem = Choco.makeIntVar(s, Options.V_BOUND); 
				cs.addVariable(rem);
				cs.addConstraint(Choco.eq(rem,Choco.mod(v1,v2)));

			}
			else {
				IntegerVariable rem = Choco.makeIntVar(s, Options.V_BOUND); 
				cs.addVariable(rem);
				cs.addConstraint(Choco.eq(rem,Choco.mod(v1,Choco.constant(other.getValue()))));

				
			}
			return jInt;
			
		}
		else if (other.isSymbol()){
			JConstraintSolver cs = JConstraintSolver.getInstance();	

			JInteger jInt = new JInteger(val);
			jInt.setSymbol(false);
			jInt.setName(s);
			
			IntegerVariable v = (IntegerVariable) cs.getVar(other.getName());
			
			IntegerVariable rem = Choco.makeIntVar(s, Options.V_BOUND); 
			cs.addVariable(rem);
			cs.addConstraint(Choco.eq(rem,Choco.mod(v,Choco.constant(value))));
			
			return jInt;
			
		}
		else {
			
			return new JInteger(val);

		}
	}

	public JInteger neg() {
		int val = -this.getValue();
		return new JInteger(val);
	}
	
	public JInteger negSymb(String s){
		int val = -this.getValue();
		if (isSymbol){
			JConstraintSolver cs = JConstraintSolver.getInstance();	
			if (name.substring(0,1).equals("T") && name.length()>4){
				s = this.name.substring(0, 4)+this.getCopies();
			}
			else {
				s = this.name.substring(0, 1)+this.getCopies();
			}
			JInteger jInt = new JInteger(val);
			jInt.setSymbol(false);
			jInt.setCopies(this.getCopies()+1);
			jInt.setName(s);
			jInt.isNegation = true;
			
			
			IntegerVariable v = (IntegerVariable) cs.getVar(name);
			
			IntegerVariable neg = Choco.makeIntVar(s, Options.V_BOUND); 
			cs.addVariable(neg);
			//A–adimos como œltima asignaci—n por si hacemos store
			Constraint c = Choco.eq(neg,Choco.neg(v));
			jInt.constraintNegation = c;
			cs.addConstraint(c);
			
			return jInt;
		}
		else {			
			return new JInteger(val);
		}
	}
	
	

	public JInteger incSymb(int inc,String s){
		JConstraintSolver cs = JConstraintSolver.getInstance();	
		IntegerVariable v1 = (IntegerVariable) cs.getVar(name);
		
		JInteger jInt = (JInteger) this.deepCopy();
		jInt.setName(s);
		jInt.setCopies(this.getCopies()+1);
		jInt.setValue(this.value+inc);
		
		IntegerVariable v2 = Choco.makeIntVar(s, Options.V_BOUND);
		cs.addVariable(v2);
		Constraint c = Choco.eq(v2,Choco.sum(v1,Choco.constant(inc)));
		JVM.addConstraint(c);
		jInt.constraintAsig = c;
		cs.addConstraint(c);
		return jInt;
		
	}

	private void setValue(int v) {
		value = v;
		
	}

	

	public boolean equals(Object other) {
		if (other == null) {
			return false;
		}
		if (!this.getClass().equals(other.getClass())) {
			return false;
		}
		JInteger other1 = (JInteger) other;
		return this.getValue() == other1.getValue();
	}

	@Override
	public JValue deepCopy() {
		return new JInteger(this);
	}
	
	@Override
	public String toString() {
		if (isSymbol()){
			return getName();
		}
		else{
			return "" + value;
		}
		
	}

	@Override
	public void setValue(JValue v) {
		if (v instanceof JInteger){
			value = ((JInteger) v).getValue();
		}
		
	}

}
