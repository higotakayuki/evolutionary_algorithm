package app.discrete.sampler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import app.discrete.util.DVector;

import optimize.util.WeightedSampleWithValue;
import optimize.util.WeightedSampleWithValue_ValueComparator;

import statistics.sampler.Sampler;

public class DiscreteBestSampler implements Sampler<DVector> {

	private final int dim;
	private final int stockNum;
	private static Random rand = new Random();

	private List<WeightedSampleWithValue<DVector>> items =
			new ArrayList<WeightedSampleWithValue<DVector>>();

	public DiscreteBestSampler(int dim, int stockNum) {
		this.dim = dim;
		this.stockNum = stockNum;
	}

	public int dim() {
		return dim;
	}

	public void fittingUpdate(
			List<WeightedSampleWithValue<DVector>> allSamples) {
		items.addAll(allSamples);
		Collections.sort(items,
				new WeightedSampleWithValue_ValueComparator());
		List<WeightedSampleWithValue<DVector>> candidate = items;
		items =
				new ArrayList<WeightedSampleWithValue<DVector>>(
						this.stockNum);
		all: for (WeightedSampleWithValue<DVector> cwswv : candidate) {
			if (items.size() == stockNum) break;
			for (WeightedSampleWithValue<DVector> item : items) {
				if (item.getSampeWithVale().getSample()
						.equals(cwswv.getSampeWithVale().getSample())) continue all;
			}
			items.add(cwswv);
		}
		if (stockNum < items.size()) items =
				items.subList(0, stockNum);
	}

	public List<DVector> sampling(int num) {
		List<DVector> list = new ArrayList<DVector>(num);
		for (int i = 0; i < num; i++) {
			list.add(sampling());
		}
		return list;
	}

	private DVector sampling() {
		return items.get(rand.nextInt(items.size()))
				.getSampeWithVale().getSample();
	}

	public double normalizedProbability(DVector s) {
		for (WeightedSampleWithValue<DVector> wswv : items) {
			if (wswv.getSampeWithVale().getSample().equals(s)) return 1d / this.stockNum;
		}

		return 0;

	}

	public double logNormalizedProbability(DVector s) {
		return Math.log(normalizedProbability(s));
	}

	@Override
	public Sampler<DVector> fittingReplace(
			List<WeightedSampleWithValue<DVector>> allSamples) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double entropy() {
		return 1;
	}

}
