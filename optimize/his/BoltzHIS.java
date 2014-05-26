package optimize.his;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import java.util.Random;

import optimize.Optimizer;
import optimize.function.CostFunction;
import optimize.util.LogSumExp;
import optimize.util.LogWeighted;
import optimize.util.SampleWithValue;
import optimize.util.SampleWithValueComparator;
import optimize.util.WeightedSampleWithValue;
import statistics.sampler.Sampler;

public class BoltzHIS<SampleType> implements Optimizer<SampleType> {

	protected final int SAMPLE_NUM;

	protected final int LEVEL;

	protected final CostFunction<SampleType> cf;

	protected final Sampler<SampleType>[] samplerSet;

	private final List<SampleWithValue<SampleType>>[] sampleWithValueSet;

	private final double[] averageEnergy;

	private final double[] varianceEnergy;

	private final double[] entropy;

	private final double[] beta;

	private int pSampler = 0;

	private boolean isInitialized = false;

	public SampleWithValue<SampleType> getBest() {
		List<SampleWithValue<SampleType>> list = new ArrayList<SampleWithValue<SampleType>>(
				this.SAMPLE_NUM * 3);
		for (int i = 0; i < LEVEL; i++) {
			list.addAll(this.sampleWithValueSet[i]);
		}
		return searchBestSamples(list, 0, 1).get(0);
	}

	public BoltzHIS(int sample_num, int level, CostFunction<SampleType> cf) {
		this.SAMPLE_NUM = sample_num;
		this.LEVEL = level;
		this.samplerSet = new Sampler[LEVEL];
		this.sampleWithValueSet = new List[LEVEL];
		this.averageEnergy = new double[LEVEL];
		this.varianceEnergy = new double[LEVEL];
		this.entropy = new double[LEVEL];
		this.beta = new double[LEVEL];
		this.cf = cf;
	}

