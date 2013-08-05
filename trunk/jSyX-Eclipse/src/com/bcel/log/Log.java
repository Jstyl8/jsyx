package com.bcel.log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;

import com.jsyx.test.DEBUG;
import com.jsyx.tipos.JValue;

public class Log {
	private File log, trace,user;
	private int nPaths = 0;
	private String output;
	
	private int nFails = 0,nSuccess = 0;
	

	private static Log l = new Log();

	public static Log getInstance() {
		return l;
	}
	
	public String getOutput(){
		return output;
	}
	
	public void incFails(){
		nFails++;
	}
	
	public void incSuccess(){
		nSuccess++;
	}

	public Log() {
		if (DEBUG.LOG){
			try {
				log = new File("./Log/log.txt");
				log.getParentFile().mkdirs();
	
				FileWriter fichero = new FileWriter(log, false);
				PrintWriter pw = new PrintWriter(fichero);
	
				pw.println("Log" + "  " + Calendar.getInstance().getTime() + "\n");
				if (null != fichero)
					fichero.close();
			} catch (Exception e) {
					e.printStackTrace();
			}
		}
		if (DEBUG.ON){
			try {
				trace = new File("./Log/trace.txt");
				trace.getParentFile().mkdirs();
	
				FileWriter fichero = new FileWriter(trace, false);
				PrintWriter pw = new PrintWriter(fichero);
	
				pw.println("Trace" + "  " + Calendar.getInstance().getTime());
				if (null != fichero)
					fichero.close();
	
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (DEBUG.USER){
			try {
				user = new File("./Log/user.txt");
				user.getParentFile().mkdirs();
	
				FileWriter fichero = new FileWriter(user, false);
				PrintWriter pw = new PrintWriter(fichero);
	
				pw.println("Test" + "  " + Calendar.getInstance().getTime());
				if (null != fichero)
					fichero.close();
	
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void writeLog(String s) {

		FileWriter fichero = null;
		PrintWriter pw = null;
		try {

			fichero = new FileWriter(log, true);
			pw = new PrintWriter(fichero);

			pw.println(s);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (null != fichero)
					fichero.close();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
	}

	public void writeTrace(String s) {
		FileWriter fichero = null;
		PrintWriter pw = null;
		try {
			fichero = new FileWriter(trace, true);
			pw = new PrintWriter(fichero);

			pw.println(s);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (null != fichero)
					fichero.close();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
	}
	
	public void writeUser(String s) {

		FileWriter fichero = null;
		PrintWriter pw = null;
		try {

			fichero = new FileWriter(user, true);
			pw = new PrintWriter(fichero);

			pw.println(s);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (null != fichero)
					fichero.close();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
	}

	public void printLog() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(log));
		String line;
		if (DEBUG.LOG)
			while ((line = br.readLine()) != null) {
				System.out.println(line);
			}
	}

	public void printTrace() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(trace));
		String line;
		if (DEBUG.ON)
			while ((line = br.readLine()) != null) {
				System.out.println(line);
			}

	}

	public void printUser() throws IOException {
		
		BufferedReader br = new BufferedReader(new FileReader(user));
		String line;
		if (DEBUG.USER)
			while ((line = br.readLine()) != null) {
				System.out.println(line);
			}

	}

	
	public void writeNewPathLog() {
		writeLog("\n+Path " + nPaths);
		nPaths++;
	}
	
	public void writeNewPathUser() {
		writeUser("\n+Path " + nPaths);
		nPaths++;
	}

	public void saveOutput(String s) {
		output = s;
	}

	public void writeOutputLog() {
		writeLog("-Output\n" + output);
	}
	
	public void writeOutputUser() {
		writeUser("-Output\n" + output);
	}

	public void writeStadistics() {
		int total = nFails + nSuccess;
		int rate = nSuccess*100 /total;
		writeUser("Stadistics:\n" + "Total branches: " + total 
				+ "\nSuccess branches: " + nSuccess + "\nFail branches: " + nFails
				+ "\nPercent success branches:" + rate + "%");
		
	}

}
