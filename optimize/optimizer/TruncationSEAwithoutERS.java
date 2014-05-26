package optimize.optimizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import optimize.Annealing;
import optimize.function.CostFunction;
import optimize.util.LogSumExp;
import optimize.util.SampleWithValue;
import optimize.util.SampleWithValueComparator;
import optimize.util.WeightedSampleWithValue;
import statistics.sampler.Sampler;

public class TruncationSEAwithoutERS<SampleType> implements
		Annealing<SampleType> {
	protected final int SAMPLE_NUM;

	protected final CostFunction<SampleType> cf;

	protected Sampler<SampleType> sampler;

	private List<SampleWithValue<SampleType>> sampleWithValue;

	private double averageEnergy;

	private double varianceEnergy;

	private double cutoff;

	private double lambda;

	public TruncationSEAwithoutERS(int sample_num, double cutoff, double lamda,
			CostFunction<SampleType> cf) {
		this.SAMPLE_NUM = sample_num;
		this.cf = cf;
		this.cutoff = cutoff;
		this.lambda = lamda;
	}

	public void optimize() {
		// sampling
		samplingAndAveraging();

		// annealing
		List<WeightedSampleWithValue<SampleType>> wsamples = mergeSamples();

		// fitting
		sampler.fittingUpdate(wsamples);
	}

	private void samplingAndAveraging() {
		sampleWithValue = sampling();
		averageEnergy = calcAverage(sampleWithValue);
		varianceEnergy = calcVariance(sampleWithValue, averageEnergy);
	}

	private List<SampleWithValue<SampleType>> sampling() {
		List<SampleType> samples = sampler.sampling(SAMPLE_NUM);
		List<SampleWithValue<SampleType>> samplesWithValue = new ArrayList<SampleWithValue<SampleType>>(
				SAMPLE_NUM);
		for (SampleType asample : samples) {
			// eval samples
			samplesWithValue.add(new SampleWithValue<SampleType>(asample, cf
					.eval(asample)));
		}
		return samplesWithValue;
	}

	private double calcAverage(List<SampleWithValue<SampleType>> samples) {
		double sumAverage = 0;
		for (SampleWithValue<SampleType> swv : samples) {
			sumAverage += swv.getValue();
		}
		double average = sumAverage / samples.size();
		return average;
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

	private List<WeightedSampleWithValue<SampleType>> mergeSamples() {
		List<SampleWithValue<SampleType>> bestSamples = searchBestSamples(
				sampleWithValue, 0, (int) (SAMPLE_NUM * (1 - cutoff)));

		double[] logW = new double[bestSamples.size()];
		for (int i = 0; i < bestSamples.size(); i++) {
			logW[i] = -sampler.logNormalizedProbability(bestSamples.get(i)
					.getSample());
			logW[i] *= lambda;
		}
		LogSumExp.normalize(logW);

		List<WeightedSampleWithValue<SampleType>> promising = new ArrayList<WeightedSampleWithValue<SampleType>>(
				bestSamples.size());
		for (int i = 0; i < bestSamples.size(); i++) {
			promising.add(new WeightedSampleWithValue<SampleType>(Math
					.exp(logW[i]), bestSamples.get(i)));
		}
		return promising;
	}

	private List<SampleWithValue<SampleType>> searchBestSamples(
			List<SampleWithValue<SampleType>> samples, int from, int to) {
		Collections.sort(samples, new SampleWithValueComparator());
		return samples.subList(from, to);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("" + ((int) (SAMPLE_NUM * (1 - cutoff))) + ": "
				+ averageEnergy + "(" + varianceEnergy + ")" + "\n best:"
				+ searchBestSamples(sampleWithValue, 0, 1) + "\n");
		sb.append(sampler);
		return sb.toString();
	}

	public SampleType getBestSolution() {
		return searchBestSamples(sampleWithValue, 0, 1).get(0).getSample();
	}

	public SampleWithValue<SampleType> getBest() {
		return searchBestSamples(sampleWithValue, 0, 1).get(0);
	}

	public double getVariance() {
		return varianceEnergy;
	}

	// @Override
	// public double getAverage() {
	// return averageEnergy;
	// }

}
