package com.jsyx;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantDouble;
import org.apache.bcel.classfile.ConstantFloat;
import org.apache.bcel.classfile.ConstantInteger;
import org.apache.bcel.classfile.ConstantLong;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantValue;
import org.apache.bcel.classfile.Field;

import com.jsyx.tipos.JArray;
import com.jsyx.tipos.JByte;
import com.jsyx.tipos.JChar;
import com.jsyx.tipos.JDouble;
import com.jsyx.tipos.JFloat;
import com.jsyx.tipos.JInteger;
import com.jsyx.tipos.JLong;
import com.jsyx.tipos.JObject;
import com.jsyx.tipos.JShort;
import com.jsyx.tipos.JValue;

/**
 * Representa un campo de una clase
 * 
 * @author JZ
 * @version 07/12/2012
 */
public class JField {
	public Field fieldInfo;
	private JValue value;
	
	private JField(JField old){
		if (old.fieldInfo == null){
			this.fieldInfo = null; 
		}
		else {
			this.fieldInfo = old.fieldInfo.copy(old.fieldInfo.getConstantPool());
		}
		if (old.fieldInfo == null){
			this.fieldInfo = null; 
		}
		else {
			this.value = old.value.deepCopy();
		}
	}

	public JField(Field field) {
		fieldInfo = field;
		ConstantValue val = field.getConstantValue();
		ConstantPool cp;
		int index;
		Constant c = null;
		if (val != null) {
			cp = val.getConstantPool();
			index = val.getConstantValueIndex();
			c = cp.getConstant(index);
		}
		//System.out.println(val);
		switch (field.getType().getType()) {
		case Constants.T_INT:
			if (val != null) {
				ConstantInteger cl = (ConstantInteger) c;
				value = new JInteger(cl.getBytes());
			} else
				value = new JInteger();
			break;
		case Constants.T_LONG:
			if (val != null) {
				ConstantLong cl = (ConstantLong) c;
				value = new JLong(cl.getBytes());
			} else
				value = new JLong();
			break;
		case Constants.T_FLOAT:
			if (val != null) {
				ConstantFloat cl = (ConstantFloat) c;
				value = new JFloat(cl.getBytes());
			} else
				value = new JFloat();
			break;
		case Constants.T_DOUBLE:
			if (val != null) {
				ConstantDouble cl = (ConstantDouble) c;
				value = new JDouble(cl.getBytes());
			} else
				value = new JDouble();
			break;
		case Constants.T_BYTE:
			if (val != null) {
				ConstantInteger cl = (ConstantInteger) c;
				value = new JByte(cl.getBytes());
			} else
				value = new JByte();
			break;
		case Constants.T_BOOLEAN:
			if (val != null) {
				ConstantInteger cl = (ConstantInteger) c;
				value = new JByte(cl.getBytes());
			} else
				value = new JByte();
			break;
		case Constants.T_CHAR:
			if (val != null) {
				ConstantInteger cl = (ConstantInteger) c;
				value = new JChar((char) cl.getBytes());
			} else
				value = new JChar();
			break;
		case Constants.T_SHORT:
			if (val != null) {
				ConstantInteger cl = (ConstantInteger) c;
				value = new JShort(cl.getBytes());
			} else
				value = new JShort();
			break;
		case Constants.T_ARRAY:
			// value = new JArray();
			value = new JArray(field.getType());
			break;
		case Constants.T_OBJECT:
			value = new JObject();
			break;
		}

	}
	
	public static JField[] copyArrayJField(JField[] src){
		JField[] dest = new JField[src.length];
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

	private JField deepCopy() {
		return new JField(this);
	}

	public JValue getValue() {
		return value;
	}

	public void setValue(JValue val) {
		value = val;
	}

	public String getName() {
		return fieldInfo.getName();
	}

	@Override
	public String toString() {
		return getName() + ":" + value;
	}

	public boolean isStatic() {
		return fieldInfo.isStatic();
	}
}
