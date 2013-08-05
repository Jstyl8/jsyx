package com.jsyx;

import java.util.Arrays;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;

import com.jsyx.test.Exec;
import com.jsyx.test.TestJVMSymb;
import com.jsyx.tipos.JValue;

/**
 * Clase para la ejecucion de la JVM
 * <p>
 * Contiene 2 referencias a clases de bcel, jClassInfo para lecturas y jClassGen
 * para modificaciones. Se usan ambas porque proporcionan diferentes metodos
 * utiles para la codificación.
 * </p>
 * <p>
 * También se incluye array de fields para valores static
 * </p>
 * 
 * @author JZ
 * @version 07/12/2012
 */
public class JClass {

	JavaClass jClassInfo;
	ClassGen jClassGen;
	private JField[] jFields;

	public JClass(JavaClass jClass) {
		jClassInfo = jClass;
		jClassGen = new ClassGen(jClass);
		jFields = new JField[jClass.getFields().length];
		for (int i = 0; i < jFields.length; i++) {
			jFields[i] = new JField(jClass.getFields()[i]);
		}
	}

	/**
	 * Copy Constructor
	 * 
	 * @return
	 */
	private JClass(JClass old) {
		if (old.jClassInfo == null) {
			this.jClassInfo = null;
		} else {
			this.jClassInfo = old.jClassInfo.copy();
		}
		if (old.jClassGen == null) {
			this.jClassGen = null;
		} else {
			this.jClassGen = (ClassGen) old.jClassGen.clone();
		}
		if (old.jFields == null) {
			this.jFields = null;
		} else {
			this.jFields = JField.copyArrayJField(old.jFields);
		}

	}

	/**
	 * @return una representacion completa del .class
	 */
	public String getBytecode() {
		StringBuffer sb = new StringBuffer();
		sb.append("------Resumen de clase------\n");
		sb.append(jClassInfo.toString());
		sb.append("\n------Constant Pool------\n");
		sb.append(jClassInfo.getConstantPool().toString());
		
		sb.append("\n------Metodos de clase------\n");
		if (jClassInfo.getMethods().length != 0) {
			for (int i = 0; i < jClassInfo.getMethods().length; i++) {
				sb.append((jClassInfo.getMethods()[i]));
				sb.append("\nSignature: "+(jClassInfo.getMethods()[i]).getSignature());
				sb.append("\n");
				Code code = jClassInfo.getMethods()[i].getCode();
				if (code != null)
					sb.append(code + "\n");
			}
		} else
			sb.append("\nNINGUNO\n");
		return sb.toString();
	}

	public ConstantPool getConstantPool() {
		return jClassInfo.getConstantPool();
	}

	public String getName() {
		return jClassInfo.getClassName();
	}

	public ConstantPoolGen getConstantPoolGen() {
		return jClassGen.getConstantPool();
	}

	public JField[] getJFields() {
		return jFields;
	}

	public Field[] getFields() {
		return jClassInfo.getFields();
	}

	public JMethod getMainMethod() {
		return getMethod("main");
	}

	public JMethod getMehtod(String methodName) {
		return getMethod(methodName);
	}

	public JMethod getCLINIT() {
		return getMethod("<clinit>");
	}

	/**
	 * @param methodName
	 *            el nombre del metodo a obtener
	 * @return el metodo o null si no se encuentra
	 */
	public JMethod getMethod(String methodName) {
		return getMethod(methodName, null);
	}

	/**
	 * Busca un metodo basandose en su nombre y/o signatura
	 * 
	 * @param methodName
	 *            nombre del metodo
	 * @param signature
	 *            signatura del metodo o null si no se requiere
	 * @return instancia del metodo encapsulado en JMethod
	 */
	public JMethod getMethod(String methodName, String signature) {
		Method searchedMethod = null;
		for (Method m : jClassInfo.getMethods())
			if (m.getName().equals(methodName)) {
				if (signature != null) {
					if (m.getSignature().equals(signature)) {
						searchedMethod = m;
						break;
					}
				} else {
					searchedMethod = m;
					break;
				}
			}
		return searchedMethod != null ? new JMethod(searchedMethod, jClassGen)
				: null;
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

	/**
	 * 
	 * 
	 * @return el numero de fields de una clase y de sus superclases
	 * 
	 */
	public int GetObjectFieldCount() {
		int count = jClassInfo.getFields().length;

		JClass superClass = null;
		int superClassFieldCount = 0;
		try {
			superClass = new JClass(jClassInfo.getSuperClass());
			if (superClass != null
					&& (superClass.getName().equals("java/lang/Object") && !superClass
							.getName().equals("java/lang/Object")))
				superClassFieldCount = superClass.GetObjectFieldCount();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		count += superClassFieldCount;
		return count;
	}

	/**
	 * Comprueba si this es subclase de superClass
	 * 
	 * @param superClass
	 *            la clase padre en JClass
	 * @return si o no en funcion de si this es subclase o no
	 */
	public boolean extiende(JClass superClass) {
		return extiende(superClass.getName());
		// JavaClass[] supers;
		// try {
		// // si es ella misma
		// if (superClass.getName().equals(this.jClassInfo.getClassName()))
		// return true;
		// supers = this.jClassInfo.getSuperClasses();
		// for (int i = 0; i < supers.length; i++) {
		// if (superClass.getName().equals(supers[i].getClassName()))
		// return true;
		// }
		// } catch (ClassNotFoundException e) {
		// e.printStackTrace();
		// }
		// return false;
	}

	/**
	 * Comprueba si this es subclase de superClass
	 * 
	 * @param superClass
	 *            la clase padre en String
	 * @return si o no en funcion de si this es subclase o no
	 */
	public boolean extiende(String superClass) {

		JavaClass[] supers;
		try {
			// si es ella misma
			if (superClass.equals(this.jClassInfo.getClassName()))
				return true;
			supers = this.jClassInfo.getSuperClasses();
			for (int i = 0; i < supers.length; i++) {
				if (superClass.equals(supers[i].getClassName()))
					return true;
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public String toString() {
		return jClassInfo.getClassName() + " - Fields: "
				+ Arrays.toString(jFields);
	}

	public JClass deepCopy() {
		return new JClass(this);
	}

	/**
	 * Convierte los valores de la clase en symbolos si no lo son
	 */
	public void convertToSymbol() {
		for (int j = 0; j < getJFields().length; j++) {
			// no usamos valor devuelto para evitar sobreescribir el
			// valor del field si existe desde la clase (static)
			if (!getJFields()[j].getValue().isSymbol()) {
				String namepro = getName() + "." + getJFields()[j].getName();
				Exec.newJValues(getJFields()[j].fieldInfo.getType(), namepro);
				getJFields()[j].getValue().setSymbol(true);
				getJFields()[j].getValue().setName(namepro);
			}
		}

	}

	public boolean isException() {
		return jClassInfo.getClassName().equals("java.lang.Throwable");
	}

	// @Override
	// public boolean equals(Object obj) {
	// if (!(obj instanceof JClass))
	// return false;
	// JClass j = (JClass) obj;
	//
	// if (j.jClassInfo.equals(this.jClassInfo))
	// return true;
	// return false;
	// }
}
