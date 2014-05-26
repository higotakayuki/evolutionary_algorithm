package app.discrete.optimize.old;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import app.discrete.function.TwoMax;
import app.discrete.sampler.UMDADiscreteSampler;
import app.discrete.util.DVector;

import optimize.function.CostFunction;
import optimize.function.RecordFunction;
import optimize.optimizer.BoltzCE;


public class UMDABoltzCE extends BoltzCE<DVector>{
	public UMDABoltzCE(int sample_num, CostFunction<DVector> cf) {
		super(sample_num, cf);
		super.sampler = new UMDADiscreteSampler(cf.dim(),1);
	}

	public static int dim = 100;

	public static void main(String args[]) {
		try {
			PrintStream ps = new PrintStream(new FileOutputStream("test.dat"));
			CostFunction<DVector> cf = new RecordFunction<DVector>(
					new TwoMax(dim), ps);
			UMDABoltzCE ceaumda = new UMDABoltzCE(20, cf);
			for (long i = 0; i < 10; i++) {
				System.out.println("----------------" + i + "----------------");
				ceaumda.optimize();
				System.out.println(ceaumda);
			}
			ps.close();

			System.out.println("end");
			System.exit(0);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
