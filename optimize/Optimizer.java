package optimize;

import optimize.optimizer.ConvergenceException;
import optimize.util.SampleWithValue;

public interface Optimizer<T> {
	public void optimize() throws ConvergenceException;
}
