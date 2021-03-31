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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Log;
import org.workflowsim.CondorVM;
import org.workflowsim.Task;

/**
 * The HEFT planning algorithm.
 *
 * @author Pedro Paulo Vezz� Campos
 * @date Oct 12, 2013
 */
public class IWDPlanningAlgorithm extends BasePlanningAlgorithm {

	private Map<Task, Map<CondorVM, Double>> computationCosts;
	private Map<Task, Map<Task, Double>> transferCosts;
	private Map<Task, Double> rank;
	private Map<CondorVM, List<Event>> schedules;
	private Map<Task, Double> earliestFinishTimes;
	private double averageBandwidth;

	private class Graph {
		private int V;

		public int count;

		private LinkedList<Integer> adj[];

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

		void DFSUtil(int v, boolean visited[]) {

			visited[v] = true;
			count++;

			Iterator<Integer> i = adj[v].listIterator();
			while (i.hasNext()) {
				int n = i.next();
				if (!visited[n])
					DFSUtil(n, visited);
			}
		}

		int DFS(int v) {

			boolean visited[] = new boolean[V];

			DFSUtil(v, visited);
			int m = --count;
			count = 0;
			return m;
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

	private class Event {

		public double start;
		public double finish;

		public Event(double start, double finish) {
			this.start = start;
			this.finish = finish;
		}
	}

	private class TaskRank implements Comparable<TaskRank> {

		public Task task;
		public Double rank;

		public TaskRank(Task task, Double rank) {
			this.task = task;
			this.rank = rank;
		}

		@Override
		public int compareTo(TaskRank o) {
			return o.rank.compareTo(rank);
		}
	}

	public IWDPlanningAlgorithm() {
		computationCosts = new HashMap<>();
		transferCosts = new HashMap<>();
		rank = new HashMap<>();
		earliestFinishTimes = new HashMap<>();
		schedules = new HashMap<>();
	}

	/**
	 * The main function
	 */
	@Override
	public void run() {
		Log.printLine("HEFT planner running with " + getTaskList().size() + " tasks.");

		averageBandwidth = calculateAverageBandwidth();

		for (Object vmObject : getVmList()) {
			CondorVM vm = (CondorVM) vmObject;
			schedules.put(vm, new ArrayList<>());
		}

		// Prioritization phase
		calculateComputationCosts();
		calculateTransferCosts();
//		calculateRanks();

		// Selection phase
//		allocateTasks();
	}

	/**
	 * Calculates the average available bandwidth among all VMs in Mbit/s
	 *
	 * @return Average available bandwidth in Mbit/s
	 */
	private double calculateAverageBandwidth() {
		double avg = 0.0;
		for (Object vmObject : getVmList()) {
			CondorVM vm = (CondorVM) vmObject;
			avg += vm.getBw();
		}
		return avg / getVmList().size();
	}

	/**
	 * Populates the computationCosts field with the time in seconds to compute a
	 * task in a vm.
	 */
	private void calculateComputationCosts() {
		for (Task task : getTaskList()) {
			Map<CondorVM, Double> costsVm = new HashMap<>();
			for (Object vmObject : getVmList()) {
				CondorVM vm = (CondorVM) vmObject;
				if (vm.getNumberOfPes() < task.getNumberOfPes()) {
					costsVm.put(vm, Double.MAX_VALUE);
				} else {
					costsVm.put(vm, task.getCloudletTotalLength() / vm.getMips());
				}
			}
			computationCosts.put(task, costsVm);
		}
	}

	/**
	 * Populates the transferCosts map with the time in seconds to transfer all
	 * files from each parent to each child
	 */
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

	private void calculateTransferCosts() {

		// Initializing the matrix
		for (Task task1 : getTaskList()) {
			Map<Task, Double> taskTransferCosts = new HashMap<>();
			for (Task task2 : getTaskList()) {
				taskTransferCosts.put(task2, 0.0);
			}
			transferCosts.put(task1, taskTransferCosts);
		}
		int t;
		t = getTaskList().size();
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
				g.addEdge(m, n);
				

//				transferCosts.get(parent).put(child, calculateTransferCost(parent, child));
			}
		}

