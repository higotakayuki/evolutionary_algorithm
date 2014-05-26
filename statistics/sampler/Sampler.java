package statistics.sampler;

import java.util.List;

import optimize.util.WeightedSampleWithValue;

public abstract interface Sampler<SampleType> extends
		ProbabilityDistribution<SampleType>, Cloneable {

	public List<SampleType> sampling(int num);

	public void fittingUpdate(
			List<WeightedSampleWithValue<SampleType>> allSamples);

	public Sampler<SampleType> fittingReplace(
			List<WeightedSampleWithValue<SampleType>> allSamples);

	public double entropy();
}