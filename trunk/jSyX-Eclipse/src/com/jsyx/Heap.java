package com.jsyx;

import java.util.Arrays;
import java.util.Stack;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.Type;

import com.jsyx.tipos.JArray;
import com.jsyx.tipos.JInteger;
import com.jsyx.tipos.JObject;
import com.jsyx.tipos.JValue;

/**
 * Memoria dinámica para la JVM
 * <p>
 * Array de JValue representa la memoria, espacio para siguiente objeto lo
 * decide nextObjectID.
 * </p>
 * 
 * @author JZ
 * @version 16/12/2012
 */
public class Heap {

	private int nextObjectID;
	private JValue[] heap;

	public Heap(int size) {
		nextObjectID = 0;
		heap = new JValue[size];
	}

	/**
	 * Copy Constructor
	 * 
	 * @param h
	 *            Source Heap
	 */
	private Heap(Heap old) {
		this.nextObjectID = old.nextObjectID;
		this.heap = JValue.copyArrayJValue(old.heap);
	}

	/**
	 * Crea un objeto de tipo mainClass y lo guarda en el heap
	 * 
	 * @param mainClass
	 *            tipo de objeto a crear
	 * @return la referencia al objeto
	 */
	public JObject createObject(JClass mainClass) {
		JObject object = new JObject(nextObjectID++, mainClass);
		heap[object.heapPtr] = object;
		return object;
	}

	public JValue getObjPointer(JObject obj) {
		return heap[obj.heapPtr];
	}

	public JValue getObj(int i) {
		return heap[i];
	}

	/**
	 * @param jclass
	 *            la clase padre
	 * @return devuelve el primer objeto del heap que sea hijo de jclass
	 */
	public JValue getObjExt(JClass jclass) {
		for (int i = 0; i < heap.length; i++) {
			try {
				JObject o = (JObject) heap[i];
				if (o.jClass.extiende(jclass))
					return o;
			} catch (Exception e) {
				// no es JObject sigo
			}
		}
		return null;
	}

	/**
	 * Crea un objeto de tipo String y lo guarda en el heap
	 * 
	 * @param strValue
	 *            valor del JString a crear
	 * @param classes
	 *            ClassArea a usar
	 * @return
	 */
	public JObject createStringObject(String strValue, ClassArea classes) {
		JClass stringClass = classes.loadClass("java.lang.String");

		JObject object = (JObject) createObject(stringClass);

		// TODO dudas
		// heap[object.heapPtr + 1] = jstr;
		// JString jstr = new JString(strValue);
		JArray arrayString = new JArray(nextObjectID, strValue.length(),
				Type.CHAR);
		arrayString.setStr(strValue);
		object.setFieldValue(0, arrayString);
		object.setFieldValue("count", new JInteger(arrayString.nElems));

		return object;
	}

	/**
	 * Crea un array de un tipo simple representado por type de tamaño size
	 * 
	 * @param type
	 *            tipo a crear
	 * @param size
	 *            tamaño del array
	 * @return referencia al array creado
	 */
	public JArray createNewArray(short type, int size) {
		Type t = null;
		switch (type) {
		case Constants.T_BOOLEAN:
			t = Type.BOOLEAN;
			break;
		case Constants.T_CHAR:
			t = Type.CHAR;
			break;
		case Constants.T_FLOAT:
			t = Type.FLOAT;
			break;
		case Constants.T_DOUBLE:
			t = Type.DOUBLE;
			break;
		case Constants.T_BYTE:
			t = Type.BYTE;
			break;
		case Constants.T_SHORT:
			t = Type.SHORT;
			break;
		case Constants.T_INT:
			t = Type.INT;
			break;
		case Constants.T_LONG:
			t = Type.LONG;
			break;

		}
		JArray array = new JArray(nextObjectID++, size, t);
		heap[array.heapPtr] = array;
		return array;
	}
	
	/**
	 * Crea un array de referencias jClass de tamaño size
	 * 
	 * @param jClass
	 *            tipo del array a crear
	 * @param size
	 *            tamaño del array 
	 * @return el array creado
	 */
	public JArray createNewObjectArray(JClass jClass, int size) {
		JArray array = new JArray(nextObjectID++, size, jClass, Type.OBJECT);
		heap[array.heapPtr] = array;
		//Creamos todos los objetos del array
		JObject jo;
		for (int i = 0; i < size; i++){
			jo = new JObject(array.heapPtr, jClass);
			array.setAt(i, jo);
		}
		return array;
	}
	

	@Override
	public String toString() {
		return "nextObjectID=" + nextObjectID
				+ System.getProperty("line.separator") + Arrays.toString(heap);
	}

	public static void main(String[] args) {
		/*
		 * Type t = null; Heap h = new Heap(5); h.heap[0] = new JChar('c'); Heap
		 * copy = new Heap(h);
		 * 
		 * if (h != copy) System.out.print("True 1"); else
		 * System.out.print("False 1"); if (h.equals(copy))
		 * System.out.print("True 2"); else System.out.print("False 2"); if
		 * (h.getClass() == copy.getClass()) System.out.print("True 3"); else
		 * System.out.print("False 3");
		 */

		Stack<JValue> s = new Stack<JValue>();
		s.push(new JInteger(5));
		s.push(new JInteger(4));
		s.push(new JInteger(3));
		s.push(new JInteger(2));
		s.push(new JInteger(1));
		Stack<JValue> dest = new Stack<JValue>();
		for (JValue j : s) {
			dest.add(j.deepCopy());
		}
	}

	public Heap deepCopy() {
		return new Heap(this);
	}

}
