package app.continuous.function;

import optimize.function.CostFunction;
import app.continuous.util.CVector;

public class Sphere extends CostFunction<CVector>{

	public Sphere(int dim) {
		super(dim);
	}
	
	@Override
	public double eval(CVector t) {
		double sum=0;
		for(int i=0;i<super.dim();i++){
			sum+=t.getValue(i)*t.getValue(i);
		}
		sum=Math.sqrt(sum);
		return sum;
	}

}
