package app.tsp.optimize.old;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import optimize.function.CostFunction;
import optimize.function.RecordFunction;
import optimize.his.old.HierarchicalIS3;

import app.tsp.Sequence;
import app.tsp.function.TSPFunction;
import app.tsp.sampler.MarkovSampler;

import statistics.sampler.RecordSampler;

public class TSPOptimization extends HierarchicalIS3<Sequence> {

	private PrintStream[] ps;
	
	public TSPOptimization(int sample_num, int level, CostFunction<Sequence> cf) {
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
			samplerSet[i] = new RecordSampler<Sequence>(new MarkovSampler(
					cf.dim()), ps[i], cf);
		}
		init();
	}

	public static void main(String args[]) {
		try {
			TSPFunction tspf = TSPFunction.createFromFile(new File(
					"./tsp/problems/2.tsp"));
			System.out.println(tspf);
			PrintStream ps = new PrintStream(new FileOutputStream("test.dat"));
			CostFunction<Sequence> cf= new RecordFunction<Sequence>(
					tspf, ps
					);
			
			TSPOptimization tspo=new TSPOptimization(40,5,cf);
			for (long i = 0; i < 1000; i++) {
				System.out.println("----------------" + i + "----------------");
				tspo.optimize();
				System.out.println(tspo);
				System.out.println(tspo.searchBest());
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		System.out.println("end");
	}
}