		ArrayList<Integer> best_path = new ArrayList<Integer>();
		best_path.add(0);

		ArrayList<Integer> m = new ArrayList<Integer>();
		m = g.childs(0);
		m = g.bpath(0, g);
		t = m.get(m.size() - 1);
		System.out.println(g.bpath(0, g) + "\n");
		int priority = 1000;
		while (m.size() != 1) {

			System.out.println("TTT" + t);
			m = g.bpath(0, g);

			for (int j = 1; j < m.size(); j++) {

				// Printing the iterated value

				// it = it.next();
				int x = m.get(j);
				System.out.println("\nUsing ListIterator:\n" + x + "EYEE " + j);
				if (x == 0) {
					continue;
				}
				Task t3 = gettask(x);
				if (t3.getCloudletPriority() == 0) {
					t3.setCloudletPriority(priority);
					priority = priority - 1;
				}
			}

			System.out.println(m + "\n");
			t = m.get(m.size() - 1);
			g.removeEdge(t);

		}
		List<Task> tlist = new ArrayList<Task>();
		tlist = getTaskList();
		int[] arr = new int[25];
		for (int i = 0; i < tlist.size(); i++) {
			System.out.println(
					"Task ID " + tlist.get(i).getCloudletId() + " Task Priority " + tlist.get(i).getCloudletPriority());
			arr[i] = tlist.get(i).getCloudletPriority();
		}
		for (int i = 0; i < 25; i++) {
			System.out.print(arr[i] + ", ");
		}
		System.out.println();

	}

	/**
	 * Accounts the time in seconds necessary to transfer all files described
	 * between parent and child
	 *
	 * @param parent
	 * @param child
	 * @return Transfer cost in seconds
	 */
//	private double calculateTransferCost(Task parent, Task child) {
//		List<FileItem> parentFiles = parent.getFileList();
//		List<FileItem> childFiles = child.getFileList();
//
//		double acc = 0.0;
//
//		for (FileItem parentFile : parentFiles) {
//			if (parentFile.getType() != Parameters.FileType.OUTPUT) {
//				continue;
//			}
//
//			for (FileItem childFile : childFiles) {
//				if (childFile.getType() == Parameters.FileType.INPUT
//						&& childFile.getName().equals(parentFile.getName())) {
//					acc += childFile.getSize();
//					break;
//				}
//			}
//		}
//
//		// file Size is in Bytes, acc in MB
//		acc = acc / Consts.MILLION;
//		// acc in MB, averageBandwidth in Mb/s
//		return acc * 8 / averageBandwidth;
//	}

	/**
	 * Invokes calculateRank for each task to be scheduled //
	 */
//	private void calculateRanks() {
//		for (Task task : getTaskList()) {
//			calculateRank(task);
//		}
//	}

	/**
	 * Populates rank.get(task) with the rank of task as defined in the HEFT paper.
	 *
	 * @param task The task have the rank calculates
	 * @return The rank
	 */
//	private double calculateRank(Task task) {
//		if (rank.containsKey(task)) {
//			return rank.get(task);
//		}
//
//		double averageComputationCost = 0.0;
//
//		for (Double cost : computationCosts.get(task).values()) {
//			averageComputationCost += cost;
//		}
//
//		averageComputationCost /= computationCosts.get(task).size();
//
//		double max = 0.0;
//		for (Task child : task.getChildList()) {
//			double childCost = transferCosts.get(task).get(child) + calculateRank(child);
//			max = Math.max(max, childCost);
//		}
//
//		rank.put(task, averageComputationCost + max);
//
//		return rank.get(task);
//	}

	/**
	 * Allocates all tasks to be scheduled in non-ascending order of schedule.
	 */
//	private void allocateTasks() {
//		List<TaskRank> taskRank = new ArrayList<>();
//		for (Task task : rank.keySet()) {
//			taskRank.add(new TaskRank(task, rank.get(task)));
//		}
//
//		// Sorting in non-ascending order of rank
//		Collections.sort(taskRank);
//		for (TaskRank rank : taskRank) {
//			allocateTask(rank.task);
//		}
//
//	}

