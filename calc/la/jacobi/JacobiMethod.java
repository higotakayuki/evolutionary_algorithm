package calc.la.jacobi;

import calc.la.Matrix;

public class JacobiMethod {

	private static final boolean DEBUG = true;

	private final boolean CORRECT;

	private SymmetricMatrix targetMatrix;

	// double[][] eigenVectorArray;
	private double[][] eigenVector;

	private final Matrix old;

	public JacobiMethod(Matrix m) {

		this.targetMatrix = new SymmetricMatrix(m);
		eigenVector = new double[targetMatrix.getDim()][];
		for (int i = 0; i < targetMatrix.getDim(); i++) {
			eigenVector[i] = new double[targetMatrix.getDim()];
			for (int j = 0; j < targetMatrix.getDim(); j++) {
				if (i == j) {
					eigenVector[i][j] = 1;
				} else {// i!=j
					eigenVector[i][j] = 0;
				}
			}
		}

		this.CORRECT = true;
		if (CORRECT || DEBUG) {
			old = targetMatrix.toMatrix();
		} else {
			old = null;
		}
	}

	private final double MIN_THETA = 1e-40;

	private void rotate() {
		int maxi = 0;
		int maxj = 0;
		double maxe = 0;
		// to find max element
		for (int i = 0; i < targetMatrix.getDim(); i++) {
			for (int j = 0; j < targetMatrix.getDim(); j++) {
				if (i == j) {
					break;
				} else {
					if (maxe < Math.abs(targetMatrix.getValue(i, j))) {
						maxe = Math.abs(targetMatrix.getValue(i, j));
						maxi = i;
						maxj = j;
					}
				}
			}
		}
		if (maxi == 0 && maxj == 0)
			return;
		else {
			if (DEBUG) {
				System.out.println("begin rotate");
				System.out.println(targetMatrix);
			}
			rotate(maxi, maxj);
		}
	}

	private void rotate(int ti, int tj) {
		assert ti > tj : "does not satisfy following condition: " + ti + ">"
				+ tj;
		double theta = targetMatrix.getValue(tj, tj)
				- targetMatrix.getValue(ti, ti);
		theta /= 2 * targetMatrix.getValue(ti, tj);
		double t = 0;
		if (theta > MIN_THETA)
			t = 1 / (2 * theta);
		else {
			if (theta > 0)
				t = 1;
			else
				t = -1;
			t /= Math.abs(theta) + Math.sqrt(theta * theta + 1);
		}

		// ////////////////////////////////////
		double c = 1d / Math.sqrt(t * t + 1);
		double s = t * c;
		// /////////////////////////////////////

		// targetMatrix
		this.eigenVector = unitaryTransformEigenVector(ti, tj, c, s);
		Matrix eigenMatrix = new Matrix(eigenVector);
		if (CORRECT) {
			this.targetMatrix = new SymmetricMatrix(Matrix.operate((Matrix
					.operate(eigenMatrix.transpose(), old)), eigenMatrix));
		} else {
			this.targetMatrix = unitaryTransformTarget(ti, tj, t, c, s);
		}

		if (DEBUG) {
			System.out.println("eigen vector matrix");
			System.out.println(eigenMatrix);
			System.out.println("are the eigen vecors orthonormal?");
			System.out.println(eigenMatrix.operateFromLeft(eigenMatrix
					.transpose()));

			System.out.println("check validity");
			System.out
					.println("calc the first matrix from eigenVector and dig matrix");
			System.out.println(Matrix.operate((Matrix.operate(eigenMatrix,
					targetMatrix.toMatrix())), eigenMatrix.transpose()));
			System.out.println(old);
			System.out.println();
			System.out.println("calc the target matrix from the first matrix");
			System.out.println(Matrix.operate((Matrix.operate(eigenMatrix
					.transpose(), old)), eigenMatrix));
			System.out.println(targetMatrix);

			System.out.println("option");
			System.out.println(eigenMatrix.transpose().operateFromLeft(old));
			System.out.println();
		}

	}

	private double[][] unitaryTransformEigenVector(int ti, int tj, double c,
			double s) {
		double[][] newEigenVector = copyArray(eigenVector);
		// ti
		for (int i = 0; i < targetMatrix.getDim(); i++) {
			newEigenVector[i][ti] = c * eigenVector[i][ti] - s
					* eigenVector[i][tj];
		}
		// tj
		for (int i = 0; i < targetMatrix.getDim(); i++) {
			newEigenVector[i][tj] = s * eigenVector[i][ti] + c
					* eigenVector[i][tj];
		}
		return newEigenVector;
	}

	private SymmetricMatrix unitaryTransformTarget(int ti, int tj, double t,
			double c, double s) {
		double tau = s / (1 + c);
		SymmetricMatrix newTargetArray = targetMatrix.clone();
		// carry out unitary operator
		for (int r = 0; r < targetMatrix.getDim(); r++) {
			// update ti
			double val = 0;
			{
				if (r == tj)
					val = 0;
				else if (r == ti)
					val = targetMatrix.getValue(ti, ti) - t
							* targetMatrix.getValue(ti, tj);
				else
					val = targetMatrix.getValue(r, ti)
							- s
							* (targetMatrix.getValue(r, tj) + tau
									* targetMatrix.getValue(r, ti));
			}
			newTargetArray.setValue(r, ti, val);
		}
		for (int r = 0; r < targetMatrix.getDim(); r++) {
			// update tj
			double val = 0;
			{
				if (r == ti)
					val = 0;
				else if (r == tj)
					val = targetMatrix.getValue(tj, tj) + t
							* targetMatrix.getValue(ti, tj);
				else
					val = targetMatrix.getValue(r, tj)
							+ s
							* (targetMatrix.getValue(r, ti) - tau
									* targetMatrix.getValue(r, tj));
			}
			newTargetArray.setValue(r, tj, val);
		}
		return newTargetArray;
	}

	private double[][] copyArray(double[][] src) {
		double[][] dest = new double[src.length][];
		for (int i = 0; i < src.length; i++) {
			dest[i] = new double[src[i].length];
			for (int j = 0; j < src[i].length; j++) {
				dest[i][j] = src[i][j];
			}
		}
		return dest;
	}

	public static void main(String args[]) {
		double[][] element = { { 0.1, 0.01, 0, .0001 }, { 0.1, 2, 3, 4 },
				{ 0.001, 4, 4, 6 }, { 0.0001, 8, 9, 10 } };
		// double[][] element={{1,-1},{-1,1}};
		// double[][] element = { { 1, 1, 2 }, {1, 1, 2 }, { 2, 2, 4 } };
		Matrix matrix = new Matrix(element);
		JacobiMethod jm = new JacobiMethod(matrix);
		for (int i = 0; i < 10; i++) {
			System.out.println("-----" + i + "-----");
			jm.rotate();
		}
	}

}
