package optimize.util;

import java.util.Comparator;

public class SampleWithValueComparator implements Comparator<SampleWithValue>{

	public int compare(SampleWithValue arg0, SampleWithValue arg1) {
		if(arg0.getValue()>arg1.getValue()) return 1;
		if(arg0.getValue()<arg1.getValue()) return -1;
		return 0;
	}

}
