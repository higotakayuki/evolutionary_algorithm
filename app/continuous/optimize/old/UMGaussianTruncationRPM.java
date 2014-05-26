package app.continuous.optimize.old;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import optimize.function.CostFunction;
import optimize.function.RecordFunction;
import optimize.population.old.TruncationRPM;
import statistics.sampler.Sampler;
import app.continuous.function.Rosenbrock;
import app.continuous.sampler.UMGaussianSampler;
import app.continuous.util.CVector;

public class UMGaussianTruncationRPM extends TruncationRPM<CVector> {

	public UMGaussianTruncationRPM(CostFunction<CVector> cf, int num_population,
			int num_sample, double cutoff) {
		super(cf, num_population, num_sample, cutoff);
	}

	@Override
	protected Sampler<CVector> getInitialSampler() {
		return new UMGaussianSampler(cf.dim(), -10, 10, 1);
	}

	public static int dim = 10;

	public static void main(String args[]) {
		for (int c = 0; c < 10; c++) {
			try {
				PrintStream ps = new PrintStream(new FileOutputStream(
						"test.dat"));

//				RecordFunction<CVector> cf = new RecordFunction<CVector>(
//						new Rastrigin(dim), ps);
//				 RecordFunction<CVector> cf = new RecordFunction<CVector>(
//				 new Rosenbrock(dim), ps);
				RecordFunction<CVector> cf = new RecordFunction<CVector>(
						 new Rosenbrock(dim), null);

				UMGaussianTruncationRPM gtr = new UMGaussianTruncationRPM(cf,
						10000,100, 0.1);

				for (long i = 0;; i++) {
					gtr.optimize();
					if (false && i % 10 == 0) {
						System.out.println("----------------" + i
								+ "----------------");
						System.out.println(gtr);
					}
					if (gtr.getVariance() < 1e-10) break;
				}
				ps.close();
				System.out.println(cf.getCount()+" "+cf.getBestValue());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

}
