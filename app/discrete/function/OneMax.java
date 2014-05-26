package app.discrete.function;

import optimize.function.CostFunction;
import app.discrete.util.DVector;

public class OneMax extends CostFunction<DVector> {

	public OneMax(int dim) {
		super(dim);
	}

	@Override
	public double eval(DVector t) {
		if (true) return eval1(t);
		else return 0;
	}

	public double eval1(DVector t) {
		double sum = 0;
		for (int i = 0; i < t.dim(); i++) {
			sum -= t.getValue(i);
		}
		// sum-=Math.random()*1e-10;
		return sum;
	}

	@Override
	public double getMinimum() {
		// TODO Auto-generated method stub
		return -dim();
	}

}
