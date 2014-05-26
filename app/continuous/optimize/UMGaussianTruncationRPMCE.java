package app.continuous.optimize;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import optimize.function.CostFunction;
import optimize.function.RecordFunction;
import optimize.population.TruncationRPM;
import statistics.sampler.Sampler;
import app.continuous.function.Rosenbrock;
import app.continuous.sampler.UMGaussianSampler;
import app.continuous.util.CVector;

public class UMGaussianTruncationRPMCE extends
		TruncationRPM<CVector> {

	public UMGaussianTruncationRPMCE(CostFunction<CVector> cf,
			int num_population, int num_sample, double cutoff) {
		super(cf, num_population, num_sample, cutoff);
	}

	@Override
	protected Sampler<CVector> getInitialSampler() {
		return new UMGaussianSampler(cf.dim(), 0, 10, 1);
	}

	public static int dim = 10;

	public static void main(String args[]) {
		int param = 0;
		for (int p = 0; p < 1; p++) {
			switch (p) {
			case 0:
				param = 50;
				break;
			case 1:
				param = 100;
				break;
			case 2:
				param = 200;
				break;
			}
			System.out.println("#" + param);
			for (int c = 0; c < 1; c++) {
				try {
					PrintStream ps =
							new PrintStream(new FileOutputStream(
									"test"+p+".dat"));

					// RecordFunction<CVector> cf = new RecordFunction<CVector>(
					// new Rastrigin(dim), ps);
					 RecordFunction<CVector> cf =
					 new RecordFunction<CVector>(
					 new Rosenbrock(dim), ps);

					UMGaussianTruncationRPMCE gtr =
							new UMGaussianTruncationRPMCE(cf, 10000,
									param, 0.1);

					for (long i = 0;; i++) {
						gtr.optimize();
						if (false && i % 10 == 0) {
							System.out.println("----------------" + i
									+ "----------------");
							System.out.println(gtr);
						}
						if (gtr.getVariance() < 1e-10) break;
						if (cf.getBestValue() < 1e-4) break;
					}
					ps.close();
					System.out.println(cf.getCount() + " "
							+ cf.getBestValue());
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
