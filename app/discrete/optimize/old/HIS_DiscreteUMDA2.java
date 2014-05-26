package app.discrete.optimize.old;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import optimize.function.CostFunction;
import optimize.function.RecordFunction;
import optimize.his.old.HierarchicalIS2;

import app.discrete.function.TwoMax;
import app.discrete.sampler.DiscreteUniformSampler;
import app.discrete.sampler.UMDADiscreteSampler;
import app.discrete.util.DVector;

import statistics.sampler.RecordSampler;


public class HIS_DiscreteUMDA2 extends HierarchicalIS2<DVector> {
	private PrintStream[] ps;
	private static int dim=100;
	public HIS_DiscreteUMDA2(int sample_num, int level, double alpha,
			CostFunction<DVector> cf) {
		super(sample_num, level, alpha, cf);
		ps=new PrintStream[level];
		for(int i=0;i<level;i++){
			try {
				ps[i] = new PrintStream(new FileOutputStream("test"+i+".dat"));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				System.exit(0);
			}
		}
		for (int i = 0; i < level; i++) {
			if(i==0)samplerSet[i] = new RecordSampler<DVector>(new DiscreteUniformSampler(cf.dim(),1),ps[i],new TwoMax(dim));
			else samplerSet[i] = new RecordSampler<DVector>(new UMDADiscreteSampler(cf.dim()),ps[i],cf);
		}
		init();
	}

	public static void main(String args[]) {
		try {
			PrintStream ps = new PrintStream(new FileOutputStream("test.dat"));
			CostFunction<DVector> cf=new RecordFunction<DVector>(new TwoMax(dim),ps);
			HIS_DiscreteUMDA2 disc = new HIS_DiscreteUMDA2(40, 5, 0.5,
					cf);
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
