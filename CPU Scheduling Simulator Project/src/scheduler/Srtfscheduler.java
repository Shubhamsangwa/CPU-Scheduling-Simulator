package scheduler;

import model.Process;
import model.ProcessState;

import java.util.List;

/**
 * Shortest Remaining Time First (SRTF)
 * Preemptive version of SJF.
 *
 * Every tick, the process with the LEAST remaining CPU time wins.
 * If a new process arrives with a shorter remaining time than the
 * currently running one, it immediately preempts it — this is the
 * key difference from plain SJF which never interrupts a running process.
 *
 * Effect on metrics you already track:
 *   - Higher context switch count vs SJF (preemption cost)
 *   - Lower average waiting time than any non-preemptive algorithm
 *   - Starvation still possible for long processes (same as SJF)
 */
public class Srtfscheduler implements Scheduler {

    @Override
    public Process getNextProcess(List<Process> processes, int currentTime) {

        Process shortest = null;

        for (Process p : processes) {
            if (p.arrivalTime <= currentTime
                    && p.remainingTime > 0
                    && p.state != ProcessState.COMPLETED) {

                if (shortest == null || p.remainingTime < shortest.remainingTime) {
                    shortest = p;
                }
            }
        }

        // Mark all ready processes as READY so the UI sidebar renders correctly
        for (Process p : processes) {
            if (p.arrivalTime <= currentTime
                    && p.remainingTime > 0
                    && p.state != ProcessState.COMPLETED
                    && p != shortest) {
                p.state = ProcessState.READY;
            }
        }

        if (shortest != null) {
            shortest.state = ProcessState.RUNNING;
        }

        // No sticky "current" — re-evaluate every single tick (that's what makes it preemptive)
        return shortest;
    }
}