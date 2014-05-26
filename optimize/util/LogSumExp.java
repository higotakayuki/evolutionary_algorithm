package optimize.util;

import java.util.List;

public class LogSumExp {
	static double log[] = new double[2];

	public static double sum(double log1, double log2) {
		log[0] = log1;
		log[1] = log2;
		return sum(log);
	}

	public static double sum(double[] log) {
		double maxLogP = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < log.length; i++) {
			if (maxLogP < log[i]) maxLogP = log[i];
		}

		double expPartSum = 0;
		for (int i = 0; i < log.length; i++) {
			if (Double.NEGATIVE_INFINITY != log[i]) {
				expPartSum += Math.exp(log[i] - maxLogP);
			}
		}
		return maxLogP + Math.log(expPartSum);
	}

	public static <T> double sum(List<LogWeighted<T>> list) {
		double maxLogP = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < list.size(); i++) {
			if (maxLogP < list.get(i).getLogWeight())
				maxLogP = list.get(i).getLogWeight();
		}

		double expPartSum = 0;
		for (int i = 0; i < list.size(); i++) {
			if (Double.NEGATIVE_INFINITY != list.get(i).getLogWeight()) {
				expPartSum += Math.exp(list.get(i).getLogWeight() - maxLogP);
			}
		}
		return maxLogP + Math.log(expPartSum);
	}

	public static void normalize(double[] log) {
		double logSum = sum(log);
		for (int i = 0; i < log.length; i++) {
			log[i] -= logSum;
		}
	}

	public static <T> void normalize(List<LogWeighted<T>> list) {
		double logSum = sum(list);
		for (LogWeighted<T> lwo : list) {
			lwo.setLogWeight(lwo.getLogWeight() - logSum);
		}
	}
}
