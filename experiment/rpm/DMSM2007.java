package experiment.rpm;

import static experiment.AnnealingTest.test;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import optimize.function.CostFunction;
import optimize.function.RecordFunction;
import optimize.population.TruncationRTR_IDEA;
import app.discrete.function.Ising2D;
import app.discrete.optimize.UMDATruncationEDA;
import app.discrete.optimize.UMDATruncationRPMCE;
import app.discrete.optimize.UMDATruncationRTR_IDEA;
import app.discrete.util.DVector;
import experiment.util.Result;
public class DMSM2007 {

	public static CostFunction<DVector> getBaseFunction() {
		// return new OneMax(400);
		// return new Ising1D(400);
		return new Ising2D(20, 20, 1);
	}

	public static void testRPM(int population, int samples, double cutoff,
			int count) {
		System.out.print(population + "," + samples + "," + cutoff + ",");
		Result[] result = new Result[count];
		for (int i = 0; i < count; i++) {
			result[i] = testRPM(population, samples, cutoff);
		}
		Result avg = Result.average(result);
		Result std = Result.std(result);
		System.out.println(avg.best + "," + std.best + "," + avg.count + ","
				+ std.count);
	}

	public static Result testRPM(int population, int samples, double cutoff) {
		CostFunction<DVector> base = getBaseFunction();
		RecordFunction<DVector> cf = new RecordFunction<DVector>(base);
		UMDATruncationRPMCE opt = new UMDATruncationRPMCE(cf, population,
				samples, cutoff);
		return test(opt, cf);
	}

	public static void testIDEARTR(int population, int samples, double cutoff,
			int count) {
		if (TruncationRTR_IDEA.IDEA) {
			System.out.print(population + "," + cutoff + ",");
		} else {
			System.out.print(population + "," + samples + "," + cutoff + ",");
		}
		Result[] result = new Result[count];
		for (int i = 0; i < count; i++) {
			result[i] = testIDEARTR(population, samples, cutoff);
		}
		Result avg = Result.average(result);
		Result std = Result.std(result);
		System.out.println(avg.best + "," + std.best + "," + avg.count + ","
				+ std.count);
	}

	public static Result testIDEARTR(int population, int samples, double cutoff) {
		CostFunction<DVector> base = getBaseFunction();
		RecordFunction<DVector> cf = new RecordFunction<DVector>(base);
		UMDATruncationRTR_IDEA opt = new UMDATruncationRTR_IDEA(cf, population,
				samples, cutoff);
		return test(opt, cf);
	}

	public static void testEDA(int sampleNum, double cutoff, int count) {
		System.out.print(sampleNum + "," + cutoff + ",");
		Result[] result = new Result[count];
		for (int i = 0; i < count; i++) {
			result[i] = testEDA(sampleNum, cutoff);
		}
		Result avg = Result.average(result);
		Result std = Result.std(result);
		System.out.println(avg.best + "," + std.best + "," + avg.count + ","
				+ std.count);
	}

	public static Result testEDA(int sampleNum, double cutoff) {

		try {
			PrintStream ps = new PrintStream(new FileOutputStream(
					"discpoptruncanneal.dat"));
			// CostFunction<DVector> base = new OneMax(400);
			// CostFunction<DVector> base = new Ising1D(400);
			// CostFunction<DVector> base = new Ising2D(20, 20);
			CostFunction<DVector> base = getBaseFunction();
			RecordFunction<DVector> cf = new RecordFunction<DVector>(base, ps);
			UMDATruncationEDA pop = new UMDATruncationEDA(sampleNum, cutoff, cf);
			return test(pop, cf);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(0);
			return null;
		}

	}

