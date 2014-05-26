package calc.la;


public class ProjectionIntoKernel {
	
	private Matrix matrix;
	
	private Matrix squaredMatrix;
	
	public ProjectionIntoKernel(Matrix matrix){
		this.matrix=matrix;
		this.squaredMatrix=Matrix.operate(matrix,matrix.transpose());
	}
	
	public ColumnVector operateFromLeft(ColumnVector x) {
		ColumnVector temp=matrix.operateFromLeft(x);
		GaussElimination ge=new GaussElimination(squaredMatrix,temp);
		ColumnVector ans=ge.getSolution();
		ans=matrix.transpose().operateFromLeft(ans);
		return x.minus(ans);
	}
	
	
	public static void main(String args[]){
		double[][] element = { { 1, 0, 0, 0 }, { 0, 1, 0, 0 } };
		double[] vec = { 44, 104, 184, 261 };
		ColumnVector vector=new ColumnVector(vec);
		Matrix m = new Matrix(element);
		
		ProjectionIntoKernel pik=new ProjectionIntoKernel(m);
		System.out.println(pik.operateFromLeft(vector));
		System.out.println(m.operateFromLeft(pik.operateFromLeft(vector)));
		
	}
}
