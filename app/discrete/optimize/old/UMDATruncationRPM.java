package app.discrete.optimize.old;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import app.discrete.function.Ising2D;
import app.discrete.sampler.UMDADiscreteSampler;
import app.discrete.util.DVector;

import optimize.function.CostFunction;
import optimize.function.RecordFunction;
import optimize.population.old.TruncationRPM;

import statistics.sampler.Sampler;

public class UMDATruncationRPM extends TruncationRPM<DVector> {

	public UMDATruncationRPM(CostFunction cf, int num_population,
			int sample_num, double cutoff) {
		super(cf, num_population, sample_num, cutoff);
	}

	@Override
	protected Sampler<DVector> getInitialSampler() {
		return new UMDADiscreteSampler(cf.dim(), 1);
	}

	public static int dim = 400;
	public static int population=200;
	public static int samples=200;
	public static double cutoff=0.01;
	
	public static void main(String args[]) {
		System.out.println(population+","+samples+","+cutoff);
		for (int c = 0; c < 5; c++) {
			//System.out.println("c=" + c);
			try {
				PrintStream ps = new PrintStream(new FileOutputStream(
						"discpoptruncanneal.dat"));
				//CostFunction<DVector> base = new OneMax(dim);
				CostFunction<DVector> base = new Ising2D(20, 20);
				RecordFunction<DVector> cf = new RecordFunction<DVector>(base, ps);
				UMDATruncationRPM opt = new UMDATruncationRPM(cf,
						population, samples, cutoff
						);
				for (long i = 0; i < 100000; i++) {
					opt.optimize();
					if(opt.getVariance()<1e-20)break;
					if (false && i % 10 == 0) {
						System.out.println("----------------" + i
								+ "("+cf.getCount()+")"+"----------------");
						System.out.println(opt);
						System.out.println(cf.getBestValue());
					}
				}
				ps.close();
				//System.out.println(opt);
				System.out.println(cf.getBestValue()+" , "+cf.getCount());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		//System.out.println("end pop");
		System.exit(0);
	}

}
