
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
package org.workflowsim.scheduling;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.workflowsim.CondorVM;
import org.workflowsim.Task;
import org.workflowsim.WorkflowSimTags;

/**
 * MaxMin algorithm.
 *
 * @author Weiwei Chen
 * @since WorkflowSim Toolkit 1.0
 * @date Apr 9, 2013
 */
public class IWDSchedulingAlgorithm extends BaseSchedulingAlgorithm {

	public class Edge {

		private double weight;
		private int dest; // Same thing here.

		public Edge(int dest, double weight) {
			this.weight = weight;
			this.dest = dest;
		}
	}

//	main 2
	public static double av = 0.1, bv = 1.5, cv = 80, as = 1, bs = 0.1, cs = 1, tau = 0.9, lr = 0.5, delsoilmin = 10,
			delsoilmax = 90, es = 0.01, ev = 0.0001, a = 20, b = 100;
//	public static double av = 0.1, bv = 1, cv = 0.5, as = 1.3, bs = 0.01, cs = 1, tau = 1, lr = 0.9, delsoilmin = 10,
//			delsoilmax = 90, es = 0.01,ev = 0.0001, a = 2,b=1;

//	MAIN
//	public static double av = 1, bv = 0.01, cv = 1, as = 1, bs = 0.01, cs = 1, tau = 1, lr = 0.9,
//			delsoilmin = 10, delsoilmax = 90, es = 0.01, ev = 0.0001, a = 2, b = 1;

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

				return this.getWeight(i, j);
			} else {

				return this.getWeight(i, j) - mini;
			}

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

		double fsoil(double a) {
			return 1 / (es + a);
		}

		double probability(int child, int child2) {

			double t = fsoil(g(child, child2));
			Cloudlet c = (Cloudlet) gettask((child2));
			double we = (double) c.getCloudletLength() / 1000;
			return t / Math.pow(we, tau);

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

				if (adj[v].get(i).dest == w) {
					wt = adj[v].get(i).weight;
					break;
				}
			}
			return wt;
		}

		public void setedgeweights() {
			for (int i = 0; i < V; i++) {
				for (int j = 0; j < adj[i].size(); j++) {
					adj[i].get(j).weight = b + a * childs(j).size();
				}
			}
		}

		double updateVelocity(int i, int j, double v) {
			return v + av / (bv + cv * this.getWeight(i, j) * this.getWeight(i, j));
		}

		double computeDeltaSoil(int i, int j, double velocity) {
			Cloudlet c1 = (Cloudlet) gettask(j);

			double time = c1.getCloudletLength() / (Math.max(ev, velocity) * 1000);
			return as / (bs + cs * time * time);

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
	 * Initialize a MaxMin scheduler.
	 */
	public IWDSchedulingAlgorithm() {
		super();
	}

	/**
	 * the check point list.
	 */
	private final List<Boolean> hasChecked = new ArrayList<>();

	@Override
	public void run() {

		int[] priority = Cloudlet.getArray();

		int size = getCloudletList().size();
		hasChecked.clear();
		for (int t = 0; t < size; t++) {
			hasChecked.add(false);
		}
		for (int i = 0; i < size; i++) {
			int maxIndex = 0;
			Cloudlet maxCloudlet = null;
			for (int j = 0; j < size; j++) {
				Cloudlet cloudlet = (Cloudlet) getCloudletList().get(j);

				if (!hasChecked.get(j)) {
					maxCloudlet = cloudlet;

					maxIndex = j;
					break;
				}
			}
			if (maxCloudlet == null) {
				break;
			}

			for (int j = 0; j < size; j++) {
				Cloudlet cloudlet = (Cloudlet) getCloudletList().get(j);
				if (hasChecked.get(j)) {
					continue;
				}
				long length = priority[cloudlet.getCloudletId()];
				if (length > priority[maxCloudlet.getCloudletId()]) {

					maxCloudlet = cloudlet;
					maxIndex = j;
				}
			}
			hasChecked.set(maxIndex, true);

			int vmSize = getVmList().size();
			CondorVM firstIdleVm = null;// (CondorVM)getVmList().get(0);
			for (int j = 0; j < vmSize; j++) {
				CondorVM vm = (CondorVM) getVmList().get(j);
				if (vm.getState() == WorkflowSimTags.VM_STATUS_IDLE) {
					firstIdleVm = vm;
					break;
				}
			}
			if (firstIdleVm == null) {
				break;
			}
			for (int j = 0; j < vmSize; j++) {
				CondorVM vm = (CondorVM) getVmList().get(j);
				if ((vm.getState() == WorkflowSimTags.VM_STATUS_IDLE)
						&& vm.getCurrentRequestedTotalMips() > firstIdleVm.getCurrentRequestedTotalMips()) {
					firstIdleVm = vm;

				}
			}
			firstIdleVm.setState(WorkflowSimTags.VM_STATUS_BUSY);
			maxCloudlet.setVmId(firstIdleVm.getId());
			getScheduledList().add(maxCloudlet);

		}
	}
}
