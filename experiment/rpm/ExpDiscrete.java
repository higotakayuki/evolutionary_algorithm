package experiment.rpm;

import static experiment.AnnealingTest.test;
import optimize.function.CostFunction;
import optimize.function.RecordFunction;
import optimize.population.TruncationRTR_IDEA;
import app.discrete.function.Ising2D;
import app.discrete.optimize.UMDATruncationEDA;
import app.discrete.optimize.UMDATruncationRPMCE;
import app.discrete.optimize.UMDATruncationRTR_IDEA;
import app.discrete.util.DVector;
import experiment.util.Result;

public class ExpDiscrete {
	public static void main(String args[]) {
		System.out.println("#" + ExpDiscrete.getBaseFunction().getClass().getName());

		// ExpRPM.testRPM(10000, 3000, 0.1, 1);

		for (int j = 1;; j++) {
			ExpDiscrete.testRPM(10000, j * 100, 0.5, 1);
			// ExpRPM.testRTR(100*j, 100*j, 0.9, 1);
		}
		// for (int c = 5; c <= 9; c++) {
		// for (int i = 1; i <= 30; i++) {
		// ExpRPM.testRTR(2000, i * 100, 1 - 0.1 * c, 1);
		// }
		// }

		// ExpRPM.testRPM(0, 1000, 0.2,20);
		// ExpRPM.testRPM(0, 2000, 0.2,20);
		// ExpRPM.testRPM(0, 3000, 0.2,20);
		// ExpRPM.testRPM(1000000, 30, 0.2,20);
		// ExpRPM.testRPM(1000000, 50, 0.2,20);
		// ExpRPM.testRPM(1000000, 100, 0.2,20);
		// for (int i = 0; i < 10000; i++) {
		// ExpRPM.testRPM(i * 10, 300, 0.2, 1);
		// }

		//
		// for (int i = 1; i <= 30; i++) {
		// ExpRPM.testRPM(10000, 100 * i, 0.2, 20);
		// }
		//
		// for (int i = 1; i <= 30; i++) {
		// ExpRPM.testRPM(10000, 100 * i, 0.15, 20);
		// }
		//

		// System.out.println("rpm");
		// for (int i = 1; i <= 30; i++) {
		// ExpRPM.testRPM(10000, 100 * i, 0.1, 20);
		// }

		//
		// for (int i = 1; i <= 30; i++) {
		// ExpRPM.testRPM(10000, 100 * i, 0.05, 20);
		// }
		// System.out.println("eda-es");
		// for (int i = 1; i <= 20; i++) {
		// TruncationRTR_IDEA.IDEA = true;
		// ExpRPM.testIDEA(i * 200, 0, 0.5, 20);
		// }
		// for (int i = 1; i <= 20; i++) {
		// TruncationRTR_IDEA.IDEA = true;
		// ExpRPM.testIDEA(100 * i, 100 * i, 0.5, 20);
		// }

		// System.out.println("eda");
		// for (int i = 1; i <= 25; i++) {
		// ExpRPM.testEDA(300 * i, 0.5, 10);
		// }
	}

	public static CostFunction<DVector> getBaseFunction() {
		// return new OneMax(100);
		// return new Ising1D(400);
		return new Ising2D(10, 10, 1);
	}

	public static void testRPM(int population, int samples, double cutoff,
			int count) {
		System.out.printf("%d %d %.2f ", population, samples, cutoff);
		Result[] result = new Result[count];
		for (int i = 0; i < count; i++) {
			result[i] = testRPM(population, samples, cutoff);
		}
		ExpDiscrete.printResult(result);
	}

	public static void printResult(Result[] results) {
		Result avg = Result.average(results);
		Result std = Result.std(results);
		System.out.printf("%.2f %.2f %.0f %.0f %.4f\n", avg.best, std.best,
				avg.count, std.count, ((double) avg.time) / avg.count);
	}

	public static Result testRPM(int population, int samples, double cutoff) {
		CostFunction<DVector> base = getBaseFunction();
		RecordFunction<DVector> cf = new RecordFunction<DVector>(base);
		UMDATruncationRPMCE opt = new UMDATruncationRPMCE(cf, population,
				samples, cutoff);
		return test(opt, cf);
	}

	public static void testIDEA(int population, int samples, double cutoff,
			int count) {
		if (TruncationRTR_IDEA.IDEA) {
			System.out.printf("%d %d %.2f ", population,
					((int) (population * cutoff)), cutoff);
			// System.out.print(population + " " + cutoff + " ");
		} else {
			System.out.printf("%d %d %.2f ", population, samples, cutoff);
		}
		Result[] result = new Result[count];
		for (int i = 0; i < count; i++) {
			result[i] = testIDEA(population, samples, cutoff);
		}
		ExpDiscrete.printResult(result);
	}

	public static Result testRTR(int population, int samples, double cutoff) {
		CostFunction<DVector> base = getBaseFunction();
		RecordFunction<DVector> cf = new RecordFunction<DVector>(base);
		UMDATruncationRTR_IDEA.IDEA = false;
		UMDATruncationRTR_IDEA opt = new UMDATruncationRTR_IDEA(cf, population,
				samples, cutoff);
		return test(opt, cf);
	}

	public static void testRTR(int population, int samples, double cutoff,
			int count) {
		System.out.printf("%d %d %.2f ", population, samples, cutoff);

		Result[] result = new Result[count];
		for (int i = 0; i < count; i++) {
			result[i] = testRTR(population, samples, cutoff);
		}
		ExpDiscrete.printResult(result);
	}

	public static Result testIDEA(int population, int samples, double cutoff) {
		CostFunction<DVector> base = getBaseFunction();
		RecordFunction<DVector> cf = new RecordFunction<DVector>(base);
		UMDATruncationRTR_IDEA opt = new UMDATruncationRTR_IDEA(cf, population,
				samples, cutoff);
		return test(opt, cf);
	}

	public static void testEDA(int sampleNum, double cutoff, int count) {
		System.out.print(sampleNum + " " + cutoff + " ");
		Result[] result = new Result[count];
		for (int i = 0; i < count; i++) {
			result[i] = testEDA(sampleNum, cutoff);
		}
		ExpDiscrete.printResult(result);
	}

	public static Result testEDA(int sampleNum, double cutoff) {
		CostFunction<DVector> base = getBaseFunction();
		RecordFunction<DVector> cf = new RecordFunction<DVector>(base);
		UMDATruncationEDA pop = new UMDATruncationEDA(sampleNum, cutoff, cf);
		return test(pop, cf);
	}

}