	/**
	 * Schedules the task given in one of the VMs minimizing the earliest finish
	 * time
	 *
	 * @param task The task to be scheduled
	 * @pre All parent tasks are already scheduled
	 */
//	private void allocateTask(Task task) {
//		CondorVM chosenVM = null;
//		double earliestFinishTime = Double.MAX_VALUE;
//		double bestReadyTime = 0.0;
//		double finishTime;
//
//		for (Object vmObject : getVmList()) {
//			CondorVM vm = (CondorVM) vmObject;
//			double minReadyTime = 0.0;
//
//			for (Task parent : task.getParentList()) {
//				double readyTime = earliestFinishTimes.get(parent);
//				if (parent.getVmId() != vm.getId()) {
//					readyTime += transferCosts.get(parent).get(task);
//				}
//				minReadyTime = Math.max(minReadyTime, readyTime);
//			}
//
//			finishTime = findFinishTime(task, vm, minReadyTime, false);
//
//			if (finishTime < earliestFinishTime) {
//				bestReadyTime = minReadyTime;
//				earliestFinishTime = finishTime;
//				chosenVM = vm;
//			}
//		}
//
//		findFinishTime(task, chosenVM, bestReadyTime, true);
//		earliestFinishTimes.put(task, earliestFinishTime);
//
//		task.setVmId(chosenVM.getId());
//	}

	/**
	 * Finds the best time slot available to minimize the finish time of the given
	 * task in the vm with the constraint of not scheduling it before readyTime. If
	 * occupySlot is true, reserves the time slot in the schedule.
	 *
	 * @param task       The task to have the time slot reserved
	 * @param vm         The vm that will execute the task
	 * @param readyTime  The first moment that the task is available to be scheduled
	 * @param occupySlot If true, reserves the time slot in the schedule.
	 * @return The minimal finish time of the task in the vmn
	 */
//	private double findFinishTime(Task task, CondorVM vm, double readyTime, boolean occupySlot) {
//		List<Event> sched = schedules.get(vm);
//		double computationCost = computationCosts.get(task).get(vm);
//		double start, finish;
//		int pos;
//
//		if (sched.isEmpty()) {
//			if (occupySlot) {
//				sched.add(new Event(readyTime, readyTime + computationCost));
//			}
//			return readyTime + computationCost;
//		}
//
//		if (sched.size() == 1) {
//			if (readyTime >= sched.get(0).finish) {
//				pos = 1;
//				start = readyTime;
//			} else if (readyTime + computationCost <= sched.get(0).start) {
//				pos = 0;
//				start = readyTime;
//			} else {
//				pos = 1;
//				start = sched.get(0).finish;
//			}
//
//			if (occupySlot) {
//				sched.add(pos, new Event(start, start + computationCost));
//			}
//			return start + computationCost;
//		}
//
//		// Trivial case: Start after the latest task scheduled
//		start = Math.max(readyTime, sched.get(sched.size() - 1).finish);
//		finish = start + computationCost;
//		int i = sched.size() - 1;
//		int j = sched.size() - 2;
//		pos = i + 1;
//		while (j >= 0) {
//			Event current = sched.get(i);
//			Event previous = sched.get(j);
//
//			if (readyTime > previous.finish) {
//				if (readyTime + computationCost <= current.start) {
//					start = readyTime;
//					finish = readyTime + computationCost;
//				}
//
//				break;
//			}
//			if (previous.finish + computationCost <= current.start) {
//				start = previous.finish;
//				finish = previous.finish + computationCost;
//				pos = i;
//			}
//			i--;
//			j--;
//		}
//
//		if (readyTime + computationCost <= sched.get(0).start) {
//			pos = 0;
//			start = readyTime;
//
//			if (occupySlot) {
//				sched.add(pos, new Event(start, start + computationCost));
//			}
//			return start + computationCost;
//		}
//		if (occupySlot) {
//			sched.add(pos, new Event(start, finish));
//		}
//		return finish;
//	}
}