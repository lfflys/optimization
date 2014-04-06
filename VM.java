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

	public int vm_lastevent;

	public int vm_runtime;

	public int vm_suspendtime;

	public VM() {}

	public VM(
			int VmId, 
			int MemorySize,
			int DiskSize,
			int NetworkSize,
			int SecurityLevel,
			int VmLaunchtime ) {
		vm_id = VmId;
		vm_state = 1;
		vm_lastevent = VmLaunchtime;
		vm_runtime = 0;
		vm_suspendtime = 0;
		vm_request = new Request(MemorySize, DiskSize, NetworkSize, SecurityLevel);
        }

	public void copy(VM src) {
		vm_id = src.vm_id;
		vm_state = src.vm_state;
		vm_lastevent = src.vm_lastevent;
		vm_runtime = src.vm_runtime;
		vm_suspendtime = src.vm_suspendtime;
		vm_request = new Request();
		vm_request.copy(src.vm_request);
	}

	public int compareTo(VM arg0) {
		return this.vm_request.memory_size < arg0.vm_request.memory_size ? -1
		     : this.vm_request.memory_size > arg0.vm_request.memory_size ? 1
		     : 0;
	}
}
