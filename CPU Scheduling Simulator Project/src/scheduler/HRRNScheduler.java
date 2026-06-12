package scheduler;

import model.Process;
import model.ProcessState;

import java.util.List;

/**
 * Highest Response Ratio Next (HRRN)
 *
 * Non-preemptive. Selects the process with the highest Response Ratio:
 *
 *   Response Ratio = (Waiting Time + Burst Time) / Burst Time
 *                  = 1 + (Waiting Time / Burst Time)
 *
 * Why this is elegant:
 *   - Short processes naturally have a high ratio (large denominator shrinks it less)
 *   - BUT as a long process waits, its waiting time grows, pushing its ratio up
 *   - So starvation is mathematically impossible — every process's ratio
 *     increases monotonically while it waits
 *
 * This is the best algorithm for demonstrating that starvation is a
 * design choice, not an inevitability. Run the Starvation Demo under
 * HRRN vs Priority to see the contrast clearly.
 *
 * Effect on your existing diagnostics:
 *   - Starvation risk panel stays LOW regardless of priority mix
 *   - Fairness gap stays moderate — not as fair as RR but better than Priority
 *   - No convoy effect (it naturally deprioritises long-running processes
 *     early in their life)
 *   - Average waiting time typically better than FCFS, close to SJF
 */
public class HRRNScheduler implements Scheduler {

    private Process current = null;

    @Override
    public Process getNextProcess(List<Process> processes, int currentTime) {

        // Non-preemptive: if current process still has work, keep running it
        if (current != null && current.remainingTime > 0
                && current.state != ProcessState.COMPLETED) {
            return current;
        }

        Process best = null;
        double bestRatio = -1.0;

        for (Process p : processes) {
            if (p.arrivalTime <= currentTime
                    && p.remainingTime > 0
                    && p.state != ProcessState.COMPLETED) {

                // Waiting time = how long since arrival minus CPU time already received
                double waitingTime = Math.max(0,
                        (currentTime - p.arrivalTime) - p.cpuTimeUsed);

                // Response ratio formula
                double ratio = (waitingTime + p.burstTime) / (double) p.burstTime;

                if (best == null || ratio > bestRatio) {
                    best = p;
                    bestRatio = ratio;
                }
            }
        }

        if (best != null) {
            best.state = ProcessState.RUNNING;
        }

        current = best;
        return current;
    }
}