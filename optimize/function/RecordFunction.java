package optimize.function;

import java.io.PrintStream;

public class RecordFunction<Type> extends CostFunction<Type>{

	protected PrintStream out=null;
	
	private long counter=0;
	
	private CostFunction<Type> cf;
	
	public boolean PRINT;
	
	private double bestValue=Double.POSITIVE_INFINITY;
	
	public RecordFunction(CostFunction<Type> cf){
		super(cf.dim());
		this.cf=cf;
		this.out=null;
		this.PRINT=false;
	}
	
	public RecordFunction(CostFunction<Type> cf, PrintStream ps) {
		super(cf.dim());
		this.cf=cf;
		this.out=ps;
		if(ps!=null)this.PRINT=true;
		else this.PRINT=false;
	}

	

	private static double rate=1.1;
	private double upper=1;
	@Override
	public double eval(Type t) {
		counter++;
		double val=cf.eval(t);
//		if(ONOFF && counter%23==0)out.println(counter+" "+val+" "+t);
//		if(PRINT && counter%23==0)out.println(counter+" "+val);
		if(false && PRINT && upper<=counter){
			out.println(counter+" "+val);
			upper=counter*rate;
		}
		
		if(val<bestValue){
			if(PRINT)out.println(counter+" "+val);
			bestValue=val;
		}
		return val;
	}
	
	public long getCount(){
		return counter;
	}
	
	public double getBestValue(){
		return bestValue;
	}
	
	public void close(){
		if(out!=null)
		out.close();
	}

	public CostFunction<Type> getRawFunction(){
		return cf;
	}

	@Override
	public double getMinimum() {
		return cf.getMinimum();
	}
}
