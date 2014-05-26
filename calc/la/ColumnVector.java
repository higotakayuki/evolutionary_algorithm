package calc.la;

public class ColumnVector extends Vector{

	public ColumnVector(double[] element) {
		super(element);
	}
	
	public ColumnVector(Vector v) {
		super(v);
	}

	public ColumnVector(int dim) {
		super(dim);
	}

	public ColumnVector minus(ColumnVector v) {
		// TODO Auto-generated method stub
		return new ColumnVector(super.minus(v));
	}

	@Override
	public ColumnVector multiply(double k) {
		// TODO Auto-generated method stub
		return new ColumnVector(super.multiply(k));
	}

	public ColumnVector plus(ColumnVector v) {
		// TODO Auto-generated method stub
		return new ColumnVector(super.plus(v));
	}

}
