package com.jsyx.tipos;

import java.util.Arrays;

import org.apache.bcel.generic.Type;

import com.jsyx.JClass;

/**
 * Tipo Array para JVM
 * 
 * @author JZ
 * @version 05/12/2012
 */
public class JArray extends JValue{

	public JValue[] data;
	public int nElems;
	public int heapPtr;
	JClass jClass;

	/**
	 * Para tipos simples
	 * 
	 * @param heapPtr
	 *            lugar en el heap
	 * @param nElems
	 *            tamaño array
	 * @param type
	 *            el tipo segun bcel
	 */
	public JArray(int heapPtr, int nElems, Type type) {
		this.heapPtr = heapPtr;
		this.nElems = nElems;
		data = new JValue[nElems];
		this.type = type;
		this.jClass = null;

	}
	
	/**
	 * Copy Constructor
	 * @param old
	 */
	private JArray(JArray old){
		this.data = copyArrayJValue(old.data); 
		this.nElems = old.nElems;
		this.heapPtr = old.heapPtr;
		if (old.jClass == null){
			this.jClass = null;
		}
		else {
			this.jClass = old.jClass.deepCopy();
		}
		
	}

	/**
	 * Para tipos simples
	 * 
	 * @param heapPtr
	 *            lugar en el heap
	 * @param nElems
	 *            tamaño array
	 * @param jClass
	 *            tipo representado en abstraccion de clase
	 */
	public JArray(int heapPtr, int nElems, JClass jClass) {
		this.heapPtr = heapPtr;
		this.jClass = jClass;
		this.nElems = nElems;
		data = new JValue[nElems];
		this.type = null;
	}
	
	/**
	 * Para tipos simples
	 * 
	 * @param heapPtr
	 *            lugar en el heap
	 * @param nElems
	 *            tamaño array
	 * @param jClass
	 *            tipo representado en abstraccion de clase
	 * @param type
	 *            de los elementos del array       
	 */
	public JArray(int heapPtr, int nElems, JClass jClass, Type type) {
		this.heapPtr = heapPtr;
		this.jClass = jClass;
		this.nElems = nElems;
		data = new JValue[nElems];
		this.type = type;
	}

	// constructores sin conocer todos los datos
	public JArray() {
	}

	public JArray(Type type) {
		this.type = type;
	}

	public JValue get(int i) {
		return data[i];
	}
	
	public int getnElems(){
		return nElems;
	}

	public void setAt(int pos, JValue val) {
		data[pos] = val;
	}
	
	public void setRawAt(int pos, JValue val) {
		data[pos].setValue(val);
	}

	public void setStr(String str) {
		for (int i = 0; i < data.length; i++)
			data[i] = new JChar(str.charAt(i));
	}
	public String toStr(){
		StringBuffer sb=new StringBuffer();
		for (int i = 0; i < data.length; i++) {
			sb.append(data[i]);
		}
		return sb.toString();
	}
	@Override
	public String toString() {
		return "h=" + heapPtr + "-Data:" + data!=null?Arrays.toString(data):"null";
	}

	@Override
	public JValue deepCopy() {
		return new JArray(this);
	}

	@Override
	public void setValue(JValue v) {
		// TODO Auto-generated method stub
	}


}
