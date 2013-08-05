package com.bcel.test;
import java.io.IOException;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

public class PruebasBCEL {

	public static void main(String[] args) {

		ClassGen myClassGen;

		try {
			JavaClass myClass = Repository.lookupClass("com/bcel/test/Test");
			myClassGen = new ClassGen(myClass);
		}
		catch(ClassNotFoundException ex) {
			ex.printStackTrace();
			return;
		}
		
		Field[] fields = myClassGen.getFields();
		for (Field f : fields){
			System.out.print("Field : " + f.toString()+"\n");
		}
		
		Method[] methods = myClassGen.getMethods();
		for (Method m : methods){
			System.out.print("Method : " + m.toString()+"\n");
		}
		
		//myClassGen.get

		//this is where you mess around with the classes

		try {
			//susittuimos por defecto
			myClassGen.getJavaClass().dump("bin/com/bcel/test/Test.class");
		}
		catch(IOException ex) {
			ex.printStackTrace();
		}
	}
}