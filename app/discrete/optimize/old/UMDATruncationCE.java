package app.discrete.optimize.old;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import app.discrete.function.TwoMax;
import app.discrete.sampler.UMDADiscreteSampler;
import app.discrete.util.DVector;

import optimize.function.CostFunction;
import optimize.function.RecordFunction;
import optimize.optimizer.old.TruncationCE;

import statistics.sampler.MixtureSampler;

public class UMDATruncationCE extends TruncationCE<DVector> {
	public UMDATruncationCE(int sample_num, double cutoff,
			CostFunction<DVector> cf) {
		super(sample_num, cutoff, cf);

		int numOfComponents = 1;
		if (false) {
			UMDADiscreteSampler[] umdas = new UMDADiscreteSampler[numOfComponents];
			for (int j = 0; j < umdas.length; j++) {
				umdas[j] = new UMDADiscreteSampler(cf.dim(), 0.5);
				umdas[j].init();
			}
			MixtureSampler<DVector> ms = new MixtureSampler<DVector>(umdas);
			super.sampler = ms;
		}
		super.sampler = new UMDADiscreteSampler(cf.dim(), 0.5);
	}

	public static int dim = 100;

	public static void main(String args[]) {
		for (int c = 0; c < 1; c++) {
			System.out.println("c=" + c);
			try {
				PrintStream ps = new PrintStream(new FileOutputStream(
						"test.dat"));
				CostFunction<DVector> cf = new RecordFunction<DVector>(
						new TwoMax(dim), ps);
				UMDATruncationCE bumda = new UMDATruncationCE(
						200, 0.1, cf);
				for (long i = 0; i < 100; i++) {

					bumda.optimize();
					if (true && i % 10 == 0) {
						System.out.println("----------------" + i
								+ "----------------");
						System.out.println(bumda);
					}
				}
				ps.close();
				System.out.println(bumda);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		System.out.println("end pop");
		System.exit(0);
	}

}
