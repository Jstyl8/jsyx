package com.jsyx.test;

import java.io.IOException;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.Type;

import choco.Choco;
import choco.Options;
import choco.kernel.model.variables.integer.IntegerVariable;
import choco.kernel.model.variables.real.RealVariable;

import com.bcel.log.Log;
import com.jsyx.ClassArea;
import com.jsyx.Heap;
import com.jsyx.JClass;
import com.jsyx.JConstraintSolver;
import com.jsyx.JMethod;
import com.jsyx.JVM;
import com.jsyx.StackFrame;
import com.jsyx.TipoExec;
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
 * Programa de prueba
 * 
 * @author JZ
 * @deprecated Usar ahora Exec que junta simbolica y normal
 */

public class TestJVMSymb {

	private static JVM jvm;

	/**
	 * 
	 * @param args
	 *            0.- Class 1.-Path 2.-Function 3.-Heap 4.-tipoExec
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		executeSymb(args[0], args[1], args[2], args[3], args[4]);
	}

	/**
	 * 
	 * @param strClass
	 *            Class of the method to test
	 * @param strPath
	 *            Class Path
	 * @param strMethod
	 *            Method to test
	 * @param size
	 *            Size of the Heap
	 * @param tipoExec
	 *            Type of Execution
	 * @throws IOException
	 */

	private static void executeSymb(String strClass, String strPath,
			String strMethod, String size, String tipoExec) throws IOException {

		// Log
		Log l = Log.getInstance();
		l.writeLog("Path -> " + strPath + "\nClass:Method -> " + strClass + ":"
				+ strMethod);

		// creamos heap
		Heap heap = new Heap(Integer.parseInt(size));
		// obtenemos directorio actual
		String dirActual = System.getProperty("user.dir");
		// creamos area de clases pasandole directorio actual
		ClassArea ca = new ClassArea(dirActual);
		// recreamos con bcel para adaptar instrucciones
		ca.generateClass(strClass, strClass.replace('.', '/') + ".class");
		// compruebo si hay que ejecutar clinit antes de main
		boolean clinit = ca.checkCLINIT(strClass);
		// leemos clase y buscamos metodo principal
		JClass mainClass = ca.loadClass(strClass);
		JMethod jMethod = mainClass.getMethod(strMethod);
		// creamos frame principal
		StackFrame mainFrame = new StackFrame(null, jMethod, mainClass);

		// maximo argumentos
		int maxArgs = jMethod.getMaxArgs();

		Type[] attributeTypes = jMethod.getArgumentTypes();

		// creamos jvm y set tipo de ejecucion
		jvm = new JVM(mainFrame, heap, ca, true);
		jvm.setTipoExec(TipoExec.valueOf(tipoExec));

		l.writeLog("\nInputs Types");
		JValue param = null;
		// modificamos parametros de entrada al mainFrame y al Choco
		for (int i = 0; i < maxArgs; i++) {
			param = newJValues(attributeTypes[i], nextDefaultName());
			param.setSymbol(true);
			mainFrame.getLocals()[i] = param;
			// si es un objeto modificamos sus fields
			if (param.type.equals(Type.OBJECT)) {
				JObject jo = (JObject) param;
				for (int j = 0; j < jo.jFields.length; j++) {
					// no usamos valor devuelto para evitar sobreescribir el
					// valor del field si existe desde la clase (static)
					String namepro = jo.getName() + "."
							+ jo.jFields[j].getName();
					newJValues(jo.jFields[j].fieldInfo.getType(), namepro);
					jo.jFields[j].getValue().setSymbol(true);
					jo.jFields[j].getValue().setName(namepro);
				}
			}
			// en lugar de usar param.type uso el tipo real
			// medio bug provocado pork al crear nuestro JArray el tipo es int
			// no como int[], k es un ArrayType en bcel, pero por ahora mejor
			// asi pa evitar follones con lo demas
			else if (attributeTypes[i] instanceof ArrayType) {
				JArray jo = (JArray) param;
				for (int j = 0; j < jo.data.length; j++) {
					String namepro = jo.getName() + "[" + j + "]";
					jo.data[j] = newJValues(param.type, namepro);
					jo.data[j].setSymbol(true);
					jo.data[j].setName(namepro);
				}
			}
		}

		// convertimos los static en symbol si hay
		for (int j = 0; j < mainClass.getJFields().length; j++) {
			// no usamos valor devuelto para evitar sobreescribir el
			// valor del field si existe desde la clase (static)
			String namepro = mainClass.getName() + "."
					+ mainClass.getJFields()[j].getName();
			newJValues(mainClass.getJFields()[j].fieldInfo.getType(), namepro);
			mainClass.getJFields()[j].getValue().setSymbol(true);
			mainClass.getJFields()[j].getValue().setName(namepro);
		}

		// estas 2 no pork main es static!
		// JValue mainObject =heap.createObject(mainClass);
		// stackFrame.operands.push(mainObject);

		// compruebo si tengo que ejecutar clinit, si es asi ejecuta previo main
		if (clinit) {
			StackFrame clinitFrame = new StackFrame(mainFrame,
					mainClass.getCLINIT(), mainClass);
			jvm.setTopFrame(clinitFrame);
		}
		// ejecucion
		jvm.runBT();
		l.printLog();
		// jvm.run();

	}

