package com.jsyx.tipos;

import org.apache.bcel.generic.Type;

/** Tipo double para JVM
 * @author JZ
 * @version 05/12/2012
 */
public class JDouble extends JValue {
	private double value;

	public JDouble(double val) {
		value = val;
		type = Type.DOUBLE;
	}
	
	private JDouble(JDouble old){
		this.value = old.value;
		type = Type.DOUBLE;
		this.isSymbol = old.isSymbol;
		this.name = old.name;
	}

	public JDouble() {
		this(0);
	}

	public double getValue() {
		return value;
	}


	public JDouble add(JDouble other) {
		double val = this.getValue() + other.getValue();
		return new JDouble(val);
	}

	public JDouble mul(JDouble other) {
		double val = this.getValue() * other.getValue();
		return new JDouble(val);
	}

	public JDouble sub(JDouble other) {
		double val = this.getValue() - other.getValue();
		return new JDouble(val);
	}

	public JDouble div(JDouble other) {
		double val = this.getValue() / other.getValue();
		return new JDouble(val);
	}

	public JDouble rem(JDouble other) {
		double val = this.getValue() % other.getValue();
		return new JDouble(val);
	}

	public JDouble neg() {
		double val = -this.getValue();
		return new JDouble(val);
	}

	public boolean equals(Object other) {
		if (other == null) {
			return false;
		}
		if (!this.getClass().equals(other.getClass())) {
			return false;
		}
		JDouble other1 = (JDouble) other;
		return this.getValue() == other1.getValue();
	}

	@Override
	public JValue deepCopy() {
		return new JDouble(this);
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
		if (v instanceof JDouble){
			value = ((JDouble) v).getValue();
		}
		
	}

}
