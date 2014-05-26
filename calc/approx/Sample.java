package calc.approx;

import calc.la.Vector;

public class Sample implements Comparable<Sample> {
	private Vector vector;

	private double value;

	public double getValue() {
		return value;
	}

	public Vector getVector() {
		return vector;
	}

	public Sample(Vector vector, double value) {
		this.vector = (Vector) vector.clone();
		this.value = value;
	}

	public int compareTo(Sample s) {
		if (this == s)
			return 0;
		if (this.value == s.value)
			return this.hashCode() < s.hashCode() ? -1 : 1;
		return this.value < s.value ? -1 : 1;
	}

	public static double averageValue(Sample[] samples){
		return 0;
	}
	
}
