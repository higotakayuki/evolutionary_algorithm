package app.continuous.sampler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sun.corba.se.spi.legacy.connection.GetEndPointInfoAgainException;

import static java.lang.Math.log;

import app.continuous.util.CVector;

import optimize.util.WeightedSampleWithValue;

import statistics.sampler.Sampler;
import calc.la.Matrix;
import calc.la.RowVector;
import calc.la.Vector;
import calc.stat.Gaussian;

public class GaussianSampler implements Sampler<CVector> {

	private final int dim;

	private Gaussian gauss = null;

	private double learningRate = 0.5;

	public GaussianSampler(int dim, Gaussian gauss, double learningRate) {
		this.dim = dim;
		this.gauss = gauss;
		this.learningRate = learningRate;
	}

	public void fittingUpdate(List<WeightedSampleWithValue<CVector>> allSamples) {
		this.gauss = calcWeightedGaussian(allSamples, learningRate, gauss
				.getMean(),gauss.getCovariance());
	}

	public List<CVector> sampling(int num) {
		ArrayList<CVector> list = new ArrayList<CVector>(num);
		for (int i = 0; i < num; i++)
			list.add(sampling());
		return list;
	}

	private CVector sampling() {
		double[] e = this.gauss.generateSample().getElement();
		return new CVector(e);
	}

	public double logNormalizedProbability(CVector s) {
		return gauss.logNormalizedProbability(s.getLA_Vector());
	}

	public static Gaussian calcGaussian(List<CVector> list) {
		// change
		List<Vector> tlist = new ArrayList<Vector>(list.size());
		for (CVector cv : list) {
			tlist.add(cv.getLA_Vector());
		}
		// mean
		Vector avg = null;
		{
			for (Vector t : tlist) {
				if (avg == null)
					avg = t;
				else
					avg = avg.plus(t);
			}
			avg = avg.multiply(1d / tlist.size());
		}
		// covariance
		RowVector[] samples = new RowVector[tlist.size()];

		{
			int i = 0;
			for (Vector t : tlist) {
				samples[i++] = new RowVector(t.minus(avg));
			}
		}

		Matrix m = new Matrix(samples);
		Matrix cov = Matrix.operate(m.transpose(), m);
		// cov = cov.multiply(1d / (neighbors.size() - 1));
		cov = cov.multiply(1d / (tlist.size()));

		return new Gaussian(avg, cov);
	}

	public static Gaussian calcWeightedGaussian(
			List<WeightedSampleWithValue<CVector>> list) {
		return calcWeightedGaussian(list, 0, null);
	}

	public static Gaussian calcWeightedGaussian(
			List<WeightedSampleWithValue<CVector>> list, double rate,
			Vector oldAVG) {
		double sum = 0;
		for (WeightedSampleWithValue<CVector> wcv : list) {
			sum += wcv.getWeight();
		}
		// change
		List<Vector> tlist = new ArrayList<Vector>(list.size() + 1);
		List<Double> wlist = new ArrayList<Double>(list.size() + 1);
		for (WeightedSampleWithValue<CVector> wcv : list) {
			tlist.add(wcv.getSampeWithVale().getSample().getLA_Vector());
			// weight is normalized
			wlist.add(wcv.getWeight() / sum);
		}
		double eff = getEffectiveNumberofSamples(wlist);
		// mean
		Vector avg = null;

		{
			Iterator<Double> weightIT = wlist.iterator();
			for (Vector t : tlist) {
				if (avg == null)
					avg = t.multiply(weightIT.next());
				else
					avg = avg.plus(t.multiply(weightIT.next()));
			}
		}

		if (oldAVG != null) {
			avg = avg.multiply(rate).plus(oldAVG.multiply(1 - rate));
		}

		// covariance
		RowVector[] samples = new RowVector[tlist.size()];

		{
			Iterator<Double> it = wlist.iterator();
			int i = 0;
			for (Vector t : tlist) {
				samples[i++] = new RowVector(t.minus(avg).multiply(
						Math.sqrt(it.next())));
			}
		}

		Matrix m = new Matrix(samples);
		Matrix cov = Matrix.operate(m.transpose(), m);
		// for stable computation of determinant
		if (eff > 2) cov = cov.multiply(eff / (eff - 1));
		cov = Matrix.plus(cov, Matrix.UnitMatrix(cov.getRow(), 1e-13));
		// cov = Matrix.UnitMatrix(cov.getRow(), 1e-20);
		//
		return new Gaussian(avg, cov);
	}

	public static Gaussian calcWeightedGaussian(
			List<WeightedSampleWithValue<CVector>> list, double rate,
			Vector oldAVG, Matrix oldCov) {
		double sum = 0;
		for (WeightedSampleWithValue<CVector> wcv : list) {
			sum += wcv.getWeight();
		}
		// change
		List<Vector> tlist = new ArrayList<Vector>(list.size() + 1);
		List<Double> wlist = new ArrayList<Double>(list.size() + 1);
		for (WeightedSampleWithValue<CVector> wcv : list) {
			tlist.add(wcv.getSampeWithVale().getSample().getLA_Vector());
			// weight is normalized
			wlist.add(wcv.getWeight() / sum);
		}
		double eff = getEffectiveNumberofSamples(wlist);
		// mean
		Vector avg = null;

		{
			Iterator<Double> weightIT = wlist.iterator();
			for (Vector t : tlist) {
				if (avg == null)
					avg = t.multiply(weightIT.next());
				else if (false && 2 < eff) {
					avg = avg.plus(t
							.multiply(weightIT.next() * eff / (eff - 1)));
				} else {
					avg = avg.plus(t.multiply(weightIT.next()));
				}
			}
		}

		if (oldAVG != null) {
			avg = avg.multiply(rate).plus(oldAVG.multiply(1 - rate));
		}

		// covariance
		RowVector[] samples = new RowVector[tlist.size()];

		{
			Iterator<Double> it = wlist.iterator();
			int i = 0;
			for (Vector t : tlist) {
				samples[i++] = new RowVector(t.minus(avg).multiply(
						Math.sqrt(it.next())));
			}
		}

		Matrix m = new Matrix(samples);
		Matrix cov = Matrix.operate(m.transpose(), m);
		if (eff > 2) cov = cov.multiply(eff / (eff - 1));
		// for stable computation of determinant
		cov = Matrix.plus(cov, Matrix.UnitMatrix(cov.getRow(), 1e-10));
//		cov = Matrix.plus(cov, Matrix.UnitMatrix(cov.getRow(), 1e-20));
//		 cov = Matrix.UnitMatrix(cov.getRow(), 1e-20);
		//
		cov = Matrix.plus(cov.multiply(rate), oldCov.multiply(1 - rate));
		return new Gaussian(avg, cov);
	}

	private static double getEffectiveNumberofSamples(List<Double> wlist) {
		double sum = 0;
		for (double w : wlist) {
			sum += w;
		}
		double ent = 0;
		for (double w : wlist) {
			double prob = w / sum;
			ent -= prob * log(prob);
		}
		return Math.exp(ent);
	}

	@Override
	public String toString() {
		return this.gauss.toString();
	}

	@Override
	public Sampler<CVector> fittingReplace(
			List<WeightedSampleWithValue<CVector>> allSamples) {
		// TODO Auto-generated method stub
		return null;
	}

	
	@Override
	public double entropy() {
		return Double.NaN;
	}

}
