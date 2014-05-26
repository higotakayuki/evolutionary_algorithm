package experiment.his;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import app.discrete.function.Ising1D;
import app.discrete.function.Ising2D;
import app.discrete.function.OneMax;
import app.discrete.optimize.UMDATruncationEDA;
import app.discrete.optimize.old.DiscPopTruncationAnnealing2;
import app.discrete.optimize.old.UMDATruncationCE;
import app.discrete.optimize.old.UMDATruncationCEWithERS;
import app.discrete.util.DVector;

import optimize.Annealing;
import optimize.function.CostFunction;
import optimize.function.RecordFunction;
import optimize.optimizer.ConvergenceException;

import experiment.util.Result;

public class ForAnnealing {

	public static Result anntest(Annealing<DVector> anneal,
			RecordFunction<DVector> cf) {
		for (long i = 0;; i++) {
			try {
				anneal.optimize();
			} catch (ConvergenceException e) {
				break;
			}
			if (cf.getBestValue() <= cf.getMinimum()) break;
			if (anneal.getVariance() < 1e-20) break;
			if (cf.getCount() > 2.9e6) {
				Result r = new Result();
				r.best = Math.floor(cf.getBestValue());
				r.count = cf.getCount();
				return r;
			}
			if (false && i % 1 == 0) {
				System.out.println("----------------" + i + "(" + cf.getCount()
						+ ")" + "----------------");
				System.out.println(anneal);
				System.out.println(cf.getBestValue());
			}
		}
		Result r = new Result();
		r.best = Math.round(cf.getBestValue());
		r.count = cf.getCount();
		return r;
	}

	public static void testEDA(int sampleNum, double cutoff, int count) {
		System.out.print(sampleNum + "," + cutoff + ",");
		Result[] result = new Result[count];
		for (int i = 0; i < count; i++) {
			result[i] = testEDA(sampleNum, cutoff);
			//System.out.println(result[i]);
		}
		Result avg = Result.average(result);
		Result std = Result.std(result);
		System.out.println(avg.best + "," + std.best + "," + avg.count + ","
				+ std.count);
	}

	public static Result testEDA(int sampleNum, double cutoff) {

		// try {
		// PrintStream ps = new PrintStream(new FileOutputStream(
		// "discpoptruncanneal.dat"));

//		CostFunction<DVector> base = new OneMax(400);
//		CostFunction<DVector> base = new Ising1D(400);
		 CostFunction<DVector> base = new Ising2D(20, 20,0.95);

		RecordFunction<DVector> cf = new RecordFunction<DVector>(base);

		UMDATruncationEDA pop = new UMDATruncationEDA(sampleNum,
				cutoff, cf);
		Result res = anntest(pop, cf);
		// ps.close();
		return res;
		// } catch (FileNotFoundException e) {
		// e.printStackTrace();
		// System.exit(0);
		// return null;
		// }

	}

	public static void test3(int sampleNum, double cutoff, int count) {
		System.out.print(sampleNum + "," + cutoff + ",");
		Result[] result = new Result[count];
		for (int i = 0; i < count; i++) {
			result[i] = test3(sampleNum, cutoff);
		}
		Result avg = Result.average(result);
		Result var = Result.std(result);
		System.out.println(avg.best + "," + var.best + "," + avg.count + ","
				+ var.count);
	}

	public static Result test3(int sampleNum, double cutoff) {
		// try {
		// cost function
		// PrintStream ps = new PrintStream(new FileOutputStream("dtest" +
		// ".dat"));

//		CostFunction<DVector> base = new OneMax(400);
//		CostFunction<DVector> base = new Ising1D(400);
		CostFunction<DVector> base = new Ising2D(20, 20);
		RecordFunction<DVector> cf = new RecordFunction<DVector>(base);

//		UMDACETruncationAnnealing pop = new UMDACETruncationAnnealing(
//				sampleNum, cutoff, cf);
		UMDATruncationCE pop = new UMDATruncationCE(
				sampleNum, cutoff, cf);
		// general procedure
		// long r = 0;
		return anntest(pop, cf);
		// while (true) {
		// pop.optimize();
		// if (pop.getVariance() < 1e-10) break;
		// if (cf.getCount() > 2000000) break;
		// }
		// ps.close();
		// System.out.println(cf.getBestValue() + " , " + cf.getCount());
		// } catch (FileNotFoundException e) {
		// e.printStackTrace();
		// }

	}

	public static void main(String args[]) {
		int c = 10;
		if (false) {
			// double cut = 0;
			// for (int i = 0; i < 4; i++) {
			// if (i == 0) cut = 0.01;
			// if (i == 1) cut = 0.05;
			// if (i == 2) cut = 0.1;
			// if (i == 3) cut = 0.2;
			// test1(50, 10, cut, c);
			// test1(50, 50, cut, c);
			// test1(100, 10, cut, c);
			// test1(100, 50, cut, c);
			// test1(100, 100, cut, c);
			// test1(200, 50, cut, c);
			// test1(200, 100, cut, c);
			// test1(200, 200, cut, c);
			// }
		} else if (true) {
			double cut = 0;
			for (int i = 1; i < 3; i++) {
//				if (i == 0) cut = 0.1;
				if (i == 1) cut = 0.3;
				if (i == 2) cut = 0.5;
				// if (i == 3) cut = 0.7;

				testEDA(100, cut, c);
				testEDA(500, cut, c);
				testEDA(1000, cut, c);
				testEDA(3000, cut, c);
				testEDA(6000, cut, c);
			}
		} else if (false) {
			double cut = 0;
			for (int i = 0; i < 3; i++) {
				if (i == 0) cut = 0.3;
				if (i == 1) cut = 0.5;
				if (i == 2) cut = 0.7;
				test3(100, cut, c);
				test3(500, cut, c);
				test3(1000, cut, c);
				test3(3000, cut, c);
				test3(6000, cut, c);
			}
		}
	}

	public static void test1(int population, int samples, double cutoff,
			int count) {
		System.out.print(population + "," + samples + "," + cutoff + ",");
		Result[] result = new Result[count];
		for (int i = 0; i < count; i++) {
			result[i] = test1(population, samples, cutoff);
		}
		Result avg = Result.average(result);
		Result var = Result.std(result);
		System.out.println(avg.best + "," + var.best + "," + avg.count + ","
				+ var.count);
	}

	public static Result test1(int population, int samples, double cutoff) {
		try {
			PrintStream ps = new PrintStream(new FileOutputStream(
					"discpoptruncanneal.dat"));
			// CostFunction<DVector> base = new OneMax(dim);
			CostFunction<DVector> base = new Ising2D(20, 20);
			RecordFunction<DVector> cf = new RecordFunction<DVector>(base, ps);
			DiscPopTruncationAnnealing2.RESAMPLING = true;
			DiscPopTruncationAnnealing2 opt = new DiscPopTruncationAnnealing2(
					cf, population, samples, cutoff);
			ps.close();
			return anntest(opt, cf);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(0);
			return null;
		}
	}
}
