/*
 * Created on 2004/10/19
 */
package calc.gui;


/**
 * @author Takayuki Higo
 */
public class IntVector {
	private int element[];
	public IntVector(int dim){
		element=new int[dim];
		for(int i=0;i<dim;i++){
			element[i]=0;
		}
	}
	public IntVector(int[] element){
		this.element=element.clone();
	}
	public String toString(){
		StringBuffer buf=new StringBuffer();
		buf.append("(");
		for(int i=0;i<element.length;i++){
			buf.append(element[i]);
			if(i!=element.length-1)buf.append(" , ");
		}
		buf.append(")");
		return buf.toString();
	}
	public int[] getElement() {
		return element.clone();
	}
	public int getValue(int i){
		return element[i];
	}
	public int getDimension(){
		return element.length;
	}
	
	public void substitute(IntVector v){
		this.element=v.element;
	}
	public Object clone(){
		return new IntVector(this.element.clone());
	}
}
