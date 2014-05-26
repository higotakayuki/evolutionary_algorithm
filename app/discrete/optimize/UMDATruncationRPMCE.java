package app.discrete.optimize;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import optimize.function.CostFunction;
import optimize.function.RecordFunction;
import optimize.population.TruncationRPM;
import statistics.sampler.Sampler;
import app.discrete.function.Ising2D;
import app.discrete.function.NoiseFunction;
import app.discrete.function.OneMax;
import app.discrete.sampler.UMDADiscreteSampler;
import app.discrete.util.DVector;
import experiment.AnnealingTest;

public class UMDATruncationRPMCE extends TruncationRPM<DVector> {

	public UMDATruncationRPMCE(CostFunction<DVector> cf, int num_population,
			int sample_num, double cutoff) {
		super(cf, num_population, sample_num, cutoff);
	}

	@Override
	protected Sampler<DVector> getInitialSampler() {
		return new UMDADiscreteSampler(cf.dim(), 1);
	}

	public static int dim = 100;
	public static int population = 10000;
	public static int samples = 100;
	public static double cutoff = 0.1;
//	public static double cutoff=1-Math.exp(-100d*samples/1e6);

	
	public static void main(String args[]){
		test1(null);
	}
	
	public static void test_simple(String args[]){
		for (int c = 0; c < 1; c++) {
			System.out.println("counter=" + c);
			try {
				PrintStream ps = new PrintStream(new FileOutputStream(
						"test.dat"));
				RecordFunction<DVector> cf = new RecordFunction<DVector>(
						new OneMax(dim), ps);
				UMDATruncationRPMCE umda = new UMDATruncationRPMCE(cf,population,samples,cutoff);
				AnnealingTest.test(umda, cf);

				ps.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		System.out.println("end");
		System.exit(0);
	}
	
	public static void test1(String args[]) {
		System.out.println("#" + population + "," + samples + "," + cutoff
				+ "(" + Math.log(1-cutoff) + ")");
		for (int c = 0; c < 1; c++) {
			// System.out.println("c=" + c);
			// try {
			// PrintStream ps = new PrintStream(new FileOutputStream(
			// "discpoptruncanneal.dat"));
			//PrintStream ps = System.out;
			PrintStream ps=null;
//			CostFunction<DVector> base = new OneMax(dim);
//			CostFunction<DVector> base = new Ising1D(dim);
			CostFunction<DVector> base = new Ising2D(20, 20);
			base=new NoiseFunction(base);
			RecordFunction<DVector> cf = new RecordFunction<DVector>(base, ps);
			UMDATruncationRPMCE opt = new UMDATruncationRPMCE(cf, population,
					samples, cutoff);
			for (long i = 0;; i++) {
				opt.optimize();
				if (opt.getVariance() < 1e-20)
					break;
				if (cf.getBestValue() <= base.getMinimum())
					break;
				if (false && i % 10 == 0) {
					System.out.println("----------------" + i + "("
							+ cf.getCount() + ")" + "----------------");
					System.out.println(opt);
					System.out.println(cf.getBestValue());
				}if(false && i%10==0){
					if(Double.isInfinite(opt.getLogZ())){
						System.out.println(i+" "+0+" "+opt.getPopulationSize());	
					}else{
					System.out.println(i+" "+opt.getLogZ()+" "+opt.getPopulationSize());
					}
				}
			}
			if(ps!=null)ps.close();
			// System.out.println(opt);
			System.out.print("#"+population+" "+samples+" "+cutoff+" ");
			System.out.println(""+cf.getBestValue() + "  " + cf.getCount());
			// } catch (FileNotFoundException e) {
			// e.printStackTrace();
			// }
			// }
			// System.out.println("end pop");
//			System.exit(0);
		}
	}

}
