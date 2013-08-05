package com.jsyx;

import java.io.IOException;
import java.util.Hashtable;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.util.ClassPath;
import org.apache.bcel.util.SyntheticRepository;

/**
 * Espacio de clases para la JVM
 * <p>
 * Guardadas en hashtable usando nombre como clave
 * </p>
 * 
 * @author JZ
 * @version 16/12/2012
 */
public class ClassArea {
	private Hashtable<String, JClass> classes;

	/**
	 * Crea el el area de clases añadiendo al classpath el directorio actual de
	 * ejecución definiendo el repositorio a usar
	 * 
	 * @param actual
	 *            el directorio actual
	 */
	public ClassArea(String actual) {
		String path = ClassPath.getClassPath()
				+ System.getProperty("path.separator") + actual;
		ClassPath cp = new ClassPath(path);
		Repository.setRepository(SyntheticRepository.getInstance(cp));

		classes = new Hashtable<String, JClass>();
	}

	public boolean addClass(JClass jClass) {
		if (jClass == null)
			return false;

		classes.put(jClass.jClassInfo.getClassName(), jClass);
		return true;
	}

	public boolean yaCargada(String strClass) {
		return classes.containsKey(strClass);
	}

	/**
	 * Comprueba si se debe ejecutar en una clase el clinit o no
	 * 
	 * @param className
	 *            nombre de laclase
	 * @return si se debe ejecutar el clinit o no
	 */
	public boolean checkCLINIT(String className) {
		JClass otra = getClass(className);
		JMethod clinit = otra.getCLINIT();
		if (!yaCargada(className) && clinit != null) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Carga una clase del hashtable o desde directorio si se encuentra, en otro
	 * caso error, ademas la anyade al classpath
	 * 
	 * @param name
	 *            clase a cargar
	 * @return
	 */
	public JClass loadClass(String name) {
		JClass jClass = classes.get(name);
		if (jClass != null)
			return jClass;
		try {
			jClass = new JClass(Repository.lookupClass(name));
			addClass(jClass);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return jClass;
		}
		return jClass;
	}

	/**
	 * Obtiene una clase del hashtable o desde directorio si se encuentra, en
	 * otro caso error, sin a;adirla al class path
	 * 
	 * @param name
	 *            clase a cargar
	 * @return
	 */
	public JClass getClass(String name) {
		JClass jClass = classes.get(name);
		if (jClass != null)
			return jClass;
		try {
			jClass = new JClass(Repository.lookupClass(name));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return jClass;
		}
		return jClass;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer("[");
		for (String clase : classes.keySet())
			sb.append(clase + ", ");
		sb.setLength(sb.length() - 2);
		sb.append("]");
		return sb.toString();
	}

	public void generateClass(String name, String rutaBin) {
		ClassGen myClassGen;
		try {
			JavaClass myClass = Repository.lookupClass(name);
			myClassGen = new ClassGen(myClass);
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
			return;
		}
		// this is where you mess around with the classes
		try {
			myClassGen.getJavaClass().dump(rutaBin);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	// public boolean loadClass(String name, JClass jClass){
	// String path, relPath;
	// if(jClass==null) return false;
	//
	// relPath=name+".class";
	//
	// if(!getAbsolutePath(relPath, path))
	// return false;
	// Repository.l
	// JavaClass bRet=Repository.lookupClass(path);
	//
	// if(!bRet) return FALSE;
	//
	// pClass->SetClassHeap(this);
	//
	// return AddClass(pClass);
	// }
	//
	// private boolean getAbsolutePath(String relative, String path) {
	// String dirActual = new File(".").getAbsolutePath();
	// path = dirActual + "\\" + relative;
	// File pathClass = new File(path);
	// return pathClass.exists() ? true : false;
	// }

}
