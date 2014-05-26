package app.discrete.function;


import java.util.Random;

import optimize.function.CostFunction;

import app.discrete.util.DVector;


public class TestFunction2 extends CostFunction<DVector> {

	private DVector[] target;

	public TestFunction2(int dim, int complexity) {
		super(dim);
		init(complexity);
	}

	@Override
	public double eval(DVector t) {
		int near_i=0;
		double min_dist=dim();
		for(int i=0;i<target.length;i++){
			double temp_dist=dist(t,target[i]);
			if(temp_dist<min_dist){
				min_dist=temp_dist;
				near_i=i;
			}
		}
		return (near_i+1)*min_dist;
	}

	private double dist(DVector v1,DVector v2){
		double sum=0;
		for(int i=0;i<v1.dim();i++){
			sum+=Math.abs(v1.getValue(i)-v2.getValue(i));
		}
		return sum+1;
	}
	
	private void init(int complexity) {
		Random rand = new Random(1234);
		this.target=new DVector[complexity];
		for(int i=0;i<complexity;i++){
			int[] temp=new int[super.dim()];
			for(int j=0;j<temp.length;j++){
				temp[j]=rand.nextInt(2);
			}
			target[i]=new DVector(temp);
		}
	}

}
