package optimize.his;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import java.util.Random;

import optimize.Optimizer;
import optimize.function.CostFunction;
import optimize.util.LogSumExp;
import optimize.util.SampleWithValue;
import optimize.util.SampleWithValueComparator;
import optimize.util.WeightedSampleWithValue;

import statistics.sampler.Sampler;

public class TruncationHIS<SampleType> implements Optimizer<SampleType> {

	protected final int SAMPLE_NUM;

	protected final int LEVEL;

	protected final CostFunction<SampleType> cf;

	protected final Sampler<SampleType>[] samplerSet;

	private final List<SampleWithValue<SampleType>>[] sampleWithValueSet;

	private final double[] averageEnergy;

	private final double[] varianceEnergy;

	private final double[] cutoffEnergy;

	private final double[] entropy;

	protected boolean CONTINUOUS = false;

	private int pSampler = 0;

	public SampleWithValue<SampleType> getBest() {
		List<SampleWithValue<SampleType>> list = new ArrayList<SampleWithValue<SampleType>>(
				this.SAMPLE_NUM * LEVEL);
		for (int i = 0; i < LEVEL; i++) {
			list.addAll(this.sampleWithValueSet[i]);
		}
		return searchBestSamples(list, 0, 1).get(0);
	}

	public TruncationHIS(int sample_num, int level, CostFunction<SampleType> cf) {
		this.SAMPLE_NUM = sample_num;
		this.LEVEL = level;
		this.samplerSet = new Sampler[LEVEL];
		this.sampleWithValueSet = new List[LEVEL];
		this.averageEnergy = new double[LEVEL];
		this.varianceEnergy = new double[LEVEL];
		this.cutoffEnergy = new double[LEVEL];
		this.entropy = new double[LEVEL];
		this.cf = cf;
	}

