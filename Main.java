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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;


/**
 * 
 * @author Tianwei Zhang
 * 
 */
public class Main {

	public static void main(String[] args) {

		final int CLOUD_SERVER_NUM = 100;
		final int CLOUD_SECURITY_NUM = 4;
		final int CLOUD_T_MIN = 10;
		final int CLOUD_T_MAX = 10;
		final int CLOUD_ST_MIN = 0;
		final int CLOUD_ST_MAX = 1;
		final List <Double> CLOUD_MEMORY_COST = Arrays.asList(1.0, 2.0, 4.0, 8.0, 16.0, 32.0, 64.0, 128.0, 256.0, 512.0);
		final List <Double> CLOUD_DISK_COST = Arrays.asList(0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.0);
		final List <Double> CLOUD_NETWORK_COST = Arrays.asList(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
		final double CLOUD_MIGRATION_COST = 0.0;
//		final List <Integer> CLOUD_MEMORY_SIZE = Arrays.asList(4096, 8192, 16384, 32768, 65536);
		final List <Integer> CLOUD_MEMORY_SIZE = Arrays.asList(1024, 2048, 4096, 8192, 16384);
		final List <Integer> CLOUD_DISK_SIZE = Arrays.asList(1024, 2048, 4096);
		final List <Integer> CLOUD_NETWORK_SIZE = Arrays.asList(100, 200, 300, 400);

//		List <Integer> VM_MEMORY_SIZE = Arrays.asList(64, 128, 256, 512, 1024, 2048, 4096); //MB
		List <Integer> VM_MEMORY_SIZE = Arrays.asList(1024, 2048, 4096); //MB
		List <Integer> VM_DISK_SIZE = Arrays.asList(0, 1, 20, 40, 80, 160);  //GB
		List <Integer> VM_NETWORK_SIZE = Arrays.asList(1, 2, 3);
		double VM_SUSPEND_PROB = 0.0;
		double VM_CHANGE_PROB = 0.0;
		int VM_LAUNCH_NUM = 10;

		int PERIOD = 1000;

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

		Cloud cloud_random = new Cloud();
		Cloud cloud_bfd = new Cloud();
		cloud_random.copy(cloud);
		cloud_bfd.copy(cloud);		

		Scanner in;

		int vmid = 0;

		int res = 0;
		int res_random = 0;
		int res_bfd = 0;

		int vm_total_num = 0;
		
		Optimization optimizer = new Optimization ();
		Random random = new Random();
		BFD bfd = new BFD(); 

		ArrayList<ArrayList<Activity>> tasks = new ArrayList<ArrayList<Activity>>();

		for (int i=0; i<PERIOD + CLOUD_T_MAX + CLOUD_ST_MAX; i++) {
			ArrayList<Activity> cur_task = new ArrayList<Activity>();
			tasks.add(cur_task);
		}

		System.out.println("******************** Simulation begin ********************");

		for (int i=0; i<PERIOD; i++) {
//			int vm_launch_num = (int)(Math.random() * VM_LAUNCH_NUM);
			int vm_launch_num = VM_LAUNCH_NUM;
			vm_total_num = vm_total_num + vm_launch_num;
			for (int j=0; j<vm_launch_num; j++) {
				int memory_size = VM_MEMORY_SIZE.get((int)(Math.random()*VM_MEMORY_SIZE.size()));
				int disk_size = VM_DISK_SIZE.get((int)(Math.random()*VM_DISK_SIZE.size()));
				int network_size = VM_NETWORK_SIZE.get((int)(Math.random()*VM_NETWORK_SIZE.size()));
//				int security_level = (int)(Math.random()*CLOUD_SECURITY_NUM);
				int security_level = 0;

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
				tasks.get(vm_terminate).add(terminate_activity);
				vmid = vmid + 1;
			}
		}

		for (int i=0; i<PERIOD + CLOUD_T_MAX + CLOUD_ST_MAX; i++) {
			optimizer.energy_update(cloud, i);
			random.energy_update(cloud_random, i);
			bfd.energy_update(cloud_bfd, i);
			for (Activity cur_activity:tasks.get(i)) {
				Activity cur_activity_random = new Activity();
				cur_activity_random.copy(cur_activity);
				Activity cur_activity_bfd = new Activity();
				cur_activity_bfd.copy(cur_activity);

				if (cur_activity.action == 0) {
					res = optimizer.optimization_launch(cloud, cur_activity.vm, i);
					res_random = random.random_launch(cloud_random, cur_activity_random.vm, i);
					res_bfd = bfd.bfd_launch(cloud_bfd, cur_activity_bfd.vm, i);
					
					if (res == -1) {
						System.out.println("Failed to launch VMs for Optimization");
						break;
					}
					if (res_random == -1) {
						System.out.println("Failed to launch VMs for Random");
						break;
					}
					if (res_bfd == -1) {
						System.out.println("Failed to launch VMs for BFD");
						break;
					}
				}

				if (cur_activity.action == 1) {
					res = optimizer.optimization_terminate(cloud, cur_activity.vm.vm_id, i);
					res_random = random.random_terminate(cloud_random, cur_activity_random.vm.vm_id, i);
					res_bfd = bfd.bfd_terminate(cloud_bfd, cur_activity_bfd.vm.vm_id, i);
					if (res == -1) {
						System.out.println("Failed to terminate VMs for Optimization");
						break;
					}
					if (res_random == -1) {
						System.out.println("Failed to terminate VMs for Random");
						break;
					}
					if (res_bfd == -1) {
						System.out.println("Failed to terminate VMs for BFD");
						break;
					}
				}

				if (cur_activity.action == 2) {
					res = optimizer.optimization_suspend(cloud, cur_activity.vm.vm_id, i);
					res_random = random.random_suspend(cloud_random, cur_activity_random.vm.vm_id, i);
					res_bfd = bfd.bfd_suspend(cloud_bfd, cur_activity_bfd.vm.vm_id, i);
					if (res == -1) {
						System.out.println("Failed to suspend VMs for Optimization");
						break;
					}
					if (res_random == -1) {
						System.out.println("Failed to suspend VMs for Random");
						break;
					}
					if (res_bfd == -1) {
						System.out.println("Failed to suspend VMs for BFD");
						break;
					}
				}

				if (cur_activity.action == 3) {
					res = optimizer.optimization_resume(cloud, cur_activity.vm.vm_id, i);
					res_random = random.random_resume(cloud_random, cur_activity_random.vm.vm_id, i);
					res_bfd = bfd.bfd_resume(cloud_bfd, cur_activity_bfd.vm.vm_id, i);
					if (res == -1) {
						System.out.println("Failed to resume VMs for Optimization");
						break;
					}
					if (res_random == -1) {
						System.out.println("Failed to resume VMs for Random");
						break;
					}
					if (res_bfd == -1) {
						System.out.println("Failed to resume VMs for BFD");
						break;
					}
				}

				if (cur_activity.action == 4) {
					res = optimizer.optimization_change(cloud, cur_activity.vm.vm_id, cur_activity.request, i);
					res_random = random.random_change(cloud_random, cur_activity_random.vm.vm_id, cur_activity_random.request, i);
					res_bfd = bfd.bfd_change(cloud_bfd, cur_activity_bfd.vm.vm_id, cur_activity_bfd.request, i);
					if (res == -1) {
						System.out.println("Failed to change VMs for Optimization");
						break;
					}
					if (res_random == -1) {
						System.out.println("Failed to change VMs for Random");
						break;
					}
					if (res_bfd == -1) {
						System.out.println("Failed to change VMs for BFD");
						break;
					}
				}

			}
			if ((res == -1)||(res_random == -1)||(res_bfd == -1)) {
				System.out.println("******************** Simulation Abort ********************");
				break;
			}
		}

		if (res != -1) {
			try {
				PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter("result.txt", true)));
				writer.println("optimization	" + vm_total_num + "	" + cloud.total_cost + "	" + cloud.total_cost/vm_total_num);
				writer.println("random	" + vm_total_num + "	" + cloud_random.total_cost + "	" + cloud_random.total_cost/vm_total_num);
				writer.println("bfd	" + vm_total_num + "	" + cloud_bfd.total_cost + "	" + cloud_bfd.total_cost/vm_total_num);
				writer.close();
			} catch (IOException e) {
			}
			System.out.println("******************** Simulation Finished ********************");
		}

	}
}
