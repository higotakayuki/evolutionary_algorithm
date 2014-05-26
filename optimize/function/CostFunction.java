package optimize.function;

public abstract class CostFunction<SampleType> {
	private int dim;
	
	public CostFunction(int dim) {
		this.dim = dim;
	}

	public int dim() {
		return dim;
	}
	
	public double getMinimum(){
		return Double.NEGATIVE_INFINITY;
	}
	
	public abstract double eval(SampleType t);
}
