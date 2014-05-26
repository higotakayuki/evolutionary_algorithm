package app.discrete.optimize;


import java.util.List;

import app.discrete.sampler.UMDADiscreteSampler;
import app.discrete.util.DVector;

import optimize.function.CostFunction;
import optimize.population.TruncationRTR_IDEA;
import optimize.util.LogWeighted;
import optimize.util.SampleWithValue;

import statistics.sampler.Sampler;

public class UMDATruncationRTR_IDEA extends TruncationRTR_IDEA<DVector> {

	public UMDATruncationRTR_IDEA(CostFunction<DVector> cf, int num_population,
			int num_sample, double cutoff) {
		super(cf, num_population, num_sample, cutoff);
	}

	@Override
	protected LogWeighted<SampleWithValue<DVector>> findClosestSample(LogWeighted<SampleWithValue<DVector>> newSample, List<LogWeighted<SampleWithValue<DVector>>> subset) {
		
		double dist=Double.MAX_VALUE;
		LogWeighted<SampleWithValue<DVector>> ans=null;
		
		DVector d1=newSample.getObject().getSample();
		for(LogWeighted<SampleWithValue<DVector>> tempSample:subset){
			double tempDist=0;
			DVector d2=tempSample.getObject().getSample();	
			for(int i=0;i<d1.dim();i++){
				if(d1.getValue(i)!=d2.getValue(i)){
					tempDist+=1;
				}
			}
			if(tempDist<dist){
				dist=tempDist;
				ans=tempSample;
			}
		}
		return ans;
	}

	@Override
	protected Sampler<DVector> getInitialSampler() {
		return new UMDADiscreteSampler(cf.dim(), 1);
	}

}
