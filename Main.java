/*
 * Title:        VM PLACEMENT
 * Description:  Optimization algorithm for allocating on-demand VMs to realize the energy-efficiency goal.
 *
 */

import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 
 * @author Tianwei Zhang
 * 
 */
public class Main {

	public static void main(String[] args) {

		final int CLOUD_SERVER_NUM = 10;
		final int CLOUD_SECURITY_NUM = 4;
		final List <Double> CLOUD_MEMORY_COST = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0);
		final List <Double> CLOUD_DISK_COST = Arrays.asList(0.01, 0.02, 0.03, 0.04, 0.05, 0.06, 0.07, 0.08, 0.09, 0.1);
		final List <Double> CLOUD_NETWORK_COST = Arrays.asList(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0);
		final double CLOUD_MIGRATION_COST = 100.0;
		final List <Integer> CLOUD_MEMORY_SIZE = Arrays.asList(1024, 2048, 4096, 8192);
		final List <Integer> CLOUD_DISK_SIZE = Arrays.asList(1024, 2048, 4096);
		final List <Integer> CLOUD_NETWORK_SIZE = Arrays.asList(10, 20, 30, 40);
	
		Cloud cloud = new Cloud ( 
					CLOUD_SERVER_NUM, 
					CLOUD_SECURITY_NUM, 
					CLOUD_MEMORY_COST, 
					CLOUD_DISK_COST, 
					CLOUD_NETWORK_COST, 
					CLOUD_MIGRATION_COST,
					CLOUD_MEMORY_SIZE,
					CLOUD_DISK_SIZE,
					CLOUD_NETWORK_SIZE);

		Cloud cloud1 = (Cloud) cloud.clone();
		cloud1.server_num = 9;
		
		cloud.display_server();
		cloud1.display_server();
		
		VM vm = new VM (
				0,
				8,
				1,
				1,
				1,
				0);
				
	}
}
