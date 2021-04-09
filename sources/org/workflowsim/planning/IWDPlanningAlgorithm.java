/**
 * Copyright 2012-2013 University Of Southern California
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.workflowsim.planning;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.workflowsim.CondorVM;
import org.workflowsim.Task;

/**
 * The HEFT planning algorithm.
 *
 * @author Pedro Paulo Vezzï¿½ Campos
 * @date Oct 12, 2013
 */
public class IWDPlanningAlgorithm extends BasePlanningAlgorithm {

	public class Edge {

		private double weight;
		private int dest; // Same thing here.

		public Edge(int dest, double weight) {
			this.weight = weight;
			this.dest = dest;
		}
	}

//	public static double av = 0.1, bv = 1.5, cv = 0.8, as = 1.1, bs = 0.01, cs = 1, tau = 1, lr = 0.9, delsoilmin = 10,
//			delsoilmax = 90, es = 0.01, a = 2;
//	public static double av = 0.1, bv = 1, cv = 0.5, as = 1.3, bs = 0.01, cs = 1, tau = 1, lr = 0.9, delsoilmin = 10,
//			delsoilmax = 90, es = 0.01, a = 2;

//	MAIN
	public static double av = 0.1, bv = 2, cv = 1, as = 1, bs = 0.01, cs = 1, tau = 1, lr = 0.9, delsoilmin = 10,
			delsoilmax = 90, es = 0.01, a = 2;

	double updateIwdSoil(double soil, double delSoil) {
		if (delSoil < delsoilmin) {
			return soil + delsoilmin;
		} else if (delSoil > delsoilmax) {
			return soil + delsoilmax;
		} else {
			return soil + delSoil;
		}
	}

	private class Graph {

		private int V;

		private LinkedList<Edge> adj[];

		@SuppressWarnings("unchecked")
		Graph(int v) {
			V = v;
			adj = new LinkedList[v];
			for (int i = 0; i < v; ++i)
				adj[i] = new LinkedList<Edge>();
		}

		void addEdge(int v, int w, int wt) {
			Edge edge = new Edge(w, wt);
			adj[v].add(edge);
		}

		void modifyEdge(int v, int w, double wt) {
			Edge edge = new Edge(w, wt);
			int indexToRemove = 0;
			for (int i = 0; i < adj[v].size(); i++) {
				if (adj[v].get(i).dest == w) {
					indexToRemove = i;
					break;
				}
			}
			adj[v].remove(indexToRemove);
			adj[v].add(edge);

		}

		double g(int i, int j) {
			ArrayList<Integer> m = new ArrayList<Integer>();

			m = this.childs(i);
			double mini = this.getWeight(i, m.get(0));
			for (int k = 0; k < m.size(); k++) {
				if (mini > this.getWeight(i, m.get(k))) {
					mini = this.getWeight(i, m.get(k));
				}
			}
			if (mini >= 0) {
//				System.out.println("HereHere +ve " + this.getWeight(i, j));
				return this.getWeight(i, j);
			} else {
//				System.out.println("HEREHERE " + (this.getWeight(i, j) - mini));
				return this.getWeight(i, j) - mini;
			}

		}

		double fsoil(double a) {
			return 1 / (es + a);
		}

		double probability(int child, int child2) {
			double rn = Math.random();
			double fi = Math.random();
			ArrayList<Integer> m = new ArrayList<Integer>();
//			System.out.println("valur of child in probability fx " + child);
			m = this.childs(child);
//			System.out.println("valur of m in probability fx " + m);
			double count = 0;
			for (int i = 0; i < m.size(); i++) {
				Cloudlet c;
				c = (Cloudlet) gettask(m.get(i));
//				System.out.println("valur of c in probability fx hello " + c.getCloudletLength());

				count = count + fsoil(g(child, m.get(i)));

			}
			if (fi < 0.5) {
				double t = fsoil(g(child, child2));
				Cloudlet c = (Cloudlet) gettask((child2));
				double we = (double) c.getCloudletLength() / 1000;
				return t / (count * Math.pow(we, tau));
			} else {
				double t = fsoil(g(child, child2));
				Cloudlet c = (Cloudlet) gettask(child2);
				double we = (double) c.getCloudletLength() / 1000;

				return Math.min(1.0, t / (count * (Math.pow(we, tau)) + rn));
			}
		}

