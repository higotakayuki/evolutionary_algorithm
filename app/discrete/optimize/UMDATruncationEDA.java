package app.discrete.optimize;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import optimize.function.CostFunction;
import optimize.function.RecordFunction;
import optimize.optimizer.old.TruncationEDA;
import app.discrete.function.Ising2D;
import app.discrete.function.NoiseFunction;
import app.discrete.sampler.UMDADiscreteSampler;
import app.discrete.util.DVector;

public class UMDATruncationEDA extends TruncationEDA<DVector> {
	public UMDATruncationEDA(int sample_num, double cutoff,
			CostFunction<DVector> cf) {
		super(sample_num, cutoff, cf);

		// if (false) {
		// int numOfComponents = 1;
		// UMDADiscreteSampler[] umdas = new
		// UMDADiscreteSampler[numOfComponents];
		// for (int j = 0; j < umdas.length; j++) {
		// umdas[j] = new UMDADiscreteSampler(cf.dim(), 1);
		// umdas[j].init();
		// }
		// MixtureSampler<DVector> ms = new MixtureSampler<DVector>(umdas);
		// super.sampler = ms;
		//
		// }
		super.sampler = new UMDADiscreteSampler(cf.dim(), 0.5);
	}

	public static int dim = 400;

	public static void main(String args[]) {
		PrintStream ps = null;
		// CostFunction<DVector> base = new OneMax(dim);
		// CostFunction<DVector> base = new Ising1D(dim);
		CostFunction<DVector> base = new Ising2D(20, 20);
		base = new NoiseFunction(base);
		RecordFunction<DVector> cf =
				new RecordFunction<DVector>(base, ps);
		UMDATruncationEDA opt = new UMDATruncationEDA(7500, 0.5, cf);

		for (long i = 0;; i++) {
			opt.optimize();
			if (opt.getVariance() < 1e-20) break;
			if (cf.getBestValue() <= base.getMinimum()) break;
			if (false && i % 10 == 0) {
				System.out.println("----------------" + i + "("
						+ cf.getCount() + ")" + "----------------");
				System.out.println(opt);
				System.out.println(cf.getBestValue());
			}
		}
		if (ps != null) ps.close();
		// System.out.println(opt);
		System.out.println("" + cf.getBestValue() + "  "
				+ cf.getCount());

		// for (int c = 0; c < 1; c++) {
		// System.out.println("c=" + c);
		// try {
		// PrintStream ps =
		// new PrintStream(new FileOutputStream(
		// "test.dat"));
		// // CostFunction<DVector> cf = new RecordFunction<DVector>(
		// // new TwoMax(dim), ps);
		// // RecordFunction<DVector> cf = new RecordFunction<DVector>(
		// // new OneMax(dim), ps);
		//
		// CostFunction<DVector> base = new Ising2D(20, 20);
		// base = new NoiseFunction(base);
		// RecordFunction<DVector> cf =
		// new RecordFunction<DVector>(base, ps);
		//
		// UMDATruncationEDA bumda =
		// new UMDATruncationEDA(100, 0.5, cf);
		// for (long i = 0; i < 100000; i++) {
		// bumda.optimize();
		// if (cf.getBestValue() <= cf.getMinimum()) break;
		// if (false && i % 10 == 0) {
		// System.out.println("----------------" + i
		// + "----------------");
		// System.out.println(bumda);
		// }
		// }
		// ps.close();
		// System.out.println(bumda);
		// } catch (FileNotFoundException e) {
		// e.printStackTrace();
		// }
		// }
		// System.out.println("end pop");
		// System.exit(0);
	}

}
