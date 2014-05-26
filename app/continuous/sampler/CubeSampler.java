package app.continuous.sampler;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import optimize.util.WeightedSampleWithValue;
import statistics.sampler.Sampler;
import app.continuous.util.CVector;

public class CubeSampler implements Sampler<CVector> {

	private int dim;

	private double minx;

	private double maxx;

	private static Random rand = new Random();

	public CubeSampler(int dim, double minx, double maxx) {
		this.dim = dim;
		this.minx = minx;
		this.maxx = maxx;
	}

	public void fittingUpdate(List<WeightedSampleWithValue<CVector>> allSamples) {
		// TODO Auto-generated method stub

	}

	public List<CVector> sampling(int num) {
		ArrayList<CVector> list = new ArrayList<CVector>(num);
		for (int j = 0; j < num; j++) {
			double[] e = new double[dim];
			for (int i = 0; i < dim; i++) {
				e[i] = rand.nextDouble() * (maxx - minx) + minx;
			}
			list.add(new CVector(e));
		}
		return list;
	}

	public double logNormalizedProbability(CVector s) {
		double logP=-dim*Math.log(maxx-minx);
		return logP;
	}

	@Override
	public Sampler<CVector> fittingReplace(
			List<WeightedSampleWithValue<CVector>> allSamples) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double entropy() {
		// TODO Auto-generated method stub
		return 0;
	}

}
