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


/**
 * 
 * @author Tianwei Zhang
 * 
 */
public class Main {

	public static void main(String[] args) {

		final int CLOUD_SERVER_NUM = 10;
		final int CLOUD_SECURITY_NUM = 4;
		final double CLOUD_T_MIN = 0.0;
		final double CLOUD_T_MAX = 20.0;
		final double CLOUD_ST_MIN = 0.0;
		final double CLOUD_ST_MAX = 5.0;
		final List <Double> CLOUD_MEMORY_COST = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0);
		final List <Double> CLOUD_DISK_COST = Arrays.asList(0.01, 0.02, 0.03, 0.04, 0.05, 0.06, 0.07, 0.08, 0.09, 0.1);
		final List <Double> CLOUD_NETWORK_COST = Arrays.asList(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0);
		final double CLOUD_MIGRATION_COST = 1000000.0;
		final List <Integer> CLOUD_MEMORY_SIZE = Arrays.asList(1024, 2048, 4096, 8192);
		final List <Integer> CLOUD_DISK_SIZE = Arrays.asList(1024, 2048, 4096);
		final List <Integer> CLOUD_NETWORK_SIZE = Arrays.asList(10, 20, 30, 40);
	
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

//		cloud.display_server();

		Scanner in;

		int vmid = 0;

		int res;
		
		Optimization optimizer = new Optimization ();

		cloud.display_server();

		System.out.println("******************** Simulation begin ********************");

		while (true) {
			System.out.println("Select Actions:");
			System.out.println("1. Launch VMs;");
			System.out.println("2. Terminate VMs;");
			System.out.println("3. Suspend VMs");
			System.out.println("4. Resume VMs");
			System.out.println("5. Change VM requirements;");


			in = new Scanner(System.in);
			double selection = in.nextInt();
			if (selection == 1) {

				System.out.println("Event time:");
				in = new Scanner(System.in);
				double time = in.nextDouble();

				System.out.println("VM number:");
				in = new Scanner(System.in);
				int vm_num = in.nextInt();

				System.out.println("Memory size:");
				in = new Scanner(System.in);
				int memory_size = in.nextInt();
		
				System.out.println("Disk size:");
				in = new Scanner(System.in);
				int disk_size = in.nextInt();
		
				System.out.println("Network size:");
				in = new Scanner(System.in);
				int network_size = in.nextInt();
		
				System.out.println("Security Level:");
				in = new Scanner(System.in);
				int security_level = in.nextInt();

				System.out.println("Begin VM Launching......");

				optimizer.energy_update(cloud, time);

				for (int i = 0; i < vm_num; i++) {
					VM vm = new VM(vmid, memory_size, disk_size, network_size, security_level, time);
					vmid = vmid + 1;
					res = optimizer.optimization_launch(cloud, vm, time);
					if (res == -1) {
						System.out.println("Failed to launch VMs");
					}
				}
				cloud.display_server();
			}

			if (selection == 2) {
				System.out.println("Event time:");
				in = new Scanner(System.in);
				double time = in.nextDouble();

				System.out.println("VM ID:");
				in = new Scanner(System.in);
				int vm_id = in.nextInt();
				
				System.out.println("Begin VM Termination......");
				optimizer.energy_update(cloud, time);
				res = optimizer.optimization_terminate(cloud, vm_id, time);
				if (res == -1) {
					System.out.println("Failed to terminate VMs");
				}
				cloud.display_server();
			}

			if (selection == 3) {
				System.out.println("Event time:");
				in = new Scanner(System.in);
				double time = in.nextDouble();

				System.out.println("VM ID:");
				in = new Scanner(System.in);
				int vm_id = in.nextInt();

				System.out.println("Begin VM Suspension......");
				optimizer.energy_update(cloud,time);
				res = optimizer.optimization_suspend(cloud, vm_id, time);
				if (res == -1) {
					System.out.println("Failed to suspend VMs");
				}
				cloud.display_server();
			}

			if (selection == 4) {
				System.out.println("Event time:");
				in = new Scanner(System.in);
				double time = in.nextDouble();

				System.out.println("VM ID:");
				in = new Scanner(System.in);
				int vm_id = in.nextInt();

				System.out.println("Begin VM Resumption......");
				optimizer.energy_update(cloud,time);
				res = optimizer.optimization_resume(cloud, vm_id, time);
				if (res == -1) {
					System.out.println("Failed to resume VMs");
				}
				cloud.display_server();
			}

			if (selection == 5) {
				System.out.println("Event time:");
				in = new Scanner(System.in);
				double time = in.nextDouble();

				System.out.println("VM ID:");
				in = new Scanner(System.in);
				int vm_id = in.nextInt();

				System.out.println("Memory size:");
				in = new Scanner(System.in);
				int memory_size = in.nextInt();
		
				System.out.println("Disk size:");
				in = new Scanner(System.in);
				int disk_size = in.nextInt();
		
				System.out.println("Network size:");
				in = new Scanner(System.in);
				int network_size = in.nextInt();
		
				System.out.println("Security Level:");
				in = new Scanner(System.in);
				int security_level = in.nextInt();

				System.out.println("Begin VM Requirement Changing......");
				optimizer.energy_update(cloud, time);
				res = optimizer.optimization_change(cloud, vm_id, memory_size, disk_size, network_size, security_level, time);
				if (res == -1) {
					System.out.println("Failed to change requirements");
				}
				cloud.display_server();
				
			}
		}
	}
}
