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

		private int weight;
		private int dest; // Same thing here.

		public Edge(int dest, int weight) {
			this.weight = weight;
			this.dest = dest;
		}
	}

	private class Graph {
		private double av = 0.1, bv = 2, cv = 1;
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

		void modifyEdge(int v, int w, int wt) {
			Edge edge = new Edge(w, wt);
			int indexToRemove = 0;
			for (int i = 0; i < adj[v].size(); i++) {
				if (adj[v].get(i).dest == w) {
					indexToRemove = adj[v].get(i).dest;
				}
			}
			adj[v].remove(indexToRemove);
			adj[v].add(edge);

		}

		void updateEdgeSoil(int v, int w, int wt) {
			Edge edge = new Edge(w, wt);
			int indexToRemove = 0;
			for (int i = 0; i < adj[v].size(); i++) {
				if (adj[v].get(i).dest == w) {
					indexToRemove = adj[v].get(i).dest;
				}
			}
			adj[v].remove(indexToRemove);
			adj[v].add(edge);

		}

		int getWeight(int v, int w) {
			int wt = 0;
			for (int i = 0; i < adj[v].size(); i++) {
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
					adj[i].get(j).weight = 1 + 2 * childs(j).size();
				}
			}
		}

		double updateVelocity(int i, int j, double v) {
			double av = 0.1, bv = 2, cv = 1;
			return v + av / (bv + cv * (1 + 2 * childs(j).size()));

		}

		double computeDeltaSoil(int i, int j, double velocity) {
			return av / (bv + cv * adj[i].get(j).weight / velocity);

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
		System.out.println("Gettask input " + a);
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

		int t, lenght;
		t = getTaskList().size();
		lenght = t;
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

		g.printWeight(g);

		ArrayList<Integer> best_path = new ArrayList<Integer>();
		best_path.add(0);

		ArrayList<Integer> m = new ArrayList<Integer>();
		m = g.childs(0);
		int priority = lenght;
		System.out.println(m);

		int currNode = 0, nextNode = 0;

		double as, bs, cs, P, probSum, vel = 100, soil = 0;
		double probArr[], probArrN[], probArrS[] = new double[lenght];
		while (lenght-- != 0) {

			m = g.childs(currNode);
			for (int j = 0; j < m.size(); j++) {
				probArrN[j] = probArr[j] / probSum;
				if (j > 0) {
					probArrN[j] += probArr[j - 1];
				}

				if (g.childs(currNode).size() == 0) {
					g.removeEdge(currNode);

				}
			}
			double x = Math.random();

			for (int j = 0; j < m.size(); j++) {
				if (probArrS[j] >= x) {
					nextNode = m.get(j);
					g.updateVelocity(currNode, nextNode, vel);

					Task t4 = gettask(nextNode);
					if (t4.getCloudletPriority() == 0) {
						t4.setCloudletPriority(priority--);
					}
					g.updateEdgeSoil(currNode, nextNode, g.getWeight(currNode, nextNode));

				}
			}
			currNode = nextNode;

		}

		List<Task> tlist = new ArrayList<Task>();
		tlist = getTaskList();
		int[] arr = new int[lenght + 1];
		// System.out.println("LENGTHHH" + lenght);
		arr[0] = 0;
		for (int i = 0; i < tlist.size(); i++) {
			System.out.println(
					"Task ID " + tlist.get(i).getCloudletId() + " Task Priority " + tlist.get(i).getCloudletPriority());
			arr[i + 1] = tlist.get(i).getCloudletPriority();
		}
		arr[0] = 0;

		System.out.println();
		Cloudlet.setArray(arr);

	}

}