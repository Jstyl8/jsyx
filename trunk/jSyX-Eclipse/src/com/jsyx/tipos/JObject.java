package com.jsyx.tipos;

import org.apache.bcel.generic.Type;

import com.jsyx.JClass;
import com.jsyx.JField;

/**
 * Clase que representa un objeto para la JVM
 * <p>
 * Contiene su posicion en el heap, la clase a la que pertenece, y espacio para
 * los campos y sus valores
 * </p>
 * 
 * @author JZ
 * @version 07/12/2012
 */
public class JObject extends JValue {
	public int heapPtr;
	public JClass jClass;
	// campos de la clase representado para objetos
	public JField[] jFields;

	//creacion a null
	public JObject() {
		jClass=null;
		jFields=null;
	}
	/**
	 * 
	 * @param old
	 */
	private JObject(JObject old){
		this.heapPtr = old.heapPtr ;
		this.isSymbol = old.isSymbol;
		this.name = old.name;
		if (old.jClass == null){
			this.jClass = null;
		}
		else {
			this.jClass = old.jClass.deepCopy();
		}
		if (old.jFields == null){
			this.jFields = null;
		}
		else {
			this.jFields = JField.copyArrayJField(old.jFields);
		}
	}

	/**
	 * Crea un objeto con posicion heapPtr de tipo jClass. Se reserva espacio
	 * para sus fields en base a los fields de jClass
	 * 
	 * @param heapPtr
	 *            posicion en el heap
	 * @param jClass
	 *            clase instanciada
	 */
	public JObject(int heapPtr, JClass jClass) {
		int count = jClass.GetObjectFieldCount();
		this.heapPtr = heapPtr;
		type = Type.OBJECT;
		this.jClass = jClass;
		if (jClass != null) {
			jFields = new JField[count];
			for (int i = 0; i < jFields.length; i++) {
				jFields[i] = new JField(jClass.getFields()[i]);
			}
		}
	}

	public void setFieldValue(int i, JValue val) {
		jFields[i].setValue(val);
	}

	public void setFieldValue(String name, JValue val) {
		for (int i = 0; i < jFields.length; i++) {
			if (jFields[i].getName().equals(name))
				jFields[i].setValue(val);
		}
	}

	public JField getField(int i) {
		return jFields[i];
	}

	public JField getField(String name) {
		for (int i = 0; i < jFields.length; i++) {
			if (jFields[i].getName().equals(name))
				return jFields[i];
		}
		return null;
	}

	@Override
	public String toString() {
		if (isSymbol()){
			return getName();
		}
		else{
			return "h=" + heapPtr + "-" + (jClass==null?"null":jClass.getName());
		}
		
	}
	@Override
	public JValue deepCopy() {
		return new JObject(this);
	}
	@Override
	public void setValue(JValue v) {
		// TODO Auto-generated method stub
		
	}
	public boolean isException() {
		return this.jClass.isException();
	}
	public boolean notNull() {
		return this.jClass!=null;
	}

}
