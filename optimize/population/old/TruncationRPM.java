package optimize.population.old;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import optimize.Annealing;
import optimize.function.CostFunction;
import optimize.util.LogSumExp;
import optimize.util.LogWeighted;
import optimize.util.LogWeightedObjectComparator;
import optimize.util.LogWeightedSWVComparator;
import optimize.util.SampleWithValue;
import optimize.util.Weighted;
import optimize.util.WeightedSampleWithValue;
import statistics.sampler.Sampler;

/*
 * The differences from ver.2 are
 * 1. the number of effective samples is defined by the entropy
 * 2. The procedure of RWOR is corrected
 */
public abstract class TruncationRPM<SampleType> implements
		Annealing<SampleType> {

	protected int num_samples;

	protected int num_population;

	protected double cutoffRate;

	protected CostFunction<SampleType> cf;

	private Sampler<SampleType> sampler;

	private List<LogWeighted<SampleWithValue<SampleType>>> samples = null;

	private List<LogWeighted<SampleWithValue<SampleType>>> population = null;

	private Random rand = new Random();

	public TruncationRPM(CostFunction<SampleType> cf, int num_population,
			int num_sample, double cutoff) {
		this.cf = cf;
		this.num_population = num_population;
		this.num_samples = num_sample;
		this.cutoffRate = cutoff;
		population = new ArrayList<LogWeighted<SampleWithValue<SampleType>>>();
	}

	private SampleWithValue<SampleType> best = null;

	public SampleWithValue<SampleType> getBest() {
		Collections
				.sort(population, new LogWeightedSWVComparator<SampleType>());
		if (best == null
				|| population.get(0).getObject().getValue() < best.getValue())
			best = population.get(0).getObject();
		return best;
	}

	public void optimize() {
		if (population.size() == 0) {
			initilize();
			return;
		}

		// merage
		List<LogWeighted<SampleWithValue<SampleType>>> allSamples = mergeSamples();
		sampler = getMLSampler(allSamples);
		samples = sampling(sampler);

		// create population
		this.population = resamplingStep(allSamples);
	}

	private void initilize() {
		this.sampler = getInitialSampler();
		population = sampling(this.sampler, num_population);
		// normalizePopulation(population);
		samples = sampling(sampler);
		Collections
				.sort(population, new LogWeightedSWVComparator<SampleType>());
		best = population.get(0).getObject();
	}

	protected abstract Sampler<SampleType> getInitialSampler();

	protected Sampler<SampleType> getMLSampler(
			List<LogWeighted<SampleWithValue<SampleType>>> allSamples) {
		LogSumExp.normalize(allSamples);

		ArrayList<WeightedSampleWithValue<SampleType>> ldata = new ArrayList<WeightedSampleWithValue<SampleType>>(
				allSamples.size());

		for (LogWeighted<SampleWithValue<SampleType>> wswv : allSamples) {
			double weight = wswv.getExpLogWeight();
			if (weight == 0) continue;
			ldata.add(new WeightedSampleWithValue<SampleType>(weight, wswv
					.getObject()));
		}

		sampler.fittingUpdate(ldata);
		return sampler;
	}

	private List<LogWeighted<SampleWithValue<SampleType>>> sampling(
			Sampler<SampleType> sampler, int num) {
		List<SampleType> samples = sampler.sampling(num);
		List<LogWeighted<SampleWithValue<SampleType>>> wsamples = new ArrayList<LogWeighted<SampleWithValue<SampleType>>>(
				num_samples);
		for (SampleType st : samples) {
			// eval samples
			wsamples.add(new LogWeighted<SampleWithValue<SampleType>>(0,
					new SampleWithValue<SampleType>(st, cf.eval(st))));
		}
		return wsamples;
	}

	private List<LogWeighted<SampleWithValue<SampleType>>> sampling(
			Sampler<SampleType> sampler) {
		return sampling(sampler, num_samples);
	}

	private List<LogWeighted<SampleWithValue<SampleType>>> mergeSamples() {
		LogSumExp.normalize(population);
		Collections
				.sort(population, new LogWeightedSWVComparator<SampleType>());
		double cutoffValue = Double.MAX_VALUE;
		{
			int num = (int) Math.ceil((population.size() * (1 - cutoffRate)));
			// int num = (int) Math.ceil((num_population * (1 - cutoffRate)));
			cutoffValue = population.get(num - 1).getObject().getValue();
			// System.out.println("cutoff energy is :"+cutoffValue);
		}

		// cut population
		double num_effectivePop = getNumberOfEffectiveSamples(population);

		double rate_weight_remainingSamples = 0;
		List<LogWeighted<SampleWithValue<SampleType>>> selectedPopulation = null;
		{
			double weight_remainingSmaples = 0;
			selectedPopulation = new ArrayList<LogWeighted<SampleWithValue<SampleType>>>(
					population.size());
			weight_remainingSmaples = 0;
			for (LogWeighted<SampleWithValue<SampleType>> lwswv : population) {
				// selection
				if (cutoffValue <= lwswv.getObject().getValue()) break;
				selectedPopulation.add(lwswv);
				weight_remainingSmaples += lwswv.getExpLogWeight();
			}
			rate_weight_remainingSamples = weight_remainingSmaples;

			if (selectedPopulation.size() == 0) {
				System.out.println("pop errror");
				rate_weight_remainingSamples = 1;
			}
		}

		// cut samples
		ArrayList<LogWeighted<SampleWithValue<SampleType>>> selectedSamples = new ArrayList<LogWeighted<SampleWithValue<SampleType>>>(
				samples.size());
		{
			Collections.sort(samples,
					new LogWeightedSWVComparator<SampleType>());
			for (LogWeighted<SampleWithValue<SampleType>> lwswv : samples) {
				// selection
				if (cutoffValue <= lwswv.getObject().getValue()) break;
				selectedSamples.add(lwswv);
			}
		}

		double logZ = 0;
		{
			if (true) {
				double partLogZ[] = new double[selectedSamples.size()];
				for (int i = 0; i < selectedSamples.size(); i++) {
					partLogZ[i] = -sampler
							.logNormalizedProbability(selectedSamples.get(i)
									.getObject().getSample());
				}
				logZ = LogSumExp.sum(partLogZ) - Math.log(samples.size());
				// if (Double.isInfinite(logZ)) {
				// System.out.println("error logZ:"+logZ);
				// }
			} else {
				// old bug: selected samples have to be used.
				double partLogZ[] = new double[samples.size()];
				for (int i = 0; i < selectedSamples.size(); i++) {
					partLogZ[i] = -sampler.logNormalizedProbability(samples
							.get(i).getObject().getSample());
				}
				logZ = LogSumExp.sum(partLogZ) - Math.log(samples.size());
				if (Double.isInfinite(logZ)) {
					System.out.println("error logZ");
				}
			}
		}

		// update weight
		for (LogWeighted<SampleWithValue<SampleType>> individual : selectedPopulation) {
			individual.setLogWeight(individual.getLogWeight()
					+ Math.log(num_effectivePop));
		}
		ArrayList<LogWeighted<SampleWithValue<SampleType>>> allSamples = new ArrayList<LogWeighted<SampleWithValue<SampleType>>>(
				num_population + num_samples);
		allSamples.addAll(selectedPopulation);
		allSamples.addAll(selectedSamples);

		double[] mrate = new double[2];
		mrate[0] = num_effectivePop / (num_effectivePop + num_samples);
		mrate[1] = (double) num_samples / (num_effectivePop + num_samples);

		// System.out.println(rate_weight_remainingSamples);

		for (LogWeighted<SampleWithValue<SampleType>> wswv : allSamples) {
			double prob[] = new double[2];
			prob[0] = Math.log(mrate[0] * (rate_weight_remainingSamples));
			prob[1] = Math.log(mrate[1])
					+ sampler.logNormalizedProbability(wswv.getObject()
							.getSample()) + logZ;
			double gLogProb = LogSumExp.sum(prob);
			wswv.setLogWeight(wswv.getLogWeight() - gLogProb);
			if (Double.isNaN(wswv.getLogWeight())) {
				System.out.println("aaa");
			}
		}

		return allSamples;
	}

	private List<LogWeighted<SampleWithValue<SampleType>>> resamplingStep(
			List<LogWeighted<SampleWithValue<SampleType>>> allSamples) {
		switch (4) {
		case 1:
			return resamplingWOR(allSamples);
		case 2:
			return simplyRandomizedResampling(allSamples);
		case 4:
			return simplyBestResampling(allSamples);

		}
		return null;
	}

	private List<LogWeighted<SampleWithValue<SampleType>>> resamplingWOR(
			List<LogWeighted<SampleWithValue<SampleType>>> allSamples) {

		LogSumExp.normalize(allSamples);

		Collections.sort(allSamples, new Comparator<LogWeighted>() {
			public int compare(LogWeighted o1, LogWeighted o2) {
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
			if (allSamples.size() >= num_population || list.size() <= 0) break;
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

	private List<LogWeighted<SampleWithValue<SampleType>>> simplyRandomizedResampling(
			List<LogWeighted<SampleWithValue<SampleType>>> allSamples) {
		// Additionally the samples with 0 weight are removed
		LogSumExp.normalize(allSamples);

		List<Weighted<LogWeighted<SampleWithValue<SampleType>>>> list = new ArrayList<Weighted<LogWeighted<SampleWithValue<SampleType>>>>(
				allSamples.size());
		for (LogWeighted<SampleWithValue<SampleType>> wswv : allSamples) {
			double randWeight = wswv.getExpLogWeight() * rand.nextDouble();
			Weighted<LogWeighted<SampleWithValue<SampleType>>> wo = new Weighted<LogWeighted<SampleWithValue<SampleType>>>(
					randWeight, wswv);
			list.add(wo);
		}

		Collections.sort(list, new Comparator<Weighted>() {
			public int compare(Weighted o1, Weighted o2) {
				if (o1.getWeight() > o2.getWeight()) return -1;
				if (o1.getWeight() < o2.getWeight()) return 1;
				return 0;
			}
		});

		if (!(list.size() < num_population))
			list = list.subList(0, num_population);

		allSamples.clear();
		for (Weighted<LogWeighted<SampleWithValue<SampleType>>> wlswv : list) {
			if (wlswv.weight == 0) continue;
			allSamples.add(wlswv.getObject());
		}
		return allSamples;
	}

	private List<LogWeighted<SampleWithValue<SampleType>>> simplyBestResampling(
			List<LogWeighted<SampleWithValue<SampleType>>> allSamples) {
		// Additionally the samples with 0 weight are removed
		LogSumExp.normalize(allSamples);

		List<Weighted<LogWeighted<SampleWithValue<SampleType>>>> list = new ArrayList<Weighted<LogWeighted<SampleWithValue<SampleType>>>>(
				allSamples.size());
		for (LogWeighted<SampleWithValue<SampleType>> wswv : allSamples) {
			double randWeight = wswv.getExpLogWeight();
			Weighted<LogWeighted<SampleWithValue<SampleType>>> wo = new Weighted<LogWeighted<SampleWithValue<SampleType>>>(
					randWeight, wswv);
			list.add(wo);
		}

		Collections.sort(list, new Comparator<Weighted>() {
			public int compare(Weighted o1, Weighted o2) {
				if (o1.getWeight() > o2.getWeight()) return -1;
				if (o1.getWeight() < o2.getWeight()) return 1;
				return 0;
			}
		});

		if (!(list.size() < num_population))
			list = list.subList(0, num_population);

		allSamples.clear();
		for (Weighted<LogWeighted<SampleWithValue<SampleType>>> wlswv : list) {
			if (wlswv.weight == 0) continue;
			allSamples.add(wlswv.getObject());
		}
		return allSamples;
	}

	private double getNumberOfEffectiveSamples(
			List<LogWeighted<SampleWithValue<SampleType>>> population) {
		if (false) return population.size();
		LogSumExp.normalize(population);
		double entropy = 0;
		for (LogWeighted<SampleWithValue<SampleType>> sample : population) {
			double prob = sample.getExpLogWeight();
			if (Double.isInfinite(Math.log(prob))) continue;
			entropy += -prob * Math.log(prob);
		}
		return Math.exp(entropy);
	}

	private double calcVariance(
			List<LogWeighted<SampleWithValue<SampleType>>> samples,
			double average) {
		double sumWeight = 0;
		double sumVariance = 0;
		for (LogWeighted<SampleWithValue<SampleType>> swv : samples) {
			double dist = swv.getObject().getValue() - average;
			sumVariance += swv.getExpLogWeight() * dist * dist;
			sumWeight += swv.getExpLogWeight();
		}
		double variance = sumVariance / sumWeight;
		return variance;
	}

	private double calcAverage(
			List<LogWeighted<SampleWithValue<SampleType>>> samples) {
		double sumWeight = 0;
		double sumAverage = 0;
		for (LogWeighted<SampleWithValue<SampleType>> swv : samples) {
			sumAverage += swv.getExpLogWeight() * swv.getObject().getValue();
			sumWeight += swv.getExpLogWeight();
		}
		double average = sumAverage / sumWeight;
		return average;
	}

	public double getVariance() {
		return calcVariance(population, calcAverage(population));
	}

	public String toString() {
		Collections.sort(population, new LogWeightedObjectComparator());
		for (LogWeighted<SampleWithValue<SampleType>> lwswv : population) {
			// System.out.println(lwswv);
		}
		return getNumberOfEffectiveSamples(population) + "\n" + "avg:"
				+ calcAverage(population) + "\n" + "var:"
				+ calcVariance(population, calcAverage(population)) + "\n"
				+ sampler;
	}

}
