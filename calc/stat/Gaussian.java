package calc.stat;

import java.util.Random;

import calc.la.ColumnVector;
import calc.la.GaussElimination;
import calc.la.Matrix;
import calc.la.RowVector;
import calc.la.Vector;

public class Gaussian implements ProbabilityDistribution {
	private int dim;

	private Vector mean;

	private Matrix covariance;

	private double[][] coefficienceMatrix = null;

	private static final Random rand = new Random();

	public Gaussian(Vector mean, Matrix covariance) {
		dim = mean.dim();
		this.mean = (Vector) mean.clone();
		this.covariance = covariance;
	}

	public Vector generateSample() {
		if (coefficienceMatrix == null)
			coefficienceMatrix = calcCoefficienceMatrix(covariance.getElement());
		double[] ans = new double[coefficienceMatrix.length];
		double[] pVariable = new double[coefficienceMatrix.length];
		for (int i = 0; i < coefficienceMatrix.length; i++) {
			pVariable[i] = rand.nextGaussian();
			ans[i] = mean.getValue(i);
			for (int j = 0; j < coefficienceMatrix[i].length; j++) {
				ans[i] += coefficienceMatrix[i][j] * pVariable[j];
				if (Double.isNaN(ans[i])) {
					System.out.println("gene samp");
				}
			}
		}
		return new Vector(ans);
	}

	public Vector[] generateSamples(int num) {
		Vector[] ans = new Vector[num];
		for (int i = 0; i < num; i++) {
			ans[i] = generateSample();
		}
		return ans;
	}

	private double[][] calcCoefficienceMatrix(double[][] covariance) {
		boolean singularFlag = false;
		double[][] coefficience = new double[covariance.length][];
		for (int i = 0; i < covariance.length; i++) {
			coefficience[i] = new double[i + 1];
			for (int j = 0; j <= i; j++) {
				if (singularFlag) {
					coefficience[i][j] = 0;
				} else {
					double sum = covariance[i][j];
					for (int k = 0; k < j; k++) {
						sum -= coefficience[i][k] * coefficience[j][k];
					}
					if (i == j) {
						if (sum < 1e-20) {
							sum = 1e-20;// *********************
							singularFlag = true;
							System.out.println("singular");
						}
						sum = Math.sqrt(sum);
					} else {
						sum /= coefficience[j][j];
					}
					coefficience[i][j] = sum;
					if (Double.isNaN(sum)) {
						coefficience[i][j] = 0;
						System.out.println("coefficient");
					}
				}
			}
		}
		return coefficience;
	}

	public double normalizedProbability(Vector x) {
		return Math.exp(logNormalizedProbability(x));
	}

	public double logNormalizedProbability(Vector x) {
		x = x.minus(mean);
		GaussElimination gelimination = new GaussElimination(covariance,
				new ColumnVector(x));
		double dist = x.dotProduct(gelimination.getSolution());
		// double sum = -0.5 * dist - 0.5*dim * Math.log(2 * Math.PI) - 0.5
		// * covariance.logAbsDeterminant();
		double sum = -0.5
				* (dist + dim * Math.log(2 * Math.PI) + covariance
						.logAbsDeterminant());
		if (Double.isNaN(sum)) {
			System.out.println("gene det");
			System.out.printf("%f", covariance.logAbsDeterminant());
		}
		return sum;
	}

	public Matrix getCovariance() {
		return covariance;
	}

	public void setCovariance(Matrix covariance) {
		this.covariance = covariance;
		coefficienceMatrix = null;
	}

	public Vector getMean() {
		return mean;
	}

	public void setMean(Vector mean) {
		this.mean = mean;
	}

	public int getDim() {
		return dim;
	}

	@Override
	public String toString() {
		String mean = "mean:\n" + this.mean + "\n";
		// String cov="cov:\n"+this.covariance+"\n";
		String cov = "";
		return mean + cov;
	}

	public static void main(String args[]) {
		double[] t = { 1, 1, 1 };
		double[] m = { 1, 1, 1 };
		double[][] c = { { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } };
		Gaussian gauss = new Gaussian(new Vector(m), new Matrix(c));
		System.out.println("mean::"
				+ gauss.normalizedProbability(new Vector(t)));
		for (int i = 0; i < 100; i++) {
			System.out.println(gauss.normalizedProbability(gauss
					.generateSample()));
		}
	}

	public static Vector average(Vector[] samples) {
		Vector avg = null;
		{
			for (Vector t : samples) {
				if (avg == null)
					avg = t;
				else
					avg = avg.plus(t);
			}
			avg = avg.multiply(1d / samples.length);
		}
		return avg;
	}

	public static Matrix covariance(Vector[] samples, Vector average) {
		RowVector[] nsamples = new RowVector[samples.length];
		{
			int i = 0;
			for (Vector t : samples) {
				nsamples[i++] = new RowVector(t.minus(average));
			}
		}

		Matrix m = new Matrix(nsamples);
		Matrix cov = Matrix.operate(m.transpose(), m);
		cov = cov.multiply(1d / (samples.length));
		return cov;
	}

	public static Gaussian gauss(Vector[] samples) {
		Vector avg = average(samples);
		Matrix cov = covariance(samples, avg);
		return new Gaussian(avg, cov);
	}
}
