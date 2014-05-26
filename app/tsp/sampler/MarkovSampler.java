package app.tsp.sampler;


import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Random;

import app.tsp.Sequence;

import optimize.util.WeightedSampleWithValue;

import statistics.sampler.Sampler;
import statistics.sampler.SamplerTest;

public class MarkovSampler implements Sampler<Sequence> {

	private static final int TERMINAL = 0;

	private double learningRate = 1;

	// tm[from][to]
	// tm[0][to] is initial move
	// tm[from][0] is terminal
	// cumulative probability
	private double[][] transitionMatrix;

	private int size;

	private int maxLength;

	private Random rand = new Random();

	public MarkovSampler(int size) {
		this(size, size*2);
	}
	
	public MarkovSampler(int size, int maxLength) {
		this.size = size;
		this.maxLength = maxLength;
		transitionMatrix = new double[size + 1][];
		for (int i = 0; i < size + 1; i++) {
			transitionMatrix[i] = new double[size + 1];
			for (int j = 0; j < size + 1; j++) {
				transitionMatrix[i][j] = 1d / (size + 1);
			}
		}
		density2cumulative();
	}

	public MarkovSampler(int size,int maxLength,double learningRate){
		this(size,maxLength);
		this.learningRate=learningRate;
	}

	private double probability(int from, int to) {
		if (0 < to)
			return transitionMatrix[from][to] - transitionMatrix[from][to - 1];
		else
			return transitionMatrix[from][to];
	}

	public void fittingUpdate(List<WeightedSampleWithValue<Sequence>> allSamples) {
		double[][] tempMatrix = new double[size + 1][];
		for (int i = 0; i < size + 1; i++) {
			tempMatrix[i] = new double[size + 1];
			for (int j = 0; j < size + 1; j++) {
				tempMatrix[i][j] = 0;
			}
		}

		for (WeightedSampleWithValue<Sequence> wswv : allSamples) {
			Sequence s = wswv.getSampeWithVale().getSample();
			int current = TERMINAL;
			for (int i = 0; i < s.size(); i++) {
				int next = s.get(i);
				tempMatrix[current][next] += wswv.getWeight();
				current = next;
			}
		}

		// normalize
		for (int i = 0; i < tempMatrix.length; i++) {
			double sum = 0;
			for (int j = 0; j < tempMatrix[i].length; j++) {
				sum += tempMatrix[i][j];
			}
			for (int j = 0; j < tempMatrix[i].length; j++) {
				if (sum < 1e-5) {
					tempMatrix[i][j] = 1d / tempMatrix[i].length;
				} else {
					tempMatrix[i][j] /= sum;
				}
			}
		}

		cumulative2density();
		// update parameter
		for (int i = 0; i < transitionMatrix.length; i++) {
			for (int j = 0; j < transitionMatrix[i].length; j++) {
				transitionMatrix[i][j] = (1 - learningRate)
						* transitionMatrix[i][j] + learningRate
						* tempMatrix[i][j];
			}
		}
		density2cumulative();

	}

	@Override
	public Sampler<Sequence> fittingReplace(
			List<WeightedSampleWithValue<Sequence>> allSamples) {
		// TODO Auto-generated method stub
		return null;
	}

	private void cumulative2density() {
		// cumulative->density
		for (int i = 0; i < transitionMatrix.length; i++) {
			for (int j = transitionMatrix[i].length - 1; 1 <= j; j--) {
				transitionMatrix[i][j] = transitionMatrix[i][j]
						- transitionMatrix[i][j - 1];
			}
		}
	}

	private void density2cumulative() {
		// density->cumulative
		for (int i = 0; i < transitionMatrix.length; i++) {
			for (int j = 1; j < transitionMatrix[i].length; j++) {
				transitionMatrix[i][j] = transitionMatrix[i][j]
						+ transitionMatrix[i][j - 1];
			}
		}
	}

	public List<Sequence> sampling(int num) {
		ArrayList<Sequence> list = new ArrayList<Sequence>(num);
		for (int i = 0; i < num; i++) {
			list.add(sampling());
		}
		return list;
	}

	private Sequence sampling() {
		ArrayList<Integer> list = new ArrayList<Integer>(this.maxLength+1);
		int current = TERMINAL;
		while (list.size() < maxLength) {
			current = getNext(current);
			if (current == TERMINAL)
				break;
			list.add(current);
		}
		list.add(TERMINAL);
		return new Sequence(list);
	}

	// TODO: binary search
	private int getNext(int from) {
		double r = rand.nextDouble();
		for (int i = 0; i < transitionMatrix[from].length; i++) {
			if (r <= transitionMatrix[from][i])
				return i;
		}
		return transitionMatrix[from].length - 1;
	}

	public double logNormalizedProbability(Sequence s) {
		double sumLogProb = 0;
		int current = TERMINAL;
		for (int i = 0; i < s.size(); i++) {
			int next = s.get(i);
			sumLogProb += Math.log(probability(current, next));
			current = next;
		}
		return sumLogProb;
	}

	public double normalizedProbability(Sequence s) {
		System.out.println("this function has not implemented yet");
		System.exit(0);
		return 0;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder("MarkovSampler:" + size + "\n");
		Formatter formatter = new Formatter(sb);
		cumulative2density();
		formatter.format("%4s     ", "to");
		
		for (int j = 0; j < transitionMatrix[0].length && j < 13; j++) {
			formatter.format("%7d",j);
		}
		sb.append("\n");
		for (int i = 0; i < transitionMatrix.length; i++) {
			if (10 < i) {
				sb.append("...........\n");
				break;
			}
			formatter.format("from%3d   ", i);
			for (int j = 0; j < transitionMatrix[i].length && j < 13; j++) {
				formatter.format("%6.3f ", transitionMatrix[i][j]);
			}
			sb.append("\n");
		}
		density2cumulative();
		return sb.toString();
	}

	public void randomParametrize() {
		double[][] tempMatrix = new double[size + 1][];
		for (int i = 0; i < size + 1; i++) {
			tempMatrix[i] = new double[size + 1];
			for (int j = 0; j < size + 1; j++) {
				tempMatrix[i][j] = 0;
			}
		}

		for (int i = 0; i < size+1; i++) {
			for(int j=0;j<size+1;j++){
				tempMatrix[i][j]=rand.nextDouble();
			}
		}

		// normalize
		for (int i = 0; i < tempMatrix.length; i++) {
			double sum = 0;
			for (int j = 0; j < tempMatrix[i].length; j++) {
				sum += tempMatrix[i][j];
			}
			for (int j = 0; j < tempMatrix[i].length; j++) {
				if (sum < 1e-5) {
					tempMatrix[i][j] =1d/tempMatrix[i].length;
				} else {
					tempMatrix[i][j] /= sum;
				}
			}
		}
		
		transitionMatrix=tempMatrix;
		density2cumulative();
	}
	
	public static void main(String args[]){
		MarkovSampler ms1=new MarkovSampler(3);
		MarkovSampler ms2=new MarkovSampler(3);
		ms1.randomParametrize();
		SamplerTest<Sequence> st=new SamplerTest<Sequence>(ms1,ms2,1000);
		for(int i=0;i<500;i++){
			System.out.println("-------"+i+"-------");
			st.test();
			System.out.println("supervisor");
			System.out.println(ms1);

			System.out.println("learner");
			System.out.println(ms2);
		}
	}

	@Override
	public double entropy() {
		// TODO Auto-generated method stub
		return 0;
	}

}
