/*
 * Title:        VM on-demand request
 * Description:  Structures of Virtual Machines and on-demand requests.
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
public class Request {
	public int memory_size;
	public int disk_size;
	public int network_size;
	public int security_level;

	public Request() {}

	public Request(
		int MemorySize,
		int DiskSize,
		int NetworkSize,
		int SecurityLevel) {
		
		memory_size = MemorySize;
		disk_size = DiskSize;
		network_size = NetworkSize;
		security_level = SecurityLevel;
	}

	public void copy(Request src) {
		memory_size = src.memory_size;
		disk_size = src.disk_size;
		network_size = src.network_size;
		security_level = src.security_level;
	}
}
