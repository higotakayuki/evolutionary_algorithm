package calc.la;

public class Vector {
	
	private double element[];
	
	public Vector(int dim){
		element=new double[dim];
		for(int i=0;i<dim;i++){
			element[i]=0;
		}
	}
	public Vector(double[] element){
		this.element=element.clone();
	}
	public Vector(Vector v){
		this.element=v.getElement();
	}
	
	
	public double[] getElement() {
		return element.clone();
	}
	public double getValue(int i){
		return element[i];
	}
	public int dim(){
		return element.length;
	}
	
	public double dotProduct(Vector v){
		double sum=0;
		for(int i=0;i<element.length;i++){
			sum+=this.element[i]*v.element[i];
		}
		return sum;
	}
	
	public double squaredNorm(){
		return this.dotProduct(this);
	}
	
	public double norm(){
		return Math.sqrt(squaredNorm());
	}
	
	public Vector multiply(double k){
		double[] ele=element.clone();
		for(int i=0;i<ele.length;i++){
			ele[i]*=k;
		}
		return new Vector(ele);
	}
	
	public Vector multiplyEach(Vector c){
		double[] ele=element.clone();
		for(int i=0;i<ele.length;i++){
			ele[i]*=c.element[i];
		}
		return new Vector(ele);
	}
	
	public Vector plus(Vector v){
		double[] ele=new double[element.length];
		for(int i=0;i<ele.length;i++){
			ele[i]=element[i]+v.element[i];
		}
		return new Vector(ele);
	}
	
	public Vector minus(Vector v){
		double[] ele=new double[element.length];
		for(int i=0;i<ele.length;i++){
			ele[i]=element[i]-v.element[i];
		}
		return new Vector(ele);
	}
	
	public void assign(Vector v){
		this.element=v.element.clone();
	}
	
	public Object clone(){
		return new Vector(this.element.clone());
	}
	
	public String toString(){
		StringBuffer buf=new StringBuffer();
		buf.append("(");
		for(int i=0;i<element.length;i++){
			buf.append(element[i]);
			if(i!=element.length-1)buf.append(" , ");
		}
		buf.append(")");
		return buf.toString();
	}
}
