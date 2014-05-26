package experiment.his;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import app.discrete.function.Ising2D;
import app.discrete.function.OneMax;
import app.discrete.optimize.UMDATruncationEDA;
import app.discrete.optimize.UMDATruncationHIS;
import app.discrete.optimize.UMDAMixtureTruncationHIS;
import app.discrete.util.DVector;

import optimize.Optimizer;
import optimize.function.RecordFunction;
import optimize.optimizer.ConvergenceException;

import experiment.util.Result;

public class ForHIS {

	static int sampleNum = 10;

	static int layer = 40;

	static int type = 0;

	static double cutoff = 0;

	static int count = 10;

	public static void main(String args[]) {
		if (args.length > 0) {
			// if (args[0].equals("a")) main2(args);
			// if (args[0].equals("h")) main1(args);
			// if (args[0].equals("h")) main3(args);
		} else {
			// type = 0;
			// sampleNum = 10;
			// layer = 20;
			// cutoff = 0.5;
			// test();
			main3(args);
		}
	}

	public static void main1(String args[]) {
		type = 0;
		for (int i = 0; i < 4; i++) {
			if (i == 0) layer = 10;
			if (i == 1) layer = 20;
			if (i == 2) layer = 30;
			if (i == 3) layer = 40;
			for (int j = 0; j < 2; j++) {
				if (j == 0) sampleNum = 10;
				if (j == 1) sampleNum = 50;
				// System.out.println("layer:" + layer);
				// System.out.println("sampleNum:" + sampleNum);
				System.out.println(layer + "," + sampleNum + ",");
				Result[] result = new Result[count];
				for (int k = 0; k < count; k++) {
					result[k] = test1();
				}
				Result avg = Result.average(result);
				Result std = Result.std(result);
				System.out.println(avg.best + "," + std.best + "," + avg.count
						+ "," + std.count);
			}
		}
	}



