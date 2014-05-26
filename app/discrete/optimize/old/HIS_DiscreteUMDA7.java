package app.discrete.optimize.old;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import optimize.function.CostFunction;
import optimize.function.RecordFunction;
import optimize.his.old.HierarchicalIS7;

import app.discrete.function.TwoMax;
import app.discrete.sampler.UMDADiscreteSampler;
import app.discrete.util.DVector;

import statistics.sampler.RecordSampler;


public class HIS_DiscreteUMDA7 extends HierarchicalIS7<DVector> {
	private PrintStream[] ps;

	private static int dim = 100;

	public HIS_DiscreteUMDA7(int sample_num, int level, CostFunction<DVector> cf) {
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
					cf.dim(),1), ps[i], cf);
		}
		init();
	}

	public static void main(String args[]) {
		try {
			PrintStream ps = new PrintStream(new FileOutputStream("test.dat"));
			CostFunction<DVector> cf = new RecordFunction<DVector>(
					new TwoMax(dim), ps);
			HIS_DiscreteUMDA7 disc = new HIS_DiscreteUMDA7(10, 10, cf);
			for (long i = 1; i < 1000; i++) {
				//System.out.println("----------------" + i + "----------------");
				disc.optimize();
				//System.out.println(disc);
				if(i%10==0){
					System.out.printf("best( %d ) %s \n",i,disc.getBest());
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
