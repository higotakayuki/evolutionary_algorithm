package calc.la;

public class RowVector extends Vector {

	public RowVector(double[] element) {
		super(element);
	}
	
	public RowVector(Vector v) {
		super(v);
	}

	public RowVector(int dim) {
		super(dim);
	}
}
