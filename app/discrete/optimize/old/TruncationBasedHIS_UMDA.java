package app.discrete.optimize.old;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import optimize.function.CostFunction;
import optimize.function.RecordFunction;
import optimize.his.old.TruncationBasedHIS;

import app.discrete.function.TwoMax;
import app.discrete.sampler.UMDADiscreteSampler;
import app.discrete.util.DVector;

import statistics.sampler.RecordSampler;


public class TruncationBasedHIS_UMDA extends TruncationBasedHIS<DVector> {
	private PrintStream[] ps;

	private static int dim = 100;

	public TruncationBasedHIS_UMDA(int sample_num, int level, CostFunction<DVector> cf) {
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
					cf.dim(),0.5), ps[i], cf);
		}
		init();
	}

	public static void main(String args[]) {
		try {
			PrintStream ps = new PrintStream(new FileOutputStream("test.dat"));
			CostFunction<DVector> cf = new RecordFunction<DVector>(
					new TwoMax(dim), ps);
			TruncationBasedHIS_UMDA disc = new TruncationBasedHIS_UMDA(20, 10, cf);
			for (long i = 1; i < 1000; i++) {
				disc.optimize();
				if(i%100==0){
					System.out.println("----------------" + i + "----------------");
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
