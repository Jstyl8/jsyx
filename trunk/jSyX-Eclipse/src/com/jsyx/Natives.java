/**
 * 
 */
package com.jsyx;

import com.jsyx.tipos.JArray;
import com.jsyx.tipos.JInteger;
import com.jsyx.tipos.JObject;
import com.jsyx.tipos.JValue;

/**
 * Clase con metodos nativos
 * 
 * @author JZ
 * @version 16/12/2012
 */
public class Natives {

	/**
	 * Nombres completos de metodos
	 */
	public static String[] SIGS = {
			"java.lang.Throwable@fillInStackTrace()Ljava/lang/Throwable;",
			"com.bcel.test.Test@print(Ljava/lang/String;)V",
			"com.bcel.test.Test@println(Ljava/lang/String;)V",
			"com.bcel.test.Test@getIntFromStr(Ljava/lang/String;)I" };

	public static JValue print(JVM jvm) {
		JObject ostr = (JObject) jvm.topFrame.locals[0];
		JArray arraystr = (JArray) ostr.getField(0).getValue();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < arraystr.nElems; i++) {
			sb.append(arraystr.get(i));
		}
		System.out.print(sb);
		return null;
	}

	public static JValue println(JVM jvm) {
		JObject ostr = (JObject) jvm.topFrame.locals[0];
		JArray arraystr = (JArray) ostr.getField(0).getValue();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < arraystr.nElems; i++) {
			sb.append(arraystr.get(i));
		}
		System.out.println(sb);
		return null;
	}

	public static JValue getIntFromStr(JVM jvm) {
		JObject ostr = (JObject) jvm.topFrame.locals[0];
		JArray arraystr = (JArray) ostr.getField(0).getValue();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < arraystr.nElems; i++) {
			sb.append(arraystr.get(i));
		}
		return new JInteger(new Integer(sb.toString()));
	}

	// Rellena el stacktrace pero no se como, asi que simplemente devuelvo la
	// referencia al objeto de la propia clase en uso buscandolo en el heap y
	// devolviendo aquel que sea subclase de Throwable, que es la clase que
	// tiene le metodo en ejecucion
	public static JValue fillInStackTrace(JVM jvm) {
		return jvm.heap.getObjExt(jvm.topFrame.clase);
	}
}
