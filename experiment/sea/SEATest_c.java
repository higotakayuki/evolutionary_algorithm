package experiment.sea;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

import optimize.Annealing;
import optimize.function.CostFunction;
import optimize.function.RecordFunction;
import app.continuous.function.Rastrigin;
import app.continuous.optimize.UMG_RSEA;
import app.continuous.util.CVector;
import experiment.AnnealingTest;
import experiment.util.Result;

public class SEATest_c {

	public static void main(String args[]) {
		int count = 20;
		AnnealingTest.print = false;

		{
			int sampleNum = 500;
			for (int i = 100; i <= 100; i++) {
				// if (j == 1 && i <= 24) continue;
				double lambda = i * 0.01;
				testSEA(sampleNum, 0.1, lambda, lambda, count);
			}
		}

		// if (false) {
		// int sampleNum = 1;
		// for (int j = 0; j < 4; j++) {
		// sampleNum *= 10;
		// // if (j == 0) continue;
		// // if (j == 1) continue;
		// for (int i = 0; i <= 50; i++) {
		// // if (j == 1 && i <= 24) continue;
		// double lambda = i * 0.01;
		// testSEA(sampleNum, 0.1, lambda, lambda, count);
		// }
		// System.out.println();
		// System.out.println();
		//
		// }
		// }
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

	public static CostFunction<CVector> getBaseFunction() {
//		return new Rosenbrock(10);
		 return new Rastrigin(10);
	}

	public static void testSEA(int sample_num, double cutoff,
			double lambda1, double lambda2, int count) {
		ArrayList<Result> list = new ArrayList<Result>(count);
		for (int i = 0; i < count; i++) {
			Result temp =
					testSEA(sample_num, cutoff, lambda1, lambda2);
			list.add(temp);
			if (Double.isInfinite(temp.best)) break;
		}
		Result[] result = list.toArray(new Result[0]);
		Result avg = Result.average(result);
		Result std = Result.std(result);
		System.out.printf("%d, %f, %f, %f, %f, %f, %f, %f, %f \n",
				sample_num, cutoff, lambda1, lambda2, avg.best,
				std.best, avg.count, std.count, avg.success);
	}

	public static Result testSEA(int sample_num, double cutoff,
			double lambda1, double lambda2) {
		try {
			PrintStream ps = null;
			if (false) ps =
					new PrintStream(new FileOutputStream(
							"seatest.dat"));
			CostFunction<CVector> base = getBaseFunction();
			RecordFunction<CVector> cf =
					new RecordFunction<CVector>(base);
			Annealing<CVector> anneal =
					new UMG_RSEA(sample_num, cutoff, lambda1,
							lambda2, cf);
			return AnnealingTest.test_c(anneal, cf);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(0);
			return null;
		}
	}
}
