package optimize.hisrpm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import optimize.Optimizer;
import optimize.function.CostFunction;
import optimize.util.LogSumExp;
import optimize.util.LogWeighted;
import optimize.util.LogWeightedSWVComparator;
import optimize.util.SampleWithValue;
import optimize.util.SampleWithValueComparator;
import optimize.util.WeightedSampleWithValue;
import statistics.sampler.Sampler;

public class TruncationHISRPM<SampleType> implements Optimizer<SampleType> {

	protected final int POPULATION_NUM;

	protected final int SAMPLE_NUM;

	protected final int LEVEL;

	protected final CostFunction<SampleType> cf;

	protected final Sampler<SampleType>[] samplerSet;

	private final List<SampleWithValue<SampleType>>[] sampleWithValueSet;

	private final List<LogWeighted<SampleWithValue<SampleType>>>[] population;

	private final double[] averageEnergy;

	private final double[] varianceEnergy;

	private final double[] cutoffEnergy;

	private boolean isInitilized = false;

	private int pSampler = 0;

	public SampleWithValue<SampleType> getBest() {
		List<SampleWithValue<SampleType>> list = new ArrayList<SampleWithValue<SampleType>>(
				this.SAMPLE_NUM * LEVEL);
		for (int i = 0; i < LEVEL; i++) {
			list.addAll(this.sampleWithValueSet[i]);
		}
		return searchBestSamples(list, 0, 1).get(0);
	}

	public TruncationHISRPM(int pop_num, int sample_num, int level,
			CostFunction<SampleType> cf) {
		this.POPULATION_NUM = pop_num;
		this.SAMPLE_NUM = sample_num;
		this.LEVEL = level;
		this.samplerSet = new Sampler[LEVEL];
		this.sampleWithValueSet = new List[LEVEL];
		this.population = new List[LEVEL];
		this.averageEnergy = new double[LEVEL];
		this.varianceEnergy = new double[LEVEL];
		this.cutoffEnergy = new double[LEVEL];
		this.cf = cf;
	}

	private void init() {
		for (int i = 0; i < LEVEL; i++) {
			samplingAndAveraging(i);
			population[i] = new ArrayList<LogWeighted<SampleWithValue<SampleType>>>(
					POPULATION_NUM);
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
		if (!isInitilized) {
			init();
			isInitilized = true;
			return;
		}
		// pSampler = (LEVEL + (pSampler + 1)) % LEVEL;
		pSampler = (LEVEL + (pSampler - 1)) % LEVEL;
		// pSampler = rand.nextInt(LEVEL);

		if (true && pSampler == 0) {
			samplingAndAveraging(pSampler);
			cutoffEnergy[pSampler] = this.sampleWithValueSet[pSampler].get(
					SAMPLE_NUM - 1).getValue();
			return;
		}

		List<SampleWithValue<SampleType>> partSamples = getPartOfSamples(pSampler);
		List<Sampler<SampleType>> partSampler = getPartOfSampler(pSampler);

		List<LogWeighted<SampleWithValue<SampleType>>> allSamples = cutoffSamples(
				pSampler, partSamples, partSampler);
		//List<LogWeighted<?>> t=(List<LogWeighted<?>>)allSamples;
		LogSumExp.normalize(allSamples);

		List<WeightedSampleWithValue<SampleType>> lData = exchangeLData(allSamples);
		samplerSet[pSampler].fittingUpdate(lData);
		samplingAndAveraging(pSampler);

		// resampling
		population[pSampler] = resamplingWOR(allSamples);
	}

	private List<WeightedSampleWithValue<SampleType>> exchangeLData(
			List<LogWeighted<SampleWithValue<SampleType>>> samples) {
		List<WeightedSampleWithValue<SampleType>> ldata = new ArrayList<WeightedSampleWithValue<SampleType>>(
				samples.size());
		for (int i = 0; i < samples.size(); i++) {
			double w = samples.get(i).getExpLogWeight();
			if (w == 0) continue;
			ldata.add(new WeightedSampleWithValue<SampleType>(w, samples.get(i)
					.getObject()));
		}
		return ldata;
	}

	private List<LogWeighted<SampleWithValue<SampleType>>> resamplingWOR(
			List<LogWeighted<SampleWithValue<SampleType>>> allSamples) {

		Collections.sort(allSamples, new Comparator<LogWeighted<?>>() {
			public int compare(LogWeighted<?> o1, LogWeighted<?> o2) {
				if (o1.getLogWeight() > o2.getLogWeight()) return 1;
				if (o1.getLogWeight() < o2.getLogWeight()) return -1;
				return 0;
			}
		});

		List<LogWeighted<SampleWithValue<SampleType>>> list = new LinkedList<LogWeighted<SampleWithValue<SampleType>>>();
		for (LogWeighted<SampleWithValue<SampleType>> wswv : allSamples) {
			double weight = wswv.getExpLogWeight();
			// 0 cutting
			if (weight == 0) continue;
			list.add(wswv);
		}

		allSamples.clear();

		double sum_weight = 1;
		while (true) {
			if (allSamples.size() >= POPULATION_NUM || list.size() <= 0) break;
			double tmp = rand.nextDouble() * sum_weight;
			for (LogWeighted<SampleWithValue<SampleType>> ws : list) {
				tmp -= ws.getExpLogWeight();
				if (tmp <= 0) {
					list.remove(ws);
					allSamples.add(ws);
					sum_weight -= ws.getExpLogWeight();
					break;
				}
			}
			if (0 < tmp) sum_weight = 0;
		}

		return allSamples;
	}

	private List<LogWeighted<SampleWithValue<SampleType>>> cutoffSamples(
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
			}
			logP[i] = LogSumExp.sum(tempLogP)
					+ Math.log(1d / listOfSampler.size());
		}

