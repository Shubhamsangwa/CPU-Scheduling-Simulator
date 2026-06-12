package scheduler;

import model.Process;
import model.ProcessState;

import java.util.List;

public class SJFScheduler implements Scheduler {

    private Process current = null;

    @Override
    public Process getNextProcess(List<Process> processes, int currentTime) {

        if (current != null && current.remainingTime > 0) {
            return current;
        }

        Process shortest = null;

        for (Process p : processes) {
            if (p.arrivalTime <= currentTime && p.remainingTime > 0) {
                if (shortest == null || p.remainingTime < shortest.remainingTime) {
                    shortest = p;
                }
            }
        }

        if (shortest != null) {
            shortest.state = ProcessState.RUNNING;
        }

        current = shortest;
        return current;
    }
}