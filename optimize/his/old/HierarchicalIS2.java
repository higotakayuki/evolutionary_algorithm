package optimize.his.old;


import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import optimize.function.CostFunction;
import optimize.util.SampleWithValue;
import optimize.util.WeightedSampleWithValue;

import statistics.sampler.Sampler;

public class HierarchicalIS2<SampleType> {

	protected final int SAMPLE_NUM;

	protected final int LEVEL;

	protected double alpha;

	protected final CostFunction<SampleType> cf;

	protected final Sampler<SampleType>[] samplerSet;

	private final List<SampleWithValue<SampleType>>[] sampleWithValueSet;

	private final double[] averageEnergy;

	private final double[] varianceEnergy;

	private final double[] inverseTemperature;

	// private static Random rand = new Random();

	public HierarchicalIS2(int sample_num, int level, double alpha,
			CostFunction<SampleType> cf) {
		this.SAMPLE_NUM = sample_num;
		this.LEVEL = level;
		this.alpha = alpha;
		this.samplerSet = new Sampler[LEVEL];
		this.sampleWithValueSet = new List[LEVEL];
		this.averageEnergy = new double[LEVEL];
		this.varianceEnergy = new double[LEVEL];
		this.inverseTemperature = new double[LEVEL];
		this.cf = cf;
	}

	protected void init() {
		// sampling
		for (int i = 0; i < LEVEL; i++) {
			sampleWithValueSet[i] = sampling(0);
		}
		// set average
		List<WeightedSampleWithValue<SampleType>> allSamples = generateMarginalizedSamples(0);
		averageEnergy[LEVEL - 1] = searchBestValueMarginal(allSamples);
		samplerSet[LEVEL - 1].fittingUpdate(allSamples);
		averageEnergy[0] = calcAverageMarginal(allSamples);
		initAverageEnergyVariance();
		for (int i = 0; i < LEVEL; i++) {
			inverseTemperature[i] = 0;
		}
	}

	private void initAverageEnergyVariance() {
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
		List<WeightedSampleWithValue<SampleType>> allSamples = generateMarginalizedSamples(0);
		for (int i = 0; i < LEVEL; i++) {
			varianceEnergy[i] = calcVarianceMarginal(allSamples,
					averageEnergy[i]);
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
		averageEnergy[pSampler] = calcAverage(sampleWithValueSet[pSampler]);
		varianceEnergy[pSampler] = calcVariance(sampleWithValueSet[pSampler],
				averageEnergy[pSampler]);

		{// importance sampling
			// beta->average->fitting
			if (pSampler == 0) {// uniform
				// do nothing
			} else {// normal
				// calc target energy
				double targetEnergy;
				if (pSampler == LEVEL - 1) {
					targetEnergy = searchBestValue(sampleWithValueSet[pSampler])
							+ Math.sqrt(varianceEnergy[pSampler]);
				} else {
					if (false) {
						targetEnergy = varianceEnergy[pSampler + 1]
								* averageEnergy[pSampler - 1]
								+ varianceEnergy[pSampler - 1]
								* averageEnergy[pSampler + 1];
						targetEnergy /= varianceEnergy[pSampler - 1]
								+ varianceEnergy[pSampler + 1];
					} else if (false) {
						targetEnergy = averageEnergy[pSampler - 1]
								+ averageEnergy[pSampler + 1];
						targetEnergy /= 2;
					} else if (false) {
						targetEnergy = Math.sqrt(varianceEnergy[pSampler - 1])
								* averageEnergy[pSampler - 1]
								+ Math.sqrt(varianceEnergy[pSampler + 1])
								* averageEnergy[pSampler + 1];
						targetEnergy /= Math.sqrt(varianceEnergy[pSampler - 1])
								+ Math.sqrt(varianceEnergy[pSampler + 1]);
					} else {
						targetEnergy = Math.sqrt(varianceEnergy[pSampler + 1])
								* averageEnergy[pSampler - 1]
								+ Math.sqrt(varianceEnergy[pSampler - 1])
								* averageEnergy[pSampler + 1];
						targetEnergy /= Math.sqrt(varianceEnergy[pSampler - 1])
								+ Math.sqrt(varianceEnergy[pSampler + 1]);
					}
				}

				// calc beta
				double beta = 0;
				if (false) {
					beta = inverseTemperature[pSampler]
							+ calcDeltaBeta(targetEnergy,
									averageEnergy[pSampler],
									varianceEnergy[pSampler]);
				} else {
					List<WeightedSampleWithValue<SampleType>> allSamples = generateMarginalizedSamples(inverseTemperature[pSampler]);
					double average = calcAverageMarginal(allSamples);
					double variance = calcVarianceMarginal(allSamples, average);
					beta = inverseTemperature[pSampler]
							+ calcDeltaBeta(targetEnergy, average, variance);
				}
				if (beta < 0) {
					beta = inverseTemperature[pSampler];
				} else {
					inverseTemperature[pSampler] = beta;
				}

				System.out.println(pSampler + ":("
						+ (targetEnergy - averageEnergy[pSampler]) + ")"
						+ targetEnergy + "->" + averageEnergy[pSampler]);

				// generating approximation function
				List<WeightedSampleWithValue<SampleType>> allSamples = generateMarginalizedSamples(beta);
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

	private double searchBestValue(List<SampleWithValue<SampleType>> samples) {
		double min = Double.MAX_VALUE;
		for (SampleWithValue<SampleType> swv : samples) {
			double val = swv.getValue();
			if (val < min) {
				min = val;
			}
		}
		return min;
	}

	private double searchBestValueMarginal(
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

	private double calcAverage(List<SampleWithValue<SampleType>> samples) {
		double sumAverage = 0;
		for (SampleWithValue<SampleType> swv : samples) {
			sumAverage += swv.getValue();
		}
		double average = sumAverage / samples.size();
		return average;
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

	private double calcAverageMarginal(
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

	private double calcVarianceMarginal(
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

	private double calcDeltaBeta(double targetEnergy, double average,
			double variance) {
		double delta = -(targetEnergy - average) / variance;
		if (Math.abs(delta) > 1 / Math.sqrt(variance)) {
			delta = delta / Math.abs(delta) / Math.sqrt(variance);
		}
		return delta;
	}

	private List<WeightedSampleWithValue<SampleType>> generateMarginalizedSamples(
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
			formatter.format("%6.3f (%6.3f) ", averageEnergy[i],
					varianceEnergy[i]);
		}
		sb.append("\n");
		sb.append("bet:");
		for (int i = 0; i < LEVEL; i++) {
			formatter.format("%6.3f ", inverseTemperature[i]);
		}
		sb.append("\n");
		if (true) {
			for (int i = LEVEL - 2; i < LEVEL; i++) {
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
