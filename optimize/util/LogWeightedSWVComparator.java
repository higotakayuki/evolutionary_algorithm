package optimize.util;

import java.util.Comparator;

public class LogWeightedSWVComparator<T>  implements
Comparator<LogWeighted<SampleWithValue<T>>> {

	public int compare(LogWeighted<SampleWithValue<T>> o1,
			LogWeighted<SampleWithValue<T>> o2) {
		if (o1.getObject().getValue() > o2.getObject().getValue())
			return 1;
		if (o1.getObject().getValue() < o2.getObject().getValue())
			return -1;
		return 0;
	}
}
