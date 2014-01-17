/*
 * Title:        Virtual Machine
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
public class VM implements Comparable<VM> {

	public int vm_id;

	public Request vm_request;

	/*
	 * VM state: 
	 * 0: Terminate
	 * 1: Active
	 * 2: Suspend
	 * 3: Migrate
	 */
	public int vm_state;

	public double vm_launchtime;

	public double vm_resumetime;

	public double vm_runtime;

	public VM() {}

	public VM(
			int VmId, 
			int MemorySize,
			int DiskSize,
			int NetworkSize,
			int SecurityLevel,
			double VmLaunchtime ) {
		vm_id = VmId;
		vm_state = 1;
		vm_launchtime = VmLaunchtime;
		vm_resumetime = vm_launchtime;
		vm_runtime = 0.0;
		vm_request = new Request(MemorySize, DiskSize, NetworkSize, SecurityLevel);
        }

	public void copy(VM src) {
		vm_id = src.vm_id;
		vm_state = src.vm_state;
		vm_launchtime = src.vm_launchtime;
		vm_resumetime = src.vm_resumetime;
		vm_runtime = src.vm_runtime;
		vm_request = new Request();
		vm_request.copy(src.vm_request);
	}

	public int compareTo(VM arg0) {
		return this.vm_request.memory_size < arg0.vm_request.memory_size ? -1
		     : this.vm_request.memory_size > arg0.vm_request.memory_size ? 1
		     : 0;
	}
}
