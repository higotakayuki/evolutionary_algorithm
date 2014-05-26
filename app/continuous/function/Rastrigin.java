package app.continuous.function;

import optimize.function.CostFunction;
import app.continuous.util.CVector;

public class Rastrigin extends CostFunction<CVector>{

	public Rastrigin(int dim) {
		super(dim);
	}

	@Override
	public double eval(CVector t) {
		double[] ele=t.getLA_Vector().getElement();
		double sum = 0;
		sum += 10 * super.dim();
		for (int i = 0; i < ele.length; i++) {
			sum += ele[i] * ele[i];
			sum -= 10 * Math.cos(2 * Math.PI * ele[i]);
		}
		return sum;
	}

	
	@Override
	public double getMinimum() {
		// TODO Auto-generated method stub
		return 1e-5;
	}

}
