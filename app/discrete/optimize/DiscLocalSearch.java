package app.discrete.optimize;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Random;

import experiment.AnnealingTest;

import optimize.function.CostFunction;
import optimize.function.RecordFunction;
import optimize.ls.LocalSearch;
import app.discrete.function.Ising2D;
import app.discrete.util.DVector;

public class DiscLocalSearch extends LocalSearch<DVector> {

	private DVector currentS = null;
	private double value = Double.NaN;

	private Random rand = new Random();

	public DiscLocalSearch(CostFunction<DVector> cf) {
		super(cf);
	}

	@Override
	public void optimize() {
		if (currentS == null) init();
		boolean changeFlag = false;
		for (int d = 0; d < cf.dim(); d++) {
			int[] ne = currentS.copy();
			ne[d] = 1 - ne[d];
			DVector next = new DVector(ne);
			double nvalue = cf.eval(next);

			if (nvalue < value) {
				currentS = next;
				value = nvalue;
				changeFlag = true;
			}
		}
		if (changeFlag == false) this.isConverged = true;
	}

	private void init() {
		int[] e = new int[cf.dim()];
		for (int i = 0; i < e.length; i++) {
			e[i] = rand.nextInt(2);
		}
		currentS = new DVector(e);
		value = cf.eval(currentS);
	}

	public static CostFunction<DVector> base = new Ising2D(20, 20);

	public static void main(String args[]) {
		try {
			PrintStream ps = new PrintStream(new FileOutputStream(
					"localsearch.dat"));
			RecordFunction<DVector> cf = new RecordFunction<DVector>(base,
					System.out);

			for (int i = 0; i < 1000; i++) {
				DiscLocalSearch dls = new DiscLocalSearch(cf);

				while (true) {
					dls.optimize();
					if (dls.isConverged) break;
				}
			}

			System.out.println(cf.getCount()+" "+cf.getBestValue());
			ps.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

}
