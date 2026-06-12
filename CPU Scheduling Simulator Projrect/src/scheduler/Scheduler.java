package scheduler;

import model.Process;
import java.util.List;

public interface Scheduler {
    Process getNextProcess(List<Process> processes, int currentTime);
}