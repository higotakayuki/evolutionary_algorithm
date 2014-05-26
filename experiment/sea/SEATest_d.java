package experiment.sea;

import static experiment.AnnealingTest.test;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import optimize.Annealing;
import optimize.function.CostFunction;
import optimize.function.RecordFunction;
import app.discrete.function.Ising2D;
import app.discrete.function.NoiseFunction;
import app.discrete.optimize.UMDA_RSEA;
import app.discrete.util.DVector;
import experiment.AnnealingTest;
import experiment.util.Result;

public class SEATest_d {

	public static void main(String args[]) {
		int count = 10;
		AnnealingTest.print = false;
		
		{
			int sampleNum=1000;
			for (int i = 0; i <= 100; i++) {
				// if (j == 1 && i <= 24) continue;
				double lambda = i * 0.01;
				testSEA(sampleNum, 0.3, lambda, 0.2, count);
			}
		}
		
//		if (false) {
//			int sampleNum = 1;
//			for (int j = 0; j < 4; j++) {
//				sampleNum *= 10;
//				// if (j == 0) continue;
//				// if (j == 1) continue;
//				for (int i = 0; i <= 50; i++) {
//					// if (j == 1 && i <= 24) continue;
//					double lambda = i * 0.01;
//					testSEA(sampleNum, 0.1, lambda, lambda, count);
//				}
//				System.out.println();
//				System.out.println();
//
//			}
//		}
		// System.out.println();
		// System.out.println();
		// for (int i = 0; i <= 100; i++) {
		// double lambda = i * 0.01;
		// testSEA(1000, 0.3, lambda, lambda, count);
		// }
		// System.out.println();
		// System.out.println();
		// for (int i = 0; i <= 100; i++) {
		// double lambda = i * 0.01;
		// testSEA(1000, 0.5, lambda, lambda, count);
		// }
		// for (int i = 0; i <= 100; i++) {
		// // testSEA(1000, 0.1, i * 0.01, 0.2, count);
		// testSEA(1000, 0.3, 0.2, i * 0.01, count);
		// // testSEA(1000, i * 0.01, 0, count);
		// }
		// System.out.println();
		// System.out.println();
		// for (int i = 0; i <= 100; i++) {
		// testSEA(1000, 0.3, i * 0.01, 0.2, count);
		// }

		// for (int i = 1; i <= 25; i++) {
		// testSEA(i*300, 0.5, 0, 0, count);
		// }
		// System.out.println();
		// System.out.println();
		// for (int i = 1; i <= 25; i++) {
		// testSEA(i*300, 0.5, 0.2, 0.2, count);
		// }
	}

	public static CostFunction<DVector> getBaseFunction() {
		// return new OneMax(400);
		// return new Ising1D(400);
		return new Ising2D(20, 20, 1);
		//ƒmƒCƒY‚ÍtestSEA‚É‚Ä’Ç‰Á
	}

	public static void testSEA(int sample_num, double cutoff, double lambda1,
			double lambda2, int count) {
		Result[] result = new Result[count];
		for (int i = 0; i < count; i++) {
			result[i] = testSEA(sample_num, cutoff, lambda1, lambda2);
		}
		Result avg = Result.average(result);
		Result std = Result.std(result);
		System.out.printf("%d, %f, %f, %f, %f, %f, %f, %f \n", sample_num,
				cutoff, lambda1, lambda2, avg.best, std.best, avg.count,
				std.count);
	}

	public static Result testSEA(int sample_num, double cutoff, double lambda1,
			double lambda2) {
		try {
			PrintStream ps = null;
			if (false) ps = new PrintStream(new FileOutputStream("seatest.dat"));
			CostFunction<DVector> base = getBaseFunction();
			base = new NoiseFunction(base);
			RecordFunction<DVector> cf = new RecordFunction<DVector>(base);
			Annealing<DVector> anneal = new UMDA_RSEA(
					sample_num, cutoff, lambda1, lambda2, cf);
			// Annealing<DVector> anneal = new UMDATruncationSEAwithoutERS(
			// sample_num, cutoff, lambda, cf);

			return test(anneal, cf);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(0);
			return null;
		}
	}
}
