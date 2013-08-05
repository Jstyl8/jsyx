package com.jsyx.test;

import java.io.IOException;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.Type;
import org.apache.commons.cli.ParseException;

import choco.Choco;
import choco.Options;
import choco.kernel.model.variables.integer.IntegerVariable;
import choco.kernel.model.variables.real.RealVariable;

import com.bcel.log.Log;
import com.jsyx.ClassArea;
import com.jsyx.Heap;
import com.jsyx.JClass;
import com.jsyx.JConstraintSolver;
import com.jsyx.JField;
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
 * Clase para ejecucion conjunta simbolica y normal con paso de parametros
 * 
 * @author JZ
 * @version 1.1 22/05/2013
 */
public class Exec {

	private static JCmdLine jcmd;
	private static JVM jvm;
	private static int array_size = 5;
	private static int limit_size = 5;

	public static void main(String[] args) throws IOException {
		boolean symbolic = true;
		boolean IHPC = true;

		int heap = 100;
		String clase, method;
		// para probar argumentos
		String[] argsX = { "-c", "com.bcel.test.Test", "-m", "nostatic","-d","-l" };

		// creamos commandlineparser
		jcmd = new JCmdLine(args);

		// parseamos argumentos
		try {
			jcmd.parse();
		} catch (ParseException e) {
			error(e.getMessage(), true);
		}

		// comprobar combinaciones no posibles
		if (jcmd.has("b") && !jcmd.has("c"))
			error("Error: -b, se necesita especificar clase con -c", false);
		if (jcmd.has("n") && jcmd.has("s"))
			error("Error: -n y -s, elija ejecucion simbolica o normal", false);
		if (jcmd.has("o") && jcmd.has("s"))
			error("Error: -o y -s, ejecucion simbolica solo disponible con IHPC",
					false);
		if (jcmd.has("o") && jcmd.has("i"))
			error("Error: -o y -i, elija ejecucion con OPCODE o con IHPC",
					false);
		if (jcmd.has("o") && !jcmd.has("n"))
			error("Error: -o, ejecucion con OPCODE solo disponible en ejecucion normal",
					false);
		if (jcmd.has("u") && jcmd.has("n"))
			error("Error: -u, muestra información solo en ejecucion simbolica",
					false);
		// comprobamos argumentos
		if (jcmd.has("h") || args.length == 0) {
			jcmd.printHelp(false);
			System.exit(0);
		}
		if (jcmd.has("v")) {
			jcmd.printVersion();
			System.exit(0);
		}
		if (jcmd.has("l")) {
			DEBUG.LOG = true;
			DEBUG.USER = false;
		}
		if (jcmd.has("u")) {
			DEBUG.USER = true;
			DEBUG.LOG = false;
		}
		if (jcmd.has("d"))
			DEBUG.ON = true;
		if (jcmd.has("n"))
			symbolic = false;
		if (jcmd.has("o"))
			IHPC = false;
		if (jcmd.has("heap-size")) {
			try {
				heap = jcmd.getAsInt("heap-size");
			} catch (NumberFormatException e) {
				error("Heap-size debe ser entero: " + e.getMessage(), false);
			}
		}
		if (jcmd.has("array-size")) {
			try {
				array_size = jcmd.getAsInt("array-size");
			} catch (NumberFormatException e) {
				error("Array-size debe ser entero: " + e.getMessage(), false);
			}
		}
		if (jcmd.has("limit-size")) {
			try {
				limit_size = jcmd.getAsInt("limit-size");
			} catch (NumberFormatException e) {
				error("Array-size debe ser entero: " + e.getMessage(), false);
			}
		}
		if (!jcmd.has("c"))
			error("Se necesita definir clase con -c", false);
		else {
			clase = jcmd.get("c");
			if (symbolic)
				if (!jcmd.has("m"))
					error("Se necesita definir metodo con -m", false);
				else {
					method = jcmd.get("m");
					// go simbolic
					executeSymb(clase, method, heap);
				}
			else {
				// go normal
				executeNormal(clase, heap, IHPC);
			}
		}
	}

