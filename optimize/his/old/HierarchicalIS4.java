package optimize.his.old;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;

import optimize.function.CostFunction;
import optimize.util.SampleWithValue;
import optimize.util.SampleWithValueComparator;
import optimize.util.WeightedSampleWithValue;

import statistics.sampler.Sampler;

public class HierarchicalIS4<SampleType> {

	protected final int SAMPLE_NUM;

	protected final int LEVEL;

	protected final CostFunction<SampleType> cf;

	protected final Sampler<SampleType>[] samplerSet;

	private final List<SampleWithValue<SampleType>>[] sampleWithValueSet;

	private final double[] averageEnergy;

	private final double[] varianceEnergy;

	private final double[] inverseTemperature;

	public SampleWithValue<SampleType> getBest() {
		List<SampleWithValue<SampleType>> list = new ArrayList<SampleWithValue<SampleType>>(
				this.SAMPLE_NUM * 3);
		for (int i = 0; i < LEVEL; i++) {
			list.addAll(this.sampleWithValueSet[i]);
		}
		return searchBestSamples(list, 0, 1).get(0);
	}

	public HierarchicalIS4(int sample_num, int level,
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
		for (int i = 0; i < LEVEL; i++) {
			samplingAndAveraging(i);
			inverseTemperature[i] = 0;
		}
	}

	private int pSampler = 0;

	private List<SampleWithValue<SampleType>> getPartOfSamples(int pSampler) {
		List<SampleWithValue<SampleType>> list = new ArrayList<SampleWithValue<SampleType>>(
				this.SAMPLE_NUM * 3);
		for (int i = pSampler - 1; i <= pSampler + 1; i++) {
			if (i < 0)
				continue;
			if (LEVEL <= i)
				continue;
			list.addAll(this.sampleWithValueSet[i]);
		}
		return list;
	}

	private List<Sampler<SampleType>> getPartOfSampler(int pSampler) {
		List<Sampler<SampleType>> list = new ArrayList<Sampler<SampleType>>(3);
		for (int i = pSampler - 1; i <= pSampler + 1; i++) {
			if (i < 0)
				continue;
			if (LEVEL <= i)
				continue;
			list.add(samplerSet[i]);
		}
		return list;
	}

	private double probGauss(double x, double u, double var) {
		return Math.exp(-(x - u) * (x - u) / (2 * var))
				/ (Math.sqrt(var * 2 * Math.PI));
	}

	private void samplingAndAveraging(int pSampler) {
		sampleWithValueSet[pSampler] = sampling(pSampler);
		averageEnergy[pSampler] = calcAverage(sampleWithValueSet[pSampler]);
		varianceEnergy[pSampler] = calcVariance(sampleWithValueSet[pSampler],
				averageEnergy[pSampler]);
	}
	
	private double getTargetEnergy(int pSampler,List<SampleWithValue<SampleType>> partSamples){
		double targetEnergy=0;
		boolean MODE_SDS = true;
		if (true) {
			if (pSampler == LEVEL - 1) {
				// minimization problem is supposed: we should go to negative
				// direction of f
				if (MODE_SDS) {
					targetEnergy = averageEnergy[pSampler] - 0.3
							* Math.sqrt(varianceEnergy[pSampler]);
				} else {
					targetEnergy = calcAverage(searchBestSamples(partSamples,
							0, (int) (SAMPLE_NUM )));
				}
			} else if (pSampler == 0) {
				targetEnergy = calcAverage(searchBestSamples(partSamples,
						(int) (SAMPLE_NUM), 2 * SAMPLE_NUM));
			} else if (true) {
				targetEnergy = calcAverage(searchBestSamples(partSamples,
						(int) (SAMPLE_NUM),
						(int) (2 * SAMPLE_NUM)));
			} else {
				double sumWeight = 0;
				double[] weight = new double[partSamples.size()];
				for (int i = 0; i < partSamples.size(); i++) {
					double f = partSamples.get(i).getValue();
					double a = probGauss(f, averageEnergy[pSampler - 1],
							varianceEnergy[pSampler - 1]);
					double b = probGauss(f, averageEnergy[pSampler],
							varianceEnergy[pSampler]);
					double c = probGauss(f, averageEnergy[pSampler + 1],
							varianceEnergy[pSampler + 1]);
					weight[i] = b / (a + b + c);
					sumWeight += weight[i];
				}
				targetEnergy = 0;
				for (int i = 0; i < partSamples.size(); i++) {
					targetEnergy += partSamples.get(i).getValue() * weight[i];
				}
				targetEnergy /= sumWeight;
				System.out.printf("%8.3f -> %8.3f", averageEnergy[pSampler],
						targetEnergy);
			}
		}
		return targetEnergy;
	}