		double updateSoil(int i, int j, double VEL) {
			double delSoil = this.computeDeltaSoil(i, j, VEL);
			double w = 0;
			if (delSoil < delsoilmin) {
				w = (1 - lr) * this.getWeight(i, j) - lr * delsoilmin;
				this.modifyEdge(i, j, w);
			} else if (delSoil > delsoilmax) {
				w = (1 - lr) * this.getWeight(i, j) - lr * delsoilmax;
				this.modifyEdge(i, j, w);
			} else {
				w = (1 - lr) * this.getWeight(i, j) - lr * delSoil;
				this.modifyEdge(i, j, w);
			}
			return delSoil;

		}

		double getWeight(int v, int w) {
			double wt = 0;

			for (int i = 0; i < adj[v].size(); i++) {
//				System.out.println("HERE v " + v + " HWEW w " + w);
				if (adj[v].get(i).dest == w) {
					wt = adj[v].get(i).weight;
					break;
				}
			}
			return wt;
		}

		public void printWeight(Graph g) {
			for (int i = 0; i < V; i++) {
				for (int j = 0; j < adj[i].size(); j++) {
					System.out.println("Weight at " + i + " to " + j + " : " + adj[i].get(j).weight);
				}
			}
		}

		public void setedgeweights() {
			for (int i = 0; i < V; i++) {
				for (int j = 0; j < adj[i].size(); j++) {
					adj[i].get(j).weight = 1 + a * childs(j).size();
				}
			}
		}

		double updateVelocity(int i, int j, double v) {
			return v + av / (bv + cv * this.getWeight(i, j) * this.getWeight(i, j));
		}

		double computeDeltaSoil(int i, int j, double velocity) {

			return as / (bs + cs * this.getWeight(i, j) / velocity);

		}

		public ArrayList<Integer> childs(int v) {
			ArrayList<Integer> numbers = new ArrayList<Integer>();
			Iterator<Edge> i = adj[v].listIterator();
			while (i.hasNext()) {
				Edge n = i.next();
				numbers.add(n.dest);
			}

			return numbers;
		}

		public ArrayList<Integer> bpath(int v, Graph g) {

			ArrayList<Integer> best_path = new ArrayList<Integer>();
			best_path.add(0);

			ArrayList<Integer> m, n2 = new ArrayList<Integer>();
			m = g.childs(0);
			while (true) {

				int m1 = -1, m2 = -1;
				Iterator<Integer> i = m.listIterator();
				while (i.hasNext()) {
					int n = i.next();
					int t = g.childs(n).size();
					if (t >= m1) {
						m1 = t;
						m2 = n;
					}

				}
				if (m1 == -1) {
					break;
				}
				best_path.add(m2);
				m = g.childs(m2);

			}
			return best_path;
		}

		void removeEdge(int v) {
			for (int i = 0; i < V; i++) {
				for (int j = 0; j < adj[i].size(); j++) {
					if (adj[i].get(j).dest == v) {
						adj[i].remove(j);
						break;
					}
				}
			}
			while (adj[v].size() > 0) {
				adj[v].remove();
			}
		}

	}

	/**
	 * The main function
	 */
	@Override
	public void run() {
		Log.printLine("IWD planner running with " + getTaskList().size() + " tasks.");

		// averageBandwidth = calculateAverageBandwidth();

		for (Object vmObject : getVmList()) {
			CondorVM vm = (CondorVM) vmObject;
			// schedules.put(vm, new ArrayList<>());
		}

		// Prioritization phase
		// calculateComputationCosts();
		calculateBestPath();
		// calculateRanks();

		// Selection phase
		// allocateTasks();
	}

