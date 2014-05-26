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

public class TruncationSEAwithERS2<SampleType> implements Annealing<SampleType> {
	protected final int SAMPLE_NUM;

	protected final CostFunction<SampleType> cf;

	protected Sampler<SampleType> sampler;

	private List<SampleWithValue<SampleType>> sampleWithValue;

	private double averageEnergy;

	private double varianceEnergy;

	private double cutoff;

	private double lambda;

	private double threshold = Double.MAX_VALUE;

	public TruncationSEAwithERS2(int sample_num, double cutoff, double lambda,
			CostFunction<SampleType> cf) {
		this.SAMPLE_NUM = sample_num;
		this.cf = cf;
		this.cutoff = cutoff;
		this.lambda = lambda;
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
		for (SampleType st : samples) {
			// eval samples
			samplesWithValue.add(new SampleWithValue<SampleType>(st, cf
					.eval(st)));
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
		List<SampleWithValue<SampleType>> sortedPreviousSamples = searchBestSamples(
				sampleWithValue, 0, SAMPLE_NUM); // sort
		{
			List<SampleWithValue<SampleType>> candidate = new ArrayList<SampleWithValue<SampleType>>(
					SAMPLE_NUM);
			for (SampleWithValue<SampleType> swv : sortedPreviousSamples) {
				if (swv.getValue() > threshold) break;
				candidate.add(swv);
			}
			if (candidate.size() == 0) {
				List<WeightedSampleWithValue<SampleType>> allSamples = new ArrayList<WeightedSampleWithValue<SampleType>>(
						sortedPreviousSamples.size());
				allSamples.add(new WeightedSampleWithValue<SampleType>(1,
						sortedPreviousSamples.get(0)));
				return allSamples;
			}
			sortedPreviousSamples = candidate;
		}

		double[] logW = new double[sortedPreviousSamples.size()];

		// adjust lambda?
		for (int i = 0; i < sortedPreviousSamples.size(); i++) {
			logW[i] = -sampler.logNormalizedProbability(sortedPreviousSamples
					.get(i).getSample());
			// logW[i] *= lambda;
		}
		// adjust lambda?

		LogSumExp.normalize(logW);
		double sum = 1;
		sum *= (1 - cutoff);

		double tempCFV = sortedPreviousSamples.get(
				sortedPreviousSamples.size() - 1).getValue();
		double oldThreshold = this.threshold;
		this.threshold = Math.min(this.threshold, tempCFV);

		List<WeightedSampleWithValue<SampleType>> promising = new ArrayList<WeightedSampleWithValue<SampleType>>(
				sortedPreviousSamples.size());
		for (int i = 0; i < sortedPreviousSamples.size(); i++) {
			if (sum < 0) {
				threshold = sortedPreviousSamples.get(i).getValue();
				break;
			}
			double w = Math.exp(lambda * logW[i]);// lambda‚ð‚±‚±‚Å“Š“ü
			sum -= w;
			promising.add(new WeightedSampleWithValue<SampleType>(w,
					sortedPreviousSamples.get(i)));
		}
		if (oldThreshold < this.threshold) {
			System.out.println("???");
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
		sb.append(sampler + "\n");
		sb.append("tilde f: " + this.threshold + "\n");
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

//	@Override
//	public double getAverage() {
//		return averageEnergy;
//	}

}
