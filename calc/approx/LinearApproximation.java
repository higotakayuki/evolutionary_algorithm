package calc.approx;

import java.util.Random;

import calc.la.ColumnVector;
import calc.la.GeneralizedGaussElimination;
import calc.la.Matrix;
import calc.la.RowVector;
import calc.la.Vector;

public class LinearApproximation {
	private Vector u = null;

	public LinearApproximation(Sample[] samples) {
		RowVector[] rowv = new RowVector[samples.length];
		double[] value = new double[samples.length];
		for (int i = 0; i < samples.length; i++) {
			rowv[i] = new RowVector(samples[i].getVector());
			value[i] = samples[i].getValue();
		}
		Matrix m = new Matrix(rowv);
		ColumnVector cv = new ColumnVector(value);
		GeneralizedGaussElimination gge = new GeneralizedGaussElimination(m, cv);
		this.u = gge.getSolution();
	}

	public double evaluate(Vector v) {
		return u.dotProduct(v);
	}

	public Vector getVector() {
		return (Vector) this.u.clone();
	}

	public static void main(String args[]) {
		for (double k = 1; k < 100000; k *= 1.3) {
			Random rand = new Random();
			double[] e = { 4, 3, 9, 5, 1, 4, 2, 5, 4, 5, 2, 3, 4, 1, 7, 4, 9,
					7, 4, 3, 0, 2, 5 };
			Vector u = new Vector(e);
			int size = (int) (e.length * k);
			Sample[] samples = new Sample[size];
			for (int i = 0; i < samples.length; i++) {
				double[] temp = new double[e.length];
				for (int j = 0; j < e.length; j++) {
					temp[j] = rand.nextDouble();
				}
				Vector v = new Vector(temp);
				samples[i] = new Sample(v, v.dotProduct(u)
						+ rand.nextGaussian());
			}
			LinearApproximation la = new LinearApproximation(samples);
			System.out.println(size + " "
					+ la.getVector().minus(u).squaredNorm());
		}
	}

}
