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
import optimize.util.SampleWithValue;
import optimize.util.Weighted;
import optimize.util.WeightedSampleWithValue;

import statistics.sampler.Sampler;

/*
 * The differences from ver.2 are
 * 1. the number of effective samples is defined by the entropy
 * 2. The procedure of RWOR is corrected
 */
public abstract class BoltzRPM<SampleType> implements Annealing<SampleType> {

	protected int num_samples;

	protected int num_population;

	protected double cutoffRate;

	protected CostFunction<SampleType> cf;

	private Sampler<SampleType> sampler;

	private List<LogWeighted<SampleWithValue<SampleType>>> samples = null;

	private List<LogWeighted<SampleWithValue<SampleType>>> population = null;

	private Random rand = new Random();

	private double beta = 0;

	private double logZ = 0;

	private double average_from_sample = 0;

	public BoltzRPM(CostFunction<SampleType> cf, int num_population,
			int num_sample, double cutoff) {
		this.cf = cf;
		this.num_population = num_population;
		this.num_samples = num_sample;
		this.cutoffRate = cutoff;
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

	private int globalCounter = 0;

	private boolean isInitilized = false;

	public void optimize() {
		if (!isInitilized) {
			initilize();
			return;
		}

		// merage
		List<LogWeighted<SampleWithValue<SampleType>>> allSamples = mergeSamples();
		sampler = getMLSampler(allSamples);
		samples = sampling(sampler);

		// create population
		this.population = resamplingStep(allSamples);

		if (true) {
			System.out.println(""
					+ globalCounter
					+ " "
					+ (calcAverage(population) * beta + logZ)
					+ " "
					+ (average_from_sample * beta + logZ)
					+ " "
					+ Math.sqrt(calcVariance(population,
							calcAverage(population))) + " " + beta);
			globalCounter++;
		}
	}

	private void initilize() {
		this.sampler = getInitialSampler();
		population = sampling(this.sampler, num_population);
		// normalizePopulation(population);
		samples = sampling(sampler);
		Collections.sort(population, new LogWeightedSWVComparator());
		best = population.get(0).getObject();
		this.isInitilized = true;
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

	private double getEstimatedVariance(double beta) {
		double deltaBeta = beta - this.beta;
		double[] logWeight = new double[population.size()];
		int count = 0;
		for (LogWeighted<SampleWithValue<SampleType>> sample : population) {
			logWeight[count] = sample.getLogWeight();
			logWeight[count] *= deltaBeta * sample.getObject().getValue();
			count++;
		}
		LogSumExp.normalize(logWeight);

		double average = 0;
		double squaredAverage = 0;
		count = 0;
		for (LogWeighted<SampleWithValue<SampleType>> sample : population) {
			average += Math.exp(logWeight[count])
					* sample.getObject().getValue();
			squaredAverage += Math.exp(logWeight[count])
					* sample.getObject().getValue()
					* sample.getObject().getValue();
			count++;
		}
		return squaredAverage - average * average;
	}

	private List<LogWeighted<SampleWithValue<SampleType>>> mergeSamples() {
		LogSumExp.normalize(population);
		double num_effectivePop = getNumberOfEffectiveSamples(population);
		// Collections.sort(population, new LogWeightedSWVComparator());

		// adjust beta

		double deltaS = Math.log(1 - cutoffRate);

		double deltaBeta = 0;
		double var = getVariance();
		if (num_effectivePop > 2)
			var *= num_effectivePop / (num_effectivePop - 1);
		switch (1) {
		case 0://
			deltaBeta = 0.01;
			break;
		case 1:// ERS

			deltaBeta = Math.sqrt(beta * beta - 2 * deltaS / var) - beta;
			break;
		case 4:
			for (int i = 0; i < 100; i++) {
				deltaBeta = Math.sqrt(beta * beta - 2 * deltaS / var) - beta;
				var = getEstimatedVariance(beta + deltaBeta);
			}
			break;
		case 2: {
			double k = 0;
			double std0 = Math.sqrt(getVariance());
			double newBeta = beta;
			for (int i = 0; i < 100; i++) {
				deltaBeta = beta
						* beta
						* (0.5 * std0 * std0 + beta
								* (2d / 3 * std0 * k + beta * 0.25 * k * k))
						- deltaS;
				deltaBeta -= newBeta * newBeta * newBeta
						* (2d / 3 * std0 * k + newBeta * 0.25 * k * k);
				deltaBeta *= 2 / (std0 * std0);
				deltaBeta = Math.sqrt(deltaBeta) - beta;
				double deltaSTD = Math.sqrt(getEstimatedVariance(beta
						+ deltaBeta))
						- std0;
				k = deltaSTD / deltaBeta;
				newBeta = beta + deltaBeta;
			}
		}
			break;
		case 3:// SDS
			deltaBeta = Math.sqrt(-0.001 * deltaS / var);
			break;
		}
		beta += deltaBeta;

		// update weight
		double logZZ = 0;
		{
			double[] logQratio = new double[population.size()];
			int count = 0;
			for (LogWeighted<SampleWithValue<SampleType>> x : population) {
				logQratio[count] = x.getLogWeight() - x.getObject().getValue()
						* deltaBeta;
				count += 1;
			}
			logZZ = LogSumExp.sum(logQratio);
		}

		{
			double[] logPQratio = new double[samples.size()];
			int count = 0;
			for (LogWeighted<SampleWithValue<SampleType>> x : samples) {
				logPQratio[count] = -x.getObject().getValue()
						* beta
						- sampler.logNormalizedProbability(x.getObject()
								.getSample());
				count += 1;
			}
			logZ = LogSumExp.sum(logPQratio);
			logZ += Math.log(1d / samples.size());
		}

		{// optional: to calc the average over generated samples
			average_from_sample = 0;
			double[] logPQratio = new double[samples.size()];
			int count = 0;
			for (LogWeighted<SampleWithValue<SampleType>> x : samples) {
				logPQratio[count] = -x.getObject().getValue()
						* beta
						- sampler.logNormalizedProbability(x.getObject()
								.getSample());
				count += 1;
			}
			LogSumExp.normalize(logPQratio);
			count = 0;
			for (LogWeighted<SampleWithValue<SampleType>> x : samples) {
				average_from_sample += Math.exp(logPQratio[count])
						* x.getObject().getValue();
				count += 1;
			}
		}

		for (LogWeighted<SampleWithValue<SampleType>> individual : population) {
			individual.setLogWeight(individual.getLogWeight()
					+ Math.log(num_effectivePop));
		}
		ArrayList<LogWeighted<SampleWithValue<SampleType>>> allSamples = new ArrayList<LogWeighted<SampleWithValue<SampleType>>>(
				num_population + num_samples);
		allSamples.addAll(population);
		allSamples.addAll(samples);

		double[] mrate = new double[2];
		mrate[0] = num_effectivePop / (num_effectivePop + num_samples);
		mrate[1] = 1 - mrate[0];

		for (LogWeighted<SampleWithValue<SampleType>> wswv : allSamples) {
			double logRatio_q = wswv.getObject().getValue() * deltaBeta + logZZ;
			double logRatio_pq = sampler.logNormalizedProbability(wswv
					.getObject().getSample())
					+ wswv.getObject().getValue() * beta + logZ;
			double prob[] = new double[2];
			prob[0] = Math.log(mrate[0]) + logRatio_q;
			prob[1] = Math.log(mrate[1]) + logRatio_pq;
			double gLogProb = LogSumExp.sum(prob);
			wswv.setLogWeight(wswv.getLogWeight() - gLogProb);// multiplication
			if (Double.isNaN(wswv.getLogWeight())) {
				System.out.println("aaa");
			}
		}
		return allSamples;
	}

	private List<LogWeighted<SampleWithValue<SampleType>>> resamplingStep(
			List<LogWeighted<SampleWithValue<SampleType>>> allSamples) {
		switch (3) {
		case 1:
			return resamplingWOR(allSamples);
		case 2:
			return simplyRandomizedResampling(allSamples);
		case 3:
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
			if (Double.isInfinite(sample.getLogWeight())) continue;
			entropy += -prob * sample.getLogWeight();
		}
		return Math.exp(entropy);
	}

	private double calcVariance(
			List<LogWeighted<SampleWithValue<SampleType>>> samples,
			double average) {
		LogSumExp.normalize(samples);
		double sumVariance = 0;
		for (LogWeighted<SampleWithValue<SampleType>> swv : samples) {
			double dist = swv.getObject().getValue() - average;
			sumVariance += swv.getExpLogWeight() * dist * dist;
		}
		double variance = sumVariance;
		return variance;
	}

	private double calcAverage(
			List<LogWeighted<SampleWithValue<SampleType>>> samples) {
		double average = 0;
		LogSumExp.normalize(samples);
		for (LogWeighted<SampleWithValue<SampleType>> swv : samples) {
			average += swv.getExpLogWeight() * swv.getObject().getValue();
		}
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
				+ "beta:" + beta + "\n" + "ent:"
				+ (calcAverage(population) * beta + logZ) + "|"
				+ Math.log(1 - cutoffRate) + "|" + logZ + "\n" + sampler;
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
