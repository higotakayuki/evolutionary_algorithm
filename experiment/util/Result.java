package experiment.util;

public class Result {
	public double best;

	public double count;
	
	public long time;
	
	public double success=0;

	public static Result average(Result[] result) {
		double[] best = new double[result.length];
		for (int i = 0; i < best.length; i++) {
			best[i] = result[i].best;
		}
		double[] count = new double[result.length];
		for (int i = 0; i < count.length; i++) {
			count[i] = result[i].count;
		}
		double[] time = new double[result.length];
		for (int i = 0; i < count.length; i++) {
			time[i] = result[i].time;
		}
		
		double[] success = new double[result.length];
		for (int i = 0; i < count.length; i++) {
			success[i] = result[i].success;
		}
		
		Result ret = new Result();
		ret.best = average(best);
		ret.count = average(count);
		ret.time=(long)average(time);
		ret.success=average(success);
		return ret;
	}

	public static Result std(Result[] result) {
		Result var = variance(result);
		var.best = Math.sqrt(var.best);
		var.count = Math.sqrt(var.count);
		return var;
	}

	private static Result variance(Result[] result) {
		double[] best = new double[result.length];
		for (int i = 0; i < best.length; i++) {
			best[i] = result[i].best;
		}
		double[] count = new double[result.length];
		for (int i = 0; i < count.length; i++) {
			count[i] = result[i].count;
		}
		Result ret = new Result();
		ret.best = variance(best);
		ret.count = variance(count);
		return ret;
	}
	
	private static double average(double[] value) {
		double sum = 0;
		for (double d : value) {
			sum += d;
		}
		return sum / value.length;
	}
	
	private static double variance(double[] value) {
		double avg = average(value);
		double sum = 0;
		for (double d : value) {
			double dist = (d - avg);
			sum += dist * dist;
		}
		return (sum / value.length);
	}

	@Override
	public String toString() {
		return ""+best+" , "+count;
	}
	
	
}
