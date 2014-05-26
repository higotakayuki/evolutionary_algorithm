package optimize.util;

import java.util.Comparator;

public class WeightedSampleWithValueComparatorByValue implements Comparator<WeightedSampleWithValue>{

	public int compare(WeightedSampleWithValue arg0, WeightedSampleWithValue arg1) {
		if(arg0.getSampeWithVale().getValue()>arg1.getSampeWithVale().getValue()) return 1;
		if(arg0.getSampeWithVale().getValue()<arg1.getSampeWithVale().getValue()) return -1;
		return 0;
	}

}