	public void optimize() {
		// select layer
		// int pSampler = rand.nextInt(LEVEL);
		pSampler = (pSampler + 1) % LEVEL;
		System.out.println(pSampler);

		samplingAndAveraging(pSampler);

		List<SampleWithValue<SampleType>> partSamples = getPartOfSamples(pSampler);
		List<Sampler<SampleType>> partSampler = getPartOfSampler(pSampler);

		double targetEnergy = getTargetEnergy(pSampler, partSamples);

		// calc beta
		double beta = 0;
		if (true) {
			beta = inverseTemperature[pSampler]
					+ calcDeltaBeta(targetEnergy, averageEnergy[pSampler],
							varianceEnergy[pSampler]);
		} else {
			List<WeightedSampleWithValue<SampleType>> allSamples = generateMarginalizedSamples(
					inverseTemperature[pSampler], partSamples, partSampler);
			if(allSamples.size()<2){
				System.out.println("continue bo allsamples in beta calc");
				return;
			}
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
				+ (targetEnergy - averageEnergy[pSampler]) + ")" + targetEnergy
				+ "->" + averageEnergy[pSampler]);

		// generating approximation function
		List<WeightedSampleWithValue<SampleType>> allSamples = generateMarginalizedSamples(
				beta, partSamples, partSampler);
		if(allSamples.size()<2){
			System.out.println("continue bo allsamples");
			return;
		}
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

	private List<SampleWithValue<SampleType>> searchBestSamples(
			List<SampleWithValue<SampleType>> samples, int from, int to) {
		Collections.sort(samples, new SampleWithValueComparator());
		return samples.subList(from, to);
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
			System.out.println("calcAverage");
			System.out.println(sumWeight);
			// System.exit(0);
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
			double beta, List<SampleWithValue<SampleType>> listOfSamples,
			List<Sampler<SampleType>> listOfSampler) {
		int sampleSize = listOfSampler.size() * SAMPLE_NUM;
		List<WeightedSampleWithValue<SampleType>> allSamples = new ArrayList<WeightedSampleWithValue<SampleType>>(
				sampleSize);
		double[] logP = new double[listOfSamples.size()];
		for (int i = 0; i < listOfSamples.size(); i++) {
			double maxLogP = Double.NEGATIVE_INFINITY;
			double[] tempLogP = new double[listOfSampler.size()];
			for (int j = 0; j < listOfSampler.size(); j++) {
				tempLogP[j] = listOfSampler.get(j).logNormalizedProbability(
						listOfSamples.get(i).getSample());
				if (maxLogP < tempLogP[j])
					maxLogP = tempLogP[j];
			}
			double expPartSum = 0;
			for (int j = 0; j < listOfSampler.size(); j++) {
				expPartSum += Math.exp(tempLogP[j] - maxLogP);
			}
			logP[i] = maxLogP + Math.log(expPartSum);
		}

		double maxLogW = -Double.MAX_VALUE;
		double[] logW = new double[listOfSamples.size()];
		for (int i = 0; i < listOfSamples.size(); i++) {
			logW[i]=-listOfSamples.get(i).getValue()*beta-logP[i];
			if(maxLogW<logW[i])maxLogW=logW[i];
		}
		for (int i = 0; i < listOfSamples.size(); i++) {
			double tempW=Math.exp(logW[i]-maxLogW);
			if(tempW<1e-5)continue;
			allSamples.add(new WeightedSampleWithValue<SampleType>(tempW, listOfSamples.get(i)));
		}
		System.out.printf("gm size=%d :beta=%f \n",allSamples.size(),beta);
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
