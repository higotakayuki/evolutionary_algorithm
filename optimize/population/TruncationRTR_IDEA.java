package optimize.population;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import optimize.Annealing;
import optimize.function.CostFunction;
import optimize.util.LogWeighted;
import optimize.util.LogWeightedObjectComparator;
import optimize.util.SampleWithValue;
import optimize.util.Weighted;
import optimize.util.WeightedSampleWithValue;
import statistics.sampler.Sampler;

public abstract class TruncationRTR_IDEA<SampleType> implements
		Annealing<SampleType> {

	public static boolean IDEA = true;

	protected int WINDOW_SIZE;

	protected int num_samples;

	protected int num_population;

	protected double cutoffRate;

	private double threashold = Double.MAX_VALUE;

	protected CostFunction<SampleType> cf;

	private Sampler<SampleType> sampler;

	private boolean changeFlag = true;

	// private List<LogWeighted<SampleWithValue<SampleType>>> samples = null;

	private List<LogWeighted<SampleWithValue<SampleType>>> population = null;

	private List<LogWeighted<SampleWithValue<SampleType>>> samples = null;

	private Random rand = new Random();

	public TruncationRTR_IDEA(CostFunction<SampleType> cf, int num_population,
			int num_sample, double cutoff) {
		this.cf = cf;
		this.num_population = num_population;

		this.cutoffRate = cutoff;
		this.num_samples = num_sample;
		this.WINDOW_SIZE = num_population / 20;
		if (WINDOW_SIZE == 0) WINDOW_SIZE = 1;
		this.WINDOW_SIZE = Math.min(this.WINDOW_SIZE, cf.dim());
		population = new ArrayList<LogWeighted<SampleWithValue<SampleType>>>();
	}

	private SampleWithValue<SampleType> best = null;

	private void initilize() {
		this.sampler = getInitialSampler();
		population = sampling(this.sampler, num_population);
		Collections.sort(population, new LogWeightedSWVComparator());
		best = population.get(0).getObject();
	}

	public SampleWithValue<SampleType> getBest() {
		Collections.sort(population, new LogWeightedSWVComparator());
		if (best == null
				|| population.get(0).getObject().getValue() < best.getValue()) best = population
				.get(0).getObject();
		return best;
	}

	public void optimize() {
		if (population.size() == 0) {
			initilize();
		}

		List<LogWeighted<SampleWithValue<SampleType>>> parents = truncationSelection();
		sampler = getMLSampler(parents);

		if (!IDEA) {
			// hBOA
			samples = sampling(sampler, num_samples);
			RTR(samples);
		} else {
			// IDEA
			// System.out.println("truncated:"+allSamples.size());
			// System.out.println(num_population - allSamples.size());

			// samples = sampling(sampler, num_samples);
			samples = sampling(sampler, num_population - population.size());

			parents.addAll(samples);
			population = parents;
		}
	}

	private List<LogWeighted<SampleWithValue<SampleType>>> truncationSelection() {
		Collections.sort(population, new LogWeightedSWVComparator());
		List<LogWeighted<SampleWithValue<SampleType>>> selectedPopulation = new ArrayList<LogWeighted<SampleWithValue<SampleType>>>(
				num_population);

		int t = (int) Math.ceil(population.size() * (1 - cutoffRate));
		double cand = population.get(t - 1).getObject().getValue();
		// if (cand < this.threashold) {
		this.threashold = cand;
		// }

		for (LogWeighted<SampleWithValue<SampleType>> individual : population) {
			if (this.threashold <= individual.getObject().getValue()) break;
			else selectedPopulation.add(individual);
		}
		if (selectedPopulation.size() == 0) {
			selectedPopulation.add(population.get(0));
		}

		return selectedPopulation;
	}

	private void RTR(List<LogWeighted<SampleWithValue<SampleType>>> samples) {
		boolean tChangeFlag = false;
		for (LogWeighted<SampleWithValue<SampleType>> newSample : samples) {
			List<LogWeighted<SampleWithValue<SampleType>>> subset = creatSubset(
					population, WINDOW_SIZE);
			LogWeighted<SampleWithValue<SampleType>> competitor = findClosestSample(
					newSample, subset);
			if (competitor.getObject().getValue() > newSample.getObject()
					.getValue()) {
				// replacement
				population.remove(competitor);
				population.add(newSample);
				tChangeFlag = true;
			}
		}
		changeFlag = tChangeFlag;
	}

	protected abstract LogWeighted<SampleWithValue<SampleType>> findClosestSample(
			LogWeighted<SampleWithValue<SampleType>> newSample,
			List<LogWeighted<SampleWithValue<SampleType>>> subset);

	private List<LogWeighted<SampleWithValue<SampleType>>> creatSubset(
			List<LogWeighted<SampleWithValue<SampleType>>> population,
			int windowsize) {
		List<Weighted<LogWeighted<SampleWithValue<SampleType>>>> list = new ArrayList<Weighted<LogWeighted<SampleWithValue<SampleType>>>>(
				population.size());
		for (LogWeighted<SampleWithValue<SampleType>> wswv : population) {
			double randWeight = rand.nextDouble();
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

		List<LogWeighted<SampleWithValue<SampleType>>> ans = new ArrayList<LogWeighted<SampleWithValue<SampleType>>>(
				windowsize);
		for (int i = 0; i < windowsize; i++) {
			ans.add(list.get(i).getObject());
		}
		return ans;
	}

	protected abstract Sampler<SampleType> getInitialSampler();

	protected Sampler<SampleType> getMLSampler(
			List<LogWeighted<SampleWithValue<SampleType>>> allSamples) {
		ArrayList<WeightedSampleWithValue<SampleType>> ldata = new ArrayList<WeightedSampleWithValue<SampleType>>(
				allSamples.size());

		for (LogWeighted<SampleWithValue<SampleType>> wswv : allSamples) {
			ldata.add(new WeightedSampleWithValue<SampleType>(1, wswv
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

	private double calcVariance(
			List<LogWeighted<SampleWithValue<SampleType>>> samples,
			double average) {
		double sumWeight = 0;
		double sumVariance = 0;
		for (LogWeighted<SampleWithValue<SampleType>> swv : samples) {
			double dist = swv.getObject().getValue() - average;
			sumVariance += dist * dist;
			sumWeight += 1;
		}
		double variance = sumVariance / sumWeight;
		return variance;
	}

	private double calcAverage(
			List<LogWeighted<SampleWithValue<SampleType>>> samples) {
		double sumWeight = 0;
		double sumAverage = 0;
		for (LogWeighted<SampleWithValue<SampleType>> swv : samples) {
			sumAverage += swv.getObject().getValue();
			sumWeight += 1;
		}
		double average = sumAverage / sumWeight;
		return average;
	}

	public double getVariance() {
		if (changeFlag == false) return -1;
		return calcVariance(samples, calcAverage(samples));
	}

	public String toString() {
		Collections.sort(population, new LogWeightedObjectComparator());
		for (LogWeighted<SampleWithValue<SampleType>> lwswv : population) {
			// System.out.println(lwswv);
		}
		return "avg:" + calcAverage(population) + "\n" + "var:"
				+ calcVariance(population, calcAverage(population)) + "\n"
				+ "state:" + this.changeFlag + "\n" + sampler;
	}

	class LogWeightedSWVComparator implements
			Comparator<LogWeighted<SampleWithValue<SampleType>>> {

		public int compare(LogWeighted<SampleWithValue<SampleType>> o1,
				LogWeighted<SampleWithValue<SampleType>> o2) {
			if (o1.getObject().getValue() > o2.getObject().getValue()) return 1;
			if (o1.getObject().getValue() < o2.getObject().getValue()) return -1;
			return 0;
		}

	}

}
