package calc.la;


public class GeneralizedGaussElimination {
	
	private Vector solution=null;
	private Matrix m;
	private Vector y;
	
	public GeneralizedGaussElimination(Matrix m){
		double[][] ele=new double[m.getRow()][];
		double[] vec=new double[m.getRow()];
		for(int i=0;i<m.getRow();i++){
			ele[i]=new double[m.getColumn()-1];
			for(int j=0;j<m.getColumn()-1;j++){
				ele[i][j]=m.getValue(i,j);
			}
			vec[i]=m.getValue(i,m.getColumn()-1);
		}
		this.m=new Matrix(ele);
		this.y=new Vector(vec);
		System.out.println(this.m);
		System.out.println(y);
	}
	
	public GeneralizedGaussElimination(Matrix m,ColumnVector y){
		this.m=m;
		this.y=y;
	}
	
	public Vector getSolution(){
		if(solution==null)this.eliminate();
		return solution;
	}
	
	public Vector getError(){
		return y.minus(this.m.operateFromLeft(new ColumnVector(getSolution())));
	}
	
	public void eliminate(){	
		assert solution==null:"eliminate() called again";
		Matrix t=m.transpose();
		Matrix newMatrix=Matrix.operate(t,m);
		Vector newVector=t.operateFromLeft(new ColumnVector(y));
		GaussElimination ge=new GaussElimination(newMatrix,new ColumnVector(newVector));
		this.solution=ge.getSolution();
	}
	
	public static void main(String[] args) {
		double[][] a={{1,1},{3,2},{4,3}};
		double[] y={1,2,3};
		Matrix m=new Matrix(a);
		Vector v=new Vector(y);
		GeneralizedGaussElimination gge=new GeneralizedGaussElimination(m,new ColumnVector(v));
		Vector ans=gge.getSolution();
		System.out.println(ans);
		System.out.println(m.operateFromLeft(new ColumnVector(ans)));
		
		double[][] b={{1,1,1},{3,2,2},{4,3,3},{5,2,10}};
		gge=new GeneralizedGaussElimination(new Matrix(b));
		System.out.println(gge.getSolution());
		System.out.println(gge.getError());
	}
}
