package app.continuous.optimize;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.List;

import optimize.function.CostFunction;
import optimize.function.RecordFunction;
import optimize.optimizer.ConvergenceException;
import optimize.optimizer.TruncationRSEA;
import statistics.sampler.Sampler;
import app.continuous.function.Rosenbrock;
import app.continuous.sampler.GaussianSampler;
import app.continuous.sampler.UniformSampler;
import app.continuous.util.CVector;
import calc.stat.Gaussian;

public class MVG_RSEA extends TruncationRSEA<CVector> {

	public MVG_RSEA(int sampleNum, double cutoff, double lambda1,
			double lambda2, CostFunction<CVector> cf) {
		super(sampleNum, cutoff, lambda1, lambda2, cf);
	}

	@Override
	protected Sampler<CVector> getInitialSampler() {
		List<CVector> list =
				new UniformSampler(cf.dim(), -10, 10).sampling(10000);
		Gaussian initGauss = GaussianSampler.calcGaussian(list);
		return new GaussianSampler(cf.dim(), initGauss, 1);
	}

	static int dim = 10;

	public static void main(String args[]) {

		DecimalFormat df = new DecimalFormat("0.00");

		for (int c = 0; c <= 50; c++) {
			double lambda = 0.01 * c;
			try {
				PrintStream ps =
						new PrintStream(new FileOutputStream(
								"rosenbrock_" + df.format(lambda)
										+ ".dat"));

				// RecordFunction<CVector> cf =
				// new RecordFunction<CVector>(
				// new Rastrigin(dim), ps);
				RecordFunction<CVector> cf =
						new RecordFunction<CVector>(new Rosenbrock(
								dim), ps);

				MVG_RSEA rsea =
						new MVG_RSEA(1000, 0.01, lambda, lambda, cf);

				for (long i = 0;; i++) {
					try {
						rsea.optimize();
					} catch (ConvergenceException e) {
						break;
					}
					if (true && i % 10 == 0) {
						System.out.println("----------------" + i
								+ "----------------");
						System.out.println(rsea);
					}
					if (rsea.getVariance() < 1e-15) break;
					if (cf.getBestValue() < 1e-5) break;
					// if (1e7 < cf.getCount()) break;
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
