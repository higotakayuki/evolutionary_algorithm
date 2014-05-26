package app.continuous.optimize;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;

import optimize.function.CostFunction;
import optimize.function.RecordFunction;
import optimize.population.old.BoltzRPM;
import statistics.sampler.Sampler;
import app.continuous.function.Rastrigin;
import app.continuous.sampler.GaussianSampler;
import app.continuous.sampler.UniformSampler;
import app.continuous.util.CVector;
import calc.stat.Gaussian;

public class GaussianBoltzRPM extends BoltzRPM<CVector> {

	public GaussianBoltzRPM(CostFunction<CVector> cf, int num_population,
			int num_sample, double cutoff) {
		super(cf, num_population, num_sample, cutoff);
	}

	@Override
	protected Sampler<CVector> getInitialSampler() {
		List<CVector> list = new UniformSampler(cf.dim(), 0, 10)
				.sampling(1000);
		Gaussian initGauss = GaussianSampler.calcGaussian(list);
		return new GaussianSampler(cf.dim(), initGauss, 1);
	}

	public static int dim = 10;

	public static int population = 200;

	public static int samples = 200;

	public static double cutoff = 0.1;

	public static void main(String args[]) {
		System.out.println("#" + population + "," + samples + "," + cutoff
				+ " " + Math.log(1 - cutoff));
		for (int c = 0; c < 1; c++) {
			try {
				PrintStream ps = new PrintStream(new FileOutputStream(
						"test.dat"));

//				RecordFunction<CVector> cf = new RecordFunction<CVector>(
//						new Sphere(dim), ps);
				 RecordFunction<CVector> cf = new RecordFunction<CVector>(
				 new Rastrigin(dim), ps);
//				 CostFunction<CVector> cf = new RecordFunction<CVector>(
//				 new Rosenbrock(dim), ps);

				GaussianBoltzRPM gtr = new GaussianBoltzRPM(cf, population, samples, cutoff);

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
				//System.out.println(gtr);
				System.out.println(cf.getCount()+" "+cf.getBestValue());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		System.out.println("end pop");
		System.exit(0);
	}

}
