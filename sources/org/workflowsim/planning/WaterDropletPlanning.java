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
import java.util.List;
import org.cloudbus.cloudsim.Vm;
import org.workflowsim.FileItem;
import org.workflowsim.Task;
import org.workflowsim.utils.Parameters;
import java.util.Collections;
import java.util.Comparator;
import org.cloudbus.cloudsim.Cloudlet;

/**
 * The Distributed HEFT planning algorithm. The difference compared to HEFT:
 * 
 * 1. We are able to specify the bandwidth between each pair of vms in the
 * bandwidths of Parameters. 2. Instead of using the average communication cost
 * in HEFT, we also aim to optimize the communication cost
 *
 * @author Weiwei Chen
 * @since WorkflowSim Toolkit 1.0
 * @date Nov 10, 2013
 */

class SortbyPriority1 implements Comparator<Task> {
    // Used for sorting in ascending order of
    // roll number
    public int compare(Task a, Task b) {
        return b.getPriority() - a.getPriority();
    }
}

public class WaterDropletPlanning extends BasePlanningAlgorithm {
    public Task gettask(int a) {
        Task t = null;
        for (Task parent : getTaskList()) {
            int m = parent.getCloudletId();
            if (m == a)
                t = parent;
            for (Task child : parent.getChildList()) {
                int n = child.getCloudletId();
                if (n == a)
                    t = child;
            }
        }
        return t;
    }

    /**
     * The main function
     */
    @Override
    public void run() {

        List<Vm> vmList = getVmList();
        double[][] bandwidths = new double[vmList.size()][vmList.size()];

        int vmNum = getVmList().size();
        int taskNum = getTaskList().size();
        double[] availableTime = new double[vmNum];
        // cloudlet id starts from 1
        double[][] earliestStartTime = new double[taskNum + 1][vmNum];
        double[][] earliestFinishTime = new double[taskNum + 1][vmNum];
        int[] allocation = new int[taskNum + 1];

        List<Task> taskList = new ArrayList(getTaskList());
        List<Task> readyList = new ArrayList<>();
        while (!taskList.isEmpty()) {
            int t;
            t = getTaskList().size();
            Graph g = new Graph(t + 1);
            g.addEdge(0, 1);
            g.addEdge(0, 2);
            g.addEdge(0, 3);
            g.addEdge(0, 4);
            g.addEdge(0, 5);
            readyList.clear();
            int priority = 1000;
            for (Task task : taskList) {
                boolean ready = true;
                for (Task parent : task.getParentList()) {
                    int m = parent.getCloudletId();
                    if (taskList.contains(parent)) {
                        for (Task child : parent.getChildList()) {
                            int n = child.getCloudletId();
                            g.addEdge(m, n);

                        }
                        ready = false;

                    }
                }
                if (ready) {
                    readyList.add(task);
                    task.setPriority(priority--);
                }
            }
            ArrayList<Integer> best_path = new ArrayList<Integer>();
            best_path.add(0);

            ArrayList<Integer> m = new ArrayList<Integer>();
            m = g.childs(0);
            m = g.bpath(0, g);
            t = m.get(m.size() - 1);
            System.out.println(g.bpath(0, g) + "\n\n\n\n\n\n");
            while (t != 0) {
                g.removeEdge(t);

                m = g.bpath(0, g);
                for (int i = 0; i < m.size(); i++) {
                    Task temp = gettask(m.get(i) + 1);
                    Cloudlet c = (Task) temp;
                    if (temp.getPriority() == 0) {
                        int y = priority;
                        temp.setPriority(y);
                        c.setCloudletPriority(y);
                        priority--;
                    }
                }

                System.out.println(m + "\n\n\n\n\n\n");
                t = m.get(m.size() - 1);

            }

            taskList.removeAll(readyList);

            Collections.sort(readyList, new SortbyPriority());

            // schedule readylist
            for (Task task : readyList) {
                long[] fileSizes = new long[task.getParentList().size()];
                int parentIndex = 0;
                for (Task parent : task.getParentList()) {
                    long fileSize = 0;
                    for (FileItem file : task.getFileList()) {
                        if (file.getType() == Parameters.FileType.INPUT) {
                            for (FileItem file2 : parent.getFileList()) {
                                if (file2.getType() == Parameters.FileType.OUTPUT
                                        && file2.getName().equals(file.getName())) {
                                    fileSize += file.getSize();
                                }
                            }
                        }
                    }
                    fileSizes[parentIndex] = fileSize;
                    parentIndex++;
                }

                double minTime = Double.MAX_VALUE;
                int minTimeIndex = 0;

                for (int vmIndex = 0; vmIndex < getVmList().size(); vmIndex++) {
                    Vm vm = (Vm) getVmList().get(vmIndex);
                    double startTime = availableTime[vm.getId()];
                    parentIndex = 0;
                    for (Task parent : task.getParentList()) {
                        int allocatedVmId = allocation[parent.getCloudletId()];
                        double actualFinishTime = earliestFinishTime[parent.getCloudletId()][allocatedVmId];
                        double communicationTime = fileSizes[parentIndex] / bandwidths[allocatedVmId][vm.getId()];

                        if (actualFinishTime + communicationTime > startTime) {
                            startTime = actualFinishTime + communicationTime;
                        }
                        parentIndex++;
                    }
                    earliestStartTime[task.getCloudletId()][vm.getId()] = startTime;
                    double runtime = task.getCloudletLength() / vm.getMips();
                    earliestFinishTime[task.getCloudletId()][vm.getId()] = runtime + startTime;

                    if (runtime + startTime < minTime) {
                        minTime = runtime + startTime;
                        minTimeIndex = vmIndex;
                    }
                }

                allocation[task.getCloudletId()] = minTimeIndex;// we do not really need it use task.getVmId
                task.setVmId(minTimeIndex);
                availableTime[minTimeIndex] = minTime;
            }
        }

    }

}
