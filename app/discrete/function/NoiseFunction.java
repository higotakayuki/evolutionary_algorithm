package app.discrete.function;

import optimize.function.CostFunction;
import app.discrete.util.DVector;

public class NoiseFunction extends CostFunction<DVector> {

	CostFunction<DVector> base;

	public NoiseFunction(CostFunction<DVector> cf) {
		super(cf.dim());
		base = cf;
	}

	@Override
	public int dim() {
		return base.dim();
	}

	@Override
	public double getMinimum() {
		return base.getMinimum();
	}

	@Override
	public double eval(DVector t) {
		// TODO Auto-generated method stub
		return base.eval(t)-Math.random()*1e-10;
	}

}
