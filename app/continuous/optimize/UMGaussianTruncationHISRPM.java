package app.continuous.optimize;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import optimize.function.CostFunction;
import optimize.function.RecordFunction;
import optimize.hisrpm.TruncationHISRPM;
import statistics.sampler.RecordSampler;
import app.continuous.function.Rosenbrock;
import app.continuous.sampler.UMGaussianSampler;
import app.continuous.util.CVector;

public class UMGaussianTruncationHISRPM extends TruncationHISRPM<CVector> {
	private PrintStream[] ps;

	public UMGaussianTruncationHISRPM(int pop_num, int sample_num, int level,
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
		for (int i = 0; i < level; i++) {
			samplerSet[i] = new RecordSampler<CVector>(new UMGaussianSampler(cf
					.dim(), -10, 10, 1), ps[i], cf);
		}
	}

	private static int dim = 10;

	public static void main(String args[]) {
		try {
			PrintStream ps = new PrintStream(new FileOutputStream("test.dat"));

			// CostFunction<CVector> cf = new RecordFunction<CVector>(
			// new Sphere(dim), ps);
//			RecordFunction<CVector> cf = new RecordFunction<CVector>(
//					new Rastrigin(dim), ps);
			 RecordFunction<CVector> cf = new RecordFunction<CVector>(
			 new Rosenbrock(dim), ps);

			TruncationHISRPM disc = new UMGaussianTruncationHISRPM(10, 10,
					20, cf);
			for (long i = 1;; i++) {
				disc.optimize();
				if (i % 1 == 0) {
					System.out.println("----------------" + i
							+ "----------------");
					System.out.printf("best( %d ) %s \n", i, disc.getBest());
					System.out.println(disc);
				}
				if(cf.getBestValue()<1e-4)break;
			}
			ps.close();

			System.out.println("end");
			System.exit(0);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

}
