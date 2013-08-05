package com.jsyx.tipos;

import java.util.Stack;

import org.apache.bcel.classfile.CodeException;
import org.apache.bcel.generic.Type;

import choco.kernel.model.constraints.Constraint;

/**
 * Tipo abstracto para la JVM, todo en la JVM es un JValue
 * 
 * @author JZ
 * @version 05/12/2012
 */
public abstract class JValue{
	public Type type;
	protected boolean isSymbol = false;
	//Nombre del símbolo que concuerda con el nombre del IntegerVariable en el Model
	protected String name = "A"; 
	//Número de símbolos de entrada
	private static int nSymbolic = 0;
	//Número de Copias del Símbolo 
	//Ejemplo A = A+5, A símbolo, esto nos dara una nueva A0 = A+5
	private int nCopies = 0;
	//Última asignación del símbolo 
	//Ej A = 5,sirva para cuando tengamos otra nueva asignación tendremos que borrar la constraint asociada A = 5... 
	protected Constraint constraintAsig;
	//
	protected boolean isNegation;
	protected Constraint constraintNegation;

	public abstract JValue deepCopy();
	
	public abstract void setValue(JValue v);
	
	public static JValue[] copyArrayJValue(JValue[] src){
		JValue[] dest = new JValue[src.length];
		for (int i= 0; i<src.length; i++){
			if (src[i] == null){
				dest[i] = null;
			}
			else {
				dest[i] = src[i].deepCopy();
			}
		}
		return dest;
	}

	public static Stack<JValue> copyStackJValue(Stack<JValue> src) {
		Stack<JValue> dest = new Stack<JValue>();
		for (JValue j : src){
			dest.add(j.deepCopy());
		}
		return dest;
	}
	/**
	 * Set an JValue as a symbol
	 * @param b True => Input , False => Temp
	 */
	public void setSymbol(boolean b)
	{
		if (b) {
			char c = (char) (name.charAt(0)+nSymbolic);
			name = Character.toString(c);
			nSymbolic++;
		}
		
		isSymbol = true;
		
	}
	
	

	public boolean isSymbol()
	{
		return isSymbol;
	}
	
	public String getName(){
		return String.valueOf(name);
	}
	
	public static int getNSymbolic(){
		return nSymbolic;
	}
	
	public abstract String toString();

	public void setName(String name2) {
		name = name2;		
	}	
	
	public Constraint getConstraintAsig(){
		return constraintAsig;
	}
	
	public void setConstraintAsig(Constraint c){
		constraintAsig = c;
	}

	public int getCopies() {
		return nCopies;
	}
	
	public void setCopies(int n){
		nCopies += n;
	}
	
	public boolean isNegation(String s) {
		return (isNegation && s.substring(0,1).equals(name.substring(0,1)) );
	}
	
	public Constraint getConstraintNegation() {
		return constraintNegation;
	}

	public void setConstraintNegation(Constraint constraintNegation) {
		this.constraintNegation = constraintNegation;
	}
	

}
		
