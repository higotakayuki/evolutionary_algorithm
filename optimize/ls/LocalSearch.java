package optimize.ls;

import optimize.Optimizer;
import optimize.function.CostFunction;

public abstract class LocalSearch<SampleType> implements Optimizer<SampleType> {

	protected boolean isConverged = false;

	protected final CostFunction<SampleType> cf;

	public boolean isConverged() {
		return isConverged;
	}

	public LocalSearch(CostFunction<SampleType> cf) {
		this.cf = cf;
	}

	@Override
	public abstract void optimize();

}
