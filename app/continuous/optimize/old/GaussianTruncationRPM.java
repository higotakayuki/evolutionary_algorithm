package app.continuous.optimize.old;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;

import optimize.function.CostFunction;
import optimize.function.RecordFunction;
import optimize.population.old.TruncationRPM;
import statistics.sampler.Sampler;
import app.continuous.function.Rosenbrock;
import app.continuous.sampler.GaussianSampler;
import app.continuous.sampler.UniformSampler;
import app.continuous.util.CVector;
import calc.stat.Gaussian;

public class GaussianTruncationRPM extends TruncationRPM<CVector> {

	public GaussianTruncationRPM(CostFunction<CVector> cf, int num_population,
			int num_sample, double cutoff) {
		super(cf, num_population, num_sample, cutoff);
	}

	@Override
	protected Sampler<CVector> getInitialSampler() {
		List<CVector> list = new UniformSampler(cf.dim(), -10, 10)
				.sampling(10000);
		Gaussian initGauss = GaussianSampler.calcGaussian(list);
		return new GaussianSampler(cf.dim(), initGauss,1);
	}

	public static int dim = 10;

	public static void main(String args[]) {
		for (int c = 0; c < 10; c++) {
			try {
				PrintStream ps = new PrintStream(new FileOutputStream(
						"test.dat"));

//				RecordFunction<CVector> cf = new RecordFunction<CVector>(
//						new Rastrigin(dim), ps);
				 RecordFunction<CVector> cf = new RecordFunction<CVector>(
				 new Rosenbrock(dim), ps);

				GaussianTruncationRPM gtr = new GaussianTruncationRPM(cf,
						100,100, 0.01);

				for (long i = 0;; i++) {
					gtr.optimize();
					if (false && i % 10 == 0) {
						System.out.println("----------------" + i
								+ "----------------");
						System.out.println(gtr);
					}
					if (gtr.getVariance() < 1e-10) break;
					if (cf.getBestValue()<1e-4)break;
				}
				ps.close();
				System.out.println(cf.getCount()+" "+cf.getBestValue());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

}
