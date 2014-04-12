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

	public Server() {
		vm_list = new ArrayList<VM>();
	}

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
	
	public void copy(Server src) {
		server_id = src.server_id;
		memory_size = src.memory_size;
		disk_size = src.disk_size;
		network_size = src.network_size;
		security_level = src.security_level;

		memory_cost = src.memory_cost;
		disk_cost = src.disk_cost;
		network_cost = src.network_cost;
		migration_cost = src.migration_cost;

		memory_usage = src.memory_usage;
		disk_usage = src.disk_usage;
		network_usage = src.network_usage;

		vm_list.clear();
		for (VM src_vm: src.vm_list){
			VM dest_vm = new VM();
			dest_vm.copy(src_vm);
			vm_list.add(dest_vm);
		}
	}

	public void display_server() {
		try {
			PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter("result.txt", true)));
			writer.println("vm_id	memory	disk	network	sec	runtime");
			for (int i = 0; i < vm_list.size(); i ++) {
				writer.println(vm_list.get(i).vm_id + "	" + vm_list.get(i).vm_request.memory_size + "	" + vm_list.get(i).vm_request.disk_size + "	" + vm_list.get(i).vm_request.network_size + "	" + vm_list.get(i).vm_request.security_level + "	" + vm_list.get(i).vm_runtime);
			}
			writer.close();
		} catch (IOException e) {
		}
	}
}
