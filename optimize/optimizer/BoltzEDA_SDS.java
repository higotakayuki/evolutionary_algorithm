package optimize.optimizer;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import optimize.Optimizer;
import optimize.function.CostFunction;
import optimize.util.SampleWithValue;
import optimize.util.SampleWithValueComparator;
import optimize.util.WeightedSampleWithValue;

import statistics.sampler.Sampler;

public class BoltzEDA_SDS<SampleType> implements Optimizer<SampleType> {

	protected final int SAMPLE_NUM;

	protected final CostFunction<SampleType> cf;

	protected Sampler<SampleType> sampler;

	private List<SampleWithValue<SampleType>> sampleWithValue;

	private double averageEnergy;

	private double varianceEnergy;

	private double inverseTemplerature = 0;

	private double dbeta = 0.01;

	private double c = 1;

	public BoltzEDA_SDS(int sample_num, double c, CostFunction<SampleType> cf) {
		this.SAMPLE_NUM = sample_num;
		this.cf = cf;
		this.c = c;
	}

	public BoltzEDA_SDS(int sample_num, CostFunction<SampleType> cf) {
		this.SAMPLE_NUM = sample_num;
		this.cf = cf;
	}

	private List<SampleWithValue<SampleType>> sampling() {
		List<SampleType> samples = sampler.sampling(SAMPLE_NUM);
		List<SampleWithValue<SampleType>> samplesWithValue = new ArrayList<SampleWithValue<SampleType>>(
				SAMPLE_NUM);
		for (SampleType st : samples) {
			// eval samples
			samplesWithValue.add(new SampleWithValue<SampleType>(st, cf
					.eval(st)));
		}
		return samplesWithValue;
	}

	private void samplingAndAveraging() {
		sampleWithValue = sampling();
		averageEnergy = calcAverage(sampleWithValue);
		varianceEnergy = calcVariance(sampleWithValue, averageEnergy);
	}

	private double calcVariance(List<SampleWithValue<SampleType>> samples,
			double average) {
		double sumVariance = 0;
		for (SampleWithValue<SampleType> swv : samples) {
			double dist = swv.getValue() - average;
			sumVariance += dist * dist;
		}
		double variance = sumVariance / samples.size();
		return variance;
	}

	private double calcAverage(List<SampleWithValue<SampleType>> samples) {
		double sumAverage = 0;
		for (SampleWithValue<SampleType> swv : samples) {
			sumAverage += swv.getValue();
		}
		double average = sumAverage / samples.size();
		return average;
	}

	public void optimize() {
		// sampling
		samplingAndAveraging();

		// annealing
		dbeta = c / Math.sqrt(this.varianceEnergy);
		if (Double.isNaN(dbeta))
			return;
		if (Double.isInfinite(dbeta))
			return;
		inverseTemplerature += dbeta;
		List<WeightedSampleWithValue<SampleType>> wsamples = generateMarginalizedSamples(sampleWithValue);

		// fitting
		sampler.fittingUpdate(wsamples);
	}

	private List<WeightedSampleWithValue<SampleType>> generateMarginalizedSamples(
			List<SampleWithValue<SampleType>> samples) {
		List<WeightedSampleWithValue<SampleType>> allSamples = new ArrayList<WeightedSampleWithValue<SampleType>>(
				samples.size());
		double maxLogW = -Double.MAX_VALUE;
		double[] logW = new double[samples.size()];
		for (int i = 0; i < samples.size(); i++) {
			logW[i] = logWeight(samples.get(i));
			if (maxLogW < logW[i])
				maxLogW = logW[i];
		}
		for (int i = 0; i < samples.size(); i++) {
			double tempW = Math.exp(logW[i] - maxLogW);
			if (Double.isNaN(tempW)) {
				System.out.println(samples.get(i));
			}
			if (tempW < 1e-5)
				continue;
			allSamples.add(new WeightedSampleWithValue<SampleType>(tempW,
					samples.get(i)));
		}

		return allSamples;
	}

	private double logWeight(SampleWithValue<SampleType> s) {
		return -s.getValue() * dbeta;
	}

	public String toString() {
		return inverseTemplerature + ": " + averageEnergy + "("
				+ varianceEnergy + ")" + "\n best:"
				+ searchBestSamples(sampleWithValue, 0, 1);
	}

	private List<SampleWithValue<SampleType>> searchBestSamples(
			List<SampleWithValue<SampleType>> samples, int from, int to) {
		Collections.sort(samples, new SampleWithValueComparator());
		return samples.subList(from, to);
	}

	public SampleWithValue<SampleType> getBest() {
		// TODO Auto-generated method stub
		return null;
	}

}
