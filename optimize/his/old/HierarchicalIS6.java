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

public class HierarchicalIS6<SampleType> {

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

	public HierarchicalIS6(int sample_num, int level,
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

	private void samplingAndAveraging(int pSampler) {
		sampleWithValueSet[pSampler] = sampling(pSampler);
		averageEnergy[pSampler] = calcAverage(sampleWithValueSet[pSampler]);
		varianceEnergy[pSampler] = calcVariance(sampleWithValueSet[pSampler],
				averageEnergy[pSampler]);
	}

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

	public void optimize() {
		// select layer
		// int pSampler = rand.nextInt(LEVEL);
		pSampler = (pSampler + 1) % LEVEL;
		System.out.println(pSampler);

		samplingAndAveraging(pSampler);

		List<SampleWithValue<SampleType>> partSamples = getPartOfSamples(pSampler);
		List<Sampler<SampleType>> partSampler = getPartOfSampler(pSampler);

		
		double beta =0;
		if(pSampler==0){
			// do nothing
			List<WeightedSampleWithValue<SampleType>> allSamples = generateMarginalizedSamples(
					beta, partSamples, partSampler);
		
			if (allSamples.size() < 2) {
				System.out.println("continue bo allsamples");
				return;
			}
			samplerSet[pSampler].fittingUpdate(allSamples);
		
			return;
		}else if(pSampler==LEVEL-1){
			partSamples=searchBestSamples(partSamples, 0, SAMPLE_NUM/4);
			beta=0;

			List<WeightedSampleWithValue<SampleType>> allSamples = generateMarginalizedSamples(
					beta, 0,partSamples, partSampler);
			samplerSet[pSampler].fittingUpdate(allSamples);
		}else{
			beta=getTargetBeta(pSampler, partSamples, partSampler);
			System.out.println("beta="+beta);
			
			if (beta < 0) {
				beta = inverseTemperature[pSampler];
			} else {
				inverseTemperature[pSampler] = beta;
			}

			// generating approximation function
			List<WeightedSampleWithValue<SampleType>> allSamples = generateMarginalizedSamples(
					beta, partSamples, partSampler);
		
			if (allSamples.size() < 2) {
				System.out.println("continue bo allsamples");
				return;
			}
			samplerSet[pSampler].fittingUpdate(allSamples);
		}
	}

	private double getTargetBeta(int sampler,
			List<SampleWithValue<SampleType>> partSamples,
			List<Sampler<SampleType>> partSampler) {
		if (sampler == LEVEL - 1)
			return getTargetBestBeta(partSamples, partSampler);
		if(true){
			return Math.pow(1.2, sampler);
		}
		double[] logP = new double[partSamples.size()];
		double[] w_u = new double[partSamples.size()];
		double[] w_l = new double[partSamples.size()];
		double[] logw_u = new double[partSamples.size()];
		double[] logw_l = new double[partSamples.size()];
		double maxLogw_u = 0;
		double maxLogw_l = 0;
		for (int i = 0; i < partSamples.size(); i++) {
			double maxLogP = Double.NEGATIVE_INFINITY;
			double[] tempLogP = new double[partSampler.size()];
			for (int j = 0; j < partSampler.size(); j++) {
				tempLogP[j] = partSampler.get(j).logNormalizedProbability(
						partSamples.get(i).getSample());
				if (maxLogP < tempLogP[j])
					maxLogP = tempLogP[j];
			}
			double expPartSum = 0;
			for (int j = 0; j < partSampler.size(); j++) {
				expPartSum += Math.exp(tempLogP[j] - maxLogP);
			}
			logP[i] = maxLogP + Math.log(expPartSum);
			// logw_u[i] = -partSamples.get(i).getValue()
			// * inverseTemperature[sampler + 1] - logP[i];
			logw_u[i] = samplerSet[sampler + 1]
					.logNormalizedProbability(partSamples.get(i).getSample());
			if (maxLogw_u < logw_u[i])
				maxLogw_u = logw_u[i];
			// logw_l[i] = -partSamples.get(i).getValue()
			// * inverseTemperature[sampler - 1] - logP[i];
			logw_l[i] = samplerSet[sampler - 1]
					.logNormalizedProbability(partSamples.get(i).getSample());
			if (maxLogw_l < logw_l[i])
				maxLogw_l = logw_l[i];
		}
		double sumW_u = 0;
		double sumW_l = 0;
		for (int i = 0; i < partSamples.size(); i++) {
			w_u[i] = Math.exp(logw_u[i] - maxLogw_u);
			sumW_u += w_u[i];
			w_l[i] = Math.exp(logw_l[i] - maxLogw_l);
			sumW_l += w_l[i];
		}
		double[] delta = new double[partSamples.size()];
		for (int i = 0; i < partSamples.size(); i++) {
			w_u[i] /= sumW_u;
			w_l[i] /= sumW_l;
			delta[i] = w_u[i] - w_l[i];
		}

		double numerator = 0;
		double denominator = 0;
		for (int i = 0; i < partSamples.size(); i++) {
			numerator += delta[i] * logP[i];
			denominator += delta[i] * -partSamples.get(i).getValue();
		}
		if (numerator == 0)
			return 0;
		double beta = numerator / denominator;
		double sum_u = 0, sum_l = 0;
		for (int i = 0; i < partSamples.size(); i++) {
			sum_u += w_u[i] * (-partSamples.get(i).getValue() * beta - logP[i]);
			sum_l += w_l[i] * (-partSamples.get(i).getValue() * beta - logP[i]);
		}
		System.out.printf("%4f ?= %4f \n", sum_u, sum_l);
		return beta;
	}

	private double getTargetBestBeta(
			List<SampleWithValue<SampleType>> partSamples,
			List<Sampler<SampleType>> partSampler) {
		List<SampleWithValue<SampleType>> bestSamples = searchBestSamples(
				partSamples, 0, SAMPLE_NUM);
		double[] logP = new double[partSamples.size()];
		double[] w_u = new double[partSamples.size()];
		double[] w_l = new double[partSamples.size()];
		double[] logw_l = new double[partSamples.size()];
		double maxLogw_l = 0;
		for (int i = 0; i < partSamples.size(); i++) {
			double maxLogP = Double.NEGATIVE_INFINITY;
			double[] tempLogP = new double[partSampler.size()];
			for (int j = 0; j < partSampler.size(); j++) {
				tempLogP[j] = partSampler.get(j).logNormalizedProbability(
						partSamples.get(i).getSample());
				if (maxLogP < tempLogP[j])
					maxLogP = tempLogP[j];
			}
			double expPartSum = 0;
			for (int j = 0; j < partSampler.size(); j++) {
				expPartSum += Math.exp(tempLogP[j] - maxLogP);
			}
			logP[i] = maxLogP + Math.log(expPartSum);

			logw_l[i] = -partSamples.get(i).getValue()
					* inverseTemperature[LEVEL - 2] - logP[i];
			if (maxLogw_l < logw_l[i])
				maxLogw_l = logw_l[i];
		}
		double sumW_u = 0;
		double sumW_l = 0;
		for (int i = 0; i < partSamples.size(); i++) {
			if (bestSamples.contains(partSamples.get(i)))
				w_u[i] = 1;
			else
				w_u[i] = 0;

			sumW_u += w_u[i];
			// w_l[i] = Math.exp(logw_l[i]-maxLogw_l);
			w_l[i] = 1;
			sumW_l += w_l[i];
		}
		double[] delta = new double[partSamples.size()];
		for (int i = 0; i < partSamples.size(); i++) {
			w_u[i] /= sumW_u;
			w_l[i] /= sumW_l;
			delta[i] = w_u[i] - w_l[i];
		}

		double numerator = 0;
		double denominator = 0;
		for (int i = 0; i < partSamples.size(); i++) {
			numerator += delta[i] * logP[i];
			denominator += delta[i] * -partSamples.get(i).getValue();
		}
		double beta = numerator / denominator;
		double sum_u = 0, sum_l = 0;
		for (int i = 0; i < partSamples.size(); i++) {
			sum_u += w_u[i] * (-partSamples.get(i).getValue() * beta - logP[i]);
			sum_l += w_l[i] * (-partSamples.get(i).getValue() * beta - logP[i]);
		}
		System.out.printf("%4e ?= %4e \n", sum_u, sum_l);
		return beta;
	}

	private List<SampleWithValue<SampleType>> searchBestSamples(
			List<SampleWithValue<SampleType>> samples, int from, int to) {
		Collections.sort(samples, new SampleWithValueComparator());
		return samples.subList(from, to);
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
			logW[i] = -listOfSamples.get(i).getValue() * beta - logP[i];
			if (maxLogW < logW[i])
				maxLogW = logW[i];
		}
		for (int i = 0; i < listOfSamples.size(); i++) {
			double tempW = Math.exp(logW[i] - maxLogW);
			if (tempW < 1e-5)
				continue;
			allSamples.add(new WeightedSampleWithValue<SampleType>(tempW,
					listOfSamples.get(i)));
		}
		System.out.printf("gm size=%d :beta=%f \n", allSamples.size(), beta);
		return allSamples;
	}

	private List<WeightedSampleWithValue<SampleType>> generateMarginalizedSamples(
			double beta, double decay,
			List<SampleWithValue<SampleType>> listOfSamples,
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
			logW[i] = -listOfSamples.get(i).getValue() * beta - decay * logP[i];
			if (maxLogW < logW[i])
				maxLogW = logW[i];
		}
		for (int i = 0; i < listOfSamples.size(); i++) {
			double tempW = Math.exp(logW[i] - maxLogW);
			if (tempW < 1e-5)
				continue;
			allSamples.add(new WeightedSampleWithValue<SampleType>(tempW,
					listOfSamples.get(i)));
		}
		System.out.printf("gm size=%d :beta=%f \n", allSamples.size(), beta);
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