	public static void main(String args[]) {
		int c = 10;
		if (false) {
			{
				testrpm(c);
				
			}
		} else if (false) {
			double cut = 0;
			for (int i = 0; i < 4; i++) {
				if (i == 0) cut = 0.1;
				if (i == 1) cut = 0.3;
				if (i == 2) cut = 0.5;
				// if (i == 3) cut = 0.7;

				testEDA(100, cut, c);
				testEDA(500, cut, c);
				testEDA(1000, cut, c);
				testEDA(3000, cut, c);
				testEDA(6000, cut, c);
			}
		} else if (false) {
			if (TruncationRTR_IDEA.IDEA) {
				double cut = 0;
				for (int i = 0; i < 4; i++) {
					if (i == 0) cut = 0.1;
					if (i == 1) cut = 0.3;
					if (i == 2) cut = 0.5;
					if (i == 3) cut = 0.7;
					testIDEARTR(100, 0, cut, c);
					testIDEARTR(500, 0, cut, c);
					testIDEARTR(1000, 0, cut, c);
					testIDEARTR(3000, 0, cut, c);
				}
			} else {
				double cut = 0;
				for (int i = 0; i < 5; i++) {
					if (i == 0) cut = 0.1;
					if (i == 1) cut = 0.3;
					if (i == 2) cut = 0.5;
					if (i == 3) cut = 0.7;
					if (i == 4) cut = 0.9;
					// test4(10, 10, cut, c);
					// test4(50, 10, cut, c);
					// test4(50, 50, cut, c);
					// test4(100, 10, cut, c);
					// test4(100, 50, cut, c);
					// test4(100, 100, cut, c);
					// test4(200, 10, cut, c);
					// test4(200, 50, cut, c);
					// test4(200, 100, cut, c);
					// test4(200, 200, cut, c);
					// test4(300, 10, cut, c);
					// test4(300, 50, cut, c);
					// test4(300, 100, cut, c);
					// test4(300, 200, cut, c);
					// test4(300, 300, cut, c);
					// test4(400, 10, cut, c);
					testIDEARTR(400, 50, cut, c);
					testIDEARTR(400, 100, cut, c);
					testIDEARTR(400, 200, cut, c);
					testIDEARTR(400, 300, cut, c);
					testIDEARTR(400, 400, cut, c);
					testIDEARTR(500, 10, cut, c);
					testIDEARTR(500, 50, cut, c);
					testIDEARTR(500, 100, cut, c);
					testIDEARTR(500, 200, cut, c);
					testIDEARTR(500, 300, cut, c);
					testIDEARTR(500, 400, cut, c);
					testIDEARTR(500, 500, cut, c);
					// test1(1000, 10, cut, c);
					// test1(1000, 100, cut, c);
					// test1(1000, 500, cut, c);
					// test1(1000, 1000, cut, c);
					// test1(3000, 10, cut, c);
					// test1(3000, 100, cut, c);
					// test1(3000, 1000, cut, c);
					// test1(3000, 3000, cut, c);
				}
			}

		}
	}

	private static void testrpm(int c) {
		// TODO Auto-generated method stub
		double cut = 3;
		for (int i = 1; i < 5; i++) {
			// if (i == 0) cut = 0;
			if (i == 1) cut = 0.01;
			if (i == 2) cut = 0.05;
			if (i == 3) cut = 0.1;
			if (i == 4) cut = 0.2;

			testRPM(10, 10, cut, c);
			testRPM(10, 50, cut, c);
			testRPM(10, 100, cut, c);
			testRPM(10, 200, cut, c);
			testRPM(50, 10, cut, c);
			testRPM(50, 50, cut, c);
			testRPM(50, 100, cut, c);
			testRPM(50, 200, cut, c);
			testRPM(100, 10, cut, c);
			testRPM(100, 50, cut, c);
			testRPM(100, 100, cut, c);
			testRPM(100, 200, cut, c);
			testRPM(200, 10, cut, c);
			testRPM(200, 50, cut, c);
			testRPM(200, 100, cut, c);
			testRPM(200, 200, cut, c);
			testRPM(0, 10, cut, c);
			testRPM(0, 50, cut, c);
			testRPM(0, 100, cut, c);
			testRPM(0, 200, cut, c);
			testRPM(0, 500, cut, c);
			testRPM(0, 1000, cut, c);

			// test1(300, 10, cut, c);
			// test1(300, 50, cut, c);
			// test1(300, 100, cut, c);
			// test1(300, 200, cut, c);
			// test1(300, 300, cut, c);
			// test1(400, 10, cut, c);
			// test1(400, 50, cut, c);
			// test1(400, 100, cut, c);
			// test1(400, 200, cut, c);
			// test1(400, 300, cut, c);
			// test1(400, 400, cut, c);
			// test1(500, 10, cut, c);
			// test1(500, 50, cut, c);
			// test1(500, 100, cut, c);
			// test1(500, 200, cut, c);
			// test1(500, 300, cut, c);
			// test1(500, 400, cut, c);
			// test1(500, 500, cut, c);
		}
	}
}
