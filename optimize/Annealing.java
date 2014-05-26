package optimize;

public interface Annealing<S> extends Optimizer<S> {
	// public double getAverage();
	public double getVariance();
}
