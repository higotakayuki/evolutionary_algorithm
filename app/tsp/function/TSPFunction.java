package app.tsp.function;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;

import optimize.function.CostFunction;

import app.tsp.Sequence;


public class TSPFunction extends CostFunction<Sequence>{

	private double[][] distanceMatrix;
	
	private double maxDist;
	
	private static final int TERMINAL=0;
	
	private TSPFunction(int dim) {
		super(dim);
	}

	@Override
	public double eval(Sequence t) {
		//dist
		double dist=0;
		int current=t.get(0);
		for(int i=1;i<t.size();i++){
			int next=t.get(i);
			if(next==TERMINAL){
				dist+=getDistance(current-1,t.get(0)-1);
				break;
			}
			dist+=getDistance(current-1,next-1);
		}
		if(false)return dist;
		//cover rate
		double revisit=0;
		boolean[] check=new boolean[dim()];
		for(int i=0;i<check.length;i++){
			check[i]=false;
		}
		for(int i=0;i<t.size();i++){
			int city=t.get(i);
			if(city==TERMINAL)break;
			if(check[city-1])revisit++;
			check[city-1]=true;
		}
		int count=0;
		for(boolean gone:check){
			if(gone)count++;
		}
		
		return dist+revisit*maxDist+(dim()-count)*maxDist;
	}
	
	private double getDistance(int from,int to){
		if(from==to)return 0;
		if(from>to){
			return distanceMatrix[from][to];
		}else{
			return distanceMatrix[to][from];
		}
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		Formatter formatter = new Formatter(sb);
		for(int i=0;i<distanceMatrix.length;i++){
			formatter.format("%4d",i);
			for(int j=0;j<i;j++){
				formatter.format("%8.1f ", distanceMatrix[i][j]);
			}
			sb.append("\n");
		}
		return sb.toString();
	}
	
	public static TSPFunction createFromFile(File file) throws FileNotFoundException{
		FileReader fis=new FileReader(file);
		BufferedReader br=new BufferedReader(fis);
		ArrayList<Position> list=new ArrayList<Position>();
		while(true){
			String temp;
			try {
				temp = br.readLine();
				if(temp==null||temp.equalsIgnoreCase("eof")||temp.equals(""))break;
				
				String[] array=temp.split("\\s+");
				Position pos=new Position(Double.parseDouble(array[1]),Double.parseDouble(array[2]));
				list.add(pos);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		

		try {
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		TSPFunction func=new TSPFunction(list.size());
		double max=0;
		
		double[][] dist_matrix;
		dist_matrix=new double[list.size()][];
		for(int i=0;i<list.size();i++){
			dist_matrix[i]=new double[i];
			for(int j=0;j<i;j++){
				dist_matrix[i][j]=list.get(i).distance(list.get(j));
				if(max<dist_matrix[i][j])max=dist_matrix[i][j];
			}
		}
		func.distanceMatrix=dist_matrix;
		func.maxDist=max;
		return func;
	}
	
}
