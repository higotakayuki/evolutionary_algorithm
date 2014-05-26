package app.continuous.util;

import java.util.Formatter;

import calc.la.Vector;

public class CVector {
	private final double element[];

	public CVector(int dim) {
		element = new double[dim];
	}

	public CVector(double[] source) {
		this.element = source;
	}

	public double getValue(int i) {
		return element[i];
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		Formatter f = new Formatter(sb);
		sb.append("(");
		for (int i = 0; i < 10 && i < element.length; i++) {
			f.format("% 6.3f", element[i]);
		}
		sb.append(")");
		return sb.toString();
	}

	public static void main(String args[]) {
		System.out.println(new CVector(4));
	}

	public Vector getLA_Vector() {
		return new Vector(this.element);
	}

	public double[] getElements() {
		return element.clone();
	}

	public CVector multiply(double m) {
		double[] temp = getElements();
		for (int i = 0; i < temp.length; i++) {
			temp[i] = temp[i] * m;
		}
		return new CVector(temp);
	}

	public CVector add(CVector v) {
		double[] t1 = this.getElements();
		double[] t2 = v.getElements();
		for (int i = 0; i < t2.length; i++) {
			t1[i] += t2[i];
		}
		return new CVector(t1);
	}
	
	public CVector minus(CVector v) {
		double[] t1 = this.getElements();
		double[] t2 = v.getElements();
		for (int i = 0; i < t2.length; i++) {
			t1[i] -= t2[i];
		}
		return new CVector(t1);
	}
	
	public double dotProduct(CVector v){
		double[] t1 = this.getElements();
		double[] t2 = v.getElements();
		double sum=0;
		
		for (int i = 0; i < t2.length; i++) {
			sum+=t1[i] * t2[i];
		}
		
		return sum;
	}
	
}