	public static String nextDefaultName() {
		return String.valueOf((char) ('A' + JValue.getNSymbolic()));
	}

	/**
	 * Create the input of the method and add new IntegerVariables to model
	 * 
	 * @param type
	 *            Type of the input
	 * @return created JValue
	 */
	public static JValue newJValues(Type type, String s) {

		Log l = Log.getInstance();

		if (type.equals(Type.BYTE)) {
			IntegerVariable iV = Choco.makeIntVar(s, (int) Byte.MIN_VALUE,
					(int) Byte.MAX_VALUE, Options.V_ENUM);
			JConstraintSolver.getInstance().addVariable(iV);
			l.writeLog(iV.toString() + " -> Byte");
			return new JByte();
		} else if (type.equals(Type.CHAR)) {
			IntegerVariable iV = Choco.makeIntVar(s, (int) Character.MIN_VALUE,
					(int) Character.MAX_VALUE, Options.V_ENUM);
			JConstraintSolver.getInstance().addVariable(iV);
			l.writeLog(iV.toString() + " -> Char");
			return new JChar();
		} else if (type.equals(Type.DOUBLE)) {
			RealVariable rV = Choco.makeRealVar(s, Double.MIN_VALUE,
					Double.MAX_VALUE, Options.V_ENUM);
			JConstraintSolver.getInstance().addVariable(rV);
			l.writeLog(rV.toString() + " -> Double");
			return new JDouble();
		} else if (type.equals(Type.FLOAT)) {
			RealVariable rV = Choco.makeRealVar(s, Float.MIN_VALUE,
					Float.MAX_VALUE, Options.V_ENUM);
			JConstraintSolver.getInstance().addVariable(rV);
			l.writeLog(rV.toString() + " -> Float");
			return new JFloat();
		} else if (type.equals(Type.INT)) {
			// TODO Warning Choco Bounds demasiado grandes
			IntegerVariable iV = Choco.makeIntVar(s, -10, 10, Options.V_BOUND);
			JConstraintSolver.getInstance().addVariable(iV);
			l.writeLog(iV.toString() + " -> Integer");
			return new JInteger();
		} else if (type.equals(Type.LONG)) {
			// TODO Warning Choco Bounds demasiado grandes
			IntegerVariable iV = Choco.makeIntVar(s, -100, 100, Options.V_ENUM);
			JConstraintSolver.getInstance().addVariable(iV);
			l.writeLog(iV.toString() + " -> Long");
			return new JLong();
		} else if (type.getType() == Constants.T_OBJECT) {
			l.writeLog(s + " -> Object");
			// genera el objeto a mano usando el nombre de la clase
			return jvm.ejecutarPreNEW(jvm.getNameClassFromSignature(type
					.getSignature()));
		} else if (type.equals(Type.SHORT)) {
			IntegerVariable iV = Choco.makeIntVar(s, (int) Short.MIN_VALUE,
					(int) Short.MAX_VALUE, Options.V_ENUM);
			JConstraintSolver.getInstance().addVariable(iV);
			l.writeLog(iV.toString() + " -> Short");
			return new JShort();
		} else {
			// TODO METER ARRAY EN EL CHOCO?¿
			ArrayType atype = (ArrayType) type;
			l.writeLog(s + " -> Array of " + atype.getElementType());
			// creamos array de tamaño 5 por defecto pss
			return jvm.ejecutarPreNEWARRAY(atype.getElementType().getType(), 5);
		}

	}
}
