package scheduler;

import model.Process;
import model.ProcessState;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Multilevel Queue Scheduler
 *
 * Partitions processes into three fixed priority bands:
 *
 *   Level 0 (HIGH)   — priority 1–2   → Round Robin, quantum 1 (interactive / real-time feel)
 *   Level 1 (MEDIUM) — priority 3–4   → Round Robin, quantum 3 (normal processes)
 *   Level 2 (LOW)    — priority 5+    → FCFS            (background batch jobs)
 *
 * Rules:
 *   1. Level 0 is always checked first. While any Level-0 process is ready,
 *      lower levels do not run (strict preemption between levels).
 *   2. Within a level, Round Robin (or FCFS for Level 2) applies.
 *   3. Processes cannot move between levels — their priority band is fixed
 *      at arrival (unlike multilevel FEEDBACK queues which allow migration).
 *
 * This is what operating systems like early Windows and Unix used for
 * separating foreground (interactive) and background (batch) workloads.
 *
 * Effect on your existing diagnostics:
 *   - Starvation risk will go HIGH for low-priority processes — this is
 *     intentional and demonstrates WHY MLQ needs aging or feedback to be fair
 *   - Fairness gap will be large — good for contrast with HRRN in your report
 *   - Context switch count is high for Level-0 processes (quantum=1)
 *   - Convoy detection may trigger if a large Level-2 FCFS job monopolises
 *     the CPU when higher queues are empty
 */
public class MultilevelQueueScheduler implements Scheduler {

    // Three queues — indexed by level
    private final Queue<Process>[] queues;

    // Time slices remaining for the current process per queue level
    private int[] timeSlice;

    // Current running process per level (null if that level is idle)
    private Process[] current;

    // Quantum sizes per level
    private static final int[] QUANTUM = {1, 3, Integer.MAX_VALUE}; // Level 2 = FCFS (no preemption)

    // Priority bands: process goes to level based on its priority value
    // priority 1-2 → level 0,  priority 3-4 → level 1,  priority 5+ → level 2
    private static int levelOf(Process p) {
        if (p.priority <= 2) return 0;
        if (p.priority <= 4) return 1;
        return 2;
    }

    @SuppressWarnings("unchecked")
    public MultilevelQueueScheduler() {
        queues    = new Queue[3];
        timeSlice = new int[3];
        current   = new Process[3];

        for (int i = 0; i < 3; i++) {
            queues[i]    = new LinkedList<>();
            timeSlice[i] = 0;
            current[i]   = null;
        }
    }

    @Override
    public Process getNextProcess(List<Process> processes, int currentTime) {

        // Enqueue newly arrived processes into their correct level queue
        for (Process p : processes) {
            if (p.arrivalTime == currentTime && p.remainingTime > 0
                    && p.state != ProcessState.COMPLETED) {
                int level = levelOf(p);
                if (!queues[level].contains(p)) {
                    queues[level].offer(p);
                }
            }
        }

        // Work through levels from highest (0) to lowest (2)
        for (int level = 0; level < 3; level++) {

            // Skip if this level has nothing to run
            if (queues[level].isEmpty() && current[level] == null) continue;

            Process running = current[level];

            // Check if quantum expired or process finished
            boolean quantumExpired = timeSlice[level] >= QUANTUM[level];
            boolean processFinished = running != null && running.remainingTime <= 0;

            if (running == null || quantumExpired || processFinished) {

                // Put the current process back in the queue if it still has work
                if (running != null && running.remainingTime > 0 && !processFinished) {
                    running.state = ProcessState.READY;
                    queues[level].offer(running);
                }

                // Pull the next from this level's queue
                current[level] = queues[level].poll();
                timeSlice[level] = 0;
            }

            running = current[level];

            if (running != null && running.remainingTime > 0) {
                running.state = ProcessState.RUNNING;
                timeSlice[level]++;

                // Mark everything else READY for the sidebar
                markOthersReady(processes, running, currentTime);

                return running;
            }

            // Nothing runnable at this level — fall through to next level
            current[level] = null;
        }

        return null; // CPU idle
    }

    private void markOthersReady(List<Process> processes, Process running, int currentTime) {
        for (Process p : processes) {
            if (p != running
                    && p.arrivalTime <= currentTime
                    && p.remainingTime > 0
                    && p.state != ProcessState.COMPLETED) {
                p.state = ProcessState.READY;
            }
        }
    }
}