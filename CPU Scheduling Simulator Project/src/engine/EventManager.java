package engine;

import model.Process;
import model.ProcessState;

import java.util.List;

public class EventManager {

    public void updateArrivals(List<Process> processes, int currentTime) {
        for (Process p : processes) {
            if (p.arrivalTime == currentTime) {
                p.state = ProcessState.READY;
            }
        }
    }
}