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
public class Cloud implements Cloneable {

	public int server_num;
	
	public int security_num;

	public double total_cost;

	public List<Server> server_list;

	public Cloud(
			int ServerNum, 
			int SecurityNum,
			List<Double> MemoryCost,
			List<Double> DiskCost,
			List<Double> NetworkCost,
			double MigrationCost,
			List<Integer> MemorySize,
			List<Integer> DiskSize,
			List<Integer> NetworkSize) {

		server_num = ServerNum;
		security_num = SecurityNum;
		total_cost = 0.0;	
	
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

	public void display_server() {
		System.out.println("id	Memory	Disk	Network	Security" );
		for (int i = 0; i< server_num; i ++) {
			System.out.println(server_list.get(i).server_id + "	" + server_list.get(i).memory_size + "	" + server_list.get(i).disk_size + "	" + server_list.get(i).network_size + "	" + server_list.get(i).security_level);
		}
	}

	public Object clone() {
		Cloud sc = null;
		try {
			sc = (Cloud) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return sc;
	}
}
