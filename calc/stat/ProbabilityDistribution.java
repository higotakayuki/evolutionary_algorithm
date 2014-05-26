package calc.stat;

import calc.la.Vector;

public interface ProbabilityDistribution {
	public double normalizedProbability(Vector x);
	public double logNormalizedProbability(Vector x);
}
