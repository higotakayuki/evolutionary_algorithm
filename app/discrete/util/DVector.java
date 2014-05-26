package app.discrete.util;

import java.util.Formatter;

public class DVector {
	private final int element[];

	private final boolean STAMP_FLAG = false;

	private Integer stamp = null;

	private void makeStamp() {
		int sum = 1;
		for (int e : element) {
			if (e == 1)
				sum >>= 1;
			else
				sum++;
		}
	}

	public DVector(int dim) {
		element = new int[dim];
	}

	public DVector(int[] source) {
		this.element = source.clone();
	}

	public boolean equals(DVector opp) {
		if (STAMP_FLAG) {
			if (this.stamp == null) this.makeStamp();
			if (opp.stamp == null) opp.makeStamp();
			if (stamp != opp.stamp) return false;
		}
		for (int i = 0; i < element.length; i++) {
			if (this.element[i] != opp.element[i]) return false;
		}
		return true;
	}

	public int dim() {
		return element.length;
	}

	public int getValue(int i) {
		return element[i];
	}

	public int[] copy(){
		return element.clone();
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		Formatter f = new Formatter(sb);
		sb.append("(");
		for (int i = 0; i < 400 && i < element.length; i++) {
			f.format("%d,", element[i]);
		}
		sb.append(")");
		return sb.toString();
	}

	public static void main(String args[]) {
		DVector dv = new DVector(10);

		long current = System.currentTimeMillis();

		for (int i = 0; i < 1000000000; i++) {
			dv.dim();
			dv.getValue(3);
		}

		System.out.println(System.currentTimeMillis() - current);
	}
}
