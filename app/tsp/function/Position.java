package app.tsp.function;


public class Position{
	public double x;
	public double y;
	
	public Position(double x,double y){
		this.x=x;
		this.y=y;
	}
	
	public double distance(Position p){
		return Math.sqrt((p.x-x)*(p.x-x)+(p.y-y)*(p.y-y));
	}
}
