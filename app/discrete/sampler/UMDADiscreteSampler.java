package app.discrete.sampler;

import java.util.ArrayList;

import java.util.Formatter;
import java.util.List;
import java.util.Random;

import optimize.util.WeightedSampleWithValue;
import statistics.sampler.Sampler;
import app.discrete.util.DVector;
import static java.lang.Math.log;

public class UMDADiscreteSampler implements Sampler<DVector> {

	public static final Random rand = new Random();

	private double[] prob; // probability of outputting 0

	private double alpha;

	public UMDADiscreteSampler(int dim) {
		this(dim, 1);
	}

	public UMDADiscreteSampler(int dim, double alpha) {
		this.alpha = alpha;
		prob = new double[dim];
		init();
	}

	public int dim() {
		return prob.length;
	}

	public void init() {
		for (int i = 0; i < prob.length; i++) {
			prob[i] = 0.5;
		}
	}

	public void randomInit() {
		for (int i = 0; i < prob.length; i++) {
			prob[i] = rand.nextDouble();
		}
	}

	public void fittingUpdate(
			List<WeightedSampleWithValue<DVector>> allSamples) {
		if (allSamples.size() == 0) {
			System.out.println(this.getClass());
			System.out.println("no given samples");
			System.exit(-1);
		}
		double[] accum_0 = new double[dim()];
		double[] accum_1 = new double[dim()];
		// int[] counter0=new int[dim()];
		// int[] counter1=new int[dim()];

		for (int i = 0; i < accum_0.length; i++) {
			accum_0[i] = 0;
			accum_1[i] = 0;
			// counter0[i]=0;
			// counter1[i]=0;
		}
		for (WeightedSampleWithValue<DVector> wswv : allSamples) {
			DVector v = wswv.getSampeWithVale().getSample();
			for (int i = 0; i < v.dim(); i++) {
				if (v.getValue(i) == 0) {
					accum_0[i] += wswv.getWeight();
					// counter0[i]++;
				} else {
					accum_1[i] += wswv.getWeight();
					// counter1[i]++;
				}
			}
		}
		for (int i = 0; i < accum_0.length; i++) {
			// accum_0[i] += 1e-10;
			// accum_1[i] += 1e-10;
			// accum_0[i]/=counter0[i];
			// accum_1[i]/=counter1[i];
			accum_0[i] /= (accum_0[i] + accum_1[i]);
			if (Double.isNaN(accum_0[i])) {
				System.out.println("prob nan:" + accum_0[i] + ","
						+ accum_1[i]);
				// System.exit(0);
			}
		}

		for (int i = 0; i < prob.length; i++) {
			prob[i] = (1 - alpha) * prob[i] + alpha * accum_0[i];
		}

		// System.out.println(this);
	}

	@Override
	public Sampler<DVector> fittingReplace(
			List<WeightedSampleWithValue<DVector>> allSamples) {
		Sampler<DVector> sampler = this.clone();
		sampler.fittingUpdate(allSamples);
		return sampler;
	}

	public List<DVector> sampling(int num) {
		ArrayList<DVector> list = new ArrayList<DVector>(num);
		for (int i = 0; i < num; i++) {
			list.add(sampling());
		}
		return list;
	}

	public DVector sampling() {
		int[] ans = new int[dim()];
		for (int i = 0; i < prob.length; i++) {
			double randomNum = rand.nextDouble();
			if (randomNum < prob[i]) {
				ans[i] = 0;
			} else {
				ans[i] = 1;
			}
		}
		return new DVector(ans);
	}

	public double normalizedProbability(DVector s) {
		double weight = 1;
		for (int i = 0; i < s.dim(); i++) {
			if (s.getValue(i) == 0) {
				weight *= prob[i];
			} else {
				weight *= (1 - prob[i]);
			}
		}
		return weight;
	}

	public double logNormalizedProbability(DVector s) {
		double weight = 0;
		for (int i = 0; i < s.dim(); i++) {
			if (s.getValue(i) == 0) {
				weight += Math.log(prob[i]);
			} else {
				weight += Math.log(1 - prob[i]);
			}
			if (false && Double.isInfinite(weight)) {
				System.out.println("umda_lognormprob(" + i + "):"
						+ weight);
				System.out.println(prob[i] + "->" + s.getValue(i));
				return weight;
				// System.exit(0);
			}
		}
		return weight;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		Formatter formatter = new Formatter(sb);

		double average = 0;
		for (double d : prob) {
			average += d;
		}
		average /= prob.length;
		double variance = 0;
		{
			for (double d : prob) {
				variance += d * d;
			}
			variance /= prob.length;
			variance -= average * average;
		}
		formatter.format(" (% 6.3f,% 6.3f): ", average, variance);
		for (int i = 0; i < prob.length && i < 10; i++) {
			formatter.format(" % 6.3f ", prob[i]);
		}
		return sb.toString();

	}

	@Override
	public UMDADiscreteSampler clone() {
		UMDADiscreteSampler sampler;
		try {
			sampler = (UMDADiscreteSampler) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
		double[] newArray = new double[prob.length];
		System.arraycopy(this.prob, 0, newArray, 0, this.prob.length);
		sampler.prob = newArray;
		return sampler;
	}

	public double entropy() {
		double ent = 0;
		for (int i = 0; i < prob.length; i++) {
			double u = prob[i];
			if (0 < u) ent -= u * log(u);
			if (0 < 1 - u) ent -= (1 - u) * log(1 - u);
		}
		return ent;
	}

}
