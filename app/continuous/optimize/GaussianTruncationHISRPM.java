package app.continuous.optimize;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;

import optimize.function.CostFunction;
import optimize.function.RecordFunction;
import optimize.hisrpm.TruncationHISRPM;
import statistics.sampler.RecordSampler;
import app.continuous.function.Rastrigin;
import app.continuous.sampler.GaussianSampler;
import app.continuous.sampler.UniformSampler;
import app.continuous.util.CVector;
import calc.stat.Gaussian;

public class GaussianTruncationHISRPM extends TruncationHISRPM<CVector> {
	private PrintStream[] ps;

	public GaussianTruncationHISRPM(int pop_num, int sample_num, int level,
			CostFunction<CVector> cf) {
		super(pop_num, sample_num, level, cf);

		ps = new PrintStream[level];
		for (int i = 0; i < level; i++) {
			try {
				ps[i] = new PrintStream(new FileOutputStream("test" + i
						+ ".dat"));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				System.exit(0);
			}
		}
		List<CVector> list = null;
		Gaussian initGauss = null;
		list = new UniformSampler(cf.dim(), 0, 10).sampling(100000);
		initGauss = GaussianSampler.calcGaussian(list);
		for (int i = 0; i < level; i++) {
			samplerSet[i] = new RecordSampler<CVector>(new GaussianSampler(cf
					.dim(), initGauss, 0.1), ps[i], cf);
		}
	}

	private static int dim = 10;

	public static void main(String args[]) {
		try {
			PrintStream ps = new PrintStream(new FileOutputStream("test.dat"));

//			 CostFunction<CVector> cf = new RecordFunction<CVector>(
//			 new Sphere(dim), ps);
			 CostFunction<CVector> cf = new RecordFunction<CVector>(
			 new Rastrigin(dim), ps);
//			CostFunction<CVector> cf = new RecordFunction<CVector>(
//					new Rosenbrock(dim), ps);

			TruncationHISRPM disc = new GaussianTruncationHISRPM(100,100, 20, cf);
			for (long i = 1; i < 30000; i++) {
				disc.optimize();
				if (i % 1 == 0) {
					System.out.println("----------------" + i
							+ "----------------");
					System.out.printf("best( %d ) %s \n", i, disc.getBest());
					System.out.println(disc);
				}
			}
			ps.close();

			System.out.println("end");
			System.exit(0);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

}
