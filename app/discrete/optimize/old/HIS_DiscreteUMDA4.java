package app.discrete.optimize.old;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import optimize.function.CostFunction;
import optimize.function.RecordFunction;
import optimize.his.old.HierarchicalIS4;

import app.discrete.function.TwoMax;
import app.discrete.sampler.UMDADiscreteSampler;
import app.discrete.util.DVector;

import statistics.sampler.RecordSampler;


public class HIS_DiscreteUMDA4 extends HierarchicalIS4<DVector> {
	private PrintStream[] ps;

	private static int dim = 100;

	public HIS_DiscreteUMDA4(int sample_num, int level, CostFunction<DVector> cf) {
		super(sample_num, level, cf);
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
			samplerSet[i] = new RecordSampler<DVector>(new UMDADiscreteSampler(
					cf.dim()), ps[i], cf);
		}
		init();
	}

	public static void main(String args[]) {
		try {
			PrintStream ps = new PrintStream(new FileOutputStream("test.dat"));
			CostFunction<DVector> cf = new RecordFunction<DVector>(
					new TwoMax(dim), ps);
			HIS_DiscreteUMDA4 disc = new HIS_DiscreteUMDA4(40, 5, cf);
			for (long i = 0; i < 1000; i++) {
				System.out.println("----------------" + i + "----------------");
				disc.optimize();
				System.out.println(disc);
			}
			ps.close();

			System.out.println("end");
			System.exit(0);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

}
