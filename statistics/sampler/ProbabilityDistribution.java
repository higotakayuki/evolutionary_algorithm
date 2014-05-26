package statistics.sampler;

public interface ProbabilityDistribution<SampleType> {
	public double logNormalizedProbability(SampleType s);
}
