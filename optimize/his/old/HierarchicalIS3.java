package optimize.his.old;


import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import optimize.function.CostFunction;
import optimize.util.SampleWithValue;
import optimize.util.WeightedSampleWithValue;

import statistics.sampler.Sampler;

public class HierarchicalIS3<SampleType> {

	protected final int SAMPLE_NUM;

	protected final int LEVEL;

	protected final CostFunction<SampleType> cf;

	protected final Sampler<SampleType>[] samplerSet;

	private final List<SampleWithValue<SampleType>>[] sampleWithValueSet;

	private final double[] averageEnergy;

	private final double[] varianceEnergy;

	private final double[] inverseTemperature;

	private double gamma = 1;

	// private static Random rand = new Random();

	public HierarchicalIS3(int sample_num, int level,
			CostFunction<SampleType> cf) {
		this.SAMPLE_NUM = sample_num;
		this.LEVEL = level;
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
			sampleWithValueSet[i] = sampling(i);
		}
		// set average
		List<WeightedSampleWithValue<SampleType>> allSamples = generateMarginalizedSamples(0);
		for (int i = 0; i < LEVEL; i++) {
			averageEnergy[i] = calcAverageMarginal(allSamples);
		}
		for (int i = 0; i < LEVEL; i++) {
			varianceEnergy[i] = calcVarianceMarginal(allSamples,
					averageEnergy[i]);
		}
		for (int i = 0; i < LEVEL; i++) {
			inverseTemperature[i] = 0;
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

		double targetEnergy;
		boolean MODE_SDS = false;
		if (true) {
			if (pSampler == LEVEL - 1) {
				// minimization problem is supposed: we should go to negative
				// direction of f
				if (MODE_SDS) {
					targetEnergy = averageEnergy[pSampler] - gamma
							* Math.sqrt(varianceEnergy[pSampler]);
				} else if (true) {
					targetEnergy = searchBestValue(sampleWithValueSet[pSampler])
							+ gamma * Math.sqrt(varianceEnergy[pSampler]);
				} else {
					targetEnergy = searchBestValue(sampleWithValueSet[pSampler]);
				}
			} else if (pSampler == 0) {
				targetEnergy = averageEnergy[pSampler + 1]
						+ gamma
						* (Math.sqrt(varianceEnergy[pSampler]) + Math
								.sqrt(varianceEnergy[pSampler + 1]));
			} else {
				// random
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
					+ calcDeltaBeta(targetEnergy, averageEnergy[pSampler],
							varianceEnergy[pSampler]);
		} else {
			System.out.println("beta1");
			List<WeightedSampleWithValue<SampleType>> allSamples = generateMarginalizedSamples(inverseTemperature[pSampler]);
			System.out.println("beta2");
			double average = calcAverageMarginal(allSamples);
			double variance = calcVarianceMarginal(allSamples, average);
			System.out.println("beta3");
			beta = inverseTemperature[pSampler]
					+ calcDeltaBeta(targetEnergy, average, variance);
			System.out.println("beta4");
		}
		if (beta < 0) {
			beta = inverseTemperature[pSampler];
		} else {
			inverseTemperature[pSampler] = beta;
		}

		System.out.println(pSampler + ":("
				+ (targetEnergy - averageEnergy[pSampler]) + ")" + targetEnergy
				+ "->" + averageEnergy[pSampler]);

		// generating approximation function
		List<WeightedSampleWithValue<SampleType>> allSamples = generateMarginalizedSamples(beta);
		samplerSet[pSampler].fittingUpdate(allSamples);

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

	public SampleWithValue searchBest() {
		double min = Double.MAX_VALUE;
		SampleWithValue best = null;
		for (int i = 0; i < LEVEL; i++) {
			for (SampleWithValue<SampleType> swv : sampleWithValueSet[i]) {
				double val = swv.getValue();
				if (val < min) {
					min = val;
					best = swv;
				}
			}
		}
		return best;
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
							- (minLogProbability + 0));
					counter++;
				}
				// double w = Math
				// .exp(-(swv.getValue() - averageEnergy[LEVEL - 1])
				// * beta);
				double w = 1;
				w /= mProbability;
				// if (w < 1e-5) {
				// System.out.println(w);
				// continue;
				// }
				if (Double.isNaN(w)) {
					System.out.println("weight error");
					System.exit(0);
				}
				allSamples.add(new WeightedSampleWithValue<SampleType>(w, swv));
			}
		}
		return allSamples;
	}

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
			for (int i = LEVEL - 2; 0 < i && i < LEVEL; i++) {
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
