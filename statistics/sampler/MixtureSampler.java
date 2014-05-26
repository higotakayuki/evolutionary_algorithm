package statistics.sampler;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import app.discrete.sampler.UMDADiscreteSampler;
import app.discrete.util.DVector;

import optimize.util.LogSumExp;
import optimize.util.WeightedSampleWithValue;


public class MixtureSampler<Type> implements Sampler<Type> {

	private static Random rand = new Random();

	private Sampler<Type>[] samplers;

	private double[] mixtureRatio;

	public MixtureSampler(Sampler<Type>[] samplers) {
		this.samplers = samplers;
		mixtureRatio = new double[samplers.length];
		for (int i = 0; i < mixtureRatio.length; i++) {
			mixtureRatio[i] = 1d / mixtureRatio.length;
		}
	}

	public void fittingUpdate(List<WeightedSampleWithValue<Type>> allSamples) {
		if (true) {
			EM(2, allSamples);
		}
	}


	private void EM(int c, List<WeightedSampleWithValue<Type>> allSamples) {
		for (int i = 0; i < mixtureRatio.length; i++) {
			mixtureRatio[i] = 1d / mixtureRatio.length;
		}
		for (int i = 0; i < c; i++) {
			EMImpl(allSamples);
		}
	}

	private void EMImpl(List<WeightedSampleWithValue<Type>> allSamples) {
		List<WeightedSampleWithValue<Type>> reWeightedSamples[] = new List[samplers.length];
		for (int i = 0; i < reWeightedSamples.length; i++) {
			reWeightedSamples[i] = new ArrayList<WeightedSampleWithValue<Type>>();
		}
		double sumH[] = new double[samplers.length];
		for (int j = 0; j < sumH.length; j++) {
			sumH[j] = 0;
		}

		for (WeightedSampleWithValue<Type> wswv : allSamples) {
			double logProbH[] = new double[samplers.length];
			double probH[] = new double[samplers.length];

			for (int j = 0; j < samplers.length; j++) {
				logProbH[j] = Math.log(mixtureRatio[j])
						+ samplers[j].logNormalizedProbability(wswv
								.getSampeWithVale().getSample());
				if (Double.isInfinite(logProbH[j])) {
					logProbH[j] = Math.log(Double.MIN_VALUE);
				}
			}

			double logProbSum = LogSumExp.sum(logProbH);
			for (int j = 0; j < samplers.length; j++) {
				probH[j] = Math.exp(logProbH[j] - logProbSum);
				if (Double.isNaN(probH[j])) {
					System.out.println("probH NAN");
				}
				double tWeight = probH[j] * wswv.getWeight();
				if (tWeight < 1e-30)
					continue;
				sumH[j] += tWeight;
				reWeightedSamples[j].add(new WeightedSampleWithValue<Type>(
						tWeight, wswv.getSampeWithVale()));
			}
		}

		// fitting sampler
		int selected = rand.nextInt(samplers.length);
		for (int j = 0; j < samplers.length; j++) {
			if (selected != j)
				continue;
			if (reWeightedSamples[j].size() < 1)
				continue;
			samplers[j].fittingUpdate(reWeightedSamples[j]);
		}

		double allSumH = 0;
		for (int j = 0; j < sumH.length; j++) {
			allSumH += sumH[j];
		}
		if (allSumH == 0) {
			System.out.println("no sampes");
			return;
		}
		for (int j = 0; j < sumH.length; j++) {
			mixtureRatio[j] = sumH[j] / allSumH;
			if (Double.isNaN(mixtureRatio[j])) {
				System.out.println("err at mix ratio");
			}
		}
	}

	public List<Type> sampling(int num) {
		int[] sum = new int[samplers.length];
		for (int i = 0; i < sum.length; i++) {
			sum[i] = 0;
		}

		for (int counter = 0; counter < num; counter++) {
			int i = 0;
			double r = rand.nextDouble();
			for (; i < samplers.length - 1; i++) {
				r -= mixtureRatio[i];
				if (r <= 0)
					break;
			}
			sum[i]++;
		}

		List<Type> list = new ArrayList<Type>(num);
		for (int i = 0; i < samplers.length; i++) {
			list.addAll(samplers[i].sampling(sum[i]));
		}
		return list;
	}

	public double logNormalizedProbability(Type s) {
		double[] logP = new double[samplers.length];
		for (int i = 0; i < logP.length; i++) {
			logP[i] = Math.log(mixtureRatio[i])
					+ samplers[i].logNormalizedProbability(s);
		}
		return LogSumExp.sum(logP);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder("");
		for (int i = 0; i < samplers.length; i++) {
			sb.append(mixtureRatio[i] + " : " + samplers[i] + "\n");
		}
		return sb.toString();
	}

	public static void main(String args[]) {
		int dim = 10;
		MixtureSampler<DVector> ms[] = new MixtureSampler[2];
		for (int i = 0; i < 2; i++) {
			int numComponents = 10;
			UMDADiscreteSampler umda[] = new UMDADiscreteSampler[numComponents];
			for (int j = 0; j < numComponents; j++) {
				umda[j] = new UMDADiscreteSampler(dim);
				if (i == 0)
					umda[j].randomInit();
				if (i == 1)
					umda[j].randomInit();
			}
			ms[i] = new MixtureSampler<DVector>(umda);
		}
		SamplerTest<DVector> st = new SamplerTest<DVector>(ms[0], ms[1], 100);
		st.test();
	}

	@Override
	public Sampler<Type> fittingReplace(
			List<WeightedSampleWithValue<Type>> allSamples) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double entropy() {
		// TODO Auto-generated method stub
		return 0;
	}

}
