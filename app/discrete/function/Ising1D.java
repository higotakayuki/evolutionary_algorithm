package app.discrete.function;

import optimize.function.CostFunction;
import app.discrete.util.DVector;

public class Ising1D extends CostFunction<DVector> {

	public Ising1D(int dim) {
		super(dim);
	}

	@Override
	public double eval(DVector t) {
		double sum = 0;
		for (int i = 0; i < t.dim(); i++) {
			if (t.getValue(i) == t.getValue((i + 1) % t.dim())) sum -= 1;
		}
		// return sum-Math.random()*1e-10;
		return sum;
	}

	@Override
	public double getMinimum() {
		return -super.dim();
	}

}
