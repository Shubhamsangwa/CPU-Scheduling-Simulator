package scheduler;

import model.Process;
import model.ProcessState;

import java.util.List;

public class AgingPriorityScheduler implements Scheduler {

    private Process current = null;

    private static final int AGING_THRESHOLD = 3;
    private static final int PRIORITY_BOOST = 1;

    @Override
    public Process getNextProcess(List<Process> processes, int currentTime) {

        applyAging(processes);

        if (current != null && current.remainingTime > 0) {
            return current;
        }

        Process highest = null;

        for (Process p : processes) {
            if (p.arrivalTime <= currentTime
                    && p.remainingTime > 0
                    && p.state != ProcessState.COMPLETED) {

                if (highest == null || p.priority < highest.priority) {
                    highest = p;
                }
            }
        }

        if (highest != null) {
            highest.state = ProcessState.RUNNING;
            highest.waitAge = 0;
        }

        current = highest;
        return current;
    }

    private void applyAging(List<Process> processes) {
        for (Process p : processes) {
            if (p.state == ProcessState.READY && p.remainingTime > 0) {
                p.waitAge++;

                if (p.waitAge >= AGING_THRESHOLD) {
                    if (p.priority > 1) {
                        p.priority -= PRIORITY_BOOST;
                    }
                    p.waitAge = 0;
                }
            }
        }
    }
}