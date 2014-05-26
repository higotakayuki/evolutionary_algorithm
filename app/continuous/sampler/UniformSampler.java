package app.continuous.sampler;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import optimize.util.WeightedSampleWithValue;

import app.continuous.util.CVector;

import statistics.sampler.Sampler;

public class UniformSampler implements Sampler<CVector>{

	private int dim;
	private double min_x,max_x;
	
	private static final Random rand=new Random();
	
	public UniformSampler(int dim,double min_x,double max_x){
		this.min_x=min_x;
		this.max_x=max_x;
		this.dim=dim;
	}


	public List<CVector> sampling(int num) {
		ArrayList<CVector> list=new ArrayList<CVector>(num);
		for(int i=0;i<num;i++)list.add(sampling());
		return list;
	}
	
	private CVector sampling(){
		double[] e=new double[dim];
		for(int i=0;i<dim;i++){
			e[i]=min_x+(max_x-min_x)*rand.nextDouble();
		}
		
		return new CVector(e);
	}

	public double logNormalizedProbability(CVector v) {
		// TODO Auto-generated method stub
		return -dim*Math.log(max_x-min_x);
	}

	@Override
	public void fittingUpdate(
			List<WeightedSampleWithValue<CVector>> allSamples) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Sampler<CVector> fittingReplace(
			List<WeightedSampleWithValue<CVector>> allSamples) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public double entropy() {
		
		return 0;
	}

}
