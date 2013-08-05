package com.jsyx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.bcel.log.Log;
import com.jsyx.test.DEBUG;

import choco.cp.model.CPModel;
import choco.cp.solver.CPSolver;
import choco.kernel.model.Model;
import choco.kernel.model.constraints.Constraint;
import choco.kernel.model.variables.Variable;
import choco.kernel.model.variables.integer.IntegerVariable;
import choco.kernel.model.variables.real.RealVariable;
import choco.kernel.solver.Solver;
import choco.kernel.solver.constraints.SConstraint;
import choco.kernel.solver.variables.integer.IntDomainVar;
import choco.kernel.solver.variables.real.RealVar;

public class JConstraintSolver {

	private Model model;
	private Solver solver;

	private ArrayList<String> nameVars;

	private HashMap<String, Variable> vars;

	private static JConstraintSolver cs = new JConstraintSolver();

	private JConstraintSolver() {
		model = new CPModel();
		solver = new CPSolver();
		vars = new HashMap<String, Variable>();
		nameVars = new ArrayList<String>();
	}

	public static JConstraintSolver getInstance() {
		return cs;
	}

	public Model getModel() {
		return model;
	}

	public Solver getSolver() {
		return solver;
	}

	public void addVariable(Variable v) {
		model.addVariable(v);
		vars.put(v.getName(), v);
		nameVars.add(v.getName());
	}

	public void addConstraint(Constraint c) {
		model.addConstraint(c);
	}

	public void printSolution() {
		Log l = null;
		if (DEBUG.LOG) {
			l = Log.getInstance();
			l.writeNewPathLog();
		}
		if (DEBUG.USER) 
			l = Log.getInstance();
		if (solver.isFeasible()) {
			if (DEBUG.LOG)
				l.writeLog("Solution -> True");
			if (DEBUG.USER) {
				l.writeNewPathUser();
				l.writeUser("Solution:");
			}
			if(l!=null)
			l.incSuccess();
			

			String s, c;
			Variable v;
			StringBuffer inputsBuffer = new StringBuffer();
			StringBuffer copiesBuffer = new StringBuffer();
			StringBuffer tempsBuffer = new StringBuffer();

			for (int i = 0; i < nameVars.size(); i++) {

				// TODO solo muestra un valor el muy puto
				s = nameVars.get(i);
				v = (Variable) vars.get(s);
				c = v.getName();
				if (v instanceof IntegerVariable) {
					IntDomainVar iD = solver.getVar(v);

					if (iD == null) {
						if (c.length() == 1) {
							inputsBuffer.append(c + " = ?" + "\n");
						} else if (c.length() > 3) {
							tempsBuffer.append(c + " = ?" + "\n");
						} else {
							copiesBuffer.append(c + " = ?" + "\n");
						}
					} else {
						if ( l != null){
							Log.getInstance();
							if (c.equals(l.getOutput())){
								l.saveOutput(iD.toString()
									+ "\n");
							}
						}
						if (c.length() == 1) {
							inputsBuffer.append(c + " = " + iD.toString()
									+ "\n");
						} else if (c.length() > 3) {
							tempsBuffer
									.append(c + " = " + iD.toString() + "\n");
						} else {
							copiesBuffer.append(c + " = " + iD.toString()
									+ "\n");
						}
					}
				} else if (v instanceof RealVariable) {
					RealVar rV = solver.getVar(v);

					if (rV == null) {
						if (c.length() == 1) {
							inputsBuffer.append(c + " = ?" + "\n");
						} else if (c.length() > 3) {
							tempsBuffer.append(c + " = ?" + "\n");
						} else {
							copiesBuffer.append(c + " = ?" + "\n");
						}
					} else {
						if ( l != null){
							Log.getInstance();
							if (c.equals(l.getOutput())){
								l.saveOutput(rV.toString()
									+ "\n");
							}
						}
						if (c.length() == 1) {
							inputsBuffer.append(c + " = " + rV.toString()
									+ "\n");
						} else if (c.length() > 3) {
							tempsBuffer
									.append(c + " = " + rV.toString() + "\n");
						} else {
							copiesBuffer.append(c + " = " + rV.toString()
									+ "\n");
						}
					}
				}

			} // look for the following solution

			if (inputsBuffer.length() == 0) {
				inputsBuffer.append("No inputs\n");
			}
			if (tempsBuffer.length() == 0) {
				tempsBuffer.append("No temporals variables\n");
			}
			if (copiesBuffer.length() == 0) {
				copiesBuffer.append("No copies\n");
			}

			if(DEBUG.LOG)l.writeLog("-Inputs\n" + inputsBuffer + "-Copies\n" + copiesBuffer
					+ "-Temps\n" + tempsBuffer);
			if(DEBUG.USER)l.writeUser("-Inputs\n" + inputsBuffer);

		} else {
			if(DEBUG.LOG)l.writeLog("Solution -> False\nWarning! No solution in this path");
			if(l!=null)l.incFails();
		}
		if(DEBUG.ON)
		System.out.println();
		@SuppressWarnings("rawtypes")
		Iterator<SConstraint> cIt = solver.getConstraintIterator();

		@SuppressWarnings("rawtypes")
		SConstraint sC = null;

		if(DEBUG.LOG)l.writeLog("-Contraints");
		while (cIt.hasNext()) {
			sC = cIt.next();
			if(DEBUG.LOG)l.writeLog(sC.toString());
		}

		if(DEBUG.LOG)l.writeOutputLog();
		if (solver.isFeasible()){
			if(DEBUG.USER)l.writeOutputUser();
		}

	}

	public Variable getVar(String name) {
		return vars.get(name);
	}

	public void removeConstraint(Constraint c) {
		model.removeConstraint(c);
	}

	public void clearSolver() {
		solver.clear();
	}

}
