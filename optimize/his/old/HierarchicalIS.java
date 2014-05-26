package optimize.his.old;


import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import optimize.function.CostFunction;
import optimize.util.SampleWithValue;
import optimize.util.WeightedSampleWithValue;

import statistics.sampler.Sampler;

public class HierarchicalIS<SampleType> {

	protected final int SAMPLE_NUM;

	protected final int LEVEL;

	protected double alpha;

	protected final CostFunction<SampleType> cf;

	protected final Sampler<SampleType>[] samplerSet;

	private final List<SampleWithValue<SampleType>>[] sampleWithValueSet;

	private final double[] averageEnergy;

	private final double[] inverseTemperature;

	// private static Random rand = new Random();

	public HierarchicalIS(int sample_num, int level, double alpha,
			CostFunction<SampleType> cf) {
		this.SAMPLE_NUM = sample_num;
		this.LEVEL = level;
		this.alpha = alpha;
		this.samplerSet = new Sampler[LEVEL];
		this.sampleWithValueSet = new List[LEVEL];
		this.averageEnergy = new double[LEVEL];
		this.inverseTemperature = new double[LEVEL];
		this.cf = cf;
	}

	protected void init() {
		// sampling
		for (int i = 0; i < LEVEL; i++) {
			sampleWithValueSet[i] = sampling(0);
		}
		// set average
		List<WeightedSampleWithValue<SampleType>> allSamples = generateMaginalizedSamples(0);
		averageEnergy[LEVEL - 1] = searchBestValue(allSamples);
		samplerSet[LEVEL - 1].fittingUpdate(allSamples);
		averageEnergy[0] = calcAverage(allSamples);
		initAverageEnergy();
		for (int i = 0; i < LEVEL; i++) {
			inverseTemperature[i] = 0;
		}
	}

	private void initAverageEnergy() {
		double sum = 0;
		double rate = 1;
		for (int i = 0; i < LEVEL; i++) {
			sum += rate;
			rate *= alpha / (1 - alpha);
		}
		rate = 1;
		for (int i = 1; i < LEVEL - 1; i++) {
			averageEnergy[i] = averageEnergy[i - 1]
					+ (averageEnergy[LEVEL - 1] - averageEnergy[0])
					* (rate / sum);
			rate *= alpha / (1 - alpha);
		}
	}

	private int pSampler = 0;

