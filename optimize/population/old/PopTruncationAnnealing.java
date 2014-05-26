package optimize.population.old;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import optimize.Optimizer;
import optimize.function.CostFunction;
import optimize.util.LogSumExp;
import optimize.util.SampleWithValue;
import optimize.util.SampleWithValueComparator;
import optimize.util.WeightedSampleWithValue;

import statistics.sampler.ProbabilityWithFactor;
import statistics.sampler.Sampler;
import statistics.sampler.TruncationFactor;

public abstract class PopTruncationAnnealing<SampleType> implements
		Optimizer<SampleType> {

	public static final boolean POPULATION_OFF = false;

	protected int num_samples;

	protected int num_population;

	protected double cutoff;

	protected CostFunction<SampleType> cf;

	private TruncationFactor factor = new TruncationFactor();

	private Sampler<SampleType> sampler;

	private double logZ;

	private ProbabilityWithFactor probwf;

	// protected Sampler<SampleType> sampler;

	private List<SampleWithValue<SampleType>> population = null;

	private Random rand = new Random();

	public PopTruncationAnnealing(CostFunction cf, int num_population,
			int num_sample, double cutoff) {
		this.cf = cf;
		this.num_population = num_population;
		this.num_samples = num_sample;
		this.cutoff = cutoff;
		population = new ArrayList<SampleWithValue<SampleType>>();
	}

	public SampleWithValue<SampleType> getBest() {
		// TODO Auto-generated method stub
		return null;
	}

	public void optimize() {
		// build sampler
		if (population.size() == 0) {
			sampler = getInitialSampler();
		} else {
			List<WeightedSampleWithValue<SampleType>> learnData = new ArrayList<WeightedSampleWithValue<SampleType>>(
					population.size());
			for (SampleWithValue<SampleType> swv : population) {
				// if(rand.nextDouble()<0)continue;
				learnData.add(new WeightedSampleWithValue<SampleType>(1, swv));
			}
			sampler = getMLSampler(learnData);
		}

		Sampler<SampleType> samp = sampler;
		// generate samples
		List<SampleWithValue<SampleType>> newsamples = null;
		if (population.size() == 0)
			newsamples = sampling(samp, num_population + num_samples);
		else
			newsamples = sampling(samp);
		if (POPULATION_OFF) population.clear();

		// calc marginal probability
		List<SampleWithValue<SampleType>> allSamples = new ArrayList<SampleWithValue<SampleType>>(
				population.size() + newsamples.size());
		allSamples.addAll(population);
		allSamples.addAll(newsamples);

		// sort
		Collections.sort(allSamples, new SampleWithValueComparator());

		double[] mrate = new double[2];

		mrate[0] = (double) population.size() / allSamples.size();
		mrate[1] = (double) newsamples.size() / allSamples.size();

		double[] gLogProb = new double[allSamples.size()];
		for (int i = 0; i < allSamples.size(); i++) {
			double prob[] = new double[2];
			if (population.size() != 0) {
				prob[0] = Math.log(mrate[0])
						+ probwf.logNormalizedProbability(allSamples.get(i)
								.getValue());
			} else {
				prob[0] = Double.NEGATIVE_INFINITY;
			}
			prob[1] = Math.log(mrate[1])
					+ samp.logNormalizedProbability(allSamples.get(i)
							.getSample());
			gLogProb[i] = LogSumExp.sum(prob);
		}

		// update target distribution
		if (population.size() == 0) {
			Collections.sort(newsamples, new SampleWithValueComparator());
			double cutEnergy = newsamples.get(num_population).getValue();
			factor.setThreshold(cutEnergy);
		} else if (false) {
			Collections.sort(newsamples, new SampleWithValueComparator());
			double cutEnergy = newsamples.get(
					(int) (newsamples.size() * (1 - cutoff))).getValue();
			factor.setThreshold(cutEnergy);
		} else {
			if (population.size() != 0) {
				double cutEnergy = population.get(
						(int) (population.size() * (1 - cutoff))).getValue();
				factor.setThreshold(cutEnergy);
			}
		}

		// calc Z
		double[] likeRatio = new double[allSamples.size()];
		for (int i = 0; i < allSamples.size(); i++) {
			likeRatio[i] = factor.logFactor(allSamples.get(i).getValue())
					- gLogProb[i];
		}
		double sum = LogSumExp.sum(likeRatio);
		logZ = sum - Math.log(allSamples.size());
		probwf = new ProbabilityWithFactor(factor, logZ);

		// calc weight
		double[] weight = new double[allSamples.size()];
		for (int i = 0; i < allSamples.size(); i++) {
			weight[i] = Math.exp(likeRatio[i] - sum);
		}

		// make population via resampling
		population = resampling(allSamples, weight);
	}

	private List<SampleWithValue<SampleType>> resampling(
			List<SampleWithValue<SampleType>> allSamples, double[] weight) {
		double sumWeight = 0;
		for (double d : weight) {
			sumWeight += d;
		}

		double[] randNum = new double[num_population];
		for (int i = 0; i < randNum.length; i++) {
			// randNum[i] = rand.nextDouble();
			randNum[i] = 1d / (randNum.length + 1) * (i + 1);
		}
		Arrays.sort(randNum);
		List<SampleWithValue<SampleType>> pop = new ArrayList<SampleWithValue<SampleType>>();
		{
			int i = 0;
			int j = 0;
			double cumWeight = weight[0];
			while (true) {
				if (cumWeight < randNum[i] * sumWeight) {
					j++;
					cumWeight += weight[j];
				} else {
					pop.add(allSamples.get(j));
					if (i == randNum.length - 1) break;
					i++;
				}
			}
		}
		return pop;
	}

	protected Sampler<SampleType> getMLSampler(
			List<WeightedSampleWithValue<SampleType>> allSamples) {
		sampler.fittingUpdate(allSamples);
		return sampler;
	}

	protected abstract Sampler<SampleType> getInitialSampler();

	private List<SampleWithValue<SampleType>> sampling(
			Sampler<SampleType> sampler) {
		return sampling(sampler, num_samples);
	}

	private List<SampleWithValue<SampleType>> sampling(
			Sampler<SampleType> sampler, int num) {
		List<SampleType> samples = sampler.sampling(num);
		List<SampleWithValue<SampleType>> samplesWithValue = new ArrayList<SampleWithValue<SampleType>>(
				num);
		for (SampleType st : samples) {
			// eval samples
			samplesWithValue.add(new SampleWithValue<SampleType>(st, cf
					.eval(st)));
		}
		return samplesWithValue;
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

	public String toString() {
		return "avg:" + calcAverage(population) + "\n" + "var:"
				+ calcVariance(population, calcAverage(population)) + "\n"
				+ sampler;

	}

}
