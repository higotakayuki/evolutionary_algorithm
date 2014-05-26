package calc.la;

import java.util.Formatter;

public class Matrix {

	private int row;// gyou

	private int column;// retu

	private double[][] element;

	public Matrix(double[][] element) {
		this.element = element.clone();
//		for(double[] vector:element){
//			for(double d:vector){
//				if(Double.isInfinite(d)){
//					System.out.println("matrix");
//				}
//			}
//		}
		this.row = element.length;
		this.column = element[0].length;
	}

	public Matrix(RowVector[] vector) {
		this.row = vector.length;
		this.column = vector[0].dim();

		this.element = new double[vector.length][];

		for (int i = 0; i < vector.length; i++) {
			element[i] = vector[i].getElement();
		}
	}

	public Matrix(ColumnVector[] vector) {
		this.row = vector[0].dim();
		this.column = vector.length;

		this.element = new double[vector.length][];

		for (int i = 0; i < vector.length; i++) {
			element[i] = vector[i].getElement();
		}

		this.element = transposeDoubleArray(element);
	}

	public double[][] getElement() {
		double[][] telement = new double[row][];
		for (int i = 0; i < row; i++) {
			telement[i] = element[i].clone();
		}
		return telement;
	}

	public int getColumn() {
		return column;
	}

	public int getRow() {
		return row;
	}

	public final double getValue(int row, int column) {
		return this.element[row][column];
	}

	public double simpleDeterminant() {
		double[][] telement = getElement();
		double sum = 1;
		for (int i = 0; i < row; i++) {
			for (int j = i + 1; j < row; j++) {
				double rate = telement[i][j] / telement[i][i];
				for (int k = i; k < row; k++) {
					telement[k][j] -= rate * telement[k][i];
				}
			}
			sum *= telement[i][i];
		}
		return sum;
	}

	/* with pivotting row */
	public double determinant() {
		double[][] telement = getElement();
		double sum = 1;
		for (int i = 0; i < row; i++) {
			// begin pivot row
			if (true) {
				double max = 0;
				int max_index = 0;
				for (int j = i; j < row; j++) {
					double tmax = Math.abs(telement[j][i]);
					if (max < tmax) {
						max = tmax;
						max_index = j;
					}
				}
				if (i != max_index) {
					double[] temp_c = telement[i];
					telement[i] = telement[max_index];
					telement[max_index] = temp_c;
					sum*=-1;
				}
			}
			// end pivot
			for (int j = i + 1; j < row; j++) {
				double rate = telement[i][j] / telement[i][i];
				for (int k = i; k < row; k++) {
					telement[k][j] -= rate * telement[k][i];
				}
				//System.out.println(new Matrix(telement));
			}
			sum *= telement[i][i];
		}
		return sum;
	}
	
	public double logAbsDeterminant() {
		double[][] telement = getElement();
		double logSum = 0;
		for (int i = 0; i < row; i++) {
			// begin pivot row
			if (true) {
				double max = 0;
				int max_index = 0;
				for (int j = i; j < row; j++) {
					double tmax = Math.abs(telement[j][i]);
					if (max < tmax) {
						max = tmax;
						max_index = j;
					}
				}
				if (i != max_index) {
					double[] temp_c = telement[i];
					telement[i] = telement[max_index];
					telement[max_index] = temp_c;
					//sum*=-1; needless because abs value is calculated
				}
			}
			// end pivot
			for (int j = i + 1; j < row; j++) {
				if(telement[i][i]==0)return Double.NEGATIVE_INFINITY;
				double rate = telement[i][j] / telement[i][i];
				for (int k = i; k < row; k++) {
					telement[k][j] -= rate * telement[k][i];
					if(Double.isNaN(telement[k][j])){
						System.out.println("telement");
					}
				}
				//System.out.println(new Matrix(telement));
			}
			logSum += Math.log(Math.abs(telement[i][i]));
		}
		return logSum;
	}

	private double[][] transposeDoubleArray(double[][] element) {
		double[][] ans = new double[element[0].length][];
		for (int i = 0; i < element[0].length; i++) {
			ans[i] = new double[element.length];
			for (int j = 0; j < element.length; j++) {
				ans[i][j] = element[j][i];
			}
		}
		return ans;
	}

	public Matrix transpose() {
		double[][] ans = transposeDoubleArray(this.element);
		return new Matrix(ans);
	}

	public static Matrix operate(Matrix left, Matrix right) {
		assert left.column == right.row : "\n" + left + "\n" + right;
		double[][] ans = new double[left.row][];
		for (int i = 0; i < ans.length; i++) {
			ans[i] = new double[right.column];
			for (int j = 0; j < ans[i].length; j++) {
				ans[i][j] = 0;
				for (int k = 0; k < left.column; k++) {
					ans[i][j] += left.element[i][k] * right.element[k][j];
				}
			}
		}
		return new Matrix(ans);
	}

	public ColumnVector operateFromLeft(ColumnVector x) {
		assert this.column == x.dim() : "" + this + "\n" + x;
		double[] ans = new double[this.row];
		for (int i = 0; i < row; i++) {
			ans[i] = 0;
			for (int j = 0; j < column; j++) {
				ans[i] += element[i][j] * x.getValue(j);
			}
		}
		return new ColumnVector(ans);
	}

	public Matrix multiply(double scalar) {
		double[][] tmp = new double[element.length][];		
		for (int i = 0; i < tmp.length; i++) {
			tmp[i]=element[i].clone();
			for (int j = 0; j < tmp[i].length; j++) {
				tmp[i][j] *= scalar;
			}
		}
		return new Matrix(tmp);
	}

	public String toString() {
		StringBuilder buf = new StringBuilder();
		Formatter formatter = new Formatter(buf);
		for (int i = 0; i < row; i++) {
			for (int j = 0; j < column; j++) {
				formatter.format(" % 6.3f ", element[i][j]);
			}
			buf.append("\n");
		}
		return buf.toString();
	}

	public static void main(String args[]) {
		double[][] element = new double[2][];
		for (int i = 0; i < element.length; i++) {
			element[i] = new double[2];
		}
		element[0][0] = 8;
		element[0][1] = 0;
		element[1][0] = 3;
		element[1][1] = 5;
		Matrix m = new Matrix(element);
		System.out.println(m.simpleDeterminant());
		System.out.println(m.determinant());
		System.out.println(Matrix.operate(m, m));
		System.out.println(Matrix.operate(m, m).transpose());
		System.out.println(Matrix.plus(m, UnitMatrix(m.row, 5)));
	}

	public Matrix operateFromLeft(Matrix eigenMatrix) {
		return operate(this, eigenMatrix);
	}

	public static Matrix plus(Matrix left, Matrix right) {
		int row = left.row;
		int column = left.column;
		double[][] ele = new double[row][];
		for (int i = 0; i < row; i++) {
			ele[i] = new double[column];
			for (int j = 0; j < column; j++) {
				ele[i][j] = left.getValue(i, j) + right.getValue(i, j);
			}
		}
		return new Matrix(ele);
	}

	public static Matrix UnitMatrix(int dim, double value) {
		int row = dim;
		int column = dim;
		double[][] ele = new double[row][];
		for (int i = 0; i < row; i++) {
			ele[i] = new double[column];
			for (int j = 0; j < column; j++) {
				if (i != j)
					ele[i][j] = 0;
				else
					ele[i][j] = value;
			}
		}
		return new Matrix(ele);
	}

}
