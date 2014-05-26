package app.discrete.optimize;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import optimize.function.CostFunction;
import optimize.function.RecordFunction;
import optimize.hisrpm.TruncationHISRPM;
import statistics.sampler.RecordSampler;
import app.discrete.function.Ising2D;
import app.discrete.sampler.UMDADiscreteSampler;
import app.discrete.util.DVector;

public class UMDATruncationHISRPM extends TruncationHISRPM<DVector> {
	private PrintStream[] ps;

	private static int dim = 400;

	public UMDATruncationHISRPM(int pop_num, int sample_num, int level,
			CostFunction<DVector> cf) {
		super(pop_num, sample_num, level, cf);
		if (true) {
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
		}
		for (int i = 0; i < level; i++) {
			 RecordSampler<DVector> recs = new RecordSampler<DVector>(
			 new UMDADiscreteSampler(cf.dim(), 1), ps[i], cf);
						
//			 if(i==level-1)recs.PRINT=true;
			 samplerSet[i] = recs;
//			samplerSet[i] = new UMDADiscreteSampler(cf.dim(), 0.5);
		}
	}

	public static void main(String args[]) {
		try {
			PrintStream ps = new PrintStream(new FileOutputStream("test.dat"));
//			RecordFunction<DVector> cf = new RecordFunction<DVector>(
//					new OneMax(400), ps);
			 RecordFunction<DVector> cf = new RecordFunction<DVector>(
			 new Ising2D(20, 20), ps);
			TruncationHISRPM disc = new UMDATruncationHISRPM(100, 100, 10, cf);
			for (long i = 1;; i++) {
				disc.optimize();
				// if (cf.getCount() > 15000) break;
				if (cf.getBestValue() <= cf.getMinimum()) break;
				if (false && i % 1 == 0) {
					System.out.println("----------------" + i
							+ "----------------");
					System.out.printf("best( %d ) %s \n", i, disc.getBest());
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
