package app.continuous.function;

import optimize.function.CostFunction;
import app.continuous.util.CVector;

public class Rosenbrock extends CostFunction<CVector> {

	public Rosenbrock(int dim) {
		super(dim);
	}

	@Override
	public double eval(CVector t) {
		double[] x = t.getElements();
		double sum = 0;
		for (int i = 0; i < super.dim() - 1; i++) {
			double temp1 = (x[i] * x[i]) - x[i + 1];
			double temp2 = (1 - x[i]);
			sum += 100 * temp1 * temp1 + temp2 * temp2;
		}
		// double temp=x[super.dim()-1]*x[super.dim()-1]-x[0];
		// sum+=100*temp*temp+(1-x[super.dim()-1])*(1-x[super.dim()-1]);
		return sum;
	}

	@Override
	public double getMinimum() {
		return 1e-5;
	}

}
