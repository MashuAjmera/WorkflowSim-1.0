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
 * The IWD planning algorithm.
 *

 * @author Pedro Paulo Vezzá Campos
 * @date Oct 12, 2013
 */
public class IWDPlanningAlgorithm extends BasePlanningAlgorithm {


	private class Graph {
		private int V;

		private LinkedList<Integer> adj[];
		
		public Task gettask(int a) {
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

		@SuppressWarnings("unchecked")
		Graph(int v) {
			V = v;
			adj = new LinkedList[v];
			for (int i = 0; i < v; ++i)
				adj[i] = new LinkedList();
		}

		void addEdge(int v, int w) {
			adj[v].add(w);
		}

		public ArrayList<Integer> childs(int v) {
			ArrayList<Integer> numbers = new ArrayList<Integer>();
			Iterator<Integer> i = adj[v].listIterator();
			while (i.hasNext()) {
				int n = i.next();
				numbers.add(n);
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
					float n = i.next();
					int q=(int)n;
					float t = (g.childs(q).size()/gettask(q).getCloudletLength());
					if (t >= m1) {
						m1 = (int)t;
						m2 = (int)n;
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
					if (adj[i].get(j) == v) {
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

//		averageBandwidth = calculateAverageBandwidth();

		for (Object vmObject : getVmList()) {
			CondorVM vm = (CondorVM) vmObject;
//			schedules.put(vm, new ArrayList<>());
		}

		// Prioritization phase
//		calculateComputationCosts();
		calculateBestPath();
//		calculateRanks();

		// Selection phase
//		allocateTasks();
	}

	public Task gettask(int a) {
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
		Graph g = new Graph(t + 1);
		for (Task parent : getTaskList()) {

			int m = parent.getCloudletId();
			g.addEdge(0, m);
		}

		// Calculating the actual values
		for (Task parent : getTaskList()) {
			int m = parent.getCloudletId();
			for (Task child : parent.getChildList()) {
				int n = child.getCloudletId();
				System.out.println("file size of cloudid"+n+" " + child.getCloudletLength() +"......");
				g.addEdge(m, n);

			}
		}

		ArrayList<Integer> best_path = new ArrayList<Integer>();
		best_path.add(0);

		ArrayList<Integer> m = new ArrayList<Integer>();
		m = g.childs(0);
		m = g.bpath(0, g);
		t = m.get(m.size() - 1);
		int priority = 1000;
		while (m.size() != 1) {

			m = g.bpath(0, g);

			for (int j = 1; j < m.size(); j++) {

				int x = m.get(j);
				if (x == 0) {
					continue;
				}
				Task t3 = gettask(x);
				if (t3.getCloudletPriority() == 0) {
					t3.setCloudletPriority(priority);
					priority = priority - 1;
				}
			}

			t = m.get(m.size() - 1);
			g.removeEdge(t);

		}
		List<Task> tlist = new ArrayList<Task>();
		tlist = getTaskList();
		int[] arr = new int[lenght + 1];
		System.out.println("LENGTHHH" + lenght);
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
