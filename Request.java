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
public class Request implements Cloneable {
	public int memory_size;
	public int disk_size;
	public int network_size;
	public int security_level;

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

	public Object clone() {
		Request sc = null;
		try {
			sc = (Request) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return sc;
	}
}
