package app.continuous.optimize;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;

import optimize.function.CostFunction;
import optimize.function.RecordFunction;
import optimize.optimizer.ConvergenceException;
import optimize.optimizer.TruncationRSEA;
import statistics.sampler.Sampler;
import app.continuous.function.Rastrigin;
import app.continuous.sampler.UMGaussianSampler;
import app.continuous.util.CVector;

public class UMG_RSEA extends TruncationRSEA<CVector> {

	public UMG_RSEA(int sampleNum, double cutoff, double lambda1,
			double lambda2, CostFunction<CVector> cf) {
		super(sampleNum, cutoff, lambda1, lambda2, cf);
	}

	static int dim = 10;

	@Override
	protected Sampler<CVector> getInitialSampler() {
		return new UMGaussianSampler(cf.dim(), 0, 10, 1);
	}

	public static void main(String args[]) {

		DecimalFormat df = new DecimalFormat("0.00");

		for (int c = 0; c <= 0; c++) {
			// double lambda = 0.01 * c;
			double lambda = 0.1;
			try {
				PrintStream ps =
						new PrintStream(new FileOutputStream(
								"rosenbrock_" + df.format(lambda)
										+ ".dat"));

				RecordFunction<CVector> cf =
						new RecordFunction<CVector>(
								new Rastrigin(dim), ps);
//				 RecordFunction<CVector> cf =
//				 new RecordFunction<CVector>(new Rosenbrock(
//				 dim), ps);

				UMG_RSEA rsea =
						new UMG_RSEA(500, 0.1, lambda, lambda, cf);

				for (long i = 0;; i++) {
					try {
						rsea.optimize();
					} catch (ConvergenceException e) {
						break;
					}
					if (false && i % 10 == 0) {
						System.out.println("----------------" + i
								+ "----------------");
						System.out.println(rsea);
					}
					if (rsea.getVariance() < 1e-20) {
						break;
					}
					if (cf.getBestValue() < 1e-5) {
						break;
					}
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
