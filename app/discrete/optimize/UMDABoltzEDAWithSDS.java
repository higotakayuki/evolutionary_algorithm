package app.discrete.optimize;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import app.discrete.function.TwoMax;
import app.discrete.sampler.UMDADiscreteSampler;
import app.discrete.util.DVector;

import optimize.function.CostFunction;
import optimize.function.RecordFunction;
import optimize.optimizer.BoltzEDA_SDS;



public class UMDABoltzEDAWithSDS extends BoltzEDA_SDS<DVector> {

	public UMDABoltzEDAWithSDS(int sample_num, double c, CostFunction<DVector> cf) {
		super(sample_num,c, cf);
		super.sampler = new UMDADiscreteSampler(cf.dim(), 0.1);
	}

	public static int dim = 100;

	public static void main(String args[]) {
		try {
			PrintStream ps = new PrintStream(new FileOutputStream("test.dat"));
			CostFunction<DVector> cf = new RecordFunction<DVector>(
					new TwoMax(dim), ps);
			UMDABoltzEDAWithSDS bumda = new UMDABoltzEDAWithSDS(10, 0, cf);
			for (long i = 0; i < 1000; i++) {

				bumda.optimize();
				if (i % 10 == 0) {
					System.out.println("----------------" + i
							+ "----------------");
					System.out.println(bumda);
				}
			}
			ps.close();

			System.out.println("end boltz");
			System.exit(0);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

}
