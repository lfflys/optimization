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
	 * energy_update tries to update the total_cost and expect_cost in the cloud
	 * right now, it does not consider the event of resumation or termination.
	 */

	public void energy_update(Cloud cloud, double t_event) {

		for (Server cur_server: cloud.server_list) {
			for (VM cur_vm: cur_server.vm_list) {
				double cur_cost = ( cur_vm.vm_request.memory_size * cur_server.memory_cost +
						    cur_vm.vm_request.disk_size * cur_server.disk_cost +
						    cur_vm.vm_request.network_size * cur_server.network_cost ) * (t_event - cur_vm.vm_resumetime);
				cloud.total_cost = cloud.total_cost + cur_cost;

				cur_vm.vm_runtime = cur_vm.vm_runtime + t_event - cur_vm.vm_resumetime;
				cur_vm.vm_resumetime = t_event;
			}
		}

		cloud.expect_cost = 0;
	}

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

				double expect_time = vm.vm_runtime < cloud.t_min ? (cloud.t_max + cloud.t_min - 2 * vm.vm_runtime)/2 : (cloud.t_max - vm.vm_runtime)/2;
				double cost = ( cur_request.memory_size * cur_server.memory_cost + 
					        cur_request.disk_size * cur_server.disk_cost + 
					        cur_request.network_size * cur_server.network_cost ) * expect_time;

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
			cloud.expect_cost = cloud.expect_cost + static_cost;
			Server static_server = cloud.server_list.get(static_index);
			static_server.memory_usage = static_server.memory_usage + cur_request.memory_size;
			static_server.disk_usage = static_server.disk_usage + cur_request.disk_size;
			static_server.network_usage = static_server.network_usage + cur_request.network_size;
			static_server.vm_list.add(vm);
	
			return 0;
		}

		// Migration is needed to allocate the VM
		if (static_index == -1) {
			Server dynamic_server = cloud.server_list.get(dynamic_index);
			cloud.expect_cost = cloud.expect_cost + dynamic_cost;
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
	
					double expect_time = cur_vm.vm_runtime < cloud.t_min ? (cloud.t_max + cloud.t_min - 2 * cur_vm.vm_runtime)/2 : (cloud.t_max - cur_vm.vm_runtime)/2;
					double cur_vm_cost = ( cur_vm.vm_request.memory_size * dynamic_server.memory_cost + 
							       cur_vm.vm_request.disk_size * dynamic_server.disk_cost + 
							       cur_vm.vm_request.network_size * dynamic_server.network_cost ) * expect_time;

					cloud.expect_cost = cloud.expect_cost - cur_vm_cost + dynamic_server.migration_cost * cur_vm.vm_request.memory_size;;
					cloud.total_cost = cloud.total_cost + dynamic_server.migration_cost * cur_vm.vm_request.memory_size;;

					res = optimization(cloud, cur_vm, t_event);
					if (res == -1) {
						return -1;
					}
				}
				else {
					break;
				}
			}
			return 1;
		
		}

		if (static_index != dynamic_index) {

			Cloud cloud_static = new Cloud();
			cloud_static.copy(cloud);

			cloud_static.expect_cost = cloud_static.expect_cost + static_cost;
			Server static_server = cloud_static.server_list.get(static_index);
			static_server.memory_usage = static_server.memory_usage + cur_request.memory_size;
			static_server.disk_usage = static_server.disk_usage + cur_request.disk_size;
			static_server.network_usage = static_server.network_usage + cur_request.network_size;
			static_server.vm_list.add(vm);

			Cloud cloud_dynamic = new Cloud();
			cloud_dynamic.copy(cloud);

			Server dynamic_server = cloud_dynamic.server_list.get(dynamic_index);
			cloud_dynamic.expect_cost = cloud_dynamic.expect_cost + dynamic_cost;
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

					double expect_time = cur_vm.vm_runtime < cloud_dynamic.t_min ? (cloud_dynamic.t_max + cloud_dynamic.t_min - 2 * cur_vm.vm_runtime)/2 : (cloud_dynamic.t_max - cur_vm.vm_runtime)/2;
					double cur_vm_cost = ( cur_vm.vm_request.memory_size * dynamic_server.memory_cost + 
							       cur_vm.vm_request.disk_size * dynamic_server.disk_cost + 
							       cur_vm.vm_request.network_size * dynamic_server.network_cost ) * expect_time;

					cloud_dynamic.expect_cost = cloud_dynamic.expect_cost - cur_vm_cost + dynamic_server.migration_cost * cur_vm.vm_request.memory_size;;
					cloud_dynamic.total_cost = cloud_dynamic.total_cost + dynamic_server.migration_cost * cur_vm.vm_request.memory_size;;

					res = optimization(cloud_dynamic, cur_vm, t_event);

					if (res == -1) {
						break;
					}
				}
				else {
					break;
				}
			}

			System.out.println("static cost:	" + cloud_static.expect_cost);
			System.out.println("dynamic cost:	" + cloud_dynamic.expect_cost);
			if ((res == -1) || (cloud_static.expect_cost <= cloud_dynamic.expect_cost)) {
				cloud.copy(cloud_static);
				return 0;
			}
			else {
				cloud.copy(cloud_dynamic);
				return 1;
			}
		}
		return -1;
	}
}