	protected void init() {
		for (int i = 0; i < LEVEL; i++) {
			samplingAndAveraging(i);
			cutoffEnergy[i] = Double.POSITIVE_INFINITY;
		}
	}

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
	}

	private List<SampleWithValue<SampleType>> getPartOfSamples(int pSampler) {
		List<SampleWithValue<SampleType>> list = new ArrayList<SampleWithValue<SampleType>>(
				this.SAMPLE_NUM * 3);
		for (int i = pSampler - 1; i <= pSampler + 1; i++) {
			if (i < 0) continue;
			if (LEVEL <= i) continue;
			list.addAll(this.sampleWithValueSet[i]);
		}
		return list;
	}

	private List<Sampler<SampleType>> getPartOfSampler(int pSampler) {
		List<Sampler<SampleType>> list = new ArrayList<Sampler<SampleType>>(3);
		for (int i = pSampler - 1; i <= pSampler + 1; i++) {
			if (i < 0) continue;
			if (LEVEL <= i) continue;
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

	Random rand = new Random();

	public void optimize() {

		// pSampler = (LEVEL + (pSampler + 1)) % LEVEL;
		pSampler = (LEVEL + (pSampler - 1)) % LEVEL;
		// pSampler = rand.nextInt(LEVEL);
		// shortcut
		if (true && pSampler == 0) {
			samplingAndAveraging(pSampler);
			cutoffEnergy[pSampler] = this.sampleWithValueSet[pSampler].get(
					SAMPLE_NUM - 1).getValue();
			return;
		}

		List<SampleWithValue<SampleType>> partSamples = getPartOfSamples(pSampler);
		List<Sampler<SampleType>> partSampler = getPartOfSampler(pSampler);

		List<WeightedSampleWithValue<SampleType>> allSamples = cutSamples(
				pSampler, partSamples, partSampler);

		if (allSamples.size() == 0) {
			// System.out.println("continue bo allsamples");
			// System.out.println("skip" + pSampler);
			samplingAndAveraging(pSampler);

			return;
		}
		// System.out.println(allSamples);
		samplerSet[pSampler].fittingUpdate(allSamples);
		// System.out.println(samplerSet[pSampler]);
		samplingAndAveraging(pSampler);
	}

	private List<WeightedSampleWithValue<SampleType>> cutSamples(
			int pSampler, List<SampleWithValue<SampleType>> samples,
			List<Sampler<SampleType>> listOfSampler) {
		// sort
		samples = searchBestSamples(samples, 0, samples.size());

		// calc logP : marginalization
		double[] logP = new double[samples.size()];
		for (int i = 0; i < samples.size(); i++) {
			double[] tempLogP = new double[listOfSampler.size()];
			for (int j = 0; j < listOfSampler.size(); j++) {
				tempLogP[j] = listOfSampler.get(j).logNormalizedProbability(
						samples.get(i).getSample());
				if (Double.isNaN(tempLogP[j])) {
					System.out.println(listOfSampler.get(j)
							.logNormalizedProbability(
									samples.get(i).getSample()));
				}
			}
			logP[i] = LogSumExp.sum(tempLogP)
					+ Math.log(1d / listOfSampler.size());
			if (Double.isNaN(logP[i])) {
				System.out.println("logp");
			}
		}

		// calc normalized weight;
		double[] logW = new double[samples.size()];
		for (int i = 0; i < samples.size(); i++) {
			logW[i] = -logP[i];// q(x)/p(x)=1/p(x)
		}
		LogSumExp.normalize(logW);

		// ///////set cutoff energy//////////
		if (pSampler == LEVEL - 1) {
			double tsum = 0;
			cutoffEnergy[pSampler] = samples.get(samples.size() - 1).getValue();
			double ent = Double.NEGATIVE_INFINITY;
			for (int i = 0; i < logW.length; i++) {
				if (0 < tsum) {
					ent -= Math.log(logP.length);
					entropy[pSampler] = ent;
					cutoffEnergy[pSampler] = samples.get(i).getValue();
					break;
				}
				tsum += Math.exp(logW[i]);
				ent = LogSumExp.sum(ent, -logP[i]);
			}
		} else {
			double sum[] = new double[3];
			for (int i = 0; i < 3; i++) {
				sum[i] = 0;
				if (i == 1) continue;
				for (int j = 0; j < samples.size(); j++) {
					if (samples.get(j).getValue() > cutoffEnergy[pSampler
							- (i - 1)]) break;
					sum[i] += Math.exp(logW[j]);
				}
			}
			sum[1] = Math.sqrt(sum[0] * sum[2]);

			cutoffEnergy[pSampler] = cutoffEnergy[pSampler - 1];
			double ent = Double.NEGATIVE_INFINITY;
			for (int j = 0; j < samples.size(); j++) {
				ent = LogSumExp.sum(ent, -logP[j]);
				if (Double.isNaN(ent)) {
					System.out.println("ent");
				}

				if (sum[1] < 0) {
					cutoffEnergy[pSampler] = samples.get(j).getValue();
					break;
				}
				sum[1] -= Math.exp(logW[j]);
			}
			ent -= Math.log(logP.length);
			entropy[pSampler] = ent;
		}

		List<WeightedSampleWithValue<SampleType>> allSamples = new ArrayList<WeightedSampleWithValue<SampleType>>(
				samples.size());
		{

			for (int i = 0; i < samples.size(); i++) {
				if (samples.get(i).getValue() >= cutoffEnergy[pSampler]) break;
				double w = Math.exp(logW[i]);
				if (w == 0) continue;
				allSamples.add(new WeightedSampleWithValue<SampleType>(w,
						samples.get(i)));
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
			formatter.format("%6.3f (%6.3f) || ", averageEnergy[i], Math
					.sqrt(varianceEnergy[i]));
		}
		sb.append("\n");
		sb.append("cut:");
		for (int i = 0; i < LEVEL; i++) {
			formatter.format("%6.3f ", cutoffEnergy[i]);
		}
		sb.append("\n");
		sb.append("ent:");
		for (int i = 0; i < LEVEL; i++) {
			formatter.format("%6.3f ", entropy[i]);
		}
		sb.append("\n");
		if (true) {
			for (int i = LEVEL - LEVEL; i < LEVEL; i++) {
				if (i < 0) continue;
				// sb.append("sampler" + i + ":\n" + samplerSet[i].toString()
				// + "\n");
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