	private static void mostrarBytecode(JClass jclass, JMethod jmethod) {
		if (jcmd.has("b")) {
			jcmd.printBytecode(jclass, jmethod);
			System.exit(0);
		}
	}

	/**
	 * Error en el paso de parametros se debe cerrar el programaF
	 * 
	 * @param m
	 *            mensaje a mostrar
	 * @param help
	 *            mostrar ayuda o no
	 */
	private static void error(String m, boolean help) {
		System.err.println(m);
		if (help)
			jcmd.printHelp(false);
		System.exit(-1);
	}

	private static void executeNormal(String strClass, int size, boolean IH) {

		// creamos heap
		Heap heap = new Heap(size);
		// obtenemos directorio actual
		String dirActual = System.getProperty("user.dir");
		// creamos area de clases pasandole directorio actual
		ClassArea ca = new ClassArea(dirActual);
		// recreamos con bcel para adaptar instrucciones
		// TODO añadir /bin para que pille directamente desde eclipse el
		// archivo recreado, quitar para version final
		ca.generateClass(strClass, strClass.replace('.', '/') + ".class");
		// compruebo si hay que ejecutar clinit antes de main
		boolean clinit = ca.checkCLINIT(strClass);
		// leemos clase y buscamos metodo principal
		JClass mainClass = ca.loadClass(strClass);
		JMethod mainMethod = mainClass.getMainMethod();
		// ------
		// mostramos bytecode si se selecciono dicha opcion
		mostrarBytecode(mainClass, null);
		// ------
		// creamos frame principal
		StackFrame mainFrame = new StackFrame(null, mainMethod, mainClass);

		// estas 2 no pork main es static!
		// JValue mainObject =heap.createObject(mainClass);
		// stackFrame.operands.push(mainObject);

		// creamos jvm y set tipo de ejecucion
		JVM jvm = new JVM(mainFrame, heap, ca);
		if (IH)
			jvm.setTipoExec(TipoExec.IHPC);
		else
			jvm.setTipoExec(TipoExec.OPCODE);

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

	private static void executeSymb(String strClass, String strMethod, int size)
			throws IOException {

		// obtenemos directorio actual
		String dirActual = System.getProperty("user.dir");
		// Log
		Log l = null;
		if (DEBUG.ON || DEBUG.LOG || DEBUG.USER)
			l = Log.getInstance();
		if (DEBUG.LOG) {
			l.writeLog("Path -> " + dirActual + "\nClass:Method -> " + strClass
					+ ":" + strMethod);
		}
		if (DEBUG.USER) {
			l.writeUser("Path -> " + dirActual + "\nClass:Method -> "
					+ strClass + ":" + strMethod);
		}
		// creamos heap
		Heap heap = new Heap(size);
		// creamos area de clases pasandole directorio actual
		ClassArea ca = new ClassArea(dirActual);
		// recreamos con bcel para adaptar instrucciones
		ca.generateClass(strClass, strClass.replace('.', '/') + ".class");
		// compruebo si hay que ejecutar clinit antes de main
		boolean clinit = ca.checkCLINIT(strClass);
		// leemos clase y buscamos metodo principal
		JClass mainClass = ca.loadClass(strClass);
		JMethod jMethod = mainClass.getMethod(strMethod);
		// ------
		// mostramos bytecode si se selecciono dicha opcion
		mostrarBytecode(mainClass, jMethod);
		// -----

		// creamos jvm y set tipo de ejecucion
		// el frame se creará posteriormente
		jvm = new JVM(null, heap, ca, true);
		jvm.setTipoExec(TipoExec.IHPC);

		if (DEBUG.LOG)
			l.writeLog("\nInputs Types");
		if (DEBUG.USER)
			l.writeUser("\nInputs Types");

		// creacion del frame principal se basa en si el metodo es static
		StackFrame mainFrame;
		// maximo argumentos
		int maxArgs = jMethod.getMaxArgs() + 1;
		int desp = 0;
		if (!jMethod.isStatic()) {
			// si no es estatico, meto referencia en locals[0]
			desp = 1;
			JObject refthis = (JObject) jvm.ejecutarPreNEW(mainClass.getName());
			refthis.setSymbol(true);
			if (DEBUG.LOG)
				l.writeLog(refthis.getName() + " -> Object (referencia this)");
			if (DEBUG.USER)
				l.writeUser(refthis.getName() + " -> Object (referencia this)");
			
			for (int j = 0; j < refthis.jFields.length; j++) {
				String namepro = refthis.getName() + "."
						+ refthis.jFields[j].getName();
				newJValues(refthis.jFields[j].fieldInfo.getType(), namepro);
				refthis.jFields[j].getValue().setSymbol(true);
				refthis.jFields[j].getValue().setName(namepro);
			}
			mainFrame = new StackFrame(null, jMethod, mainClass, refthis);
		} else {
			maxArgs--;
			mainFrame = new StackFrame(null, jMethod, mainClass);
		}
		jvm.setTopFrame(mainFrame);
		// set limit
		jvm.limit = limit_size;

		//paso de parametros
		Type[] attributeTypes = jMethod.getArgumentTypes();
		JValue param = null;
		// modificamos parametros de entrada al mainFrame y al Choco
		for (int i = 0; i < maxArgs - desp; i++) {
			param = newJValues(attributeTypes[i], nextDefaultName());
			param.setSymbol(true);
			mainFrame.getLocals()[i + desp] = param;
			// si es un objeto modificamos sus fields
			if ((param instanceof JArray) && param.type.equals(Type.OBJECT)) {
				JArray jarray = (JArray) param;
				int length = jarray.getnElems();
				JObject jo;
				for (int j = 0; j < length; j++) {
					jo = (JObject) jarray.get(j);
					jo.setSymbol(true);
					jo.setName(jarray.getName() + "[" + jo.getName() + "]");
					for (int k = 0; k < jo.jFields.length; k++) {
						// no usamos valor devuelto para evitar sobreescribir el
						// valor del field si existe desde la clase (static)
						// String namepro = jarray.getName()+"["+jo.getName() +
						// "."
						// + jo.jFields[k].getName()+"]";
						String namepro = jo.getName() + "."
								+ jo.jFields[k].getName();
						newJValues(jo.jFields[k].fieldInfo.getType(), namepro);
						jo.jFields[k].getValue().setSymbol(true);
						jo.jFields[k].getValue().setName(namepro);
					}
				}

			} else if (param.type.equals(Type.OBJECT)) {
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
			JField t =mainClass.getJFields()[j];
			if(t.isStatic()){
				String namepro = mainClass.getName() + "."
						+ t.getName();
				newJValues(t.fieldInfo.getType(), namepro);
				t.getValue().setSymbol(true);
				t.getValue().setName(namepro);
			}
		}

		// compruebo si tengo que ejecutar clinit, si es asi ejecuta previo main
		if (clinit) {
			StackFrame clinitFrame = new StackFrame(mainFrame,
					mainClass.getCLINIT(), mainClass);
			jvm.setTopFrame(clinitFrame);
		}
		// ejecucion
		jvm.runBT();
		if (DEBUG.LOG)
			l.printLog();
		if (DEBUG.USER) {
			l.writeStadistics();
			l.printUser();
		}

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
		Log l = null;
		if (DEBUG.LOG || DEBUG.USER)
			l = Log.getInstance();

		if (type.equals(Type.BYTE)) {
			IntegerVariable iV = Choco.makeIntVar(s, (int) Byte.MIN_VALUE,
					(int) Byte.MAX_VALUE, Options.V_ENUM);
			JConstraintSolver.getInstance().addVariable(iV);
			if (DEBUG.LOG)
				l.writeLog(iV.toString() + " -> Byte");
			if (DEBUG.USER)
				l.writeUser(iV.toString() + " -> Byte");
			return new JByte();
		} else if (type.equals(Type.CHAR)) {
			IntegerVariable iV = Choco.makeIntVar(s, (int) Character.MIN_VALUE,
					(int) Character.MAX_VALUE, Options.V_ENUM);
			JConstraintSolver.getInstance().addVariable(iV);
			if (DEBUG.LOG)
				l.writeLog(iV.toString() + " -> Char");
			if (DEBUG.USER)
				l.writeUser(iV.toString() + " -> Char");
			return new JChar();
		} else if (type.equals(Type.DOUBLE)) {
			RealVariable rV = Choco.makeRealVar(s, Double.MIN_VALUE,
					Double.MAX_VALUE, Options.V_ENUM);
			JConstraintSolver.getInstance().addVariable(rV);
			if (DEBUG.LOG)
				l.writeLog(rV.toString() + " -> Double");
			if (DEBUG.USER)
				l.writeUser(rV.toString() + " -> Double");
			return new JDouble();
		} else if (type.equals(Type.FLOAT)) {
			RealVariable rV = Choco.makeRealVar(s, Float.MIN_VALUE,
					Float.MAX_VALUE, Options.V_ENUM);
			JConstraintSolver.getInstance().addVariable(rV);
			if (DEBUG.LOG)
				l.writeLog(rV.toString() + " -> Float");
			if (DEBUG.USER)
				l.writeUser(rV.toString() + " -> Float");
			return new JFloat();
		} else if (type.equals(Type.INT)) {
			// TODO Warning Choco Bounds demasiado grandes
			IntegerVariable iV = Choco.makeIntVar(s, -10, 10, Options.V_BOUND);
			JConstraintSolver.getInstance().addVariable(iV);
			if (DEBUG.LOG)
				l.writeLog(iV.toString() + " -> Integer");
			if (DEBUG.USER)
				l.writeUser(iV.toString() + " -> Integer");
			return new JInteger();
		} else if (type.equals(Type.LONG)) {
			// TODO Warning Choco Bounds demasiado grandes
			IntegerVariable iV = Choco.makeIntVar(s, -100, 100, Options.V_ENUM);
			JConstraintSolver.getInstance().addVariable(iV);
			if (DEBUG.LOG)
				l.writeLog(iV.toString() + " -> Long");
			if (DEBUG.USER)
				l.writeUser(iV.toString() + " -> Long");
			return new JLong();
		} else if (type.getType() == Constants.T_OBJECT) {
			if (DEBUG.LOG)
				l.writeLog(s + " -> Object");
			if (DEBUG.USER)
				l.writeUser(s + " -> Object");
			// genera el objeto a mano usando el nombre de la clase
			return jvm.ejecutarPreNEW(jvm.getNameClassFromSignature(type
					.getSignature()));
		} else if (type.equals(Type.SHORT)) {
			IntegerVariable iV = Choco.makeIntVar(s, (int) Short.MIN_VALUE,
					(int) Short.MAX_VALUE, Options.V_ENUM);
			JConstraintSolver.getInstance().addVariable(iV);
			if (DEBUG.LOG)
				l.writeLog(iV.toString() + " -> Short");
			if (DEBUG.USER)
				l.writeUser(iV.toString() + " -> Short");
			return new JShort();
		} else {
			// TODO METER ARRAY EN EL CHOCO?¿
			ArrayType atype = (ArrayType) type;
			if (DEBUG.LOG)
				l.writeLog(s + " -> Array of " + atype.getElementType());
			if (DEBUG.USER)
				l.writeUser(s + " -> Array of " + atype.getElementType());
			// creamos array de tamaño 5 por defecto pss
			if (atype.getElementType().getType() == Constants.T_OBJECT) {
				return jvm.ejecutarPreNEWOBJECTARRAY(jvm
						.getNameClassFromSignature(atype.getBasicType()
								.getSignature()), array_size);
			}

			else {
				return jvm.ejecutarPreNEWARRAY(
						atype.getElementType().getType(), array_size);
			}
		}

	}
}
