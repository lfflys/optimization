/*
 * Title:        Server
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
public class Server {
	public int server_id;
	public int memory_size;
	public int disk_size;
	public int network_size;
	public int security_level;

	public double memory_cost;
	public double disk_cost;
	public double network_cost;
	public double migration_cost;

	public int memory_usage;
	public int disk_usage;
	public int network_usage;

	public List<VM> vm_list;

	public Server(
			int ServerId,
			int MemorySize,
			int DiskSize,
			int NetworkSize,
			int SecurityLevel,
			double MemoryCost,
			double DiskCost,
			double NetworkCost,
			double MigrationCost) {
		server_id = ServerId;
		memory_size = MemorySize;
		disk_size = DiskSize;
		network_size = NetworkSize;
		security_level = SecurityLevel;
		
		memory_cost = MemoryCost;
		disk_cost = DiskCost;
		network_cost = NetworkCost;
		migration_cost = MigrationCost;
		
		memory_usage = 0;
		disk_usage = 0;
		network_usage = 0;
		
		vm_list = new ArrayList<VM>();
	}
}
