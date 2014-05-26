package statistics.sampler;

import java.util.ArrayList;
import java.util.List;

import optimize.util.SampleWithValue;
import optimize.util.WeightedSampleWithValue;


public class SamplerTest<Type> {
	private Sampler<Type> supervisor, learner;

	private int sample_size;

	public SamplerTest(Sampler<Type> supervisor, Sampler<Type> learner,
			int sample_size) {
		this.supervisor = supervisor;
		this.learner = learner;
		this.sample_size = sample_size;
	}

	public void test() {
		ArrayList<WeightedSampleWithValue<Type>> list = new ArrayList<WeightedSampleWithValue<Type>>();
		List<Type> samples = supervisor.sampling(sample_size);
		double super_sum=0;
		for (Type sample : samples) {
			super_sum+=supervisor.logNormalizedProbability(sample);
		}
		for (int i = 0; i < 10; i++) {
			System.out.println(supervisor);
			System.out.println(learner);
//			log likelihood
			double sum=0;
			for (Type sample : samples) {
				sum+=learner.logNormalizedProbability(sample);
			}
			System.out.println( i+":"+ -super_sum +"<"+ -sum+"\n");
			
			for (Type sample : samples) {
				WeightedSampleWithValue<Type> wswv = new WeightedSampleWithValue<Type>(
						1, new SampleWithValue<Type>(sample, 0));
				list.add(wswv);
			}
			learner.fittingUpdate(list);
			
		}
	}
}
