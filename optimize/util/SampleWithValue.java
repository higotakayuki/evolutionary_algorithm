package optimize.util;

public class SampleWithValue <SampleType>{
	private final SampleType sample;
	private final double value;
	
	public SampleWithValue(SampleType st,double value){
		this.sample=st;
		this.value=value;
	}

	public SampleType getSample() {
		return sample;
	}

	public double getValue() {
		return value;
	}
	
	public String toString(){
		return value+" "+sample;
	}
	
}
