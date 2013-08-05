package com.jsyx.tipos;

import org.apache.bcel.generic.Type;

/** Tipo char para JVM
 * @author JZ
 * @version 16/12/2012
 */
public class JChar extends JValue {
	private char value;

	public JChar(char val) {
		value = (char) val;
		type = Type.INT;
	}
	
	private JChar(JChar old){
		value = (char)old.value;
		type = Type.INT;
		this.isSymbol = old.isSymbol;
		this.name = old.name;
	}

	public JChar() {
		this((char) 0);
	}

	public char getValue() {
		return value;
	}


	public boolean equals(Object other) {
		if (other == null) {
			return false;
		}
		if (!this.getClass().equals(other.getClass())) {
			return false;
		}
		JChar other1 = (JChar) other;
		return this.getValue() == other1.getValue();
	}

	@Override
	public JValue deepCopy() {
		return new JChar(this);
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
		if (v instanceof JChar){
			value = ((JChar) v).getValue();
		}
		
	}

}
