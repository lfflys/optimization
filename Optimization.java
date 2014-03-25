/*
 * Title:        Allocation Algorithm.
 * Description:  Make decisions to select servers.
 */

import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.lang.Math;

/**
 * 
 * @author Tianwei Zhang
 * 
 */
public class Optimization {

	/**
 	 * 
 	 * optimization tries to find the best server without migrating the VMs
 	 * So there is no Migration cost
 	 * 
 	 */
	public Cloud optimization(Cloud cloud, VM vm) {

		double static_cost = 10000.0;
		double dynamic_cost = 10000.0;
		int static_server = -1;
		int dynamic_server = -1;

		Request cur_request = vm.vm_request;

		for (int i = 0; i < cloud.server_num; i ++) {
			Server cur_server = cloud.server_list.get(i);
			if (cur_request.security_level <= cur_server.security_level) {

				double cost = cur_request.memory_size * cur_server.memory_cost + 
					      cur_request.disk_size * cur_server.disk_cost + 
					      cur_request.network_size * cur_server.network_cost;

				if (cost < dynamic_cost) {
					dynamic_cost = cost;
					dynamic_server = i;
				}

				if ( (cur_request.memory_size < (cur_server.memory_size - cur_server.memory_usage)) &&
				     (cur_request.disk_size < (cur_server.disk_size - cur_server.disk_usage)) &&
				     (cur_request.network_size < (cur_server.network_size - cur_server.network_usage)) ) {
				
					if (cost < static_cost) {
						static_cost = cost;
						static_server = i;
					}
				}
			}
		}
		return cloud;
	}
}
