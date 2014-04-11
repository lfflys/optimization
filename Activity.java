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
public class Activity {

	public int event_time;

	public VM vm;

	/*
	 * VM action:
	 * 0: Launch
	 * 1: Terminate
	 * 2: Suspend
	 * 3: Resume
	 * 4: Change Request
	 */
	public int action;

	public Request request;

	public Activity() {}

	public Activity(
		int EventTime,
		VM cur_vm,
		int Action,
		Request cur_request) {
				
		event_time = EventTime;
		vm = cur_vm;
		action = Action;
		request = new Request();
		request.copy(cur_request);
	}

	public void copy(Activity src) {
		event_time = src.event_time;
		vm = new VM();
		vm.copy(src.vm);
		action = src.action;
		request = new Request();
		request.copy(src.request);
	}
}
