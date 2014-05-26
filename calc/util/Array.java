package calc.util;

import java.text.DecimalFormat;

public class Array {
	public static double sum(double[] array) {
		double tmp = 0;
		for (int i = 0; i < array.length; i++) {
			tmp += array[i];
		}
		return tmp;
	}

	public static double[] normalize(double[] array) {
		double sum = Array.sum(array);
		double[] ret = new double[array.length];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = array[i] / sum;
		}
		return ret;
	}

	public static double[] map(Function func, double[] array) {
		double[] ret = new double[array.length];
		for (int i = 0; i < array.length; i++) {
			ret[i] = func.op(array[i]);
		}
		return ret;

	}

	public static Function exp = new Function() {
		@Override
		public double op(double d) {
			return Math.exp(d);
		}
	};

	public static Function abs = new Function() {
		@Override
		public double op(double d) {
			return Math.abs(d);
		}
	};
	

	public static Integer minIndex(double[] array) {
		if (array.length == 0) return null;
		int idx = 0;
		double val = array[0];
		for (int i = 1; i < array.length; i++) {
			if (array[i] < val) {
				idx = i;
				val = array[i];
			}
		}
		return idx;
	}
	
	

	public static Integer maxIndex(double[] array) {
		if (array.length == 0) return null;
		int idx = 0;
		double val = array[0];
		for (int i = 1; i < array.length; i++) {
			if (val < array[i]) {// compare
				idx = i;
				val = array[i];
			}
		}
		return idx;
	}

	public static void println(double[] array, String fstr) {
		DecimalFormat format = new DecimalFormat(fstr);
		for (int i = 0; i < array.length; i++) {
			System.out.print(format.format(array[i]));
			if (i != array.length - 1) System.out.print(",");
		}
		System.out.println();
	}

	public static double dotProduct(double[] v1, double[] v2) {
		if (v1.length != v2.length) return Double.NaN;
		else {
			double sum = 0;
			for (int i = 0; i < v1.length; i++) {
				sum += v1[i] * v2[i];
			}
			return sum;
		}
	}

	public static double dotProduct(double[] v1, double[] v2,
			int length) {
		if (!(length <= v1.length && length <= v2.length)) return Double.NaN;
		else {
			double sum = 0;
			for (int i = 0; i < length; i++) {
				sum += v1[i] * v2[i];
			}
			return sum;
		}
	}

	public static double[] plus(double[] v1, double[] v2) {
		if (v1.length != v2.length) return null;
		double[] temp = new double[v1.length];

		for (int i = 0; i < v1.length; i++) {
			temp[i] = v1[i] + v2[i];
		}
		return temp;
	}

	public static double[] plus(double[] v1, double[] v2, int length) {
		if (!(length <= v1.length && length <= v2.length)) return null;
		else {
			double[] temp = new double[v1.length];
			for (int i = 0; i < length; i++) {
				temp[i] = v1[i] + v2[i];
			}
			return temp;
		}
	}

	public static double[] minus(double[] v1, double[] v2) {
		if (v1.length != v2.length) return null;
		double[] temp = new double[v1.length];

		for (int i = 0; i < v1.length; i++) {
			temp[i] = v1[i] - v2[i];
		}
		return temp;
	}

	public static double[] minus(double[] v1, double[] v2, int length) {
		if (!(length <= v1.length && length <= v2.length)) return null;
		else {
			double[] temp = new double[v1.length];
			for (int i = 0; i < length; i++) {
				temp[i] = v1[i] - v2[i];
			}
			return temp;
		}
	}

	public static double[] subArray(double[] src, int from, int to) {
		double[] ret = new double[to - from];
		System.arraycopy(src, from, ret, 0, to - from);
		return ret;
	}

	public static double[] mult(double c, double[] array) {
		double[] ret = new double[array.length];
		for (int i = 0; i < array.length; i++) {
			ret[i] = c * array[i];
		}
		return ret;
	}

	public static double[] zeros(int size) {
		double[] ret = new double[size];
		for (int i = 0; i < size; i++) {
			ret[i] = 0;
		}
		return ret;
	}
	
	public static double[] ones(int size){
		double[] ret = new double[size];
		for (int i = 0; i < size; i++) {
			ret[i] = 1;
		}
		return ret;	
	}
}
