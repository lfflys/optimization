/*
 * Title:        Cloud System
 * Description:  Initialization and definition of the cloud system.
 *
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
public class Cloud {

	public int server_num;
	
	public int security_num;

	public double total_cost;
	
	public double expect_cost;

	/*
	 * Assuming the vm runtime satisfy the normal distribution N(t_avg, t_var)
	 * f(t) = 1/(2*pi*t_var^2)^(1/2) * exp[-(t-t_avg)^2/(2 * t_var^2)]
	 * This is hard to calculate. Consider the even distribution first
	public double t_avg;

	public double t_var;
	 */

	/*
	 * Assuming the vm runtime satisfy the normal distribution
	 * runtime t can be falling into the range [t_min, t_max]
	 */
	public double t_min;

	public double t_max;

	public List<Server> server_list;

	public Cloud() {
		server_list = new ArrayList<Server>();
	}

	public Cloud(
			int ServerNum, 
			int SecurityNum,
			double TMin,
			double TMax,
			List<Double> MemoryCost,
			List<Double> DiskCost,
			List<Double> NetworkCost,
			double MigrationCost,
			List<Integer> MemorySize,
			List<Integer> DiskSize,
			List<Integer> NetworkSize) {

		server_num = ServerNum;
		security_num = SecurityNum;
		t_min = TMin;
		t_max = TMax;
		total_cost = 0.0;	
		expect_cost = 0.0;
	
		set_server_list(MemoryCost, DiskCost, NetworkCost, MigrationCost, MemorySize, DiskSize, NetworkSize);
        }

	public void set_server_list(List<Double> MemoryCost, List<Double> DiskCost, List<Double> NetworkCost, double MigrationCost, List<Integer> MemorySize, List<Integer> DiskSize, List<Integer> NetworkSize) {
		server_list = new ArrayList<Server>(server_num);
		for (int i = 0; i < server_num; i ++) {
			int j = (int) (Math.random() * security_num);
			Server server_node = new Server(
							i,
							MemorySize.get((int) (Math.random() * MemorySize.size())),
							DiskSize.get((int) (Math.random() * DiskSize.size())),
							NetworkSize.get((int) (Math.random() * NetworkSize.size())),
							j,
							MemoryCost.get(j),
							DiskCost.get(j),
							NetworkCost.get(j),
							MigrationCost);

			server_list.add(server_node);
		}
	}

	public void copy(Cloud src) {
		server_num = src.server_num;
		security_num = src.security_num;
		t_min = src.t_min;
		t_max = src.t_max;
		total_cost = src.total_cost;
		expect_cost = src.expect_cost;

		server_list.clear();
		for (Server src_server: src.server_list){
			Server dest_server = new Server();
			dest_server.copy(src_server);
			server_list.add(dest_server);
		}
	}

	public void display_server() {
		
		System.out.println("id	Memory	usage	Disk	usage	Network	usage	Sec");
		for (int i = 0; i< server_num; i ++) {
			System.out.println(server_list.get(i).server_id + "	" + 
					   server_list.get(i).memory_size + "	" + 
					   server_list.get(i).memory_usage + "	" +
					   server_list.get(i).disk_size + "	" + 
					   server_list.get(i).disk_usage + "	" + 
					   server_list.get(i).network_size + "	" + 
					   server_list.get(i).network_usage + "	" + 
					   server_list.get(i).security_level + "	" );
			for (int j = 0; j < server_list.get(i).vm_list.size(); j ++){
				System.out.println("										" + server_list.get(i).vm_list.get(j).vm_id + "	" + server_list.get(i).vm_list.get(j).vm_runtime);
			}
		}
		System.out.println("Total cost:	" + total_cost);
	}
}
