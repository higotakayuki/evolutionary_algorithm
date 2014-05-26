package optimize.util;


import java.util.Comparator;


public class WeightedObjectComparator implements Comparator<Weighted>{

	public int compare(Weighted o1, Weighted o2) {
		if(o1.getWeight()>o2.getWeight())return 1;
		if(o1.getWeight()<o2.getWeight())return -1;
		return 0;
	}

}
