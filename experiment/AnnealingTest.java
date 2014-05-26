package experiment;

import optimize.Annealing;
import optimize.function.RecordFunction;
import optimize.optimizer.ConvergenceException;
import app.continuous.util.CVector;
import app.discrete.util.DVector;
import experiment.util.Result;

public class AnnealingTest {

	private static final double minVar = 1e-10;
//	private static final double minVar = 1e-15;
	private static final long maxEvaluation = (long) 1e7;

	public static boolean print = false;

	public static  Result test(Annealing<DVector> anneal,
			RecordFunction<DVector> cf) {
		if (print) System.out.println(anneal.getClass());
		long start = System.currentTimeMillis();
		long i = 1;
		for (;; i++) {
			try {
				anneal.optimize();
			} catch (ConvergenceException e) {
				System.out.println("conv except");
				break;
			}
			if (cf.getBestValue() <= cf.getMinimum()) {
				if (print) System.out.println("best");
				break;
			}
			if (anneal.getVariance() < minVar) {
				if (print) System.out.println("minvar" + anneal.getVariance());
				break;
			}
			if (maxEvaluation < cf.getCount()) {
				if (print) System.out.println("count");
				break;
			}
			if (print && i % 1 == 0) {
				System.out.println("----------------" + i + "(" + cf.getCount()
						+ ")" + "----------------");
				System.out.println(anneal);
				System.out.println(cf.getBestValue());
			}
		}
		if (print) {
			System.out.println("----------------" + i + "(" + cf.getCount()
					+ ")" + "----------------");
			System.out.println(anneal);
			System.out.println(cf.getBestValue());
		}
		long end = System.currentTimeMillis();
		Result r = new Result();
		r.best = Math.round(cf.getBestValue());
		r.count = cf.getCount();
		r.time = end - start;
		if (print) {
			System.out.println(r);
		}
		return r;
	}
	
	public static  Result test_c(Annealing<CVector> anneal,
			RecordFunction<CVector> cf) {
		if (print) System.out.println(anneal.getClass());
		long start = System.currentTimeMillis();
		long i = 1;
		for (;; i++) {
			try {
				anneal.optimize();
			} catch (ConvergenceException e) {
				break;
			}
			if (cf.getBestValue() <= cf.getMinimum()) {
				if (print) System.out.println("best");
				break;
			}
			if (anneal.getVariance() < minVar) {
				if (print) System.out.println("minvar" + anneal.getVariance());
				break;
			}
			if (maxEvaluation < cf.getCount()) {
				if (print) System.out.println("count");
				break;
			}
			if (print && i % 1 == 0) {
				System.out.println("----------------" + i + "(" + cf.getCount()
						+ ")" + "----------------");
				System.out.println(anneal);
				System.out.println(cf.getBestValue());
			}
		}
		if (print) {
			System.out.println("----------------" + i + "(" + cf.getCount()
					+ ")" + "----------------");
			System.out.println(anneal);
			System.out.println(cf.getBestValue());
		}
		long end = System.currentTimeMillis();
		Result r = new Result();
		r.best = cf.getBestValue();
		if (maxEvaluation < cf.getCount()) r.best=Double.POSITIVE_INFINITY;
		if(r.best<=cf.getMinimum())r.success=1;
		r.count = cf.getCount();
		r.time = end - start;
		if (print) {
			System.out.println(r);
		}
		return r;
	}
	
}
