package app.tsp;


import java.util.List;

public class Sequence{

	private List<Integer> list;
	
	public Sequence(List<Integer> list){
		this.list=list;
	}
	
	public int get(int i){
		return list.get(i);
	}
	
	public int size(){
		return list.size();
	}
	
	public String toString(){
		StringBuilder sb=new StringBuilder();
		for(int i:list){
			sb.append((i-1)+",");
		}
		return sb.toString();
	}
}
