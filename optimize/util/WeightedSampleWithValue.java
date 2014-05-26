package optimize.util;

public class WeightedSampleWithValue<SampleType> {
	private double weight;

	private final SampleWithValue<SampleType> swv;

	public WeightedSampleWithValue(double weight,
			SampleWithValue<SampleType> swv) {
		this.weight = weight;
		this.swv = swv;
	}

	public SampleWithValue<SampleType> getSampeWithVale(){
		return swv;
	}

	public double getWeight() {
		return weight;
	}
	
	public void setWeight(double w){
		this.weight=w;
	}
	
	public String toString(){
		return this.weight+":"+swv+"\n";
	}
}
