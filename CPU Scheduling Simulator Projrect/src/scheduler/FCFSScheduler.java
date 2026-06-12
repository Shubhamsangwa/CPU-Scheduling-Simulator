package scheduler;

import model.Process;
import model.ProcessState;

import java.util.List;

public class FCFSScheduler implements Scheduler {

    private Process current = null;

    @Override
    public Process getNextProcess(List<Process> processes, int currentTime) {

        if (current != null && current.remainingTime > 0) {
            return current;
        }

        for (Process p : processes) {
            if (p.arrivalTime <= currentTime && p.remainingTime > 0) {
                current = p;
                current.state = ProcessState.RUNNING;
                return current;
            }
        }

        return null;
    }
}