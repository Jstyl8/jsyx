package com.jsyx.test;

import java.io.File;
import java.io.IOException;

import com.jsyx.ClassArea;
import com.jsyx.Heap;
import com.jsyx.JClass;
import com.jsyx.JMethod;
import com.jsyx.JVM;
import com.jsyx.StackFrame;
import com.jsyx.TipoExec;

/**
 * Programa de prueba
 * 
 * @author JZ
 * @deprecated Usar ahora Exec que junta simbolica y normal
 */
public class TestJVM {

	public static void main(String[] args) {
		execute(args[0], args[1], args[2]);
	}

	private static void execute(String strClass, String size, String tipoExec) {

		// creamos heap
		Heap heap = new Heap(Integer.parseInt(size));
		// obtenemos directorio actual
		String dirActual=System.getProperty("user.dir");
		// creamos area de clases pasandole directorio actual
		ClassArea ca = new ClassArea(dirActual);
		// recreamos con bcel para adaptar instrucciones
		//TODO añadido /bin para que pille directamente desde eclipse el archivo recreado, quitar para version final
		ca.generateClass(strClass, strClass.replace('.', '/') + ".class");
		// compruebo si hay que ejecutar clinit antes de main
		boolean clinit = ca.checkCLINIT(strClass);
		// leemos clase y buscamos metodo principal
		JClass mainClass = ca.loadClass(strClass);
		JMethod mainMethod = mainClass.getMainMethod();
		// creamos frame principal
		StackFrame mainFrame = new StackFrame(null, mainMethod, mainClass);

		// estas 2 no pork main es static!
		// JValue mainObject =heap.createObject(mainClass);
		// stackFrame.operands.push(mainObject);

		// creamos jvm y set tipo de ejecucion
		JVM jvm = new JVM(mainFrame, heap, ca);
		jvm.setTipoExec(TipoExec.valueOf(tipoExec));

		// compruebo si tengo que ejecutar clinit, si es asi ejecuta previo main
		if (clinit) {
			StackFrame clinitFrame = new StackFrame(mainFrame,
					mainClass.getCLINIT(), mainClass);
			jvm.setTopFrame(clinitFrame);
		}
		// ejecucion
		jvm.run();
	}
	
	/**
	 * 
	 * @param strClass
	 * @param size
	 * @param tipoExec
	 */
	/*
	private static void execute(String strClass,String strPath, String strMethod,String strInType,
				String strOutType,String size, String tipoExec, String) {

		
		// creamos heap
		Heap heap = new Heap(Integer.parseInt(size));
		// obtenemos directorio actual
		String dirActual = "";
		try {
			dirActual = new File(".").getCanonicalPath();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// creamos area de clases pasandole directorio actual
		ClassArea ca = new ClassArea(dirActual);
		// recreamos con bcel para adaptar instrucciones
		ca.generateClass(strClass, strClass.replace('.', '/') + ".class");
		// compruebo si hay que ejecutar clinit antes de main
		boolean clinit = ca.checkCLINIT(strClass);
		// leemos clase y buscamos metodo principal
		JClass mainClass = ca.loadClass(strClass);
		JMethod mainMethod = mainClass.getMainMethod();
		// creamos frame principal
		StackFrame mainFrame = new StackFrame(null, mainMethod, mainClass);

		// estas 2 no pork main es static!
		// JValue mainObject =heap.createObject(mainClass);
		// stackFrame.operands.push(mainObject);

		// creamos jvm y set tipo de ejecucion
		JVM jvm = new JVM(mainFrame, heap, ca);
		jvm.setTipoExec(TipoExec.valueOf(tipoExec));

		// compruebo si tengo que ejecutar clinit, si es asi ejecuta previo main
		if (clinit) {
			StackFrame clinitFrame = new StackFrame(mainFrame,
					mainClass.getCLINIT(), mainClass);
			jvm.setTopFrame(clinitFrame);
		}
		// ejecucion
		// jvm.runBT();
		jvm.run();

		
	}*/

}
