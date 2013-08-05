package com.jsyx.test;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.jsyx.JClass;
import com.jsyx.JMethod;

/**
 * Clase para el paso de parametros al programa
 * 
 * @author JZ
 * @version 1.0 20/05/2013
 */
public class JCmdLine {

	private CommandLineParser parser;
	private Options options;
	private CommandLine line;
	private String[] args;
	private HelpFormatter formatter;

	public JCmdLine(String[] args) {
		// create the command line parser
		parser = new BasicParser();

		// create the Options
		options = new Options();
		options.addOption("b", "bytecode", false,
				"imprime el bytecode del .class especificado con -c o del metodo con -m");
		options.addOption("h", "help", false, "imprime este mensaje de ayuda");
		options.addOption("v", "version", false,
				"imprime la version del programa");
		options.addOption("s", "symbolic", false,
				"ejecucion simbolica (por defecto)");
		options.addOption("u", "user", false,
				"muestra informacion al usuario sobre los caminos de exito (no por defecto)");
		options.addOption("l", "log", false,
				"generacion de archivo de log y traza para simbolica con todos los caminos en el arbol de ejcucion (no por defecto)");
		options.addOption("d", "debug", false,
				"muestra informacion de ejecucion (no por defecto)");
		options.addOption("n", "normal", false, "ejecucion no simbolica");
		options.addOption("o", "opcode", false,
				"ejecucion no simbolica con opcode");
		options.addOption("i", "ihpc", false,
				"ejecucion no simbolica con instruction handler (por defecto)");
		options.addOption(OptionBuilder
				.withLongOpt("heap-size")
				.withDescription(
						"define tamanyo de heap (por defecto 100)")
				.hasArg().withArgName("SIZE").create());
		options.addOption(OptionBuilder
				.withLongOpt("limit-size")
				.withDescription(
						"define limite de profundidad de ejecucion (por defecto 5)")
				.hasArg().withArgName("SIZE").create());
		options.addOption(OptionBuilder
				.withLongOpt("array-size")
				.withDescription(
						"define tamanyo de arrays para ejecucion simbolica (por defecto 5)")
				.hasArg().withArgName("SIZE").create());
		options.addOption(OptionBuilder.withArgName("classname").hasArg()
				.withDescription("nombre completo de clase a usar (*)")
				.create("c"));
		options.addOption(OptionBuilder
				.withArgName("methodname")
				.hasArg()
				.withDescription(
						"metodo a ejecutar dentro de clase (* con simbolica)")
				.create("m"));
		formatter = new HelpFormatter();
		this.args = args;
	}

	public void parse() throws ParseException {
		line = parser.parse(options, args);
	}

	public boolean has(String option) {
		return line.hasOption(option) ? true : false;
	}

	public String get(String option) {
		return line.getOptionValue(option);
	}

	public int getAsInt(String option) throws NumberFormatException {
		return Integer.parseInt(get(option));
	}

	public String getIfHas(String option) {
		return has(option) ? get(option) : null;
	}

	/**
	 * Imprime la ayuda generada automaticamente por la libreria
	 * 
	 * @param usage
	 *            para mostrar ejemplo de uso o no
	 */
	public void printHelp(boolean usage) {
		formatter.printHelp("jsyx", options, usage);
		System.out.println(" * indica parametro obligatorio");
	}

	public void printVersion() {
		System.out
				.println("jSyX - Java Symbolic Execution Machine [Version 1.0]");
	}

	/**
	 * @return the args
	 */
	public String[] getArgs() {
		return args;
	}

	/**
	 * @param args
	 *            the args to set
	 */
	public void setArgs(String[] args) {
		this.args = args;
	}

	/**
	 * Muestra el bytecode de la clase
	 * 
	 * @param jclass
	 *            a mostrar
	 * @param jmethod
	 */
	public void printBytecode(JClass jclass, JMethod jmethod) {
		if (jmethod == null)
			System.out.println(jclass.getBytecode());
		else
			System.out.println(jmethod.getBytecode());
	}
}
