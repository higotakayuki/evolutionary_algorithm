package app.discrete.sampler;


import java.util.List;

import app.discrete.util.DVector;

import optimize.util.WeightedSampleWithValue;



public class DiscreteUniformSampler extends UMDADiscreteSampler{

	public DiscreteUniformSampler(int dim,int range) {
		super(dim);
	}

	@Override
	public void fittingUpdate(List<WeightedSampleWithValue<DVector>> allSamples) {
		// do nothing
	}
	
	
	
}