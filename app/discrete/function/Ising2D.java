package app.discrete.function;

import java.util.Random;

import optimize.function.CostFunction;

import app.discrete.util.DVector;

public class Ising2D extends CostFunction<DVector> {

	private boolean link[][];

	private int height, width;

	public Ising2D(int x, int y, double r) {
		super(x * y);
		this.width = x;
		this.height = y;
		link = new boolean[2][];
		link[0] = new boolean[dim()];
		link[1] = new boolean[dim()];
		Random rand = new Random(12345);
		for (int i = 0; i < dim(); i++) {
			if (rand.nextDouble() <= r) link[0][i] = true;
			else link[0][i] = false;
			if (rand.nextDouble() <= r) link[1][i] = true;
			else link[1][i] = false;
		}
	}

	public Ising2D(int x, int y) {
		this(x, y, 1);
	}

	@Override
	public double eval(DVector t) {
		double sum = 0;
		for (int i = 0; i < t.dim(); i++) {
			Point c = translate(i);
			// int above=translate(new Point(c.x,(c.y-1+height)%height));
			int below = translate(new Point(c.x, (c.y + 1) % height));
			// int left=translate(new Point((c.x-1+width)%width,c.y));
			int right = translate(new Point((c.x + 1) % width, c.y));

			double b_val = 1;
			if (t.getValue(i) != t.getValue(below)) b_val = -1;
			if (!link[0][i]) b_val *= -1;

			double r_val = 1;
			if (t.getValue(i) != t.getValue(right)) r_val = -1;
			if (!link[1][i]) r_val *= -1;

			double tsum = b_val + r_val;
			tsum = (tsum + 2) / 2;
			sum -= tsum;
		}
		// return sum - Math.random() * 1e-10;
		return sum;
	}

	private Point translate(int t) {
		int x = t % width;
		int y = t / width;
		return new Point(x, y);
	}

	private int translate(Point c) {
		return c.x + c.y * width;
	}

	class Point {
		public int x, y;

		public Point(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}

	@Override
	public double getMinimum() {
		// TODO Auto-generated method stub
		return -super.dim() * 2;
	}

}
