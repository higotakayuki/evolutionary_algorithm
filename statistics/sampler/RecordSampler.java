package statistics.sampler;

import java.io.PrintStream;
import java.util.List;

import app.discrete.util.DVector;

import optimize.function.CostFunction;
import optimize.util.WeightedSampleWithValue;

public class RecordSampler<SampleType> implements Sampler<SampleType> {

	public boolean PRINT = true;

	protected PrintStream out = null;

	private long counter = 0;

	private Sampler<SampleType> samp;

	private CostFunction<SampleType> cf;

	public RecordSampler(Sampler<SampleType> samp, PrintStream ps,
			CostFunction<SampleType> cf) {
		this.out = ps;
		this.samp = samp;
		this.cf = cf;
	}

	// public int dim() {
	// return samp.dim();
	// }

	public void fittingUpdate(List<WeightedSampleWithValue<SampleType>> allSamples) {
		samp.fittingUpdate(allSamples);
	}

	public List<SampleType> sampling(int num) {
		List<SampleType> list = samp.sampling(num);
		if (PRINT) {
			for (SampleType st : list) {
				counter += +1;
				if (counter % 7 == 0) {
					out.println(counter + " " + cf.eval(st) + " " + st);
					if (false) {
						DVector dv = (DVector) st;
						for (int i = 0; i < dv.dim(); i++) {
							out.print(dv.getValue(i));
							if ((i + 1) % 20 == 0) out.println();
						}
						out.println();
						out.println();
					}
					out.flush();
				}
			}
		}
		return list;
	}

	public double logNormalizedProbability(SampleType s) {
		return samp.logNormalizedProbability(s);
	}

	public String toString() {
		return samp.toString();
	}

	@Override
	public Sampler<SampleType> fittingReplace(
			List<WeightedSampleWithValue<SampleType>> allSamples) {
		return samp.fittingReplace(allSamples);
	}

	@Override
	public double entropy() {
		
		return samp.entropy();
	}
}
