package optimize.util;

public class LogWeighted<Type> {
	private Type object;
	private double logWeight;
	
	public LogWeighted(double logW,Type t){
		this.object=t;
		this.logWeight=logW;
	}
	
	public Type getObject(){
		return object;
	}

	public double getLogWeight() {
		return logWeight;
	}
	
	public double getExpLogWeight(){
		return Math.exp(logWeight);
	}

	public void setLogWeight(double logWeight) {
		this.logWeight = logWeight;
	}
	
	public String toString(){
		return logWeight+"("+getExpLogWeight()+")"+","+object;
	}
}
