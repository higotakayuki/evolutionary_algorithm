package app.continuous.sampler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import optimize.util.WeightedSampleWithValue;
import statistics.sampler.Sampler;
import app.continuous.util.CVector;
import calc.la.Vector;

public class UMGaussianSampler implements Sampler<CVector> {

	private final int dim;

	private UGaussian[] gauss = null;

	private double learningRate;

	private UMGaussianSampler(int dim) {
		this.dim = dim;
	}

	public UMGaussianSampler(int dim, double minx, double maxx,
			double learningRate) {
		this.dim = dim;
		this.gauss = new UGaussian[dim];
		this.learningRate = learningRate;
		for (int i = 0; i < dim; i++) {
			gauss[i] =
					new UGaussian((minx + maxx) / 2, (maxx - minx)
							* (maxx - minx) / 12);
		}
	}

	public void fittingUpdate(
			List<WeightedSampleWithValue<CVector>> allSamples) {
		Vector oldAVG = null;
		Vector oldVar = null;
		{
			double[] e = new double[dim];

			for (int i = 0; i < dim; i++) {
				e[i] = gauss[i].getMean();
				oldAVG = new Vector(e);
			}
			for (int i = 0; i < dim; i++) {
				e[i] = gauss[i].getVariance();
				oldVar = new Vector(e);
			}
		}
		double sum = 0;
		for (WeightedSampleWithValue<CVector> wcv : allSamples) {
			sum += wcv.getWeight();
		}
		// change
		List<Vector> tlist =
				new ArrayList<Vector>(allSamples.size() + 1);
		List<Double> wlist =
				new ArrayList<Double>(allSamples.size() + 1);
		for (WeightedSampleWithValue<CVector> wcv : allSamples) {
			tlist.add(wcv.getSampeWithVale().getSample()
					.getLA_Vector());
			// weight is normalized
			wlist.add(wcv.getWeight() / sum);
		}

		// mean
		Vector avg = null;
		{
			Iterator<Double> it = wlist.iterator();
			for (Vector t : tlist) {
				if (avg == null) avg = t.multiply(it.next());
				else avg = avg.plus(t.multiply(it.next()));
			}
		}

		if (oldAVG != null) {
			avg =
					avg.multiply(learningRate).plus(
							oldAVG.multiply(1 - learningRate));
		}

		// var
		Vector var = null;
		{
			Iterator<Double> it = wlist.iterator();
			for (Vector t : tlist) {
				Vector dist = t.minus(avg);
				double[] e = new double[dim];
				for (int i = 0; i < dim; i++) {
					double temp = dist.getValue(i);
					e[i] = temp * temp;
				}
				Vector dist2 = new Vector(e);
				if (var == null) var = dist2.multiply(it.next());
				else var = var.plus(dist2.multiply(it.next()));
			}
			if (oldVar != null) {
				var =
						var.multiply(learningRate).plus(
								oldVar.multiply(1 - learningRate));
			}
		}

		// System.out.println(avg);
		// double tsum=0;
		// for(double d:wlist){
		// tsum+=d;
		// }
		// System.out.println(tsum);
		// int k=0;

		for (int i = 0; i < dim; i++) {
			if (Double.isNaN(avg.getValue(i))) {
				System.out.println("avg");
			}
			gauss[i] =
					new UGaussian(avg.getValue(i), var.getValue(i));
		}
	}

	@Override
	public Sampler<CVector> fittingReplace(
			List<WeightedSampleWithValue<CVector>> allSamples) {
		Vector oldAVG = null;
		Vector oldVar = null;
		{
			double[] e = new double[dim];

			for (int i = 0; i < dim; i++) {
				e[i] = gauss[i].getMean();
				oldAVG = new Vector(e);
			}
			for (int i = 0; i < dim; i++) {
				e[i] = gauss[i].getVariance();
				oldVar = new Vector(e);
			}
		}
		double sum = 0;
		for (WeightedSampleWithValue<CVector> wcv : allSamples) {
			sum += wcv.getWeight();
		}
		// change
		List<Vector> tlist =
				new ArrayList<Vector>(allSamples.size() + 1);
		List<Double> wlist =
				new ArrayList<Double>(allSamples.size() + 1);
		for (WeightedSampleWithValue<CVector> wcv : allSamples) {
			tlist.add(wcv.getSampeWithVale().getSample()
					.getLA_Vector());
			// weight is normalized
			wlist.add(wcv.getWeight() / sum);
		}

		// mean
		Vector avg = null;
		{
			Iterator<Double> it = wlist.iterator();
			for (Vector t : tlist) {
				if (avg == null) avg = t.multiply(it.next());
				else avg = avg.plus(t.multiply(it.next()));
			}
		}
		avg =
				avg.multiply(learningRate).plus(
						oldAVG.multiply(1 - learningRate));

		// var
		Vector var = null;
		{
			Iterator<Double> it = wlist.iterator();
			for (Vector t : tlist) {
				Vector dist = t.minus(avg);
				Vector dist2 = dist.multiplyEach(dist);
				if (var == null) var = dist2.multiply(it.next());
				else var = var.plus(dist2.multiply(it.next()));
			}
		}
		var =
				var.multiply(learningRate).plus(
						oldVar.multiply(1 - learningRate));

		UMGaussianSampler newGauss = new UMGaussianSampler(this.dim);
		newGauss.learningRate=this.learningRate;
		newGauss.gauss = new UGaussian[this.dim];
		

		for (int i = 0; i < dim; i++) {
			if (Double.isNaN(avg.getValue(i))) {
				System.out.println("avg");
			}
			newGauss.gauss[i] =
					new UGaussian(avg.getValue(i), var.getValue(i));
		}
		return newGauss;
	}

	public List<CVector> sampling(int num) {
		ArrayList<CVector> list = new ArrayList<CVector>(num);
		for (int i = 0; i < num; i++) {
			CVector cv = sampling();
			list.add(cv);
		}
		return list;
	}

	private CVector sampling() {
		double[] e = new double[dim];
		for (int i = 0; i < dim; i++) {
			e[i] = gauss[i].generateSample();
		}
		return new CVector(e);
	}

	public double logNormalizedProbability(CVector s) {
		double logSum = 0;
		for (int i = 0; i < dim; i++) {
			logSum +=
					gauss[i].logNormalizedProbability(s.getValue(i));
		}
		return logSum;
	}

	@Override
	public String toString() {
		return "" + this.getClass().getName();
	}

	@Override
	public double entropy() {
		double ent = 0;
		for (int i = 0; i < gauss.length; i++) {
			ent += gauss[i].entropy();
		}
		return ent;
	}

}
