package optimize.population.old;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import optimize.Annealing;
import optimize.function.CostFunction;
import optimize.util.LogSumExp;
import optimize.util.LogWeighted;
import optimize.util.LogWeightedObjectComparator;
import optimize.util.SampleWithValue;
import optimize.util.Weighted;
import optimize.util.WeightedObjectComparator;
import optimize.util.WeightedSampleWithValue;


import statistics.sampler.Sampler;

public abstract class PopTruncationAnnealing2<SampleType> implements
		Annealing<SampleType> {

	public static boolean RESAMPLING = true;

	protected int num_samples;

	protected int num_population;

	protected double cutoff;

	protected CostFunction<SampleType> cf;

	private Sampler<SampleType> sampler;

	private List<LogWeighted<SampleWithValue<SampleType>>> samples;

	// logWeight
	private List<LogWeighted<SampleWithValue<SampleType>>> population = null;

	private Random rand = new Random();

	public PopTruncationAnnealing2(CostFunction<SampleType> cf,
			int num_population, int num_sample, double cutoff) {
		this.cf = cf;
		this.num_population = num_population;
		this.num_samples = num_sample;
		this.cutoff = cutoff;
		population = new ArrayList<LogWeighted<SampleWithValue<SampleType>>>();
	}

	private SampleWithValue<SampleType> best = null;

	public SampleWithValue<SampleType> getBest() {
		Collections.sort(population, new LogWeightedSWVComparator());
		if (best == null
				|| population.get(0).getObject().getValue() < best.getValue())
			best = population.get(0).getObject();
		return best;
	}

	public void optimize() {
		if (population.size() == 0) initilize();

		// sampler = getMLSampler(population);
		// samples = sampling(sampler);

		// merage
		List<LogWeighted<SampleWithValue<SampleType>>> allSamples = mergeSamples();

		LogSumExp.normalize(allSamples);

		sampler = getMLSampler(allSamples);
		samples = sampling(sampler);

		// create population
		this.population = resampling(allSamples);
	}

	private void initilize() {
		this.sampler = getInitialSampler();
		population = sampling(this.sampler, num_population);
		normalizePopulation(population);
		samples = sampling(sampler);
		Collections.sort(population, new LogWeightedSWVComparator());
		best = population.get(0).getObject();
	}

	protected abstract Sampler<SampleType> getInitialSampler();

	protected Sampler<SampleType> getMLSampler(
			List<LogWeighted<SampleWithValue<SampleType>>> allSamples) {
		double logSum = LogSumExp.sum(allSamples);

		ArrayList<WeightedSampleWithValue<SampleType>> ldata = new ArrayList<WeightedSampleWithValue<SampleType>>(
				allSamples.size());

		for (LogWeighted<SampleWithValue<SampleType>> wswv : allSamples) {
			ldata.add(new WeightedSampleWithValue<SampleType>(Math.exp(wswv
					.getLogWeight()
					- logSum), wswv.getObject()));
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
		Collections.sort(population, new LogWeightedSWVComparator());
		// System.out.println(Math.exp(LogSumExp.calc((List<LogWeighted>)population)));
		double rate_weight_remainingSamples = 0;
		double cutoffValue = 0;
		{
			double weight_remainingSmaples = 0;
			int num = (int) (population.size() * (1 - cutoff));
			cutoffValue = population.get(num - 1).getObject().getValue();
			List<LogWeighted<SampleWithValue<SampleType>>> tempPop = new ArrayList<LogWeighted<SampleWithValue<SampleType>>>(
					population.size());
			weight_remainingSmaples = 0;
			for (LogWeighted<SampleWithValue<SampleType>> lwswv : population) {
				// if(count++<num){
				if (cutoffValue > lwswv.getObject().getValue()) {
					tempPop.add(lwswv);
					weight_remainingSmaples += lwswv.getExpLogWeight();
				} else {
					break;
				}
			}
			rate_weight_remainingSamples = weight_remainingSmaples
					/ num_population;

			if (tempPop.size() > 0) {
				population = tempPop;
			} else {
				rate_weight_remainingSamples = 1;
			}
		}
		// cut samples
		{
			Collections.sort(samples, new LogWeightedSWVComparator());
			ArrayList<LogWeighted<SampleWithValue<SampleType>>> tempSamp = new ArrayList<LogWeighted<SampleWithValue<SampleType>>>(
					samples.size());
			for (LogWeighted<SampleWithValue<SampleType>> lwswv : samples) {
				if (cutoffValue <= lwswv.getObject().getValue()) break;
				tempSamp.add(lwswv);
			}
			samples = tempSamp;
		}

		double logZ = 0;
		{
			if (true) {
				double partLogZ[] = new double[samples.size()];
				for (int i = 0; i < samples.size(); i++) {
					partLogZ[i] = -sampler.logNormalizedProbability(samples
							.get(i).getObject().getSample());
				}
				logZ = LogSumExp.sum(partLogZ) - Math.log(num_samples);
				if (Double.isInfinite(logZ)) {
					//System.out.println();
				}
			} else {
				double partLogZ[] = new double[population.size()];
				for (int i = 0; i < population.size(); i++) {
					partLogZ[i] = Math.log(population.get(i).getExpLogWeight())
							+ sampler.logNormalizedProbability(population
									.get(i).getObject().getSample());
				}
				logZ = LogSumExp.sum(partLogZ)
						- Math.log(num_population
								* rate_weight_remainingSamples);
				logZ = -logZ;
				if(Double.isInfinite(logZ)){
					logZ=-logZ;
				}
			}
		}

		ArrayList<LogWeighted<SampleWithValue<SampleType>>> allSamples = new ArrayList<LogWeighted<SampleWithValue<SampleType>>>(
				num_population + num_samples);
		allSamples.addAll(population);
		allSamples.addAll(samples);

		double[] mrate = new double[2];
		mrate[0] = (double) num_population / allSamples.size();
		mrate[1] = (double) num_samples / allSamples.size();

		System.out.println(rate_weight_remainingSamples);
		
		for (LogWeighted<SampleWithValue<SampleType>> wswv : allSamples) {
			double prob[] = new double[2];
			prob[0] = Math.log(mrate[0] * (rate_weight_remainingSamples));
			prob[1] = Math.log(mrate[1])
					+ sampler.logNormalizedProbability(wswv.getObject()
							.getSample()) + logZ;
			double gLogProb = LogSumExp.sum(prob);
			if (!RESAMPLING) gLogProb = 0;
			wswv.setLogWeight(wswv.getLogWeight() - gLogProb);
			if (Double.isNaN(wswv.getLogWeight())) {
				System.out.println("aaa");
			}
		}

		return allSamples;
	}

	private List<LogWeighted<SampleWithValue<SampleType>>> resampling(
			List<LogWeighted<SampleWithValue<SampleType>>> allSamples) {
		LogSumExp.normalize(allSamples);

		List<Weighted<LogWeighted<SampleWithValue<SampleType>>>> list = new ArrayList<Weighted<LogWeighted<SampleWithValue<SampleType>>>>(
				allSamples.size());
		for (LogWeighted<SampleWithValue<SampleType>> wswv : allSamples) {
			double randWeight = Math.exp(wswv.getLogWeight())
					* rand.nextDouble();
			Weighted<LogWeighted<SampleWithValue<SampleType>>> wo = new Weighted<LogWeighted<SampleWithValue<SampleType>>>(
					randWeight, wswv);
			list.add(wo);
		}

		Collections.sort(list, new WeightedObjectComparator());
		Collections.reverse(list);

		if (!(list.size() < num_population))
			list = list.subList(0, num_population);

		allSamples.clear();
		for (Weighted<LogWeighted<SampleWithValue<SampleType>>> wlswv : list) {
			if (wlswv.weight == 0) continue;
			allSamples.add(wlswv.getObject());
			// wlswv.getObject().setLogWeight(1);// smoothing
		}

		normalizePopulation(allSamples);

		// for (LogWeighted<SampleWithValue<SampleType>> wswv : allSamples) {
		// System.out.println(wswv);
		// }
		return allSamples;
	}

	private void normalizePopulation(
			List<LogWeighted<SampleWithValue<SampleType>>> allSamples) {
		LogSumExp.normalize(allSamples);
		for (LogWeighted<SampleWithValue<SampleType>> lswv : allSamples) {
			lswv.setLogWeight(lswv.getLogWeight() + Math.log(num_population));
		}
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
			System.out.println(lwswv);
		}
		return "avg:" + calcAverage(population) + "\n" + "var:"
				+ calcVariance(population, calcAverage(population)) + "\n"
				+ sampler;
	}

	class LogWeightedSWVComparator implements
			Comparator<LogWeighted<SampleWithValue<SampleType>>> {

		public int compare(LogWeighted<SampleWithValue<SampleType>> o1,
				LogWeighted<SampleWithValue<SampleType>> o2) {
			if (o1.getObject().getValue() > o2.getObject().getValue())
				return 1;
			if (o1.getObject().getValue() < o2.getObject().getValue())
				return -1;
			return 0;
		}

	}

}
