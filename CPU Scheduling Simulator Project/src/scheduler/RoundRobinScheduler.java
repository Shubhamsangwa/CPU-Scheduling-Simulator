package scheduler;

import model.Process;
import model.ProcessState;

import java.util.*;

public class RoundRobinScheduler implements Scheduler {

    private Queue<Process> queue = new LinkedList<>();
    private int quantum;
    private int timeSlice = 0;
    private Process current = null;

    public RoundRobinScheduler(int quantum) {
        this.quantum = quantum;
    }

    @Override
    public Process getNextProcess(List<Process> processes, int currentTime) {

        for (Process p : processes) {
            if (p.arrivalTime == currentTime) {
                queue.offer(p);
            }
        }

        if (current == null || timeSlice >= quantum || current.remainingTime <= 0) {

            if (current != null && current.remainingTime > 0) {
                current.state = ProcessState.READY;
                queue.offer(current);
            }

            current = queue.poll();
            timeSlice = 0;
        }

        if (current != null) {
            current.state = ProcessState.RUNNING;
            timeSlice++;
        }

        return current;
    }
}