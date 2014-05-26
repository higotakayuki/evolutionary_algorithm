package optimize.optimizer;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import app.discrete.sampler.UMDADiscreteSampler;

import calc.util.Array;
import calc.util.Function;

import optimize.Annealing;
import optimize.function.CostFunction;
import optimize.util.LogSumExp;
import optimize.util.SampleWithValue;
import optimize.util.SampleWithValueComparator;
import optimize.util.WeightedSampleWithValue;
import statistics.sampler.Sampler;

public abstract class TruncationRSEA<SampleType> implements
		Annealing<SampleType> {
	private boolean initilized = false;

	protected final int SAMPLE_NUM;

	protected final CostFunction<SampleType> cf;

	private Sampler<SampleType> sampler;

	// private List<SampleWithValue<SampleType>> sampleWithValue;
	private List<SampleWithValue<SampleType>> sortedGeneratedSamples;
	private List<SampleWithValue<SampleType>> candidate;

	private double averageEnergy;

	private double varianceEnergy;

	private double cutoff;

	private double lambda1; // for convergence
	private double lambda2; // for estimation
	private double[] logP;
	// private double[] logW;
	// private double[] w;
	private double threshold = Double.MAX_VALUE;

	public TruncationRSEA(int sample_num, double cutoff,
			double lambda1, double lambda2,
			CostFunction<SampleType> cf) {
		this.SAMPLE_NUM = sample_num;
		this.cf = cf;
		this.cutoff = cutoff;
		this.lambda1 = lambda1;
		this.lambda2 = lambda2;
	}

	public void optimize() throws ConvergenceException {
		if (!initilized) initilize();

		// sampling
		samplingAndAveraging();
		updateCandidate();
		if (candidate.size() <= 1) throw new ConvergenceException();
		{
			logP = new double[candidate.size()];
			for (int i = 0; i < candidate.size(); i++) {
				logP[i] =
						sampler.logNormalizedProbability(candidate
								.get(i).getSample());
			}
		}

		this.lambda1 = 1;
		updateTargetDist();
		updateCandidate();
		if (candidate.size() <= 1) throw new ConvergenceException();

		this.lambda2 = 1;
		updateSampler();

		// System.out.println("ent(q_t,p_t):"+calcSearchSpaceSize());
		// annealing
		// List<WeightedSampleWithValue<SampleType>> wsamples =
		// mergeSamples();

		// fitting
		// System.out.println("ent(q_t+1,p_t):"+calcSearchSpaceSize());
		// sampler.fittingUpdate(wsamples);

	}

	private void samplingAndAveraging() {
		List<SampleWithValue<SampleType>> sampleWithValue =
				sampling();
		sortedGeneratedSamples =
				searchBestSamples(sampleWithValue, 0, SAMPLE_NUM);

		averageEnergy = calcAverage(sortedGeneratedSamples);
		varianceEnergy =
				calcVariance(sortedGeneratedSamples, averageEnergy);
	}

	private void updateCandidate() {
		List<SampleWithValue<SampleType>> candidate =
				new ArrayList<SampleWithValue<SampleType>>(SAMPLE_NUM);

		{// 閾値より低い目的関数値を持つサンプルを捨てる
			for (SampleWithValue<SampleType> swv : sortedGeneratedSamples) {
				if (threshold < swv.getValue()) break;
				candidate.add(swv); // if value <= threshold
			}
		}
		this.candidate = candidate;

	}

	private void updateTargetDist() {

		double[] logW = Array.zeros(candidate.size());
		logW = Array.minus(logW, logP, candidate.size());
		LogSumExp.normalize(logW);

		double sum = (1 - cutoff);
		this.threshold =
				candidate.get(candidate.size() - 1).getValue();

		for (int i = 0; i < candidate.size() - 1; i++) {// 必ず１個捨てる
			double w = Math.exp(logW[i]);
			sum -= w;
			if (sum < 0) {
				this.threshold = candidate.get(i).getValue();
				break;
			}
		}

	}

	DecimalFormat format = new DecimalFormat("###");

	private int delta = 10;

	private void updateSampler() {
		double[] logW = Array.zeros(candidate.size());
		logW = Array.minus(logW, logP, candidate.size());
		LogSumExp.normalize(logW);
		double[] w = Array.map(Array.exp, logW);

		double[] wlogw = new double[delta + 1];
		double[] avgW = new double[delta + 1];
		double[] entropy = new double[delta + 1];
		double[] wdifflogWlambdaP = new double[delta + 1];
		double[] logLike = new double[delta + 1];
		double[] aic = new double[delta + 1];
		double[] score = new double[delta + 1];
		double[] wlogwPN = new double[delta + 1];
		Sampler[] sampler = new Sampler[delta + 1];

		double span = 1d / delta;
		for (int i = 0; i < delta + 1; i++) {
			double lambda = i * span;
			double[] logW_lambda =
					Array.subArray(logW, 0, candidate.size());
			logW_lambda = Array.mult(lambda, logW_lambda);
			LogSumExp.normalize(logW_lambda);
			double[] w_lambda = Array.map(Array.exp, logW_lambda);
			double[] w_diff =
					Array.minus(w, w_lambda, candidate.size());

			wlogw[i] =
					-Array.dotProduct(w_lambda, logW_lambda,
							candidate.size());
			double wlogP =
					-Array.dotProduct(w_lambda, this.logP,
							candidate.size());
			wlogwPN[i] =
					wlogw[i] + wlogP - Math.log(candidate.size());

			double[] logWlambdaP =
					Array.plus(logW_lambda, this.logP,
							candidate.size());
			wdifflogWlambdaP[i] =
					Array.dotProduct(w_diff, logWlambdaP,
							candidate.size());

			List<WeightedSampleWithValue<SampleType>> ldata =
					new ArrayList<WeightedSampleWithValue<SampleType>>(
							candidate.size());
			for (int j = 0; j < candidate.size(); j++) {
				ldata.add(new WeightedSampleWithValue<SampleType>(
						Math.exp(logW_lambda[j]), candidate.get(j)));
			}
			sampler[i] = this.sampler.fittingReplace(ldata);

			double[] logPnew = new double[candidate.size()];
			for (int j = 0; j < candidate.size(); j++) {
				logPnew[j] =
						sampler[i].logNormalizedProbability(candidate
								.get(j).getSample());
			}
			avgW = Array.map(Array.exp, Array.mult(-1, wlogw));
			logLike[i] = Array.dotProduct(w_lambda, logPnew);
			aic[i] = -400d / Math.exp(wlogw[i]);

			entropy[i] = sampler[i].entropy();
			score = Array.plus(entropy, wdifflogWlambdaP);
		}

		// debug print
		// Array.println(entropy, " 000");//
		// Array.println(wlogw, "000");
		// Array.println(avgW, "000");

		// Array.println(wlogwPN, " 000");//
		// Array.println(Array.plus(wlogw, wlogPN), "000");
		// Array.println(logLike, "000");
		// Array.println(aic, "000");
		// Array.println(wdifflogWlambdaP, "000");
		// Array.println(score, "000");
		// int maxIdx = Array.maxIndex(score);
		// maxIdx = 1;

		// ent adjust
		
		double curEnt = this.sampler.entropy();
		double[] curEntVec = Array.mult(curEnt,// + Math.log(1 - cutoff),
				Array.ones(delta + 1));
		//double[] diff = Array.minus(curEntVec, wlogwPN);
		double[] diff = Array.minus(entropy, wlogwPN);
		diff = Array.map(Array.abs, diff);

		int selIdx = 0;
		switch (1) {
		case 0:
			selIdx=0;
			break;
		case 1:
			selIdx = Array.minIndex(diff);
			break;
		case 2:
			selIdx = delta;
			break;
		}

		this.sampler = sampler[selIdx];
		System.out.print(this.sampler.entropy());
		System.out.print(" " + this.sampler.entropy());
		System.out.println(" " +wlogwPN[selIdx]+" " + wlogwPN[delta]);
		if (false) {
			Array.println(entropy, " 000");//
			Array.println(wlogwPN, " 000");//

			System.out
					.println(selIdx
							+ ":"
							+ format.format(((UMDADiscreteSampler) this.sampler)
									.entropy())
							+ "->"
							+ format.format(((UMDADiscreteSampler) sampler[selIdx])
									.entropy()));
		}

	}

	private void initilize() {
		initilized = true;
		threshold = Double.MAX_VALUE;
		this.sampler = getInitialSampler();
	}

	protected abstract Sampler<SampleType> getInitialSampler();

	/*
	 * // lambdaに関するテスト private void calcEntropyWithVaringLambda() { int trial =
	 * 10; double delta = 1.0 / trial; double[] ent = new double[trial + 1];
	 * 
	 * for (int i = 0; i < trial + 1; i++) { double lambda = i * delta; double[]
	 * logP = new double[sampleWithValue.size()]; // double[] logQ = new
	 * double[sampleWithValue.size()]; double[] lbmdaLogQOverP = new
	 * double[sampleWithValue.size()]; double[] logQOverP = new
	 * double[sampleWithValue.size()]; for (int j = 0; j <
	 * lbmdaLogQOverP.length; j++) { logP[j] =
	 * sampler.logNormalizedProbability(sampleWithValue .get(j).getSample()); if
	 * (sampleWithValue.get(j).getValue() <= this.threshold) { lbmdaLogQOverP[j]
	 * = -lambda * logP[j]; logQOverP[j] = -logP[j]; } else { lbmdaLogQOverP[j]
	 * = Double.NEGATIVE_INFINITY; logQOverP[j] = Double.NEGATIVE_INFINITY; } }
	 * 
	 * double logZ = LogSumExp.sum(lbmdaLogQOverP);
	 * LogSumExp.normalize(logQOverP);
	 * 
	 * double[] logWX = new double[sampleWithValue.size()]; double[] wx = new
	 * double[sampleWithValue.size()]; for (int j = 0; j < logWX.length; j++) {
	 * logWX[j] = lbmdaLogQOverP[j] - logZ; wx[j] = Math.exp(logWX[j]); }
	 * 
	 * ent[i] = 0; double entWx = 0; double kl = 0; for (int j = 0; j <
	 * sampleWithValue.size(); j++) { if (Double.isInfinite(logWX[j])) continue;
	 * double temp = logP[j] + logWX[j]; ent[i] += wx[j] * temp; // entWx +=
	 * wx[j] * logWX[j]; entWx += wx[j] * logWX[j]; kl += Math.exp(logQOverP[j])
	 * * logWX[j]; } ent[i] += Math.log(sampleWithValue.size()); ent[i] *= -1;
	 * entWx *= -1; System.out.print(Math.exp(entWx) + " "); //
	 * System.out.print(Math.exp(entWx) + " "); } System.out.println();
	 * 
	 * }
	 */

	private List<SampleWithValue<SampleType>> sampling() {
		List<SampleType> samples = sampler.sampling(SAMPLE_NUM);
		List<SampleWithValue<SampleType>> samplesWithValue =
				new ArrayList<SampleWithValue<SampleType>>(SAMPLE_NUM);
		for (SampleType st : samples) {
			// eval samples
			samplesWithValue.add(new SampleWithValue<SampleType>(st,
					cf.eval(st)));
		}
		return samplesWithValue;
	}

	private double calcAverage(
			List<SampleWithValue<SampleType>> samples) {
		double sumAverage = 0;
		for (SampleWithValue<SampleType> swv : samples) {
			sumAverage += swv.getValue();
		}
		double average = sumAverage / samples.size();
		return average;
	}

	private double calcVariance(
			List<SampleWithValue<SampleType>> samples, double average) {
		double sumVariance = 0;
		for (SampleWithValue<SampleType> swv : samples) {
			double dist = swv.getValue() - average;
			sumVariance += dist * dist;
		}
		double variance = sumVariance / samples.size();
		return variance;
	}

	// call CalcCandidate() before call this method.
	private double calcSearchSpaceSize() {
		double[] logP = new double[candidate.size()];
		for (int i = 0; i < candidate.size(); i++) {
			logP[i] =
					-sampler.logNormalizedProbability(candidate
							.get(i).getSample());
		}
		double ent = LogSumExp.sum(logP) - Math.log(this.SAMPLE_NUM);
		return ent;
	}

	/*
	 * 2wlog(w)+　wlog(p) を計算する
	 */

	private double testLambdaAdaptation() {
		try {
			double[] result = new double[11];
			for (int i = 0; i <= 10; i++) {
				double lambda = i * 0.1;
				double[] logP = new double[candidate.size()];
				double[] logW = new double[candidate.size()];
				for (int j = 0; j < candidate.size(); j++) {
					logP[j] =
							sampler.logNormalizedProbability(candidate
									.get(j).getSample());
					logW[j] = -logP[j] * lambda;// log(1/p)=-log(p)
				}
				LogSumExp.normalize(logW);
				double temp = 0;
				for (int j = 0; j < candidate.size(); j++) {
					double w = Math.exp(logW[j]);
					temp += w * (2 * logW[j] + logP[j]);
					// temp += w * (logW[j] + logP[j]);
					// temp += w * (2 * logW[j]);
				}
				result[i] = temp;
				if (i != 0) System.out.print(",");
				System.out.print(result[i]);
			}
			int lambdaIdx = Array.minIndex(result);
			System.out.println(":" + lambdaIdx);
			return lambdaIdx * 0.1;
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}

	private List<WeightedSampleWithValue<SampleType>> mergeSamples() {

		// the num of candidate is greater than 2.

		double[] logW = new double[candidate.size()];
		double[] logW_lambda1 = new double[candidate.size()];
		double[] logW_lambda2 = new double[candidate.size()];

		// adjust lambda?
		for (int i = 0; i < candidate.size(); i++) {
			logW[i] =
					-sampler.logNormalizedProbability(candidate
							.get(i).getSample());
			logW_lambda1[i] = logW[i] * lambda1;
			logW_lambda2[i] = logW[i] * lambda2;
		}
		logW = null;
		// adjust lambda?

		LogSumExp.normalize(logW_lambda1); // for convergence
		LogSumExp.normalize(logW_lambda2); // for estimation

		double sum = (1 - cutoff);

		List<WeightedSampleWithValue<SampleType>> selectedSamples =
				new ArrayList<WeightedSampleWithValue<SampleType>>(
						candidate.size());
		{// promising
			for (int i = 0; i < candidate.size() - 1; i++) {// 必ず１個捨てる
				if (sum < 0) break;

				double w = Math.exp(logW_lambda1[i]);
				sum -= w;
				selectedSamples
						.add(new WeightedSampleWithValue<SampleType>(
								Math.exp(logW_lambda2[i]), candidate
										.get(i)));
			}
			this.threshold =
					selectedSamples.get(selectedSamples.size() - 1)
							.getSampeWithVale().getValue();
			// this.threshold = sortedPreviousSamples.get(promising.size())
			// .getValue();
		}

		return selectedSamples;
	}

	private List<SampleWithValue<SampleType>> searchBestSamples(
			List<SampleWithValue<SampleType>> samples, int from,
			int to) {
		Collections.sort(samples, new SampleWithValueComparator());
		return samples.subList(from, to);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("" + ((int) (SAMPLE_NUM * (1 - cutoff))) + ": "
				+ averageEnergy + "(" + varianceEnergy + ")"
				+ "\n best:"
				+ searchBestSamples(sortedGeneratedSamples, 0, 1)
				+ "\n");
		sb.append(sampler + "\n");
		sb.append("tilde f: " + this.threshold + "\n");
		return sb.toString();
	}

	public SampleType getBestSolution() {
		return sortedGeneratedSamples.get(0).getSample();
	}

	public SampleWithValue<SampleType> getBest() {
		return sortedGeneratedSamples.get(0);
	}

	public double getVariance() {
		return varianceEnergy;
	}

	// @Override
	// public double getAverage() {
	// return averageEnergy;
	// }

	private double effectiveNumber(double[] weight) {
		Function entFunc = new Function() {
			@Override
			public double op(double d) {
				if (0 < d) return -d * Math.log(d);
				else return 0;
			}
		};

		double[] nweight = Array.normalize(weight);
		double[] ent = Array.map(entFunc, nweight);
		return Math.exp(Array.sum(ent));
	}

}
