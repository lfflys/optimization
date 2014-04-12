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

	public void energy_update(Cloud cloud, int t_event) {

		double new_cost = 0.0;

		for (Server cur_server: cloud.server_list) {
			for (VM cur_vm: cur_server.vm_list) {
				double cur_cost = 0.0;
				if (cur_vm.vm_state == 1) {
					cur_cost = ( cur_vm.vm_request.memory_size * cur_server.memory_cost +
						     cur_vm.vm_request.disk_size * cur_server.disk_cost +
						     cur_vm.vm_request.network_size * cur_server.network_cost ) * (t_event - cur_vm.vm_lastevent);
					cur_vm.vm_runtime = cur_vm.vm_runtime + t_event - cur_vm.vm_lastevent;
				}
				if (cur_vm.vm_state == 2) {
					cur_cost = cur_vm.vm_request.disk_size * cur_server.disk_cost * (t_event - cur_vm.vm_lastevent);
					cur_vm.vm_suspendtime = cur_vm.vm_suspendtime + t_event - cur_vm.vm_lastevent;
				}
				new_cost = new_cost + cur_cost;
				cur_vm.vm_lastevent = t_event;
			}
		}

		cloud.total_cost = cloud.total_cost + new_cost;
		cloud.expect_cost = 0;
	}

	/**
 	 * 
 	 * optimization_launch tries to find the best server to locate the machines
 	 *
 	 */
	public int optimization_launch(Cloud cloud, VM vm, int t_event) {
		// Make sure static_cost and dynamic_cost is large enough.
		double static_cost = 100000000.0;
		double dynamic_cost = 100000000.0;
		int static_index = -1;
		int dynamic_index = -1;

		int res = -1;

		for (int i = 0; i < cloud.server_num; i ++) {
			
			if ( (vm.vm_state == 1) && (vm.vm_request.security_level <= cloud.server_list.get(i).security_level)) {
				int stay_memory = vm.vm_request.memory_size;
				int stay_disk = vm.vm_request.disk_size;
				int stay_network = vm.vm_request.network_size;
				for(VM cur_vm:cloud.server_list.get(i).vm_list){
					if (cur_vm.vm_request.memory_size >= vm.vm_request.memory_size) {
						stay_memory = stay_memory + cur_vm.vm_request.memory_size;
						stay_disk = stay_disk + cur_vm.vm_request.disk_size;
						stay_network = stay_network + cur_vm.vm_request.network_size;
					}
				}

				int expect_time  = vm.vm_runtime < cloud.t_min ? (cloud.t_max + cloud.t_min - 2 * vm.vm_runtime)/2 : (cloud.t_max - vm.vm_runtime)/2;
				double cost = ( vm.vm_request.memory_size * cloud.server_list.get(i).memory_cost + 
						vm.vm_request.disk_size * cloud.server_list.get(i).disk_cost + 
						vm.vm_request.network_size * cloud.server_list.get(i).network_cost ) * expect_time;
				if ( (stay_memory <= cloud.server_list.get(i).memory_size) && 
				     (stay_disk <= cloud.server_list.get(i).disk_size) &&
				     (stay_network <= cloud.server_list.get(i).network_size) ) {
					if (cost < dynamic_cost) {
						dynamic_cost = cost;
						dynamic_index = i;
					}
				}

				if ( (vm.vm_request.memory_size <= (cloud.server_list.get(i).memory_size - cloud.server_list.get(i).memory_usage)) &&
				     (vm.vm_request.disk_size <= (cloud.server_list.get(i).disk_size - cloud.server_list.get(i).disk_usage)) &&
				     (vm.vm_request.network_size <= (cloud.server_list.get(i).network_size - cloud.server_list.get(i).network_usage)) ) {
				
					if (cost < static_cost) {
						static_cost = cost;
						static_index = i;
						if (cost == dynamic_cost) {
							dynamic_cost = cost;
							dynamic_index = i;
						}
					}
				}
			}

			if (vm.vm_state == 2) {
				int stay_memory = vm.vm_request.memory_size;
				int stay_disk = vm.vm_request.disk_size;
				int stay_network = vm.vm_request.network_size;
				for(VM cur_vm:cloud.server_list.get(i).vm_list){
					if (cur_vm.vm_request.memory_size >= vm.vm_request.memory_size) {
						stay_memory = stay_memory + cur_vm.vm_request.memory_size;
						stay_disk = stay_disk + cur_vm.vm_request.disk_size;
						stay_network = stay_network + cur_vm.vm_request.network_size;
					}
				}

				int expect_time = vm.vm_suspendtime < cloud.st_min ? (cloud.st_max + cloud.st_min - 2 * vm.vm_suspendtime)/2 : (cloud.st_max - vm.vm_suspendtime)/2;
				double cost = vm.vm_request.disk_size * cloud.server_list.get(i).disk_cost * expect_time;

				if ( (stay_memory <= cloud.server_list.get(i).memory_size) && 
				     (stay_disk <= cloud.server_list.get(i).disk_size) &&
				     (stay_network <= cloud.server_list.get(i).network_size) ) {
					if (cost < dynamic_cost) {
						dynamic_cost = cost;
						dynamic_index = i;
					}
				}

				if (vm.vm_request.disk_size <= (cloud.server_list.get(i).disk_size - cloud.server_list.get(i).disk_usage)) {
					if (cost < static_cost) {
						static_cost = cost;
						static_index = i;
						if (cost == dynamic_cost) {
							dynamic_cost = cost;
							dynamic_index = i;
						}
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
			if (vm.vm_state == 1) {
				cloud.server_list.get(static_index).memory_usage = cloud.server_list.get(static_index).memory_usage + vm.vm_request.memory_size;
				cloud.server_list.get(static_index).disk_usage = cloud.server_list.get(static_index).disk_usage + vm.vm_request.disk_size;
				cloud.server_list.get(static_index).network_usage = cloud.server_list.get(static_index).network_usage + vm.vm_request.network_size;
			}
			if (vm.vm_state == 2) {
				cloud.server_list.get(static_index).disk_usage = cloud.server_list.get(static_index).disk_usage + vm.vm_request.disk_size;
			}
			cloud.server_list.get(static_index).vm_list.add(vm);
	
			return 0;
		}

		// Migration is needed to allocate the VM
		if (static_index == -1) {
			cloud.expect_cost = cloud.expect_cost + dynamic_cost;
	
			if (vm.vm_state == 1) {
				cloud.server_list.get(dynamic_index).memory_usage = cloud.server_list.get(dynamic_index).memory_usage + vm.vm_request.memory_size;
				cloud.server_list.get(dynamic_index).disk_usage = cloud.server_list.get(dynamic_index).disk_usage + vm.vm_request.disk_size;
				cloud.server_list.get(dynamic_index).network_usage = cloud.server_list.get(dynamic_index).network_usage + vm.vm_request.network_size;
			}
			if (vm.vm_state == 2) {
				cloud.server_list.get(dynamic_index).disk_usage = cloud.server_list.get(dynamic_index).disk_usage + vm.vm_request.disk_size;
			}
			cloud.server_list.get(dynamic_index).vm_list.add(vm);

			while (true) {
				Collections.sort(cloud.server_list.get(dynamic_index).vm_list);
				VM cur_vm = cloud.server_list.get(dynamic_index).vm_list.get(0);
				if ( (cloud.server_list.get(dynamic_index).memory_size < cloud.server_list.get(dynamic_index).memory_usage) ||
				     (cloud.server_list.get(dynamic_index).disk_size < cloud.server_list.get(dynamic_index).disk_usage) ||
				     (cloud.server_list.get(dynamic_index).network_size < cloud.server_list.get(dynamic_index).network_usage) ) {
					cloud.server_list.get(dynamic_index).vm_list.remove(cur_vm);
				
					double cur_vm_cost = 0.0;
					int expect_time = 0;
					if (cur_vm.vm_state == 1) {
						cloud.server_list.get(dynamic_index).memory_usage = cloud.server_list.get(dynamic_index).memory_usage - cur_vm.vm_request.memory_size;
						cloud.server_list.get(dynamic_index).disk_usage = cloud.server_list.get(dynamic_index).disk_usage - cur_vm.vm_request.disk_size;
						cloud.server_list.get(dynamic_index).network_usage = cloud.server_list.get(dynamic_index).network_usage - cur_vm.vm_request.network_size;
				
						expect_time = cur_vm.vm_runtime < cloud.t_min ? (cloud.t_max + cloud.t_min - 2 * cur_vm.vm_runtime)/2 : (cloud.t_max - cur_vm.vm_runtime)/2;
						cur_vm_cost = ( cur_vm.vm_request.memory_size * cloud.server_list.get(dynamic_index).memory_cost + 
								cur_vm.vm_request.disk_size * cloud.server_list.get(dynamic_index).disk_cost + 
								cur_vm.vm_request.network_size * cloud.server_list.get(dynamic_index).network_cost ) * expect_time;
					}
					if (cur_vm.vm_state == 2) {
						cloud.server_list.get(dynamic_index).disk_usage = cloud.server_list.get(dynamic_index).disk_usage - cur_vm.vm_request.disk_size;
						expect_time = cur_vm.vm_suspendtime < cloud.st_min ? (cloud.st_max + cloud.st_min - 2 * cur_vm.vm_suspendtime)/2 : (cloud.st_max - cur_vm.vm_suspendtime)/2;
						cur_vm_cost = cur_vm.vm_request.disk_size * cloud.server_list.get(dynamic_index).disk_cost * expect_time;
					}

					cloud.expect_cost = cloud.expect_cost - cur_vm_cost + cloud.server_list.get(dynamic_index).migration_cost * cur_vm.vm_request.memory_size;
					cloud.total_cost = cloud.total_cost + cloud.server_list.get(dynamic_index).migration_cost * cur_vm.vm_request.memory_size;

					res = optimization_launch(cloud, cur_vm, t_event);
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

			Cloud cloud_dynamic = new Cloud();
			cloud_dynamic.copy(cloud);

			cloud_static.expect_cost = cloud_static.expect_cost + static_cost;
			if (vm.vm_state == 1) {
				cloud_static.server_list.get(static_index).memory_usage = cloud_static.server_list.get(static_index).memory_usage + vm.vm_request.memory_size;
				cloud_static.server_list.get(static_index).disk_usage = cloud_static.server_list.get(static_index).disk_usage + vm.vm_request.disk_size;
				cloud_static.server_list.get(static_index).network_usage = cloud_static.server_list.get(static_index).network_usage + vm.vm_request.network_size;
			}
			if (vm.vm_state == 2) {
				cloud_static.server_list.get(static_index).disk_usage = cloud_static.server_list.get(static_index).disk_usage + vm.vm_request.disk_size;
			}
			cloud_static.server_list.get(static_index).vm_list.add(vm);


			cloud_dynamic.expect_cost = cloud_dynamic.expect_cost + dynamic_cost;
			if (vm.vm_state == 1) {
				cloud_dynamic.server_list.get(dynamic_index).memory_usage = cloud_dynamic.server_list.get(dynamic_index).memory_usage + vm.vm_request.memory_size;
				cloud_dynamic.server_list.get(dynamic_index).disk_usage = cloud_dynamic.server_list.get(dynamic_index).disk_usage + vm.vm_request.disk_size;
				cloud_dynamic.server_list.get(dynamic_index).network_usage = cloud_dynamic.server_list.get(dynamic_index).network_usage + vm.vm_request.network_size;
			}
			if (vm.vm_state == 2) {
				cloud_dynamic.server_list.get(dynamic_index).disk_usage = cloud_dynamic.server_list.get(dynamic_index).disk_usage + vm.vm_request.disk_size;
			}
			cloud_dynamic.server_list.get(dynamic_index).vm_list.add(vm);

			while (true) {
				Collections.sort(cloud_dynamic.server_list.get(dynamic_index).vm_list);
				VM cur_vm = cloud_dynamic.server_list.get(dynamic_index).vm_list.get(0);
				if ( (cloud_dynamic.server_list.get(dynamic_index).memory_size < cloud_dynamic.server_list.get(dynamic_index).memory_usage) ||
				     (cloud_dynamic.server_list.get(dynamic_index).disk_size < cloud_dynamic.server_list.get(dynamic_index).disk_usage) ||
				     (cloud_dynamic.server_list.get(dynamic_index).network_size < cloud_dynamic.server_list.get(dynamic_index).network_usage) ) {
					cloud_dynamic.server_list.get(dynamic_index).vm_list.remove(cur_vm);

					double cur_vm_cost = 0.0;
					int expect_time = 0;
					if (cur_vm.vm_state == 1) {
						cloud_dynamic.server_list.get(dynamic_index).memory_usage = cloud_dynamic.server_list.get(dynamic_index).memory_size - cur_vm.vm_request.memory_size;
						cloud_dynamic.server_list.get(dynamic_index).disk_usage = cloud_dynamic.server_list.get(dynamic_index).disk_usage - cur_vm.vm_request.disk_size;
						cloud_dynamic.server_list.get(dynamic_index).network_usage = cloud_dynamic.server_list.get(dynamic_index).network_usage - cur_vm.vm_request.network_size;
						expect_time = cur_vm.vm_runtime < cloud_dynamic.t_min ? (cloud_dynamic.t_max + cloud_dynamic.t_min - 2 * cur_vm.vm_runtime)/2 : (cloud_dynamic.t_max - cur_vm.vm_runtime)/2;
						cur_vm_cost = ( cur_vm.vm_request.memory_size * cloud_dynamic.server_list.get(dynamic_index).memory_cost + 
								cur_vm.vm_request.disk_size * cloud_dynamic.server_list.get(dynamic_index).disk_cost + 
								cur_vm.vm_request.network_size * cloud_dynamic.server_list.get(dynamic_index).network_cost ) * expect_time;
					}
					if (cur_vm.vm_state == 2) {
						cloud_dynamic.server_list.get(dynamic_index).disk_usage = cloud_dynamic.server_list.get(dynamic_index).disk_usage - cur_vm.vm_request.disk_size;
						expect_time = cur_vm.vm_suspendtime < cloud_dynamic.st_min ? (cloud_dynamic.st_max + cloud_dynamic.st_min - 2 * cur_vm.vm_suspendtime)/2 : (cloud_dynamic.st_max - cur_vm.vm_suspendtime)/2;
						cur_vm_cost = cur_vm.vm_request.disk_size * cloud_dynamic.server_list.get(dynamic_index).disk_cost * expect_time;
					}
					
					cloud_dynamic.expect_cost = cloud_dynamic.expect_cost - cur_vm_cost + cloud_dynamic.server_list.get(dynamic_index).migration_cost * cur_vm.vm_request.memory_size;
					cloud_dynamic.total_cost = cloud_dynamic.total_cost + cloud_dynamic.server_list.get(dynamic_index).migration_cost * cur_vm.vm_request.memory_size;
					res = optimization_launch(cloud_dynamic, cur_vm, t_event);

					if (res == -1) {
						break;
					}
				}
				else {
					break;
				}
			}

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


	/**
	 *
	 * optimization_terminate tries to shutdown a VM; 
	 * right now just shutdown without any optimization;
	 * If performs optimization, needs to find other VMs that can migrate to this server;
	 * This is a huge cost: O(vm_num);
	 * Another reason is without optimization, this can leave some bugs for the customer to game.
	 *
	 */
	public int optimization_terminate(Cloud cloud, int vm_id, int t_event) {
		int res = -1;
		for (Server cur_server: cloud.server_list) {
			for (VM cur_vm: cur_server.vm_list) {
				if (cur_vm.vm_id == vm_id){
					if (cur_vm.vm_state == 1) {
						cur_server.memory_usage = cur_server.memory_usage - cur_vm.vm_request.memory_size;
						cur_server.disk_usage = cur_server.disk_usage - cur_vm.vm_request.disk_size;
						cur_server.network_usage = cur_server.network_usage - cur_vm.vm_request.network_size;
					}
					if (cur_vm.vm_state == 2) {
						cur_server.disk_usage = cur_server.disk_usage - cur_vm.vm_request.disk_size;
					}		
					cur_server.vm_list.remove(cur_vm);

					res = 0;
					break;
				}
			}
			if (res == 0) {
				break;
			}
		}
		
		return res;
	}

	/**
	 *
	 * optimization_change tries to change the VM's requirements and (or) reallocate them.
	 *
	 */
	public int optimization_change(Cloud cloud, int vm_id, Request request, int t_event) {
		int res = -1;
		int i = 0;
		int j = 0;
		for (i = 0; i < cloud.server_list.size(); i ++) {
			for (j = 0; j < cloud.server_list.get(i).vm_list.size(); j ++) {
				if (cloud.server_list.get(i).vm_list.get(j).vm_id == vm_id) {
					res = 0;
					break;
				}
			}
			if (res == 0) {
				break;
			}
		}

		// cannot find VM with such vm_id
		if (res == -1) {
			return -1;
		}

		VM cur_vm = cloud.server_list.get(i).vm_list.get(j);

		if (cur_vm.vm_state == 1) {
			cloud.server_list.get(i).memory_usage = cloud.server_list.get(i).memory_usage - cur_vm.vm_request.memory_size;
			cloud.server_list.get(i).disk_usage = cloud.server_list.get(i).disk_usage - cur_vm.vm_request.disk_size;
			cloud.server_list.get(i).network_usage = cloud.server_list.get(i).network_usage - cur_vm.vm_request.network_size;
		}
		if (cur_vm.vm_state == 2) {
			cloud.server_list.get(i).disk_usage = cloud.server_list.get(i).disk_usage - cur_vm.vm_request.disk_size;
		}
		cloud.server_list.get(i).vm_list.remove(cur_vm);

		double cur_migration_cost = cur_vm.vm_request.memory_size * cloud.server_list.get(i).migration_cost;
		
		cur_vm.vm_request.memory_size = request.memory_size;
		cur_vm.vm_request.disk_size = request.disk_size;
		cur_vm.vm_request.network_size = request.network_size;
		cur_vm.vm_request.security_level = request.security_level;

		Cloud cloud_dynamic = new Cloud();
		cloud_dynamic.copy(cloud);

		VM vm_dynamic = new VM();
		vm_dynamic.copy(cur_vm);

		int expect_time = 0;
		double cur_vm_cost = 0.0;

		if (cur_vm.vm_state == 1) {
			expect_time = cur_vm.vm_runtime < cloud.t_min ? (cloud.t_max + cloud.t_min - 2 * cur_vm.vm_runtime)/2 : (cloud.t_max - cur_vm.vm_runtime)/2;
			cur_vm_cost = ( cur_vm.vm_request.memory_size * cloud.server_list.get(i).memory_cost + 
				       cur_vm.vm_request.disk_size * cloud.server_list.get(i).disk_cost + 
				       cur_vm.vm_request.network_size * cloud.server_list.get(i).network_cost ) * expect_time;
		}

		if (cur_vm.vm_state == 2) {
			expect_time = cur_vm.vm_suspendtime < cloud.st_min ? (cloud.st_max + cloud.st_min - 2 * cur_vm.vm_suspendtime)/2 : (cloud.st_max - cur_vm.vm_suspendtime)/2;
			cur_vm_cost = cur_vm.vm_request.disk_size * cloud.server_list.get(i).disk_cost * expect_time;
		}

		cloud.expect_cost = cur_vm_cost;
		cloud.server_list.get(i).vm_list.add(cur_vm);
		if (cur_vm.vm_state == 1) {
			cloud.server_list.get(i).memory_usage = cloud.server_list.get(i).memory_usage + cur_vm.vm_request.memory_size;
			cloud.server_list.get(i).disk_usage = cloud.server_list.get(i).disk_usage + cur_vm.vm_request.disk_size;
			cloud.server_list.get(i).network_usage = cloud.server_list.get(i).network_usage + cur_vm.vm_request.network_size;
		}
		if (cur_vm.vm_state == 2) {
			cloud.server_list.get(i).disk_usage = cloud.server_list.get(i).disk_usage + cur_vm.vm_request.disk_size;
		}

		cloud_dynamic.total_cost = cloud_dynamic.total_cost + cur_migration_cost;
		cloud_dynamic.expect_cost = cloud_dynamic.expect_cost + cur_migration_cost;
		res = optimization_launch(cloud_dynamic, vm_dynamic, t_event);

		// the original server cannot satisfy the vm's requirement any more.
		if ((cur_vm.vm_state == 1)&&((cur_vm.vm_request.memory_size > (cloud.server_list.get(i).memory_size - cloud.server_list.get(i).memory_usage))||
					     (cur_vm.vm_request.disk_size > (cloud.server_list.get(i).disk_size - cloud.server_list.get(i).disk_usage))||   
					     (cur_vm.vm_request.network_size > (cloud.server_list.get(i).network_size - cloud.server_list.get(i).network_size))||
					     (cur_vm.vm_request.security_level > cloud.server_list.get(i).security_level))) {
			if (res == -1) {
				return -1;
			}
			cloud.copy(cloud_dynamic);
			return 0;
		}
		if ((cur_vm.vm_state == 2)&&(cur_vm.vm_request.disk_size > (cloud.server_list.get(i).disk_size - cloud.server_list.get(i).disk_usage))) {
			if (res == -1) {
				return -1;
			}
			cloud.copy(cloud_dynamic);
			return 0;
		}

		// the original server can still satisfy the vm's requirement, need to consider whether to migrate or not.

		if ((res == -1)||(cloud.expect_cost < cloud_dynamic.expect_cost)) {
			return 0;
		}
		else {
			cloud.copy(cloud_dynamic);
			return 0;
		}

	}

	public int optimization_suspend(Cloud cloud, int vm_id, int t_event) {
		int res = -1;
		int i = 0;
		int j = 0;
		for (i = 0; i < cloud.server_list.size(); i ++) {
			for (j = 0; j < cloud.server_list.get(i).vm_list.size(); j ++) {
				if ((cloud.server_list.get(i).vm_list.get(j).vm_id == vm_id) && (cloud.server_list.get(i).vm_list.get(j).vm_state == 1)) {
					res = 0;
					break;
				}
			}
			if (res == 0) {
				break;
			}
		}

		if (res == -1) {
			return -1;
		}

		VM cur_vm = cloud.server_list.get(i).vm_list.get(j);

		double cur_migration_cost = cur_vm.vm_request.memory_size * cloud.server_list.get(i).migration_cost;

		cloud.server_list.get(i).vm_list.remove(cur_vm);
		cur_vm.vm_state = 2;
		cloud.server_list.get(i).memory_usage = cloud.server_list.get(i).memory_usage - cur_vm.vm_request.memory_size;
		cloud.server_list.get(i).network_usage = cloud.server_list.get(i).network_usage - cur_vm.vm_request.network_size;
		cur_vm.vm_suspendtime = 0;

		Cloud cloud_dynamic = new Cloud();
		cloud_dynamic.copy(cloud);

		VM vm_dynamic = new VM();
		vm_dynamic.copy(cur_vm);

		cloud.expect_cost = cur_vm.vm_request.disk_size * cloud.server_list.get(i).disk_cost * (cloud.st_max + cloud.st_min)/2;
		cloud.server_list.get(i).vm_list.add(cur_vm);

		cloud_dynamic.total_cost = cloud_dynamic.total_cost + cur_migration_cost;
		cloud_dynamic.expect_cost = cloud_dynamic.expect_cost + cur_migration_cost;

		res = optimization_launch(cloud_dynamic, vm_dynamic, t_event);

		if ((res == -1)||(cloud.expect_cost < cloud_dynamic.expect_cost)) {
			return 0;
		}
		else {
			cloud.copy(cloud_dynamic);
			return 0;
		}
	}

	public int optimization_resume(Cloud cloud, int vm_id, int t_event) {
		int res = -1;
		int i = 0;
		int j = 0;
		for (i = 0; i < cloud.server_list.size(); i ++) {
			for (j = 0; j < cloud.server_list.get(i).vm_list.size(); j ++) {
				if ((cloud.server_list.get(i).vm_list.get(j).vm_id == vm_id) && (cloud.server_list.get(i).vm_list.get(j).vm_state == 2)) {
					res = 0;
					break;
				}
			}
			if (res == 0) {
				break;
			}
			
		}

		if (res == -1) {
			return -1;
		}

		VM cur_vm = cloud.server_list.get(i).vm_list.get(j);

		double cur_migration_cost = cur_vm.vm_request.memory_size * cloud.server_list.get(i).migration_cost;

		cloud.server_list.get(i).vm_list.remove(cur_vm);
		cur_vm.vm_state = 1;

		Cloud cloud_dynamic = new Cloud();
		cloud_dynamic.copy(cloud);

		VM vm_dynamic = new VM();
		vm_dynamic.copy(cur_vm);

		int expect_time = cur_vm.vm_runtime < cloud.t_min ? (cloud.t_max + cloud.t_min - 2 * cur_vm.vm_runtime)/2 : (cloud.t_max - cur_vm.vm_runtime)/2;
		double cur_vm_cost = ( cur_vm.vm_request.memory_size * cloud.server_list.get(i).memory_cost + 
				       cur_vm.vm_request.disk_size * cloud.server_list.get(i).disk_cost +
				       cur_vm.vm_request.network_size * cloud.server_list.get(i).network_cost ) * expect_time;

		cloud.expect_cost = cur_vm_cost;
		cloud.server_list.get(i).vm_list.add(cur_vm);
		cloud.server_list.get(i).memory_usage = cloud.server_list.get(i).memory_usage + cur_vm.vm_request.memory_size;
		cloud.server_list.get(i).network_usage = cloud.server_list.get(i).network_usage + cur_vm.vm_request.network_size;

		cloud_dynamic.total_cost = cloud_dynamic.total_cost + cur_migration_cost;
		cloud_dynamic.expect_cost = cloud_dynamic.expect_cost = cur_migration_cost;

		res = optimization_launch(cloud_dynamic, vm_dynamic, t_event);

		if ( (cur_vm.vm_request.memory_size > (cloud.server_list.get(i).memory_size - cloud.server_list.get(i).memory_usage)) ||
		     (cur_vm.vm_request.disk_size > (cloud.server_list.get(i).disk_size - cloud.server_list.get(i).disk_usage)) ||
		     (cur_vm.vm_request.network_size > (cloud.server_list.get(i).network_size - cloud.server_list.get(i).network_usage)) || 
		     (cur_vm.vm_request.security_level > cloud.server_list.get(i).security_level)) {
			if (res == -1) {
				return -1;
			}
			cloud.copy(cloud_dynamic);
			return 0;
		}

		if ((res == -1)||(cloud.expect_cost <= cloud_dynamic.expect_cost)) {
			return 0;
		}
		else {
			cloud.copy(cloud_dynamic);
			return 0;
		}
	}

}
