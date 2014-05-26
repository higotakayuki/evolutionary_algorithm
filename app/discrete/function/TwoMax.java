package app.discrete.function;

import optimize.function.CostFunction;
import app.discrete.util.DVector;

public class TwoMax extends CostFunction<DVector> {

	public TwoMax(int dim) {
		super(dim);
	}

	@Override
	public double eval(DVector t) {
		return eval2(t);
	}

		public double eval2(DVector t) {
		double sum = t.dim() / 2;
		for (int i = 0; i < t.dim(); i++) {
			sum -= t.getValue(i);
		}
		return -Math.abs(sum);
	}

}