	private static Result test1() {
		try {
			// cost function
			PrintStream ps = new PrintStream(new FileOutputStream("dtest"
					+ type + ".dat"));
			// RecordFunction<DVector> rf = new RecordFunction<DVector>(
			// new TestFunction1(dim), ps);
			// RecordFunction<DVector> rf = new RecordFunction<DVector>(
			// new Ising2D(20, 20), ps);
//			RecordFunction<DVector> rf = new RecordFunction<DVector>(
//					new Ising1D(400));
			 RecordFunction<DVector> rf = new RecordFunction<DVector>(
			 new OneMax(400));

			Optimizer<DVector> opt = null;

			switch (type) {

			case 0: {
				UMDATruncationHIS umda = new UMDATruncationHIS(sampleNum,
						layer, rf);
				opt = umda;
			}
				break;
			case 1: {
				UMDAMixtureTruncationHIS disc = new UMDAMixtureTruncationHIS(
						sampleNum, layer, rf);
				opt = disc;
			}
				break;
			case 2: {
				UMDATruncationEDA ann = new UMDATruncationEDA(
						sampleNum, cutoff, rf);
				opt = ann;
			}
				break;
			default: {
				System.out.println("end");
				System.exit(0);
			}

				// end selection

			}
			// general procedure
			long r = 0;
			while (true) {
				try {
					opt.optimize();
				} catch (ConvergenceException e) {
					break;
				}
				if (rf.getCount() > r) {
					r += 100;
					if (false) {
						System.out.println("----------------" + rf.getCount()
								+ "----------------");
//						System.out.printf("best %f/%s \n", rf.getBestValue(),
//								opt.getBest());
						System.out.println(opt);
					}
				}
				if (rf.getCount() > 1e10 || rf.getBestValue() <= -400)
				// 100000 for one max
					// 1000000 for 1D(400) or 2D(20,20)
					// 2000000 for 1D(400) or 2D(20,20)
					break;
			}

			ps.close();
			// System.out.println(opt);
			Result result = new Result();
			result.best = Math.round(rf.getBestValue());
			result.count = rf.getCount();
			// System.out.println("end");
			return result;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static void main3(String args[]) {
		type = 0;
		for (int i = 0; i < 4; i++) {
			// int i = 3;
			if (i == 0) layer = 10;
			if (i == 1) layer = 20;
			if (i == 2) layer = 30;
			if (i == 3) layer = 40;
			for (int j = 0; j < 2; j++) {
				if (j == 0) sampleNum = 10;
				if (j == 1) sampleNum = 50;
				// System.out.println("layer:" + layer);
				// System.out.println("sampleNum:" + sampleNum);
				System.out.println();
				System.out.println();
				System.out.println("#L=" + layer + ",M=" + sampleNum + "");

				Result[][] result_all = new Result[count][];
				for (int k = 0; k < count; k++) {
					result_all[k] = test3();
				}
				for (int c = 0; c < result_all[0].length; c++) {
					Result[] result = new Result[count];
					for (int k = 0; k < count; k++) {
						result[k] = result_all[k][c];
					}
					Result avg = Result.average(result);
					Result std = Result.std(result);
					System.out.println(avg.count + " " + avg.best + " "
							+ std.best);
				}
				System.out.println();
			}
		}
	}

	private static Result[] test3() {
		{
			// cost function
			// PrintStream ps = new PrintStream(new FileOutputStream("dtest"
			// + type + "_" + layer + "_" + sampleNum + ".dat"));
			// RecordFunction<DVector> rf = new RecordFunction<DVector>(
			// new TestFunction1(dim), ps);
			 RecordFunction<DVector> rf = new RecordFunction<DVector>(
			 new Ising2D(20, 20,0.85));
//			RecordFunction<DVector> rf = new RecordFunction<DVector>(
//					new Ising1D(400));
//			 RecordFunction<DVector> rf = new RecordFunction<DVector>(
//			 new OneMax(400));

			Optimizer<DVector> opt = null;

			switch (type) {

			case 0: {
				UMDATruncationHIS umda = new UMDATruncationHIS(sampleNum,
						layer, rf);
				opt = umda;
			}
				break;
			case 1: {
				UMDAMixtureTruncationHIS disc = new UMDAMixtureTruncationHIS(
						sampleNum, layer, rf);
				opt = disc;
			}
				break;
			case 2: {
				UMDATruncationEDA ann = new UMDATruncationEDA(
						sampleNum, cutoff, rf);
				opt = ann;
			}
				break;
			default: {
				System.out.println("end");
				System.exit(0);
			}

				// end selection

			}
			// general procedure
			List<Result> result_list = new ArrayList<Result>(100);
			long limit = 1000;
			double r = 2.5;
			while (true) {
				try {
					opt.optimize();
				} catch (ConvergenceException e) {
					break;
				}
				if (false) {
					System.out.println("----------------" + rf.getCount()
							+ "----------------");
//					System.out.printf("best %f/%s \n", rf.getBestValue(), opt
//							.getBest());
					System.out.println(opt);
				}
				if (rf.getCount() > 2e6) {
					limit *= r;
					Result tresult = new Result();
					tresult.best = rf.getBestValue();
					tresult.count = rf.getCount();
					result_list.add(tresult);
					break;
				}
				if (rf.getCount() > limit) {
					limit *= r;
					Result tresult = new Result();
					tresult.best = Math.floor(rf.getBestValue());
					tresult.count = rf.getCount();
					result_list.add(tresult);
				}
			}

			// ps.close();
			// System.out.println(opt);

			// System.out.println("end");
			return result_list.toArray(new Result[0]);
		}
	}
	
//	public static void main2(String args[]) {
//	type = 2;
//	for (int i = 0; i < 6; i++) {
//		if (i == 0) sampleNum = 100;
//		if (i == 1) sampleNum = 500;
//		if (i == 2) sampleNum = 1000;
//		if (i == 3) sampleNum = 1500;
//		if (i == 4) sampleNum = 2000;
//		if (i == 5) sampleNum = 3000;
//
//		for (int j = 0; j < 3; j++) {
//			if (j == 0) cutoff = 0.3;
//			if (j == 1) cutoff = 0.5;
//			if (j == 2) cutoff = 0.7;
//			System.out.print("cutoff:" + cutoff);
//			System.out.println("sampleNum:" + sampleNum);
//			for (int k = 0; k < 10; k++) {
//				test1();
//			}
//		}
//	}
//}

}