	protected void init() {
		for (int i = 0; i < LEVEL; i++) {
			samplingAndAveraging(i);
			beta[i] = 0;
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

	private void samplingAndReplacement(int pSampler) {
		if (pSampler == LEVEL - 1) {
			samplingAndAveraging(pSampler);
			return;
		}
		sampleWithValueSet[pSampler] = sampling(pSampler);
		sampleWithValueSet[pSampler] = searchBestSamples(
				sampleWithValueSet[pSampler], 0, sampleWithValueSet[pSampler]
						.size());
	}

	private void samplingAndAveraging(int pSampler) {
		sampleWithValueSet[pSampler] = sampling(pSampler);
		averageEnergy[pSampler] = calcAverage(sampleWithValueSet[pSampler]);
		varianceEnergy[pSampler] = calcVariance(sampleWithValueSet[pSampler],
				averageEnergy[pSampler]);
		sampleWithValueSet[pSampler] = searchBestSamples(
				sampleWithValueSet[pSampler], 0, sampleWithValueSet[pSampler]
						.size());
	}

	private List<SampleWithValue<SampleType>> getPartOfSamples(int pSampler) {
		List<SampleWithValue<SampleType>> list = new ArrayList<SampleWithValue<SampleType>>(
				this.SAMPLE_NUM * 3);
		for (int i = pSampler - 1; i <= pSampler + 1; i++) {
			if (i < 0) continue;
			if (LEVEL <= i) continue;
			list.addAll(this.sampleWithValueSet[i]);
		}
		return list;
	}

	private List<Sampler<SampleType>> getPartOfSampler(int pSampler) {
		List<Sampler<SampleType>> list = new ArrayList<Sampler<SampleType>>(3);
		for (int i = pSampler - 1; i <= pSampler + 1; i++) {
			if (i < 0) continue;
			if (LEVEL <= i) continue;
			list.add(samplerSet[i]);
		}
		return list;
	}

	// unbiased variance
	private double calcVariance(List<SampleWithValue<SampleType>> samples,
			double average) {
		double sumVariance = 0;
		for (SampleWithValue<SampleType> swv : samples) {
			double dist = swv.getValue() - average;
			sumVariance += dist * dist;
		}
		double variance = sumVariance / (samples.size() - 1);
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

	Random rand = new Random();

	public void optimize() {
		if (!isInitialized) {
			init();
			isInitialized = true;
		}
		// pSampler = (LEVEL + (pSampler + 1)) % LEVEL;
		pSampler = (LEVEL + (pSampler - 1)) % LEVEL;
		// pSampler = rand.nextInt(LEVEL);
		// shortcut
		if (true && pSampler == 0) {
			samplingAndAveraging(pSampler);
			beta[pSampler] = 0;
			return;
		}

		List<SampleWithValue<SampleType>> partSamples = getPartOfSamples(pSampler);
		List<Sampler<SampleType>> partSampler = getPartOfSampler(pSampler);

		// adjustBeta(pSampler);

		List<WeightedSampleWithValue<SampleType>> allSamples = mergeSamples(
				pSampler, partSamples, partSampler);

		if (allSamples.size() < 1) {
			samplingAndReplacement(pSampler);
			return;
		}

		samplerSet[pSampler].fittingUpdate(allSamples);
		// samplingAndAveraging(pSampler);
		samplingAndReplacement(pSampler);
	}

	private void adjustBeta(int pSampler) {
		if (pSampler == LEVEL - 1) {
			beta[pSampler] = 8000;
			// do nothing
		} else {
			double upStd = Math.sqrt(varianceEnergy[pSampler])
					+ Math.sqrt(varianceEnergy[pSampler + 1]);
			upStd /= 2;
			double upVar = upStd * upStd;
			double downStd = Math.sqrt(varianceEnergy[pSampler])
					+ Math.sqrt(varianceEnergy[pSampler - 1]);
			downStd /= 2;
			double downVar = downStd * downStd;

			upVar += 1e-30;
			downVar += 1e-30;

			double newBeta = upVar * beta[pSampler + 1] * beta[pSampler + 1]
					+ downVar * beta[pSampler - 1] * beta[pSampler - 1];
			newBeta /= upVar + downVar;
			newBeta = Math.sqrt(newBeta);
			if (Double.isNaN(newBeta)) {
				// System.out.println("beta");
				return;
			}
			beta[pSampler] = newBeta;
		}
	}

	private List<WeightedSampleWithValue<SampleType>> mergeSamples(
			int pSampler, List<SampleWithValue<SampleType>> samples,
			List<Sampler<SampleType>> listOfSampler) {
		// sort
		samples = searchBestSamples(samples, 0, samples.size());

		// create merged pop
		// calc logP : marginalization
		double[] logP = new double[samples.size()];
		for (int i = 0; i < samples.size(); i++) {
			double[] tempLogP = new double[listOfSampler.size()];
			for (int j = 0; j < listOfSampler.size(); j++) {
				tempLogP[j] = listOfSampler.get(j).logNormalizedProbability(
						samples.get(i).getSample());
			}
			logP[i] = LogSumExp.sum(tempLogP);
			logP[i] += Math.log(1d / listOfSampler.size());
			if (Double.isInfinite(logP[i])) {
				System.out.println("logp");
			}
		}


		
		// calc normalized weight;
		double[] logW = new double[samples.size()];
		
		if (pSampler == LEVEL - 1) {			
			double best = samples.get(0).getValue();
			for (int i = 0; i < samples.size(); i++) {
				if (samples.get(i).getValue() > best) {
					logW[i] = Double.NEGATIVE_INFINITY;
				} else {
					logW[i] = - logP[i];
				}
			}
			LogSumExp.normalize(logW);
			List<WeightedSampleWithValue<SampleType>> allSamples = new ArrayList<WeightedSampleWithValue<SampleType>>(
					samples.size());

			for (int i = 0; i < samples.size(); i++) {
				if (samples.get(i).getValue() > best)
					break;
				allSamples.add(new WeightedSampleWithValue<SampleType>(
						Math.exp(logW[i]), samples.get(i)));
			}
			return allSamples;		
		}

		double[] eff = new double[3];
		double[] entropy = new double[3];
		double[] avgs = new double[3];
		double[] vars = new double[3];
		for (int j = -1; j < 2; j++) {
			if (pSampler+j == LEVEL - 1) {
				List<WeightedSampleWithValue<SampleType>> allSamples = new ArrayList<WeightedSampleWithValue<SampleType>>(
						samples.size());

				double best = samples.get(0).getValue();
				for (int i = 0; i < samples.size(); i++) {
					if (samples.get(i).getValue() > best) {
						logW[i] = Double.NEGATIVE_INFINITY;
					} else {
						logW[i] = - logP[i];
					}
				}
			} else {
				for (int i = 0; i < samples.size(); i++) {
					logW[i] = -samples.get(i).getValue() * beta[pSampler + j]
							- logP[i];
				}
			}

			eff[j + 1] = getEffectiveNumber(logW);

			double logSum = LogSumExp.sum(logW);
			double logZ = logSum + Math.log(1d / samples.size());
			avgs[j + 1] = 0;
			double squaredAvg = 0;
			for (int i = 0; i < samples.size(); i++) {
				double w = Math.exp(logW[i] - logSum);
				avgs[j + 1] += w * samples.get(i).getValue();
				squaredAvg += w * samples.get(i).getValue()
						* samples.get(i).getValue();
			}
			entropy[j + 1] = avgs[j + 1] * beta[pSampler + j] + logZ;
			vars[j + 1] = squaredAvg - avgs[j + 1] * avgs[j + 1];
			if (eff[j + 1] >= 2) {
				vars[j + 1] *= eff[j + 1] / (eff[j + 1] - 1);
			}

			if (pSampler + j == 0) {
				this.entropy[0] = entropy[j + 1];
			}else if(pSampler+j==LEVEL-1){
				this.entropy[LEVEL-1]=entropy[j+1];
			}

		}
		double var = vars[0];
		this.entropy[pSampler] = entropy[1];

		double targetEnt = (entropy[0] + entropy[2]) / 2;
		// double deltaS = targetEnt - entropy[1];
		double deltaS = targetEnt - entropy[0];
		// System.out.println(Math.log(Math.sqrt(eff[0]*eff[2]))-Math.log(eff[1])+"vs"+deltaS);
		// deltaS=0.5*Math.log(eff[0]*eff[2])-Math.log(eff[0]);
		while (true) {
			if (var != 0) {
				double diff = beta[pSampler - 1] * beta[pSampler - 1] - 2
						* deltaS / var;
				if (diff >= 0) {
					beta[pSampler] = Math.sqrt(diff);
					break;
				} else {
					System.out.println("diff is minus");
					// beta[pSampler] = beta[pSampler - 1];
					deltaS /= 2;
				}
			} else {
				System.out.println("large diff:" + deltaS + "/" + var + "="
						+ (deltaS / var));
				beta[pSampler] = beta[pSampler - 1];
				break;
			}
			if (beta[pSampler] > beta[pSampler + 1]) {
				System.out.println("large diff:" + deltaS + "/" + var + "="
						+ (deltaS / var));
				// beta[pSampler] = beta[pSampler - 1];
				deltaS /= 2;
			}
		}

		{
			int j = 0;
			for (int i = 0; i < samples.size(); i++) {
				logW[i] = -samples.get(i).getValue() * beta[pSampler + j]
						- logP[i];
			}
			double logSum = LogSumExp.sum(logW);
			double logZ = logSum + Math.log(1d / samples.size());
			double avg = 0;
			double squaredAvg = 0;
			for (int i = 0; i < samples.size(); i++) {
				double w = Math.exp(logW[i] - logSum);
				avg += w * samples.get(i).getValue();
				squaredAvg += w * samples.get(i).getValue()
						* samples.get(i).getValue();
			}
			entropy[j + 1] = avg * beta[pSampler + j] + logZ;
			System.out.println(pSampler + ":" + targetEnt + "-"
					+ entropy[j + 1] + "=" + (targetEnt - entropy[j + 1]));
			if (Math.abs(targetEnt - entropy[j + 1]) > 10) {
				System.out.println("large diff:" + deltaS + "/" + var + "="
						+ (deltaS / var));
				System.out.println();
			}
		}

		LogSumExp.normalize(logW);
		double[] weight = new double[samples.size()];
		for (int i = 0; i < samples.size(); i++) {
			weight[i] = Math.exp(logW[i]);
		}

		// calc avg and var
		double avg = 0;
		double squaredAvg = 0;
		for (int i = 0; i < samples.size(); i++) {
			double w = weight[i];
			avg += w * samples.get(i).getValue();
			squaredAvg += w * samples.get(i).getValue()
					* samples.get(i).getValue();
		}
		averageEnergy[pSampler] = avg;
		varianceEnergy[pSampler] = squaredAvg - avg * avg;

		if (Double.isNaN(varianceEnergy[pSampler])) {
			System.out.println("vara");
		}

		// to merge samples for building a probability model
		List<WeightedSampleWithValue<SampleType>> allSamples = new ArrayList<WeightedSampleWithValue<SampleType>>(
				samples.size());

		for (int i = 0; i < samples.size(); i++) {
			allSamples.add(new WeightedSampleWithValue<SampleType>(weight[i],
					samples.get(i)));
			if (pSampler == LEVEL - 1) break;
		}

		return allSamples;
	}

	private List<SampleWithValue<SampleType>> searchBestSamples(
			List<SampleWithValue<SampleType>> samples, int from, int to) {
		Collections.sort(samples, new SampleWithValueComparator());
		return samples.subList(from, to);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		Formatter formatter = new Formatter(sb);
		sb.append("ene:");
		for (int i = 0; i < LEVEL; i++) {
			formatter.format("%6.3f (%6.3f) || ", averageEnergy[i],
					varianceEnergy[i]);
		}
		sb.append("\n");
		sb.append("bet:");
		for (int i = 0; i < LEVEL; i++) {
			formatter.format("%6.3f ", beta[i]);
		}
		sb.append("\n");
		sb.append("ent:");
		for (int i = 0; i < LEVEL; i++) {
			formatter.format("%6.3f ", entropy[i]);
		}
		sb.append("\n");
		if (true) {
			for (int i = LEVEL - 3; i < LEVEL; i++) {
				if (i < 0) continue;
				// sb.append("sampler" + i + ":\n" + samplerSet[i].toString()
				// + "\n");
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

	private double getEffectiveNumber(double logw[]) {
		double logSum = LogSumExp.sum(logw);
		double[] newlogW = new double[logw.length];
		for (int i = 0; i < logw.length; i++) {
			newlogW[i] = logw[i] - logSum;
		}
		double entropy = 0;
		for (double lw : newlogW) {
			double prob = Math.exp(lw);
			if (Double.isInfinite(lw)) continue;
			entropy += -prob * lw;
		}
		return Math.exp(entropy);
	}
}
