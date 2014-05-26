package statistics.sampler;

import optimize.util.LogSumExp;

public class MixtureProbability<SampleType> implements
		ProbabilityDistribution<SampleType> {

	public MixtureProbability(ProbabilityDistribution<SampleType>[] pds,
			double[] mixtureRatio) {
		this.pds = pds.clone();
		this.mixtureRatio = mixtureRatio.clone();
	}

	ProbabilityDistribution<SampleType>[] pds;

	private double[] mixtureRatio;

	public double logNormalizedProbability(SampleType s) {
		double[] logP = new double[pds.length];
		for (int i = 0; i < logP.length; i++) {
			logP[i] = Math.log(mixtureRatio[i])
					+ pds[i].logNormalizedProbability(s);
		}
		return LogSumExp.sum(logP);
	}

}
