package app.discrete.optimize;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import optimize.function.CostFunction;
import optimize.function.RecordFunction;
import optimize.population.old.BoltzRPM;
import statistics.sampler.Sampler;
import app.discrete.function.Ising1D;
import app.discrete.sampler.UMDADiscreteSampler;
import app.discrete.util.DVector;

public class UMDABoltzRPM extends BoltzRPM<DVector> {

	public UMDABoltzRPM(CostFunction cf, int num_population, int sample_num,
			double cutoff) {
		super(cf, num_population, sample_num, cutoff);
	}

	@Override
	protected Sampler<DVector> getInitialSampler() {
		return new UMDADiscreteSampler(cf.dim(), 1);
	}

	public static int dim = 400;

	public static int population = 200;

	public static int samples = 200;

	public static double cutoff = 0.1;

	public static void main(String args[]) {
		for (int cut = 0; cut < 1; cut++) {
			// switch (cut) {
			// case 0:
			// cutoff = 0.07;
			// break;
			// case 1:
			// cutoff = 0.05;
			// break;
			// case 2:
			// cutoff = 0.03;
			// break;
			// case 3:
			// cutoff = 0.01;
			// break;
			// case 4:
			// cutoff = 0.001;
			// break;
			// }
			cut = 5;
			switch (cut) {
			case 0:
				cutoff = 0.4;
				break;
			case 1:
				cutoff = 0.2;
				break;
			case 2:
				cutoff = 0.1;
				break;
			case 3:
				cutoff = 0.05;
				break;
			default:
				break;
			}
			System.out.println("#" + population + "," + samples + "," + cutoff
					+ " " + Math.log(1 - cutoff));
			for (int c = 0; c < 1; c++) {
				// System.out.println("c=" + c);
				try {
					PrintStream ps = new PrintStream(new FileOutputStream(
							"test.dat"));
//					 CostFunction<DVector> base = new OneMax(dim);
					 CostFunction<DVector> base = new Ising1D(dim);
//					CostFunction<DVector> base = new Ising2D(20, 20);
					RecordFunction<DVector> cf = new RecordFunction<DVector>(
							base, ps);
					UMDABoltzRPM opt = new UMDABoltzRPM(cf, population,
							samples, cutoff);
					for (long i = 0; i < 100000000; i++) {
						opt.optimize();
						if (opt.getVariance() < 1e-20) break;
						if (cf.getBestValue() <= cf.getMinimum()) break;
						if (false && i % 10 == 0) {
							System.out.println("----------------" + i + "("
									+ cf.getCount() + ")" + "----------------");
							System.out.println(opt);
							System.out.println(cf.getBestValue());
						}
					}
					ps.close();
					// System.out.println(opt);
					System.out.println(cf.getBestValue() + " , "
							+ cf.getCount());
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
			// System.out.println("end pop");
			// System.exit(0);
		}
	}

}
