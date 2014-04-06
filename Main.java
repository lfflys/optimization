/*
 * Title:        VM PLACEMENT
 * Description:  Optimization algorithm for allocating on-demand VMs to realize the energy-efficiency goal.
 *
 */

import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Comparator;
import java.util.Collections;
import java.util.Scanner;
import java.lang.Math;

/**
 * 
 * @author Tianwei Zhang
 * 
 */
public class Main {

	public static void main(String[] args) {

		final int CLOUD_SERVER_NUM = 20;
		final int CLOUD_SECURITY_NUM = 4;
		final int CLOUD_T_MIN = 0;
		final int CLOUD_T_MAX = 2;
		final int CLOUD_ST_MIN = 0;
		final int CLOUD_ST_MAX = 1;
		final List <Double> CLOUD_MEMORY_COST = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0);
		final List <Double> CLOUD_DISK_COST = Arrays.asList(0.01, 0.02, 0.03, 0.04, 0.05, 0.06, 0.07, 0.08, 0.09, 0.1);
		final List <Double> CLOUD_NETWORK_COST = Arrays.asList(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0);
		final double CLOUD_MIGRATION_COST = 1000000.0;
		final List <Integer> CLOUD_MEMORY_SIZE = Arrays.asList(1024, 2048, 4096, 8192);
		final List <Integer> CLOUD_DISK_SIZE = Arrays.asList(1024, 2048, 4096);
		final List <Integer> CLOUD_NETWORK_SIZE = Arrays.asList(10, 20, 30, 40);

		List <Integer> VM_MEMORY_SIZE = Arrays.asList(512, 1024, 2048);
		List <Integer> VM_DISK_SIZE = Arrays.asList(512, 1024);
		List <Integer> VM_NETWORK_SIZE = Arrays.asList(1, 2, 3);
		double VM_SUSPEND_PROB = 0.1;
		double VM_CHANGE_PROB = 0.05;
		int VM_LAUNCH_NUM = 3;

		int PERIOD = 10;

		Cloud cloud = new Cloud ( 
					CLOUD_SERVER_NUM, 
					CLOUD_SECURITY_NUM,
					CLOUD_T_MIN,
					CLOUD_T_MAX,
					CLOUD_ST_MIN,
					CLOUD_ST_MAX,
					CLOUD_MEMORY_COST, 
					CLOUD_DISK_COST, 
					CLOUD_NETWORK_COST, 
					CLOUD_MIGRATION_COST,
					CLOUD_MEMORY_SIZE,
					CLOUD_DISK_SIZE,
					CLOUD_NETWORK_SIZE);

		Scanner in;

		int vmid = 0;

		int res = 0;
		
		Optimization optimizer = new Optimization ();

		ArrayList<ArrayList<Activity>> tasks = new ArrayList<ArrayList<Activity>>();

		for (int i=0; i<PERIOD + (CLOUD_T_MAX - CLOUD_T_MIN) + (CLOUD_ST_MAX - CLOUD_ST_MIN); i++) {
			ArrayList<Activity> cur_task = new ArrayList<Activity>();
			tasks.add(cur_task);
		}

		System.out.println("******************** Simulation begin ********************");

		for (int i=0; i<PERIOD; i++) {
			int vm_launch_num = (int)(Math.random() * VM_LAUNCH_NUM);
			for (int j=0; j<vm_launch_num; j++) {
				int memory_size = VM_MEMORY_SIZE.get((int)(Math.random()*VM_MEMORY_SIZE.size()));
				int disk_size = VM_DISK_SIZE.get((int)(Math.random()*VM_DISK_SIZE.size()));
				int network_size = VM_NETWORK_SIZE.get((int)(Math.random()*VM_NETWORK_SIZE.size()));
				int security_level = (int)(Math.random()*CLOUD_SECURITY_NUM);

				VM vm = new VM(vmid, memory_size, disk_size, network_size, security_level, i);

				int vm_life = CLOUD_T_MIN + (int)(Math.random()*(CLOUD_T_MAX - CLOUD_T_MIN));
				int vm_launch = i;
				int vm_terminate = i + vm_life;

				int vm_suspend = 0;
				int vm_resume = 0;
				int vm_change = 0;

				int is_suspend = Math.random() < VM_SUSPEND_PROB? 1:0;
				int is_change = Math.random() < VM_CHANGE_PROB? 1:0;


				Activity launch_activity = new Activity(vm_launch, vm, 0, vm.vm_request);
				Activity terminate_activity = new Activity(vm_terminate, vm, 1,vm.vm_request);
				Activity suspend_activity = new Activity(vm_suspend, vm, 2, vm.vm_request);
				Activity resume_activity = new Activity(vm_resume, vm, 3, vm.vm_request);
				Activity change_activity = new Activity(vm_change, vm, 4, vm.vm_request);
				tasks.get(vm_launch).add(launch_activity);

				if (is_suspend == 1) {
					vm_suspend = vm_launch + (int)(Math.random()*vm_life);
					vm_resume = vm_suspend + CLOUD_ST_MIN + (int)(Math.random()*(CLOUD_ST_MAX - CLOUD_ST_MIN));
					vm_terminate = vm_terminate + vm_resume - vm_suspend;

					suspend_activity.event_time = vm_suspend;
					tasks.get(vm_suspend).add(suspend_activity);

					resume_activity.event_time = vm_resume;
					tasks.get(vm_resume).add(resume_activity);
				}

				if (is_change == 1) {
					int new_security_level = (int)(Math.random()*CLOUD_SECURITY_NUM);
					vm_change = vm_launch + (int)(Math.random()*vm_life);
					if (new_security_level != security_level) {
						change_activity.request.security_level = new_security_level;
					}
					tasks.get(vm_change).add(change_activity);
				}
				vmid = vmid + 1;
			}

			optimizer.energy_update(cloud, i);
			for (Activity cur_activity:tasks.get(i)) {
				if (cur_activity.action == 0) {
					res = optimizer.optimization_launch(cloud, cur_activity.vm, i);
					if (res == -1) {
						System.out.println("Failed to launch VMs");
						break;
					}
				}

				if (cur_activity.action == 1) {
					res = optimizer.optimization_terminate(cloud, cur_activity.vm.vm_id, i);
					if (res == -1) {
						System.out.println("Failed to terminate VMs");
						break;
					}
				}

				if (cur_activity.action == 2) {
					res = optimizer.optimization_suspend(cloud, cur_activity.vm.vm_id, i);
					if (res == -1) {
						System.out.println("Failed to suspend VMs");
						break;
					}
				}

				if (cur_activity.action == 3) {
					res = optimizer.optimization_resume(cloud, cur_activity.vm.vm_id, i);
					if (res == -1) {
						System.out.println("Failed to resume VMs");
						break;
					}
				}

				if (cur_activity.action == 4) {
					res = optimizer.optimization_change(cloud, cur_activity.vm.vm_id, cur_activity.request, i);
					if (res == -1) {
						System.out.println("Failed to change VMs");
						break;
					}
				}

			}
			if (res == -1) {
				System.out.println("******************** Simulation Abort ********************");
				break;
			}
		}

		if (res != -1) {
			cloud.display_server();
			System.out.println("******************** Simulation Finished ********************");
		}

	}
}
