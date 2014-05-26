package app.continuous.optimize;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;

import optimize.function.CostFunction;
import optimize.function.RecordFunction;
import optimize.his.TruncationHIS;
import app.continuous.function.Rastrigin;
import app.continuous.sampler.GaussianSampler;
import app.continuous.sampler.UniformSampler;
import app.continuous.util.CVector;
import calc.stat.Gaussian;

public class GaussianTruncationHIS extends TruncationHIS<CVector> {
	private PrintStream[] ps;

	public GaussianTruncationHIS(int sample_num, int level,
			CostFunction<CVector> cf) {
		super(sample_num, level, cf);
		super.CONTINUOUS = true;

		// ps = new PrintStream[level];
		// for (int i = 0; i < level; i++) {
		// try {
		// ps[i] = new PrintStream(new FileOutputStream("test" + i
		// + ".dat"));
		// } catch (FileNotFoundException e) {
		// e.printStackTrace();
		// System.exit(0);
		// }
		// }
		List<CVector> list = null;
		Gaussian initGauss = null;
		UniformSampler unis = new UniformSampler(cf.dim(), 0, 10);
		list = unis.sampling(10000);
		initGauss = GaussianSampler.calcGaussian(list);
		for (int i = 0; i < level; i++) {
			if (true && i == 0) {
				samplerSet[i] = unis;
			} else {
				// samplerSet[i] = new RecordSampler<CVector>(new
				// GaussianSampler(
				// cf.dim(), initGauss, 0.5), ps[i], cf);
				samplerSet[i] = new GaussianSampler(cf.dim(), initGauss, 0.5);
			}
		}
		init();
	}

	private static int dim = 10;

	public static void main(String args[]) {
		int param = 0;
		for (int p = 0; p < 3; p++) {
			switch (p) {
			case 0:
				param = 20;
				break;
			case 1:
				param = 40;
				break;
			case 2:
				param = 60;
				break;
			}
			System.out.println("#"+param);
			for (int c = 0; c < 10; c++) {
				try {
					PrintStream ps = new PrintStream(new FileOutputStream(
							"test.dat"));

					// CostFunction<CVector> cf = new RecordFunction<CVector>(
					// new Sphere(dim), ps);
					RecordFunction<CVector> cf = new RecordFunction<CVector>(
							new Rastrigin(dim), ps);
//					 RecordFunction<CVector> cf = new RecordFunction<CVector>(
//							new Rosenbrock(dim), ps);

					TruncationHIS disc = new GaussianTruncationHIS(100, param, cf);
					for (long i = 1;; i++) {
						disc.optimize();
						if (false && i % 1 == 0) {
							System.out.println("----------------" + i
									+ "----------------");
							System.out.printf("best( %d ) %s \n", i, disc
									.getBest());
							System.out.println(disc);
						}
						if (cf.getBestValue() < 1e-4) break;
						if (1e7 < cf.getCount()) break;
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