		// calc normalized weight;
		double[] logW = new double[samples.size()];
		for (int i = 0; i < samples.size(); i++) {
			logW[i] = -logP[i];// q(x)/p(x)=1/p(x)
		}
		LogSumExp.normalize(logW);

		// ///////set cutoff energy//////////
		// double ZZ = 0;
		if (pSampler == LEVEL - 1) {
			cutoffEnergy[pSampler] = samples.get(0).getValue();
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
			// double oldSum1 = sum[1];
			sum[1] = Math.sqrt(sum[0] * sum[2]);
			// ZZ = sum[1] / oldSum1;
			cutoffEnergy[pSampler] = cutoffEnergy[pSampler - 1];

			for (int j = 0; j < samples.size(); j++) {
				sum[1] -= Math.exp(logW[j]);
				if (sum[1] <= 0) {
					cutoffEnergy[pSampler] = samples.get(j).getValue();
//					System.out.println(pSampler + ":" + cutoffEnergy[pSampler]);
					break;
				}
			}
		}

		// //////////////mix samples with population//////////////
		Collections.sort(population[pSampler],
				new LogWeightedSWVComparator<SampleType>());
		LogSumExp.normalize(population[pSampler]);

		// ZZ
		double remainsWeight = 0;
		for (LogWeighted<SampleWithValue<SampleType>> samp : population[pSampler]) {
			if (samp.getObject().getValue() > cutoffEnergy[pSampler]) break;
			remainsWeight += samp.getExpLogWeight();
		}
		double ZZ = remainsWeight;

		// logZ
		double logZ = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < samples.size(); i++) {
			if (samples.get(i).getValue() > cutoffEnergy[pSampler]) break;
			logZ = LogSumExp.sum(logZ, -logP[i]);
			logZ += Math.log(1d / samples.size());
		}

		// logPpop
		double[] logPpop = new double[population[pSampler].size()];
		for (int i = 0; i < population[pSampler].size(); i++) {
			double[] tempLogP = new double[listOfSampler.size()];
			for (int j = 0; j < listOfSampler.size(); j++) {
				tempLogP[j] = listOfSampler.get(j).logNormalizedProbability(
						population[pSampler].get(i).getObject().getSample());
			}
			logPpop[i] = LogSumExp.sum(tempLogP)
					+ Math.log(1d / listOfSampler.size());
		}

		// proc
		List<LogWeighted<SampleWithValue<SampleType>>> allSamples = new ArrayList<LogWeighted<SampleWithValue<SampleType>>>(
				samples.size());

		double eff = getNumberOfEffectiveSamples(population[pSampler]);
		double[] mrate = new double[2];
		mrate[0] = eff / (eff + samples.size());
		mrate[1] = 1d - mrate[0];

		// proc population
		for (int i = 0; i < population[pSampler].size(); i++) {
			if (population[pSampler].get(i).getObject().getValue() > cutoffEnergy[pSampler])
				break;
			double prob[] = new double[2];
			prob[0] = Math.log(mrate[0] * ZZ);
			prob[1] = Math.log(mrate[1]) + logPpop[i] + logZ;
			double gLogProb = LogSumExp.sum(prob);
			population[pSampler].get(i).setLogWeight(
					population[pSampler].get(i).getLogWeight() - gLogProb
							+ Math.log(eff));
			allSamples.add(population[pSampler].get(i));
		}

		// proc samples
		for (int i = 0; i < samples.size(); i++) {
			if (samples.get(i).getValue() > cutoffEnergy[pSampler]) break;
			double logProb[] = new double[2];
			logProb[0] = Math.log(mrate[0] * ZZ);
			logProb[1] = Math.log(mrate[1]) + logP[i] + logZ;
			double gLogProb = LogSumExp.sum(logProb);
			// System.out.println(logProb[1]+"vs"+gLogProb);
			LogWeighted<SampleWithValue<SampleType>> wswv = new LogWeighted<SampleWithValue<SampleType>>(
					-gLogProb, samples.get(i));
			allSamples.add(wswv);
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
		sb.append("bet:");
		for (int i = 0; i < LEVEL; i++) {
			formatter.format("%6.3f ", cutoffEnergy[i]);
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

	private double getNumberOfEffectiveSamples(
			List<LogWeighted<SampleWithValue<SampleType>>> population) {
		if (false) return population.size();
		if (population.size() == 0) return 0;
		LogSumExp.normalize(population);
		double entropy = 0;
		for (LogWeighted<SampleWithValue<SampleType>> sample : population) {
			double prob = sample.getExpLogWeight();
			if (Double.isInfinite(Math.log(prob))) continue;
			entropy += -prob * Math.log(prob);
		}
		return Math.exp(entropy);
	}
}
