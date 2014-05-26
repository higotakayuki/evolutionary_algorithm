package optimize.util;

public class Weighted<Type> {
	private Type object;
	public double weight;
	
	public Weighted(double w,Type t){
		this.object=t;
		this.weight=w;
	}
	
	public double getWeight(){
		return weight;
	}
	
	public Type getObject(){
		return object;
	}
	
	public String toString(){
		return weight+","+object;
	}
}
