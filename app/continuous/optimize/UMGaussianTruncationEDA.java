package app.continuous.optimize;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import optimize.function.CostFunction;
import optimize.function.RecordFunction;
import optimize.optimizer.old.TruncationEDA;
import app.continuous.function.Rosenbrock;
import app.continuous.sampler.UMGaussianSampler;
import app.continuous.util.CVector;

public class UMGaussianTruncationEDA extends TruncationEDA<CVector> {

	public UMGaussianTruncationEDA(int sample_num, double cutoff,
			CostFunction<CVector> cf) {
		super(sample_num, cutoff, cf);
		super.sampler = new UMGaussianSampler(cf.dim(), 0, 10, 0.5);
	}

	public static int dim = 10;

	public static void main(String args[]) {
		int param = 0;

		for (int p = 0; p < 1; p++) {
			switch (p) {
			case 0:
				param = 500;
				break;
			case 1:
				param = 1000;
				break;
			case 2:
				param = 3000;
				break;
			}
			System.out.println("#" + param);
			for (int c = 0; c < 1; c++) {
				try {
					PrintStream ps = new PrintStream(new FileOutputStream(
							"test_eda"+p+".dat"));

//					RecordFunction<CVector> cf = new RecordFunction<CVector>(
//							new Rastrigin(dim), ps);
					 RecordFunction<CVector> cf = new RecordFunction<CVector>(
					 new Rosenbrock(dim), ps);

					UMGaussianTruncationEDA gta = new UMGaussianTruncationEDA(
							param, 0.5, cf);

					for (long i = 0;; i++) {
						gta.optimize();
						if (false && i % 10 == 0) {
							System.out.println("----------------" + i
									+ "----------------");
							System.out.println(gta);
						}
						if (gta.getVariance() < 1e-10) break;
						if (cf.getBestValue() < 1e-4) break;
//						if (1e7 < cf.getCount()) break;
					}
					ps.close();
					System.out.println(cf.getCount() + " " + cf.getBestValue());
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
