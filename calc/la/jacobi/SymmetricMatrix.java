package calc.la.jacobi;

import java.util.Formatter;

import calc.la.Matrix;

public class SymmetricMatrix{

	private double[] element;
	private int dim;
	
	SymmetricMatrix(Matrix m){
		this(m.getRow());
		for(int i=0;i<dim;i++){
			for(int j=0;j<i+1;j++){
				this.setValue(i, j, m.getValue(i, j));
			}
		}
	}
	
	public SymmetricMatrix(int dim){
		element=new double[dim*(1+dim)/2];
		this.dim=dim;
	}
	
	public final double getValue(int i,int j){
		if(i<j){
			int t=i;
			i=j;
			j=t;
		}
		return element[(i)*(i+1)/2+j];
	}
	
	public final void setValue(int i,int j,double value){
		if(i<j){
			int t=i;
			i=j;
			j=t;
		}
		element[(i)*(i+1)/2+j]=value;
	}

	public final int getDim() {
		return dim;
	}
	
	public SymmetricMatrix clone(){
		SymmetricMatrix ret=new SymmetricMatrix(this.dim);
		//ret.element=this.element.clone();
		System.arraycopy(this.element, 0, ret.element, 0, this.element.length);
		return ret;
	}
	
	public String toString() {
		StringBuilder buf = new StringBuilder();
		Formatter formatter=new Formatter(buf);
		for (int i = 0; i < dim; i++) {
			for (int j = 0; j < dim; j++) {
				formatter.format(" % 6.3f ", getValue(i,j));
			}
			buf.append("\n");
		}
		return buf.toString();
	}
	
	public Matrix toMatrix(){
		double[][] ele=new double[dim][];
		for(int i=0;i<dim;i++){
			ele[i]=new double[dim];
			for(int j=0;j<dim;j++){
				ele[i][j]=getValue(i,j);
			}
		}
		return new Matrix(ele);
	}
}
