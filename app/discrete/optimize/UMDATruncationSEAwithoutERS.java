package app.discrete.optimize;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import optimize.function.CostFunction;
import optimize.function.RecordFunction;
import optimize.optimizer.TruncationSEAwithoutERS;
import app.discrete.function.Ising1D;
import app.discrete.sampler.UMDADiscreteSampler;
import app.discrete.util.DVector;
import experiment.AnnealingTest;
import experiment.util.Result;

public class UMDATruncationSEAwithoutERS extends TruncationSEAwithoutERS<DVector> {
	public UMDATruncationSEAwithoutERS(int sample_num, double cutoff, double lambda,
			CostFunction<DVector> cf) {
		super(sample_num, cutoff, lambda, cf);
		super.sampler = new UMDADiscreteSampler(cf.dim(), 1);
	}

	public static int dim = 400;

	public static void main(String args[]) {
		for (int c = 0; c < 1; c++) {
			System.out.println("c=" + c);
			try {
				PrintStream ps = new PrintStream(new FileOutputStream(
						"test.dat"));
				RecordFunction<DVector> cf = new RecordFunction<DVector>(
						new Ising1D(dim), ps);
				UMDATruncationSEAwithoutERS umda = new UMDATruncationSEAwithoutERS(100, 0.3,0.1, cf);
				AnnealingTest.test(umda,cf);
				
				ps.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		System.out.println("end");
		System.exit(0);
	}

}
