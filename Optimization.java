////////// Important! Timing and Cost model is wrong. You need to re-consider them.




/*
 * Title:        Allocation Algorithm.
 * Description:  Make decisions to select servers.
 */

import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.lang.Math;
import java.util.Comparator;
import java.util.Collections;

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
public class Optimization {

	/**
 	 * 
 	 * optimization tries to find the best server to locate the machines
 	 * 
 	 */
	public int optimization(Cloud cloud, VM vm, double t_event) {

		// Make sure static_cost and dynamic_cost is large enough.
		double static_cost = 100000000.0;
		double dynamic_cost = 100000000.0;
		int static_index = -1;
		int dynamic_index = -1;

		int res = -1;

		Request cur_request = vm.vm_request;

		for (int i = 0; i < cloud.server_num; i ++) {
			Server cur_server = cloud.server_list.get(i);
			if (cur_request.security_level <= cur_server.security_level) {

				int stay_memory = cur_request.memory_size;
				int stay_disk = cur_request.disk_size;
				int stay_network = cur_request.network_size;
				for(VM cur_vm:cur_server.vm_list){
					if (cur_vm.vm_request.memory_size >= cur_request.memory_size) {
						stay_memory = stay_memory + cur_vm.vm_request.memory_size;
						stay_disk = stay_disk + cur_vm.vm_request.disk_size;
						stay_network = stay_network + cur_vm.vm_request.network_size;
					}
				}

				double cost = ( cur_request.memory_size * cur_server.memory_cost + 
					        cur_request.disk_size * cur_server.disk_cost + 
					        cur_request.network_size * cur_server.network_cost ) * ((cloud.t_max + cloud.t_min)/2 - (vm.vm_runtime + t_event - vm.vm_resumetime));

				if ( (stay_memory <= cur_server.memory_size) && 
				     (stay_disk <= cur_server.disk_size) &&
				     (stay_network <= cur_server.network_size) ) {
			
					if (cost < dynamic_cost) {
						dynamic_cost = cost;
						dynamic_index = i;
//						try {
//							PrintWriter writer = new PrintWriter("log.txt", "UTF-8");
//							writer.println("stay_memory:	" + stay_memory);
//							writer.println("server_memory:	" + cur_server.memory_size);
//							writer.println("server_id:	" + cur_server.server_id);
//							writer.println("dynamic_index:	" + dynamic_index);
//							writer.close();
//						} catch ( IOException e ) {
//							e.printStackTrace();
//						}
					}
				}

				if ( (cur_request.memory_size <= (cur_server.memory_size - cur_server.memory_usage)) &&
				     (cur_request.disk_size <= (cur_server.disk_size - cur_server.disk_usage)) &&
				     (cur_request.network_size <= (cur_server.network_size - cur_server.network_usage)) ) {
				
					if (cost < static_cost) {
						static_cost = cost;
						static_index = i;
					}
				}
			}
		}
		
		
		// This cloud system is full and no available server can satisfy the VM any more.
		if (dynamic_index == -1) {
			return -1;
		}

		// No migration is needed to allocate the VM.
		if (static_index == dynamic_index) {
			cloud.total_cost = cloud.total_cost + static_cost;
			Server static_server = cloud.server_list.get(static_index);
			static_server.memory_usage = static_server.memory_usage + cur_request.memory_size;
			static_server.disk_usage = static_server.disk_usage + cur_request.disk_size;
			static_server.network_usage = static_server.network_usage + cur_request.network_size;
			static_server.vm_list.add(vm);
	
			return 0;
		}

		// Migration is needed to allocate the VM
		if (static_index == -1) {
			Cloud cloud_dynamic = (Cloud) cloud.clone();
			Server dynamic_server = cloud_dynamic.server_list.get(dynamic_index);
			cloud_dynamic.total_cost = cloud_dynamic.total_cost + dynamic_cost;
			dynamic_server.memory_usage = dynamic_server.memory_usage + cur_request.memory_size;
			dynamic_server.disk_usage = dynamic_server.disk_usage + cur_request.disk_size;
			dynamic_server.network_usage = dynamic_server.network_usage + cur_request.network_size;
			dynamic_server.vm_list.add(vm);

			while (true) {
				Collections.sort(dynamic_server.vm_list);
				VM cur_vm = dynamic_server.vm_list.get(0);
				if ( (dynamic_server.memory_size < dynamic_server.memory_usage) ||
				     (dynamic_server.disk_size < dynamic_server.disk_usage) ||
				     (dynamic_server.network_size < dynamic_server.network_usage) ) {
					dynamic_server.memory_usage = dynamic_server.memory_usage - cur_vm.vm_request.memory_size;
					dynamic_server.disk_usage = dynamic_server.disk_usage - cur_vm.vm_request.disk_size;
					dynamic_server.network_usage = dynamic_server.network_usage - cur_vm.vm_request.network_size;
					dynamic_server.vm_list.remove(cur_vm);
	
					double cur_vm_cost = ( cur_vm.vm_request.memory_size * dynamic_server.memory_cost + 
							       cur_vm.vm_request.disk_size * dynamic_server.disk_cost + 
							       cur_vm.vm_request.network_size * dynamic_server.network_cost ) * ((cloud.t_max + cloud.t_min)/2 - (cur_vm.vm_runtime + t_event - vm.vm_resumetime));
					cloud_dynamic.total_cost = cloud_dynamic.total_cost - cur_vm_cost + cur_vm.vm_request.memory_size * dynamic_server.migration_cost;

					res = optimization(cloud_dynamic, cur_vm, t_event);
					if (res == -1) {
						return -1;
					}
				}
				else {
					break;
				}
			}
			cloud = (Cloud) cloud_dynamic.clone();
			return 1;
		
		}

		if (static_index != dynamic_index) {

			System.out.println("static cost: " + static_cost);
			System.out.println("dynamic cost: " + dynamic_cost);

			Cloud cloud_static = (Cloud) cloud.clone();
			cloud_static.total_cost = cloud_static.total_cost + static_cost;
			Server static_server = cloud_static.server_list.get(static_index);
			static_server.memory_usage = static_server.memory_usage + cur_request.memory_size;
			static_server.disk_usage = static_server.disk_usage + cur_request.disk_size;
			static_server.network_usage = static_server.network_usage + cur_request.network_size;
			static_server.vm_list.add(vm);

			Cloud cloud_dynamic = (Cloud) cloud.clone();
			Server dynamic_server = cloud_dynamic.server_list.get(dynamic_index);
			cloud_dynamic.total_cost = cloud_dynamic.total_cost + dynamic_cost;
			dynamic_server.memory_usage = dynamic_server.memory_usage + cur_request.memory_size;
			dynamic_server.disk_usage = dynamic_server.disk_usage + cur_request.disk_size;
			dynamic_server.network_usage = dynamic_server.network_usage + cur_request.network_size;
			dynamic_server.vm_list.add(vm);

			while (true) {
				Collections.sort(dynamic_server.vm_list);
				VM cur_vm = dynamic_server.vm_list.get(0);
				if ( (dynamic_server.memory_size < dynamic_server.memory_usage) ||
				     (dynamic_server.disk_size < dynamic_server.disk_usage) ||
				     (dynamic_server.network_size < dynamic_server.network_usage) ) {
					dynamic_server.memory_usage = dynamic_server.memory_usage - cur_vm.vm_request.memory_size;
					dynamic_server.disk_usage = dynamic_server.disk_usage - cur_vm.vm_request.disk_size;
					dynamic_server.network_usage = dynamic_server.network_usage - cur_vm.vm_request.network_size;
					dynamic_server.vm_list.remove(cur_vm);

					double cur_vm_cost = ( cur_vm.vm_request.memory_size * dynamic_server.memory_cost + 
							       cur_vm.vm_request.disk_size * dynamic_server.disk_cost + 
							       cur_vm.vm_request.network_size * dynamic_server.network_cost ) * ((cloud.t_max + cloud.t_min)/2 - (cur_vm.vm_runtime + t_event - vm.vm_resumetime));
					cloud_dynamic.total_cost = cloud_dynamic.total_cost - cur_vm_cost + cur_vm.vm_request.memory_size * dynamic_server.migration_cost;

					res = optimization(cloud_dynamic, cur_vm, t_event);

					if (res == -1) {
						break;
					}
				}
				else {
					break;
				}
			}

			System.out.println("static cost: " + cloud_static.total_cost);
			System.out.println("dynamic cost: " + cloud_dynamic.total_cost);

			System.out.println("static cloud:");
			cloud_static.display_server();
			System.out.println("dynamic cloud:");
			cloud_dynamic.display_server();
			

			if ((res == -1) || (cloud_static.total_cost <= cloud_dynamic.total_cost)) {
				
				cloud = (Cloud) cloud_static.clone();
				return 0;
			}
			else {
				cloud = (Cloud) cloud_dynamic.clone();
				return 1;
			}
		}
		return -1;
	}
}
