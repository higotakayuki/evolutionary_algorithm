package app.continuous.sampler;

import java.util.Random;

import calc.la.Vector;
import calc.stat.ProbabilityDistribution;

public class UGaussian implements ProbabilityDistribution {

	private final double mean;

	private final double variance;

	private final double std;

	private static final Random rand = new Random();

	public UGaussian(double mean, double variance) {
		super();
		this.mean = mean;
		this.variance = variance;
		this.std = Math.sqrt(variance);
		// System.out.println("mean:"+mean+", std:"+std);
	}

	public double logNormalizedProbability(double x) {
		if (variance == 0) {
			if (x == mean) return 0;
			else return Double.NEGATIVE_INFINITY;
		} else {
			double dist = (x - mean);
			double logexp = -0.5 * dist * dist / variance;
			double logz = -0.5 * Math.log(2 * Math.PI * variance);
			return logexp + logz;
		}
	}

	public double logNormalizedProbability(Vector x) {
		return logNormalizedProbability(x.getValue(0));
	}

	public double normalizedProbability(Vector x) {
		return Math.exp(logNormalizedProbability(x));
	}

	public double generateSample() {
		return mean + std * rand.nextGaussian();
	}

	public double getMean() {
		return mean;
	}

	public double getVariance() {
		return variance;
	}

	
	public double entropy() {	
		return 0.5*Math.log(2*Math.PI*Math.E*variance);
	}

}
