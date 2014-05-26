package optimize.his.old;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;

import optimize.function.CostFunction;
import optimize.util.SampleWithValue;
import optimize.util.SampleWithValueComparator;
import optimize.util.WeightedSampleWithValue;

import statistics.sampler.Sampler;


public class TruncationBasedHIS<SampleType> {

	protected final int SAMPLE_NUM;

	protected final int LEVEL;

	protected final CostFunction<SampleType> cf;

	protected final Sampler<SampleType>[] samplerSet;

	private final List<SampleWithValue<SampleType>>[] sampleWithValueSet;

	private final double[] averageEnergy;

	private final double[] varianceEnergy;

	private final double[] cutoffEnergy;

	private double cutoff = 0.8;

	public SampleWithValue<SampleType> getBest() {
		List<SampleWithValue<SampleType>> list = new ArrayList<SampleWithValue<SampleType>>(
				this.SAMPLE_NUM * 3);
		for (int i = 0; i < LEVEL; i++) {
			list.addAll(this.sampleWithValueSet[i]);
		}
		return searchBestSamples(list, 0, 1).get(0);
	}

	public TruncationBasedHIS(int sample_num, int level, CostFunction<SampleType> cf) {
		this.SAMPLE_NUM = sample_num;
		this.LEVEL = level;
		this.samplerSet = new Sampler[LEVEL];
		this.sampleWithValueSet = new List[LEVEL];
		this.averageEnergy = new double[LEVEL];
		this.varianceEnergy = new double[LEVEL];
		this.cutoffEnergy = new double[LEVEL];
		this.cf = cf;
	}

	protected void init() {
		for (int i = 0; i < LEVEL; i++) {
			samplingAndAveraging(i);
		}
	}

	private int pSampler = 0;

	private List<SampleWithValue<SampleType>> sampling(int pSampler) {
		List<SampleType> samples = samplerSet[pSampler].sampling(SAMPLE_NUM);
		List<SampleWithValue<SampleType>> samplesWithValue = new ArrayList<SampleWithValue<SampleType>>(
				SAMPLE_NUM);
		for (SampleType st : samples) {
			// eval samples
			samplesWithValue.add(new SampleWithValue<SampleType>(st, cf
					.eval(st)));
		}
		return samplesWithValue;
	}

	private void samplingAndAveraging(int pSampler) {
		sampleWithValueSet[pSampler] = sampling(pSampler);
		averageEnergy[pSampler] = calcAverage(sampleWithValueSet[pSampler]);
		varianceEnergy[pSampler] = calcVariance(sampleWithValueSet[pSampler],
				averageEnergy[pSampler]);
		sampleWithValueSet[pSampler] = searchBestSamples(
				sampleWithValueSet[pSampler], 0, sampleWithValueSet[pSampler]
						.size());
		if (true) {
			int num=(int) (SAMPLE_NUM * (1 - cutoff));
			//System.out.println("num"+num);
			cutoffEnergy[pSampler] = sampleWithValueSet[pSampler].get(
					num).getValue();
		} else {

		}
		if (pSampler != 0) {
			if (cutoffEnergy[pSampler - 1] < cutoffEnergy[pSampler])
				cutoffEnergy[pSampler] = cutoffEnergy[pSampler - 1];
		}
	}

	private List<SampleWithValue<SampleType>> getPartOfSamples(int pSampler) {
		List<SampleWithValue<SampleType>> list = new ArrayList<SampleWithValue<SampleType>>(
				this.SAMPLE_NUM * 3);
		for (int i = pSampler - 1; i <= pSampler + 1; i++) {
			if (i < 0)
				continue;
			if (LEVEL <= i)
				continue;
			list.addAll(this.sampleWithValueSet[i]);
		}
		return list;
	}

	private List<Sampler<SampleType>> getPartOfSampler(int pSampler) {
		List<Sampler<SampleType>> list = new ArrayList<Sampler<SampleType>>(3);
		for (int i = pSampler - 1; i <= pSampler + 1; i++) {
			if (i < 0)
				continue;
			if (LEVEL <= i)
				continue;
			list.add(samplerSet[i]);
		}
		return list;
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

		pSampler = (LEVEL + (pSampler - 1)) % LEVEL;

		if (false && pSampler == 0) {
			samplingAndAveraging(pSampler);
			return;
		}

		List<SampleWithValue<SampleType>> partSamples = getPartOfSamples(pSampler);
		List<Sampler<SampleType>> partSampler = getPartOfSampler(pSampler);

		List<WeightedSampleWithValue<SampleType>> allSamples = cutoffSamples(
				pSampler, cutoff, partSamples, partSampler);

		if (allSamples.size() < 1) {
			// System.out.println("continue bo allsamples");
			//System.out.println("skip" + pSampler);
			samplingAndAveraging(pSampler);
			return;
		}

		samplerSet[pSampler].fittingUpdate(allSamples);
		samplingAndAveraging(pSampler);
	}

