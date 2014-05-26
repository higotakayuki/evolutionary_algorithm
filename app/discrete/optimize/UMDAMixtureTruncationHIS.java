package app.discrete.optimize;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import app.discrete.function.TwoMax;
import app.discrete.sampler.UMDADiscreteSampler;
import app.discrete.util.DVector;

import optimize.Optimizer;
import optimize.function.CostFunction;
import optimize.function.RecordFunction;
import optimize.his.TruncationHIS;
import optimize.optimizer.ConvergenceException;

import statistics.sampler.MixtureSampler;
import statistics.sampler.RecordSampler;

public class UMDAMixtureTruncationHIS extends TruncationHIS<DVector> {
	private PrintStream[] ps;

	private static int dim = 100;

	public UMDAMixtureTruncationHIS(int sample_num, int level,
			CostFunction<DVector> cf) {
		super(sample_num, level, cf);
		ps = new PrintStream[level];
		for (int i = 0; i < level; i++) {
			try {
				ps[i] =
						new PrintStream(new FileOutputStream("test"
								+ i + ".dat"));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				System.exit(0);
			}
		}
		for (int i = 0; i < level; i++) {
			int numOfComponents = 2;
			UMDADiscreteSampler[] umdas =
					new UMDADiscreteSampler[numOfComponents];
			for (int j = 0; j < umdas.length; j++) {
				umdas[j] = new UMDADiscreteSampler(cf.dim(), 0.5);
				umdas[j].init();
			}
			MixtureSampler<DVector> ms =
					new MixtureSampler<DVector>(umdas);
			samplerSet[i] = new RecordSampler<DVector>(ms, ps[i], cf);
		}
		init();
	}

	public static void main(String args[]) {
		try {
			PrintStream ps =
					new PrintStream(new FileOutputStream("test.dat"));
			RecordFunction<DVector> cf =
					new RecordFunction<DVector>(new TwoMax(dim), ps);
			UMDAMixtureTruncationHIS disc =
					new UMDAMixtureTruncationHIS(10, 10, cf);
			Optimizer opt = disc;
			for (long i = 1; i < 10000; i++) {
				try {
					opt.optimize();
				} catch (ConvergenceException e) {
					break;
				}
				// if(cf.getBestValue()<=-400)break;
				if (i % 100 == 0) {
					System.out.println("----------------" + i
							+ "----------------");
					System.out.printf("best( %d ) %s \n", i,
							disc.getBest());
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
