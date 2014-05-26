package optimize.util;


import java.util.Comparator;


public class WeightedSampleWithValue_ValueComparator implements Comparator<WeightedSampleWithValue>{

	public int compare(WeightedSampleWithValue o1, WeightedSampleWithValue o2) {
		if(o1.getSampeWithVale().getValue()>o2.getSampeWithVale().getValue())return 1;
		if(o1.getSampeWithVale().getValue()<o2.getSampeWithVale().getValue())return -1;
		return 0;
	}

}
