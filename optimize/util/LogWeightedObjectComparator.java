package optimize.util;

import java.util.Comparator;

public class LogWeightedObjectComparator implements Comparator<LogWeighted>{

	public int compare(LogWeighted o1, LogWeighted o2) {
		if(o1.getLogWeight()>o2.getLogWeight())return 1;
		if(o1.getLogWeight()<o2.getLogWeight())return -1;
		return 0;
	}

}
