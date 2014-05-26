package app.tsp.optimize.old;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import app.tsp.Sequence;
import app.tsp.function.TSPFunction;
import app.tsp.function.TSPGraph;
import app.tsp.sampler.MarkovSampler;

import optimize.function.CostFunction;
import optimize.function.RecordFunction;
import optimize.optimizer.old.TruncationEDA;



public class TSP_PopAnnealing extends TruncationEDA<Sequence> {

	public TSP_PopAnnealing(int sample_num, double cutoff,
			CostFunction<Sequence> cf) {
		super(sample_num, cutoff, cf);
		super.sampler = new MarkovSampler(cf.dim(),cf.dim(), 0.1);
		//super.sampler = new MarkovSamplerWithReplacement(cf.dim(), 0.1);
	}

	public static void main(String args[]) {
		for (int c = 0; c < 1; c++) {
			System.out.println("c=" + c);
			String problem = "./tsp/problems/1.tsp";
			try {
				TSPFunction tspf = TSPFunction
						.createFromFile(new File(problem));
				System.out.println(tspf);
				TSPGraph tspg = TSPGraph.createFromFile(new File(
						problem));
				tspg.drawGraph(new PrintStream(new FileOutputStream(
								"tsp_graph_all.dat")));
				
				PrintStream ps = new PrintStream(new FileOutputStream(
						"test.dat"));
				CostFunction<Sequence> cf = new RecordFunction<Sequence>(tspf,
						ps);
				TSP_PopAnnealing tspa = new TSP_PopAnnealing(100, 0.1, cf);

				for (long i = 0; i < 1500; i++) {
					tspa.optimize();
					if (true && i % 10 == 0) {
						System.out.println("----------------" + i
								+ "----------------");
						System.out.println(tspa);
					}
					if (true && i % 100 == 0) {
						tspg.drawGraph(tspa.getBestSolution(),new PrintStream(new FileOutputStream(
								"tsp_graph"+i+".dat")));
					}
				}
				ps.close();
				System.out.println(tspa);
				tspg.drawGraph(tspa.getBestSolution(),new PrintStream(new FileOutputStream(
						"tsp_graph.dat")));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		System.out.println("end pop");
		System.exit(0);
	}
}
