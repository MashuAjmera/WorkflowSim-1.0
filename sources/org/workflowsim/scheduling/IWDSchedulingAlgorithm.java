
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
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.workflowsim.CondorVM;
import org.workflowsim.WorkflowSimTags;

/**
 * MaxMin algorithm.
 *
 * @author Weiwei Chen
 * @since WorkflowSim Toolkit 1.0
 * @date Apr 9, 2013
 */
public class IWDSchedulingAlgorithm extends BaseSchedulingAlgorithm {

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

		int[] priority = { 0,949, 993, 937, 966, 946, 901, 953, 968, 970, 915, 961, 932, 963, 972, 902, 909, 903, 910, 918, 925, 934, 911, 919, 935, 943, 950, 956, 964, 973, 912, 904, 921, 927, 938, 944, 920, 907, 922, 974, 975, 928, 929, 940, 947, 952, 942, 926, 951, 906, 939, 954, 905, 913, 955, 914, 976, 957, 916, 958, 923, 930, 960, 948, 959, 924, 945, 917, 933, 992, 941, 967, 962, 971, 936, 965, 969, 931, 908, 991, 1000, 977, 978, 979, 980, 981, 982, 983, 984, 985, 986, 987, 988, 989, 990, 994, 999, 998, 997, 996, 995 };

		// Log.printLine("Schedulin Cycle");
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
				System.out.println(cloudlet.getCloudletId());
				if (!hasChecked.get(j)) {
					maxCloudlet = cloudlet;
					int m = cloudlet.getCloudletId();
					System.out.println("Hello " + m);
					System.out.println("MaxUntilHere " + priority[m]);

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
			Log.printLine("Schedules " + maxCloudlet.getCloudletId() + " with " + maxCloudlet.getCloudletPriority()
					+ " to VM " + firstIdleVm.getId() + " with " + firstIdleVm.getCurrentRequestedTotalMips());

		}
	}
}
