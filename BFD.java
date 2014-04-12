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
public class BFD {

	/**
	 *
	 * energy_update tries to update the total_cost and expect_cost in the cloud
	 * right now, it does not consider the event of resumation or termination.
	 */

	public void energy_update(Cloud cloud, int t_event) {
		double new_cost = 0;
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
                try {
                        PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter("result.txt1", true)));
                        writer.println(" ");
                        writer.println("time: " + t_event + "	new_cost: " + new_cost);
                        writer.close();
                } catch (IOException e) {
                }

		cloud.display_cloud1();
	}

	/**
 	 * 
 	 * bfd_launch tries to find the best server to locate the machines
 	 *
 	 */
	public int bfd_launch(Cloud cloud, VM vm, int t_event) {

		// Make sure static_cost and dynamic_cost is large enough.
		double static_cost = 100000000.0;
		int static_index = -1;

		int res = -1;

		Request cur_request = vm.vm_request;

		for (int i = 0; i < cloud.server_num; i ++) {
			Server cur_server = cloud.server_list.get(i);
			
			if (cur_request.security_level <= cur_server.security_level) {
				if ( (cur_request.memory_size <= (cur_server.memory_size - cur_server.memory_usage)) &&
				     (cur_request.disk_size <= (cur_server.disk_size - cur_server.disk_usage)) &&
				     (cur_request.network_size <= (cur_server.network_size - cur_server.network_usage)) ) {
				
					int expect_time = 0;
					double cost = 0.0;

					if (vm.vm_state == 1) {
						expect_time  = vm.vm_runtime < cloud.t_min ? (cloud.t_max + cloud.t_min - 2 * vm.vm_runtime)/2 : (cloud.t_max - vm.vm_runtime)/2;
						cost = ( cur_request.memory_size * cur_server.memory_cost + 
							 cur_request.disk_size * cur_server.disk_cost + 
							 cur_request.network_size * cur_server.network_cost ) * expect_time;
					}
					if (vm.vm_state == 2) {
						expect_time = vm.vm_suspendtime < cloud.st_min ? (cloud.st_max + cloud.st_min - 2 * vm.vm_suspendtime)/2 : (cloud.st_max - vm.vm_suspendtime)/2;
						cost = cur_request.disk_size * cur_server.disk_cost * expect_time;
					}
					if (cost < static_cost) {
						static_cost = cost;
						static_index = i;
					}
				}
			}
		}

		if (static_index == -1) {
			return -1;
		}

		cloud.expect_cost = cloud.expect_cost + static_cost;
		Server static_server = cloud.server_list.get(static_index);
		static_server.memory_usage = static_server.memory_usage + cur_request.memory_size;
		static_server.disk_usage = static_server.disk_usage + cur_request.disk_size;
		static_server.network_usage = static_server.network_usage + cur_request.network_size;
		static_server.vm_list.add(vm);

		return 0;
	}

	/**
	 *
	 * bfd_terminate tries to shutdown a VM; 
	 * right now just shutdown without any optimization;
	 * If performs optimization, needs to find other VMs that can migrate to this server;
	 * This is a huge cost: O(vm_num);
	 * Another reason is without optimization, this can leave some bugs for the customer to game.
	 *
	 */
	public int bfd_terminate(Cloud cloud, int vm_id, int t_event) {
		int res = -1;
		for (Server cur_server: cloud.server_list) {
			for (VM cur_vm: cur_server.vm_list) {
				if (cur_vm.vm_id == vm_id){
					cur_server.memory_usage = cur_server.memory_usage - cur_vm.vm_request.memory_size;
					cur_server.disk_usage = cur_server.disk_usage - cur_vm.vm_request.disk_size;
					cur_server.network_usage = cur_server.network_usage - cur_vm.vm_request.network_size;
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
	 * bfd_change tries to change the VM's requirements and (or) reallocate them.
	 *
	 */
	public int bfd_change(Cloud cloud, int vm_id, Request request, int t_event) {
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

		Server cur_server = cloud.server_list.get(i);
		VM cur_vm = cur_server.vm_list.get(j);

		cur_server.memory_usage = cur_server.memory_usage - cur_vm.vm_request.memory_size;
		cur_server.disk_usage = cur_server.disk_usage - cur_vm.vm_request.disk_size;
		cur_server.network_usage = cur_server.network_usage - cur_vm.vm_request.network_size;

		double cur_migration_cost = cur_vm.vm_request.memory_size * cur_server.migration_cost;

		cur_vm.vm_request.memory_size = request.memory_size;
		cur_vm.vm_request.disk_size = request.disk_size;
		cur_vm.vm_request.network_size = request.network_size;
		cur_vm.vm_request.security_level = request.security_level;

		if ((cur_vm.vm_request.memory_size <= (cur_server.memory_size - cur_server.memory_usage)) &&
		    (cur_vm.vm_request.disk_size <= (cur_server.disk_size - cur_server.disk_usage)) &&   
		    (cur_vm.vm_request.network_size <= (cur_server.network_size - cur_server.network_size)) &&
		    (cur_vm.vm_request.security_level <= cur_server.security_level)) {
			cur_server.memory_usage = cur_server.memory_usage + cur_vm.vm_request.memory_size;
			cur_server.disk_usage = cur_server.disk_usage + cur_vm.vm_request.disk_size;
			cur_server.network_usage = cur_server.network_usage + cur_vm.vm_request.network_size;
			return 0;
		}

		cur_server.vm_list.remove(cur_vm);
		cloud.total_cost = cloud.total_cost + cur_migration_cost;

		res = bfd_launch(cloud, cur_vm, t_event);
		if (res == -1) {
			return -1;
		}
		
		return 0;
	}

	public int bfd_suspend(Cloud cloud, int vm_id, int t_event) {
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

		Server cur_server = cloud.server_list.get(i);
		VM cur_vm = cur_server.vm_list.get(j);

		cur_vm.vm_state = 2;
		cur_vm.vm_suspendtime = 0;

		return 0;
	}

	public int bfd_resume(Cloud cloud, int vm_id, int t_event) {
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

		Server cur_server = cloud.server_list.get(i);
		VM cur_vm = cur_server.vm_list.get(j);

		cur_vm.vm_state = 1;

		if ( (cur_vm.vm_request.memory_size > (cur_server.memory_size - cur_server.memory_usage)) ||
		     (cur_vm.vm_request.disk_size > (cur_server.disk_size - cur_server.disk_usage)) ||
		     (cur_vm.vm_request.network_size > (cur_server.network_size - cur_server.network_usage)) || 
		     (cur_vm.vm_request.security_level > cur_server.security_level)) {
			return -1;
		}
		return 0;
	}
}
