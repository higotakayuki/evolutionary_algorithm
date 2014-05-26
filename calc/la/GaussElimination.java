package calc.la;


public class GaussElimination {

	public static final boolean PRINT = false;

	public static final boolean PIVOT = true;

	private int[] cSequence;

	private double[][] element;

	private int row;

	private int column;

	private ColumnVector solution = null;

	public GaussElimination(Matrix m) {

		this.set(m);
	}

	public GaussElimination(Matrix m, ColumnVector v) {
		set(m, v);
	}

	private void set(Matrix m) {
		cSequence = new int[m.getColumn()];
		for (int i = 0; i < cSequence.length; i++) {
			cSequence[i] = i;
		}
		this.element = m.getElement();
		this.row = m.getRow();
		this.column = m.getColumn();
		assert ((row + 1) == column) : "row!=column+1" + "[row:" + column
				+ ",column:" + row + "]";
	}

	private void set(Matrix m, ColumnVector y) {
		double[][] me = m.getElement();
		double[][] nme = new double[me.length][];
		for (int i = 0; i < me.length; i++) {
			nme[i] = new double[me[i].length + 1];
			System.arraycopy(me[i], 0, nme[i], 0, me[i].length);
			nme[i][me[i].length] = y.getValue(i);
		}
		set(new Matrix(nme));
	}

	public void eliminate() {
		assert (solution == null) : "eliminate() called again";
		if (PRINT)
			System.out.println("start elimination");
		printMatrix();
		for (int i = 0; i < row; i++) {
			// begin pivot
			if (PIVOT) {
				double max = 0;
				int max_index = 0;
				for (int j = i; j < row; j++) {
					double tmax = Math.abs(element[j][i]);
					if (max < tmax) {
						max = tmax;
						max_index = j;
					}
				}
				if (i != max_index) {
					double[] temp_c = element[i];
					int temp_i = cSequence[i];
					cSequence[i] = cSequence[max_index];
					cSequence[max_index] = temp_i;
					element[i] = element[max_index];
					element[max_index] = temp_c;
				}
				assert element[i][i] != 0 : "pivot error";
				printMatrix();
			}
			// end pivot

			double diavalue = element[i][i];
			element[i][i] = 1;
			for (int j = i + 1; j < column; j++) {
				element[i][j] /= diavalue;
			}
			for (int k = i + 1; k < row; k++) {
				double times = element[k][i];
				element[k][i] = 0;
				for (int j = i + 1; j < element[0].length; j++) {
					element[k][j] -= times * element[i][j];
				}
			}
			printMatrix();
		}
		double[] answer = new double[row];
		for (int i = row - 1, count = 0; i >= 0; i--, count++) {
			answer[i] = element[i][column - 1];
			for (int j = 0; j < count; j++) {
				answer[i] -= element[i][column - 2 - j] * answer[row - 1 - j];
			}
		}
		solution = new ColumnVector(answer);
	}

	public ColumnVector getSolution() {
		if (solution == null)
			eliminate();
		return solution;
	}

	private void printMatrix() {
		if (!PRINT)
			return;
		System.out.println();
		for (int i = 0; i < row; i++) {
			System.out.print("(" + cSequence[i] + ")");
			for (int j = 0; j < column; j++) {
				System.out.print("  " + element[i][j] + "  ");
			}
			System.out.println("");
		}
		System.out.println();
	}

	public static void main(String args[]) {
		double[][] element = { { 2, 4, 6, 4 }, { 2, 7, 12, 13 },
				{ 3, 10, 19, 26 }, { 4, 13, 25, 39 } };
		double[] vec = { 44, 104, 184, 261 };
		Matrix m = new Matrix(element);
		GaussElimination ge = new GaussElimination(m, new ColumnVector(vec));
		Vector ans = ge.getSolution();
		System.out.println(ans);
		System.out.println(m.operateFromLeft(new ColumnVector(ans)));
	}
}