	public Task gettask(int a) {
//		System.out.println("Gettask input " + a);
		Task returnTask = null;
		for (Task parent : getTaskList()) {

			int m = parent.getCloudletId();
			for (Task child : parent.getChildList()) {
				int n = child.getCloudletId();
				if (m == a) {
					returnTask = parent;
				}
				if (n == a) {
					returnTask = child;
				}
			}
		}
		return returnTask;

	}

	private void calculateBestPath() {

		int t, length;
		t = getTaskList().size();
		length = t;
		int wt = 1;
		Graph g = new Graph(t + 1);
		for (Task parent : getTaskList()) {
			if (parent.getParentList().size() == 0) {
				int m = parent.getCloudletId();
				g.addEdge(0, m, wt);
			}
		}

		// Calculating the actual values
		for (Task parent : getTaskList()) {
			int m = parent.getCloudletId();
			for (Task child : parent.getChildList()) {
				int n = child.getCloudletId();
				g.addEdge(m, n, wt);

			}
		}

		ArrayList<Integer> best_path = new ArrayList<Integer>();
		best_path.add(0);

		g.setedgeweights();
//		g.printWeight(g);

//		System.out.println("Child of 16 " + g.childs(16));

//		System.out.println("Weight,4,14" + g.getWeight(3, 14));

		// m = g.childs(0);
		int priority = length;

		int currNode = 0, nextNode = 0;

		while (length != 0) {
			double vel = 100, soil = 0;
			ArrayList<Integer> m = new ArrayList<Integer>();

			m = g.childs(currNode);
//			System.out.println("value of m in forst loop" + m.size());

			if (g.childs(currNode).size() == 0) {
//				System.out.println("currNode " + currNode);
				Task t5 = gettask(currNode);
				if (t5.getCloudletPriority() == 0) {
					t5.setCloudletPriority(priority--);
				}
				g.removeEdge(currNode);
				length--;
				currNode = 0;
				nextNode = 0;
			} else {
				double probArr[] = new double[m.size()], probArrN[] = new double[m.size()];
				double probSum = 0;
				for (int j = 0; j < m.size(); j++) {
					probArr[j] = g.probability(currNode, m.get(j));
					probSum += probArr[j];

				}
				for (int j = 0; j < m.size(); j++) {
					probArr[j] = probArr[j] / probSum;

				}
				probArrN[0] = probArr[0];
				for (int j = 1; j < m.size(); j++) {
//					probArr[j] = probArr[j] / probSum;
					probArrN[j] = probArrN[j - 1] + probArr[j];

				}

				double x = Math.random();

				for (int j = 0; j < m.size(); j++) {

					if (probArrN[j] >= x) {
						nextNode = m.get(j);
						System.out.println("currNode " + currNode + " nextNode " + nextNode + " j " + j);
						g.updateVelocity(currNode, nextNode, vel);

						Task t4 = gettask(nextNode);
						if (t4.getCloudletPriority() == 0) {
							t4.setCloudletPriority(priority--);
						}
						double delSoil = g.updateSoil(currNode, nextNode, vel);
						soil = updateIwdSoil(soil, delSoil);
						break;
					}

				}
				currNode = nextNode;

			}

		}

		List<Task> tlist = new ArrayList<Task>();
		tlist = getTaskList();
		int[] arr = new int[getTaskList().size() + 1];
		// System.out.println("LENGTHHH" + length);
		arr[0] = 0;
		for (int i = 0; i < tlist.size(); i++) {
			System.out.println(
					"Task ID " + tlist.get(i).getCloudletId() + " Task Priority " + tlist.get(i).getCloudletPriority());
			arr[i] = tlist.get(i).getCloudletPriority();
		}

//		System.out.println();
		Cloudlet.setArray(arr);

	}

}
