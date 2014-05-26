package experiment.his;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import app.continuous.function.Rastrigin;
import app.continuous.optimize.GaussianTruncationEDA;
import app.continuous.optimize.GaussianTruncationHIS;
import app.continuous.util.CVector;

import optimize.Annealing;
import optimize.Optimizer;
import optimize.function.CostFunction;
import optimize.function.RecordFunction;
import optimize.optimizer.ConvergenceException;

import experiment.util.Result;

public class CTest {
	public static Result test(Optimizer<CVector> opt,
			RecordFunction<CVector> cf) {
		for (long i = 0;; i++) {
			try {
				opt.optimize();
			} catch (ConvergenceException e) {
				break;
			}
			if (cf.getBestValue() < 1e-10)
				break;
			if (cf.getCount() > 1e6) {
				break;
			}
			if (true && i % 1 == 0) {
				System.out.println("----------------" + i + "(" + cf.getCount()
						+ ")" + "----------------");
				System.out.println(opt);
				System.out.println(cf.getBestValue());
			}
		}
		Result r = new Result();
		r.best = Math.floor(cf.getBestValue());
		r.count = cf.getCount();
		return r;
	}
	public static void test1(int sampleNum, int layer, int count) {
		System.out.print(sampleNum + "," + layer + ",");
		Result[] result = new Result[count];
		for (int i = 0; i < count; i++) {
			result[i] = test1(sampleNum, layer);
		}
		Result avg = Result.average(result);
		Result std = Result.std(result);
		System.out.println(avg.best + "," + std.best + "," + avg.count + ","
				+ std.count);
	}

	public static Result test1(int sampleNum, int layer) {

		try {
			PrintStream ps = new PrintStream(new FileOutputStream(
					"discpoptruncanneal.dat"));

			// CostFunction<CVector> base = new Rosenbrock(10);
			CostFunction<CVector> base = new Rastrigin(20);

			RecordFunction<CVector> cf = new RecordFunction<CVector>(base, ps);

			Optimizer<CVector> ann = new GaussianTruncationHIS(sampleNum,
					layer, cf);
			ps.close();
			return test(ann, cf);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(0);
			return null;
		}
	}

	
	public static Result test(Annealing<CVector> anneal,
			RecordFunction<CVector> cf) {
		for (long i = 0;; i++) {
			try {
				anneal.optimize();
			} catch (ConvergenceException e) {
				break;
			}
			if (anneal.getVariance() < 1e-20 || cf.getBestValue() < 1e-10)
				break;
			if (cf.getCount() > 1e7) {
				break;
			}
			if (true && i % 1 == 0) {
				System.out.println("----------------" + i + "(" + cf.getCount()
						+ ")" + "----------------");
				System.out.println(anneal);
				System.out.println(cf.getBestValue());
			}
		}
		Result r = new Result();
		r.best = Math.floor(cf.getBestValue());
		r.count = cf.getCount();
		return r;
	}

	public static void test2(int sampleNum, double cutoff, int count) {
		System.out.print(sampleNum + "," + cutoff + ",");
		Result[] result = new Result[count];
		for (int i = 0; i < count; i++) {
			result[i] = test2(sampleNum, cutoff);
		}
		Result avg = Result.average(result);
		Result std = Result.std(result);
		System.out.println(avg.best + "," + std.best + "," + avg.count + ","
				+ std.count);
	}

	public static Result test2(int sampleNum, double cutoff) {

		try {
			PrintStream ps = new PrintStream(new FileOutputStream(
					"test1.dat"));

			// CostFunction<CVector> base = new Rosenbrock(10);
			CostFunction<CVector> base = new Rastrigin(10);

			RecordFunction<CVector> cf = new RecordFunction<CVector>(base, ps);

			Annealing<CVector> ann = new GaussianTruncationEDA(sampleNum,
					cutoff, cf);
			ps.close();
			return test(ann, cf);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(0);
			return null;
		}
	}

	public static void main(String args[]) {
		test1(100,15,1);
//		test2(3000, 0.5, 1);
	}
}
