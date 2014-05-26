package optimize.population;

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
 * a version where Z_{t+1} / Z_t is removed
 */
public abstract class TruncationRPM<SampleType> implements
		Annealing<SampleType> {

	private static final double Epsilon = 1e-10;

	protected int num_samples;

	protected int num_population;

	protected double cutoffRate;

	protected CostFunction<SampleType> cf;

	private Sampler<SampleType> sampler;

	private List<LogWeighted<SampleWithValue<SampleType>>> samples =
			null;

	private List<LogWeighted<SampleWithValue<SampleType>>> population =
			null;

	private Random rand = new Random();

	private double cutoffValue = Double.MAX_VALUE;

	private boolean isInitilized = false;

	private double avg = 0;

	private double var = 0;

	private double logZ = 0;

	private int init_num_population = 0;

	public TruncationRPM(CostFunction<SampleType> cf,
			int num_population, int num_sample, double cutoff) {
		this.cf = cf;
		this.num_population = num_population;
		this.num_samples = num_sample;
		this.cutoffRate = cutoff;
		population =
				new ArrayList<LogWeighted<SampleWithValue<SampleType>>>();
	}

	public void optimize() {
		if (!isInitilized) {
			initilize();
		}

		this.samples = sampling(sampler);

		ArrayList<LogWeighted<SampleWithValue<SampleType>>> allSamples =
				mergeSamples();
		allSamples = selection(allSamples);
		this.sampler = getMLSampler(allSamples);
		
		System.out.println(calcNumberOfEffectiveSamples(allSamples)+" "+sampler.entropy()+" "+getLogZ());
		
		this.population = resamplingStep(allSamples);

		{
			this.avg = calcAverage(allSamples);
			this.var = calcVariance(allSamples, this.avg);
		}

	}

	private void initilize() {
		isInitilized = true;
		this.sampler = getInitialSampler();
		population =
				new ArrayList<LogWeighted<SampleWithValue<SampleType>>>(
						0);
		population = sampling(this.sampler, init_num_population);
		// normalizePopulation(population);
		// samples = sampling(sampler);
		// {
		// this.avg = calcAverage(samples);
		// this.var = calcVariance(samples, this.avg);
		// }
		// Collections
		// .sort(population, new LogWeightedSWVComparator<SampleType>());
	}

	protected abstract Sampler<SampleType> getInitialSampler();

	private List<LogWeighted<SampleWithValue<SampleType>>> sampling(
			Sampler<SampleType> sampler, int num) {
		List<SampleType> samples = sampler.sampling(num);
		List<LogWeighted<SampleWithValue<SampleType>>> wsamples =
				new ArrayList<LogWeighted<SampleWithValue<SampleType>>>(
						num_samples);
		for (SampleType st : samples) {
			// eval samples
			wsamples.add(new LogWeighted<SampleWithValue<SampleType>>(
					0, new SampleWithValue<SampleType>(st, cf
							.eval(st))));
		}
		return wsamples;
	}

	private List<LogWeighted<SampleWithValue<SampleType>>> sampling(
			Sampler<SampleType> sampler) {
		return sampling(sampler, num_samples);
	}

	private ArrayList<LogWeighted<SampleWithValue<SampleType>>> mergeSamples() {
		// init pop weight. assuming there is no sample with smaller weight than
		// threshold
		LogSumExp.normalize(population);
		double num_effectivePop =
				calcNumberOfEffectiveSamples(population);
		for (LogWeighted<SampleWithValue<SampleType>> individual : population) {
			individual.setLogWeight(individual.getLogWeight()
					+ Math.log(num_effectivePop));
		}

		// cut a part of the generated samples
		ArrayList<LogWeighted<SampleWithValue<SampleType>>> selectedSamples =
				new ArrayList<LogWeighted<SampleWithValue<SampleType>>>(
						samples.size());
		{
			Collections.sort(samples,
					new LogWeightedSWVComparator<SampleType>());
			for (LogWeighted<SampleWithValue<SampleType>> lwswv : samples) {
				// to select effective samples from generated samples
				if (cutoffValue <= lwswv.getObject().getValue()) {
					break;
				} else {
					selectedSamples.add(lwswv);
				}
			}
		}

		// calc the Z of current q(x)
		logZ = Double.NEGATIVE_INFINITY;
		if (0 < selectedSamples.size()) {
			double[] partLogZ = new double[selectedSamples.size()];
			for (int i = 0; i < selectedSamples.size(); i++) {
				partLogZ[i] =
						-sampler.logNormalizedProbability(selectedSamples
								.get(i).getObject().getSample());
			}
			logZ = LogSumExp.sum(partLogZ) - Math.log(samples.size());
			if (Double.isInfinite(logZ)) {
				System.out.println("error logZ");
				System.out
						.println("the number of selected samples is "
								+ samples.size());
			}
		}

		ArrayList<LogWeighted<SampleWithValue<SampleType>>> allSamples =
				new ArrayList<LogWeighted<SampleWithValue<SampleType>>>(
						population.size() + selectedSamples.size());
		allSamples.addAll(population);
		allSamples.addAll(selectedSamples);

		double[] mrate = new double[2];
		mrate[0] =
				num_effectivePop
						/ (num_effectivePop + samples.size());
		mrate[1] =
				((double) samples.size())
						/ (num_effectivePop + samples.size());

		for (LogWeighted<SampleWithValue<SampleType>> wswv : allSamples) {
			double denom[] = new double[2];
			denom[0] = Math.log(mrate[0]);
			if (selectedSamples.size() == 0) {
				denom[1] = Double.NEGATIVE_INFINITY;
			} else {
				denom[1] =
						Math.log(mrate[1])
								+ sampler
										.logNormalizedProbability(wswv
												.getObject()
												.getSample()) + logZ;
			}
			double gLogProb = LogSumExp.sum(denom);
			wswv.setLogWeight(wswv.getLogWeight() - gLogProb);
			if (Double.isNaN(wswv.getLogWeight())) {
				System.out.println("weight cannot be defined");
			}
		}
		return allSamples;
	}

	private ArrayList<LogWeighted<SampleWithValue<SampleType>>> selection(
			ArrayList<LogWeighted<SampleWithValue<SampleType>>> allSamples) {

		ArrayList<LogWeighted<SampleWithValue<SampleType>>> allSelectedSamples =
				new ArrayList<LogWeighted<SampleWithValue<SampleType>>>(
						allSamples.size());
		LogSumExp.normalize(allSamples);
		Collections.sort(allSamples,
				new LogWeightedSWVComparator<SampleType>());

		double logSumWeight = Math.log(0);
		// at least one sample should be removed.
		double logWeightThrethold = Math.log(1 - cutoffRate);
		for (int i = 0;; i++) {
			LogWeighted<SampleWithValue<SampleType>> samp =
					allSamples.get(i);

			if (logWeightThrethold < logSumWeight
					|| i == allSamples.size() - 1) {
				cutoffValue = samp.getObject().getValue();
				break;
			}

			// 0 weight samples are removed
			if (Epsilon < samp.getExpLogWeight()) allSelectedSamples
					.add(samp);
			else continue;

			logSumWeight =
					LogSumExp.sum(logSumWeight, samp.getLogWeight());

		}
		return allSelectedSamples;
	}

	protected Sampler<SampleType> getMLSampler(
			List<LogWeighted<SampleWithValue<SampleType>>> allSamples) {
		LogSumExp.normalize(allSamples);

		ArrayList<WeightedSampleWithValue<SampleType>> ldata =
				new ArrayList<WeightedSampleWithValue<SampleType>>(
						allSamples.size());

		for (LogWeighted<SampleWithValue<SampleType>> wswv : allSamples) {
			double weight = wswv.getExpLogWeight();
			if (weight == 0) continue;
			ldata.add(new WeightedSampleWithValue<SampleType>(weight,
					wswv.getObject()));
		}

		sampler.fittingUpdate(ldata);
		return sampler;
	}

	private List<LogWeighted<SampleWithValue<SampleType>>> resamplingStep(
			List<LogWeighted<SampleWithValue<SampleType>>> allSamples) {
		switch (3) {
		case 0:
			return resamplingWOR_fast(allSamples);
		case 1:
			return resamplingWOR(allSamples);
		case 2:
			return simplyRandomizedResampling(allSamples);
		case 3:
			return simpleResampling(allSamples);
		}
		return null;
	}

	private List<LogWeighted<SampleWithValue<SampleType>>> resamplingWOR_fast(
			List<LogWeighted<SampleWithValue<SampleType>>> allSamples) {

		if (allSamples.size() < num_population) return allSamples;

		LogSumExp.normalize(allSamples);

		List<Weighted<LogWeighted<SampleWithValue<SampleType>>>> list =
				new ArrayList<Weighted<LogWeighted<SampleWithValue<SampleType>>>>();
		for (LogWeighted<SampleWithValue<SampleType>> wswv : allSamples) {
			double w = wswv.getExpLogWeight();
			if (w == 0) continue;
			double key = Math.pow(rand.nextDouble(), 1d / w);
			Weighted<LogWeighted<SampleWithValue<SampleType>>> wsamp =
					new Weighted<LogWeighted<SampleWithValue<SampleType>>>(
							key, wswv);
			list.add(wsamp);
		}

		allSamples.clear();

		Collections.sort(list, new Comparator<Weighted<?>>() {
			public int compare(Weighted<?> o1, Weighted<?> o2) {
				if (o1.getWeight() < o2.getWeight()) return 1;
				if (o1.getWeight() > o2.getWeight()) return -1;
				return 0;
			}
		});

		for (int i = 0; i < this.num_population && i < list.size(); i++) {
			allSamples.add(list.get(i).getObject());
			// System.out.println(list.get(i).getWeight());
		}

		return allSamples;
	}

	private List<LogWeighted<SampleWithValue<SampleType>>> resamplingWOR(
			List<LogWeighted<SampleWithValue<SampleType>>> allSamples) {

		LogSumExp.normalize(allSamples);

		Collections.sort(allSamples,
				new Comparator<LogWeighted<?>>() {
					public int compare(LogWeighted<?> o1,
							LogWeighted<?> o2) {
						if (o1.getLogWeight() < o2.getLogWeight()) return 1;
						if (o1.getLogWeight() > o2.getLogWeight()) return -1;
						return 0;
					}
				});

		List<LogWeighted<SampleWithValue<SampleType>>> list =
				new LinkedList<LogWeighted<SampleWithValue<SampleType>>>();
		for (LogWeighted<SampleWithValue<SampleType>> wswv : allSamples) {
			double weight = wswv.getExpLogWeight();
			// 0 cutting
			if (weight == 0) continue;
			list.add(wswv);
		}

		allSamples.clear();

		double sum_weight = 1;
		for (int i = 0; i < list.size(); i++) {
			if (allSamples.size() >= num_population) break;
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

		List<Weighted<LogWeighted<SampleWithValue<SampleType>>>> list =
				new ArrayList<Weighted<LogWeighted<SampleWithValue<SampleType>>>>(
						allSamples.size());
		for (LogWeighted<SampleWithValue<SampleType>> wswv : allSamples) {
			double randWeight =
					wswv.getExpLogWeight() * rand.nextDouble();
			Weighted<LogWeighted<SampleWithValue<SampleType>>> wo =
					new Weighted<LogWeighted<SampleWithValue<SampleType>>>(
							randWeight, wswv);
			list.add(wo);
		}

		Collections.sort(list, new Comparator<Weighted<?>>() {
			public int compare(Weighted<?> o1, Weighted<?> o2) {
				if (o1.getWeight() > o2.getWeight()) return -1;
				if (o1.getWeight() < o2.getWeight()) return 1;
				return 0;
			}
		});

		if (!(list.size() < num_population)) list =
				list.subList(0, num_population);

		allSamples.clear();
		for (Weighted<LogWeighted<SampleWithValue<SampleType>>> wlswv : list) {
			if (wlswv.weight == 0) continue;
			allSamples.add(wlswv.getObject());
		}
		return allSamples;
	}

	private List<LogWeighted<SampleWithValue<SampleType>>> simpleResampling(
			List<LogWeighted<SampleWithValue<SampleType>>> allSamples) {
		// Additionally the samples with 0 weight are removed
		// LogSumExp.normalize(allSamples);
		Collections.sort(allSamples,
				new Comparator<LogWeighted<?>>() {
					public int compare(LogWeighted<?> o1,
							LogWeighted<?> o2) {
						if (o1.getLogWeight() > o2.getLogWeight()) return -1;
						if (o1.getLogWeight() < o2.getLogWeight()) return 1;
						return 0;
					}
				});
		if (allSamples.size() <= this.num_population) {
			return allSamples;
		} else {

		}
		List<LogWeighted<SampleWithValue<SampleType>>> selectedSamples =
				new ArrayList<LogWeighted<SampleWithValue<SampleType>>>(
						this.num_population);
		for (int i = 0; i < this.num_population; i++) {
			selectedSamples.add(allSamples.get(i));
		}
		return selectedSamples;

		// List<Weighted<LogWeighted<SampleWithValue<SampleType>>>> list = new
		// ArrayList<Weighted<LogWeighted<SampleWithValue<SampleType>>>>(
		// allSamples.size());
		// for (LogWeighted<SampleWithValue<SampleType>> wswv : allSamples) {
		// double randWeight = wswv.getExpLogWeight();
		// Weighted<LogWeighted<SampleWithValue<SampleType>>> wo = new
		// Weighted<LogWeighted<SampleWithValue<SampleType>>>(
		// randWeight, wswv);
		// list.add(wo);
		// }

		// Collections.sort(list, new Comparator<Weighted<?>>() {
		// public int compare(Weighted<?> o1, Weighted<?> o2) {
		// if (o1.getWeight() > o2.getWeight())
		// return -1;
		// if (o1.getWeight() < o2.getWeight())
		// return 1;
		// return 0;
		// }
		// });

		// if (!(list.size() < num_population))
		// list = list.subList(0, num_population);

		// allSamples.clear();
		// {
		// for (Weighted<LogWeighted<SampleWithValue<SampleType>>> wlswv : list)
		// if (wlswv.weight == 0)
		// continue;
		// allSamples.add(wlswv.getObject());
		// }
		// return allSamples;
	}

	public int getPopulationSize() {
		return (int) calcNumberOfEffectiveSamples(population);
	}

	private double calcNumberOfEffectiveSamples(
			List<LogWeighted<SampleWithValue<SampleType>>> population) {
		if (false) return population.size();
		if (population.size() == 0) return 0;
		LogSumExp.normalize(population);
		double entropy = 0;
		for (LogWeighted<SampleWithValue<SampleType>> sample : population) {
			double logProb = sample.getLogWeight();
			if (Double.isInfinite(logProb)) continue;
			entropy += -Math.exp(logProb) * logProb;
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
			sumAverage +=
					swv.getExpLogWeight()
							* swv.getObject().getValue();
			sumWeight += swv.getExpLogWeight();
		}
		double average = sumAverage / sumWeight;
		return average;
	}

	public double getVariance() {
		return this.var;
	}

	public double getLogZ() {
		return this.logZ;
	}

	public String toString() {
		Collections.sort(population,
				new LogWeightedObjectComparator());
		// for (LogWeighted<SampleWithValue<SampleType>> lwswv : population) {
		// System.out.println(lwswv);
		// }
		return calcNumberOfEffectiveSamples(population) + "\n"
				+ "avg:" + calcAverage(population) + "\n" + "var:"
				+ calcVariance(population, calcAverage(population))
				+ "\n" + sampler;
	}

}
