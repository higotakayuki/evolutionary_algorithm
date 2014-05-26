package app.continuous.optimize;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Random;

import optimize.function.CostFunction;
import optimize.function.RecordFunction;
import optimize.ls.LocalSearch;
import app.continuous.function.Rosenbrock;
import app.continuous.util.CVector;

public class ContLocalSearch extends LocalSearch<CVector> {

	private CVector currentS = null;
	private double distance;
	private double value;
	private Random r = new Random();

	private int M;

	public ContLocalSearch(CostFunction<CVector> cf, int M) {
		super(cf);
		double[] e = new double[cf.dim()];
		for (int i = 0; i < e.length; i++) {
			e[i] = 10;
		}
		currentS = new CVector(e);
		distance = 10;
		value = cf.eval(currentS);
		this.M = M;
	}

	@Override
	public void optimize() {
		// random samples
		CVector[] vectors = new CVector[M];
		double[] values = new double[M];

		int besti = 0;
		for (int i = 0; i < M; i++) {
			CVector random = generateGaussianSample();
			random = random.multiply(distance);
			vectors[i] = currentS.add(random);
			values[i] = cf.eval(vectors[i]);
			if (i != 0 && values[i] < values[besti]) {
				besti = i;
			}
		}
		// update
		if ( values[besti]<value) {
			CVector diff = currentS.minus(vectors[besti]);
			double squaredDist = diff.dotProduct(diff);
			this.distance = Math.sqrt(squaredDist);
			currentS = vectors[besti];
			value=values[besti];
			if (distance == 0) this.isConverged = true;
		}

		
	}

	public CVector generateGaussianSample() {
		int d = cf.dim();
		double[] elements = new double[d];
		for (int i = 0; i < d; i++) {
			elements[i] = r.nextGaussian();
		}
		return new CVector(elements);
	}

	public static void main(String args[]) {
		try {
			PrintStream ps =
					new PrintStream(new FileOutputStream(
							"localsearch.txt"));
			RecordFunction<CVector> cf =
					new RecordFunction<CVector>(new Rosenbrock(10),
							System.out);

			for (int i = 0; i < 1000; i++) {
				ContLocalSearch cls = new ContLocalSearch(cf, 100);

				while (true) {
					cls.optimize();
					if (cls.isConverged) break;
				}
			}

			System.out.println(cf.getCount() + " "
					+ cf.getBestValue());
			ps.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

}
