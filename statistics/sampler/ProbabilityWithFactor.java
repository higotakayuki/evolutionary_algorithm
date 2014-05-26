package statistics.sampler;

public class ProbabilityWithFactor{

	
	private Factor factor;
	private double logZ;
	
	public ProbabilityWithFactor(Factor f,double logZ) {
		this.factor=f;
		this.logZ=logZ;
	}
	
	public double logNormalizedProbability(double energy) {
		return factor.logFactor(energy)-logZ;
	}
	
	public String toString(){
		return "("+factor+","+logZ+")";
	}

}