	public void optimize() {
		// select layer
		// int pSampler = rand.nextInt(LEVEL);
		pSampler = (pSampler + 1) % LEVEL;
		System.out.println(pSampler);
		// sampling
		sampleWithValueSet[pSampler] = sampling(pSampler);

		{// importance sampling
			// beta->average->fitting
			List<WeightedSampleWithValue<SampleType>> allSamples = generateMaginalizedSamples(0);
			if (pSampler == LEVEL - 1) {// preserve best
				averageEnergy[pSampler] = searchBestValue(allSamples);
				samplerSet[pSampler].fittingUpdate(allSamples);
			} else if (pSampler == 0) {// uniform
				averageEnergy[pSampler] = calcAverage(allSamples);
			} else {// normal
				// setting beta
				// if (pSampler == LEVEL - 2) {
				System.out.println("alpha;" + alpha);
				// alpha=(averageEnergy[LEVEL-2]+1-averageEnergy[LEVEL-4])/
				// (averageEnergy[LEVEL-2]-averageEnergy[LEVEL-4]);
				// System.out.println("->"+alpha);
				// }
				double targetEnergy = (1 - alpha) * averageEnergy[pSampler - 1]
						+ (alpha) * averageEnergy[pSampler + 1];
				if (pSampler == LEVEL - 2) {
					targetEnergy = averageEnergy[LEVEL - 1];
				}

				double beta = 0;
				allSamples = generateMaginalizedSamples(inverseTemperature[pSampler]);
				beta = inverseTemperature[pSampler]
						+ calcDeltaBeta(targetEnergy, allSamples);
				if (beta < 0)
					beta = 0;
				inverseTemperature[pSampler] = beta;

				// calc average
				allSamples = generateMaginalizedSamples(beta);
				averageEnergy[pSampler] = calcAverage(allSamples);
				System.out.println(pSampler + ":("
						+ (targetEnergy - averageEnergy[pSampler]) + ")"
						+ targetEnergy + "->" + averageEnergy[pSampler]);
				// generating approximation function
				allSamples = generateMaginalizedSamples(beta);
				samplerSet[pSampler].fittingUpdate(allSamples);
			}
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

	private double searchBestValue(
			List<WeightedSampleWithValue<SampleType>> allSamples) {
		double min = Double.MAX_VALUE;
		for (WeightedSampleWithValue<SampleType> wswv : allSamples) {
			double val = wswv.getSampeWithVale().getValue();
			if (val < min) {
				min = val;
			}
		}
		return min;
	}

	private double calcAverage(
			List<WeightedSampleWithValue<SampleType>> allSamples) {
		double sumWeight = 0;
		double sumAverage = 0;
		for (WeightedSampleWithValue<SampleType> wswv : allSamples) {
			sumWeight += wswv.getWeight();
			sumAverage += wswv.getWeight() * wswv.getSampeWithVale().getValue();
		}
		if (sumWeight < 1e-10) {
			System.out.println(sumWeight);
			System.exit(0);
		}
		return sumAverage / sumWeight;
	}

	private double calcVariance(
			List<WeightedSampleWithValue<SampleType>> allSamples, double average) {
		double sumWeight = 0;
		double sumVariance = 0;
		for (WeightedSampleWithValue<SampleType> wswv : allSamples) {
			sumWeight += wswv.getWeight();
			double dist = wswv.getSampeWithVale().getValue() - average;
			sumVariance += wswv.getWeight() * (dist * dist);
		}
		double variance = sumVariance / sumWeight;
		return variance;
	}

	private double calcDeltaBeta(double targetEnergy,
			List<WeightedSampleWithValue<SampleType>> allSamples) {
		double bar_f = calcAverage(allSamples);
		double variance=calcVariance(allSamples, bar_f);
		return -(targetEnergy - bar_f) / variance;
	}

	private List<WeightedSampleWithValue<SampleType>> generateMaginalizedSamples(
			double beta) {
		int sampleSize = LEVEL * SAMPLE_NUM;
		List<WeightedSampleWithValue<SampleType>> allSamples = new ArrayList<WeightedSampleWithValue<SampleType>>(
				sampleSize);
		double[] probabilityArray = new double[sampleSize * LEVEL];
		double minLogProbability = 0;
		int counter = 0;
		for (List<SampleWithValue<SampleType>> list : sampleWithValueSet) {
			for (SampleWithValue<SampleType> swv : list) {
				double max_in_allProb = Double.NEGATIVE_INFINITY;
				for (Sampler<SampleType> sampler : samplerSet) {
					probabilityArray[counter] = swv.getValue() * beta
							+ sampler.logNormalizedProbability(swv.getSample());
					if (max_in_allProb < probabilityArray[counter])
						max_in_allProb = probabilityArray[counter];
					counter++;
				}
				if (max_in_allProb < minLogProbability)
					minLogProbability = max_in_allProb;
			}
		}
		// minLogProbability=Math.rint(minLogProbability);
		System.out.println("minLogP:" + minLogProbability);
		counter = 0;
		for (List<SampleWithValue<SampleType>> list : sampleWithValueSet) {
			for (SampleWithValue<SampleType> swv : list) {
				double mProbability = 0;
				for (int i = 0; i < LEVEL; i++) {
					// System.out.print(probabilityArray[counter] + ",");
					mProbability += Math.exp(probabilityArray[counter]
							- (minLogProbability + 10));
					counter++;
				}
				// double w = Math
				// .exp(-(swv.getValue() - averageEnergy[LEVEL - 1])
				// * beta);
				double w = 1;
				w /= mProbability;
				if (w < 1e-5) {
					// System.out.println(w);
					// continue;
				}
				if (Double.isNaN(w)) {
					System.out.println("weight error");
					System.exit(0);
				}
				// System.out.println(w);
				allSamples.add(new WeightedSampleWithValue<SampleType>(w, swv));
			}
		}
		// System.out.println("w");
		// for (WeightedSampleWithValue<SampleType> wswv : allSamples) {
		// System.out.print(wswv.getWeight() + ",");
		// }
		// System.out.println();
		return allSamples;
	}

	// private double calcBeta(double targetEnergy,
	// List<WeightedSampleWithValue<SampleType>> wsamples) {
	// double f_h = 0;
	// double sum_h = 0;
	// double f_l = 0;
	// double sum_l = 0;
	//
	// for (WeightedSampleWithValue<SampleType> ws : wsamples) {
	// double value = ws.getSampeWithVale().getValue();
	// if (value > targetEnergy) {
	// f_h += ws.getWeight() * value;
	// sum_h += ws.getWeight();
	// } else {
	// f_l += ws.getWeight() * value;
	// sum_l += ws.getWeight();
	// }
	// }
	// f_h = f_h / sum_h;
	// f_l = f_l /= sum_l;
	//
	// double log_size_ratio = Math.log(sum_h) - Math.log(sum_l);
	// double log_diff = Math.log((f_h - targetEnergy) / (targetEnergy - f_l));
	// double tBeta = (log_size_ratio + log_diff) / (f_h - f_l);
	//
	// if (Double.isNaN(tBeta)) {
	// System.out.println("tBeta is " + tBeta);
	// return 0;
	// // System.exit(0);
	// }
	// if (tBeta < 0)
	// tBeta = 0;
	// return tBeta;
	// }

	public String toString() {
		StringBuilder sb = new StringBuilder();
		Formatter formatter = new Formatter(sb);
		sb.append("ene:");
		for (int i = 0; i < LEVEL; i++) {
			formatter.format("%6.3f ", averageEnergy[i]);
		}
		sb.append("\n");
		sb.append("bet:");
		for (int i = 0; i < LEVEL; i++) {
			formatter.format("%6.3f ", inverseTemperature[i]);
		}
		sb.append("\n");
		if (true) {
			for (int i = LEVEL - 3; i < LEVEL - 1; i++) {
				sb.append(i + ":" + samplerSet[i].toString() + "\n");
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
