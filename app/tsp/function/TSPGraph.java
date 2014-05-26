package app.tsp.function;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

import app.tsp.Sequence;


public class TSPGraph {
	private ArrayList<Position> cities = new ArrayList<Position>();

	private static final int TERMINAL = 0;

	public void drawGraph(Sequence s, PrintStream out) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.size(); i++) {
			int c = s.get(i) - 1;
			if (c == TERMINAL - 1)
				c = s.get(0) - 1;
			Position city = cities.get(c);
			sb.append(city.x + " " + city.y + "(" + c + ")");
			sb.append("\n");
		}
		sb.append("\n");

		if (true) {
			try {
				out.println(sb);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void drawGraph(PrintStream out) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < cities.size(); i++) {
			Position city = cities.get(i);
			sb.append(city.x + " " + city.y + "(" + i + ")");
			sb.append("\n");
		}
		sb.append("\n");

		if (true) {
			try {
				out.println(sb);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}


	public static TSPGraph createFromFile(File file)
			throws FileNotFoundException {
		FileReader fis = new FileReader(file);
		BufferedReader br = new BufferedReader(fis);
		ArrayList<Position> list = new ArrayList<Position>();
		while (true) {
			String temp;
			try {
				temp = br.readLine();
				if (temp == null || temp.equalsIgnoreCase("eof")
						|| temp.equals(""))
					break;
				String[] array = temp.split("\\s+");
				Position pos = new Position(Double.parseDouble(array[1]),
						Double.parseDouble(array[2]));
				list.add(pos);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		TSPGraph tspg = new TSPGraph();
		tspg.cities = list;
		return tspg;
	}
}
