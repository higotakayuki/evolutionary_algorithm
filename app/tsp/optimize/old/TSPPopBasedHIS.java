package app.tsp.optimize.old;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import optimize.function.CostFunction;
import optimize.function.RecordFunction;
import optimize.his.old.TruncationBasedHIS;

import app.tsp.Sequence;
import app.tsp.function.TSPFunction;
import app.tsp.sampler.MarkovSampler;

import statistics.sampler.RecordSampler;

public class TSPPopBasedHIS extends TruncationBasedHIS<Sequence>{

	private PrintStream[] ps;
	
	public TSPPopBasedHIS(int sample_num, int level, CostFunction<Sequence> cf) {
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
					cf.dim(),cf.dim(),1), ps[i], cf);
		}
		init();
	}
	
	public static void main(String args[]) {
		try {
			TSPFunction tspf = TSPFunction.createFromFile(new File(
					"./tsp/problems/1.tsp"));
			System.out.println(tspf);
			PrintStream ps = new PrintStream(new FileOutputStream("test.dat"));
			CostFunction<Sequence> cf= new RecordFunction<Sequence>(
					tspf, ps
					);
			
			TSPPopBasedHIS tspo=new TSPPopBasedHIS(100,10,cf);
			for (long i = 0; i < 10000; i++) {
				tspo.optimize();
				if(i%10!=0)continue;
				//System.out.println("----------------" + i + "----------------");
				//System.out.println(tspo);
				//System.out.println("best:"+tspo.getBest());
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("end");
	}

}
