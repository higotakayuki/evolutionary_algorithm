package statistics.sampler;

public class TruncationFactor implements Factor{
	
	private double threshold;
	
	public TruncationFactor(){
		threshold=Double.MAX_VALUE;
	}
	
	public TruncationFactor(double t){
		this.threshold=t;
	}
	
	public double logFactor(double energy) {
		if(energy<=threshold) return 0;
		else return Double.NEGATIVE_INFINITY;
	}

	public double getThreshold() {
		return threshold;
	}

	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}
	
	public String toString(){
		return ""+threshold;
	}
}
