package scheduler;

import model.Process;
import model.ProcessState;

import java.util.List;

/**
 * Earliest Deadline First (EDF)
 *
 * The process whose absolute deadline is closest to expiring runs next.
 * This is a PREEMPTIVE algorithm — if a new process arrives with an
 * earlier deadline than the currently running one, it preempts immediately.
 *
 * EDF is provably optimal for single-CPU real-time systems: if ANY
 * scheduling algorithm can meet all deadlines for a given workload,
 * EDF will also meet them. This is what legitimises calling this
 * project an "RTOS simulator" rather than just a scheduling visualiser.
 *
 * Effect on your existing diagnostics:
 *   - Deadline miss count drops dramatically vs FCFS/RR on the Deadline Demo
 *   - Convoy effect panel shows "No convoy effect" (EDF ignores burst length)
 *   - Fairness gap is typically high — EDF sacrifices fairness for timeliness
 *   - Starvation possible: a process with a far-future deadline may never run
 *     if short-deadline processes keep arriving
 */
public class EDFScheduler implements Scheduler {

    @Override
    public Process getNextProcess(List<Process> processes, int currentTime) {

        Process earliest = null;

        for (Process p : processes) {
            if (p.arrivalTime <= currentTime
                    && p.remainingTime > 0
                    && p.state != ProcessState.COMPLETED) {

                if (earliest == null || p.deadline < earliest.deadline) {
                    earliest = p;
                }
            }
        }

        // Keep all non-selected arrived processes in READY state for the sidebar
        for (Process p : processes) {
            if (p.arrivalTime <= currentTime
                    && p.remainingTime > 0
                    && p.state != ProcessState.COMPLETED
                    && p != earliest) {
                p.state = ProcessState.READY;
            }
        }

        if (earliest != null) {
            earliest.state = ProcessState.RUNNING;
        }

        return earliest;
    }
}