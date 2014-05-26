package app.discrete.optimize.old;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import app.discrete.function.Ising2D;
import app.discrete.function.OneMax;
import app.discrete.sampler.UMDADiscreteSampler;
import app.discrete.util.DVector;

import optimize.Optimizer;
import optimize.function.CostFunction;
import optimize.function.RecordFunction;
import optimize.optimizer.ConvergenceException;
import optimize.population.old.PopTruncationAnnealing;

import statistics.sampler.Sampler;

public class DiscPopTruncationAnnealing extends PopTruncationAnnealing<DVector> {

	public DiscPopTruncationAnnealing(CostFunction cf, int num_population,
			int sample_num, double cutoff) {
		super(cf, num_population, sample_num, cutoff);
	}

	@Override
	protected Sampler<DVector> getInitialSampler() {
		return new UMDADiscreteSampler(cf.dim(), 1);
	}

	public static int dim = 400;

	public static void main(String args[]) {
		for (int c = 0; c < 1; c++) {
			System.out.println("c=" + c);
			try {
				PrintStream ps = new PrintStream(new FileOutputStream(
						"discpoptruncanneal.dat"));
				// CostFunction<DVector> base = new OneMax(dim);
				CostFunction<DVector> base = new Ising2D(20, 20);
				CostFunction<DVector> cf = new RecordFunction<DVector>(base, ps);
				Optimizer<DVector> opt = new DiscPopTruncationAnnealing(cf,
						100, 10, 0.01);
				for (long i = 0; i < 1000; i++) {
					try {
						opt.optimize();
					} catch (ConvergenceException e) {
						break;
					}
					if (true && i % 1 == 0) {
						System.out.println("----------------" + i
								+ "----------------");
						System.out.println(opt);
					}
				}
				ps.close();
				System.out.println(opt);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		System.out.println("end pop");
		System.exit(0);
	}

}
