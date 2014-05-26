package app.discrete.optimize;

import optimize.function.CostFunction;
import optimize.function.RecordFunction;
import optimize.optimizer.TruncationRSEA;
import statistics.sampler.Sampler;
import app.discrete.function.Ising2D;
import app.discrete.function.NoiseFunction;
import app.discrete.sampler.UMDADiscreteSampler;
import app.discrete.util.DVector;
import experiment.AnnealingTest;

public class UMDA_RSEA extends TruncationRSEA<DVector> {
	public UMDA_RSEA(int sample_num, double cutoff, double lambda1,
			double lambda2, CostFunction<DVector> cf) {
		super(sample_num, cutoff, lambda1, lambda2, cf);
	}

	// public static int dim = 100;
	public static int samples = 1000;
	public static double cutoff = 0.1;
	// public static double lambda = 0.2;
	// public static CostFunction<DVector> base=new OneMax(400);
	// public static CostFunction<DVector> base = new Ising1D(400);

	public static CostFunction<DVector> base = new Ising2D(20, 20);

	public static void main(String args[]) {
		base = new NoiseFunction(base);
		double lambda1 = 1;// convergence
		double lambda2 = 1;// estimation
		try {
			RecordFunction<DVector> cf =
					new RecordFunction<DVector>(base, null);
			UMDA_RSEA umda =
					new UMDA_RSEA(samples, cutoff, lambda1, lambda2,
							cf);
			// AnnealingTest.print=true;
			AnnealingTest.test(umda, cf);
			System.out.println(cf.getBestValue());
			// ps.close();
			// System.out.println("end:" +
			// umda.getClass()+" "+cf.getBestValue());
			// } catch (FileNotFoundException e) {
			// e.printStackTrace();
		} catch (Exception e) {}
	}

	// public static void main(String args[]) {
	// int num = 1;
	// for (int i = 0; i <= 100; i++) {
	// for (int j = 0; j <= 100; j++) {
	// double lambda1 = 0.01 * i;
	// double lambda2 = 0.01 * j;
	//
	// double avg = 0;
	// for (int c = 0; c < num; c++) {
	//
	// // System.out.println("counter=" + c);
	// try {
	// // PrintStream ps =
	// // new PrintStream(new FileOutputStream(
	// // "test_l03_c01.dat"));
	// RecordFunction<DVector> cf =
	// new RecordFunction<DVector>(base,
	// null);
	// UMB_RSEA umda =
	// new UMB_RSEA(samples, cutoff,
	// lambda1, lambda2, cf);
	// AnnealingTest.test(umda, cf);
	// avg += cf.getBestValue();
	// // ps.close();
	// // System.out.println("end:" +
	// // umda.getClass()+" "+cf.getBestValue());
	// // } catch (FileNotFoundException e) {
	// // e.printStackTrace();
	// } catch (Exception e) {}
	// }
	// avg /= num;
	// System.out.print(avg + " ");
	// }
	// System.out.println();
	// }
	// System.exit(0);
	// }

	@Override
	protected Sampler<DVector> getInitialSampler() {
		return new UMDADiscreteSampler(cf.dim(), 1);
	}

}