	private List<WeightedSampleWithValue<SampleType>> cutoffSamples(
			int pSampler, double cutoff,
			List<SampleWithValue<SampleType>> samples,
			List<Sampler<SampleType>> listOfSampler) {
		// sort
		samples = searchBestSamples(samples, 0, samples.size());

		double[] prob = new double[samples.size()];
		for (int i = 0; i < samples.size(); i++) {
			prob[i] = samplerSet[pSampler].logNormalizedProbability(samples
					.get(i).getSample());
		}
		int sampleSize = samples.size();
		List<WeightedSampleWithValue<SampleType>> allSamples = new ArrayList<WeightedSampleWithValue<SampleType>>(
				sampleSize);
		double[] logP = new double[samples.size()];
		for (int i = 0; i < samples.size(); i++) {
			double maxLogP = Double.NEGATIVE_INFINITY;
			double[] tempLogP = new double[listOfSampler.size()];
			for (int j = 0; j < listOfSampler.size(); j++) {
				tempLogP[j] = listOfSampler.get(j).logNormalizedProbability(
						samples.get(i).getSample());
				if (maxLogP < tempLogP[j])
					maxLogP = tempLogP[j];
			}
			if (Double.isInfinite(maxLogP)) {
				System.out.println(pSampler);
				logP[i] = maxLogP;
			} else {
				double expPartSum = 0;
				for (int j = 0; j < listOfSampler.size(); j++) {
					expPartSum += Math.exp(tempLogP[j] - maxLogP);
				}
				logP[i] = maxLogP + Math.log(expPartSum);
			}
			if (Double.isNaN(logP[i])) {
				System.out.println("logp[]");
			}
		}

		double maxLogW = -Double.MAX_VALUE;
		double[] logW = new double[samples.size()];
		for (int i = 0; i < samples.size(); i++) {
			if (pSampler == 0) {
				logW[i] = -logP[i];
			} else {
				// //////////////important///////////////////////
				if(false && pSampler!=0){
				logW[i] = 0
						* samplerSet[pSampler-1]
								.logNormalizedProbability(samples.get(i)
										.getSample()) - logP[i];
				}else{
				  logW[i] =  - logP[i];
				}
				// /////////////important////////////////////////
			}
			if (Double.isNaN(logW[i])) {
				System.out.println("GENE MARGE");
				logW[i] = Double.NEGATIVE_INFINITY;
			}
			if (maxLogW < logW[i])
				maxLogW = logW[i];
		}

		double sumW = 0;
		for (int i = 0; i < samples.size(); i++) {
			double tempW = Math.exp(logW[i] - maxLogW);
			sumW += tempW;
			if (Double.isNaN(tempW)) {
				System.out.println("tempW");
			}
		}

		// //////////////depend on layer//////////////
		if (pSampler == 0) {
			for (int i = 0; i < samples.size(); i++) {
				double tempW = Math.exp(logW[i] - maxLogW);
				if (Double.isNaN(tempW)) {
					System.out.println("tempW");
				}
				allSamples.add(new WeightedSampleWithValue<SampleType>(tempW
						/ sumW, samples.get(i)));
			}
		} else if (false && pSampler == LEVEL - 1) {
			for (int i = 0; i < SAMPLE_NUM; i++) {
				allSamples.add(new WeightedSampleWithValue<SampleType>(1,
						samples.get(i)));
			}

		} else {
			for (int i = 0; i < samples.size(); i++) {
				if (samples.get(i).getValue() >= cutoffEnergy[pSampler - 1])
					break;
				double tempW = Math.exp(logW[i] - maxLogW);
				if (Double.isNaN(tempW)) {
					System.out.println("tempW");
				}
				allSamples.add(new WeightedSampleWithValue<SampleType>(tempW
						/ sumW, samples.get(i)));
			}
		}
		return allSamples;
	}

	private List<SampleWithValue<SampleType>> searchBestSamples(
			List<SampleWithValue<SampleType>> samples, int from, int to) {
		Collections.sort(samples, new SampleWithValueComparator());
		return samples.subList(from, to);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		Formatter formatter = new Formatter(sb);
		sb.append("ene:");
		for (int i = 0; i < LEVEL; i++) {
			formatter.format("%6.3f/%6.3f (%6.3f) || ", averageEnergy[i],
					cutoffEnergy[i], varianceEnergy[i]);
		}
		sb.append("\n");
		sb.append("bet:");
		for (int i = 0; i < LEVEL; i++) {
			// formatter.format("%6.3f ", inverseTemperature[i]);
		}
		sb.append("\n");
		if (true) {
			for (int i = LEVEL - 2; 0 < i && i < LEVEL; i++) {
				// sb.append(i + ":" + samplerSet[i].toString() + "\n");
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	public void debug_marginalizedSamples(
			List<WeightedSampleWithValue<SampleType>> allSamples) {
		StringBuilder sb = new StringBuilder();
		Formatter formatter = new Formatter(sb);
		int counter = 0;
		for (WeightedSampleWithValue<SampleType> wswv : allSamples) {
			if (counter++ % SAMPLE_NUM == 0)
				formatter.format("%6.3e ", wswv.getWeight());
		}
		System.out.println(sb);
	}
}
