package com.bcel.test;

/**
 * Clase para pruebas con objetos
 * 
 * @author JZ
 * @version 07/12/2012
 */
public class A {

	private int valor;
//	static String strStatic="WEEA";

	public A(int valor) {
		this.valor = valor;
		//valStatic = valor;
	}

	public int get() {
		return valor;
	}
	public void set(int b){
		this.valor=b;
	}

	//static int valStatic;
//	public int getStatic() {
//		return valStatic;
//	}
//	
//	public void setStatic(int b){
//		valStatic=b;
//	}
}
