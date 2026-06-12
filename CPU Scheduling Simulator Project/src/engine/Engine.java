package engine;

import model.Process;
import model.ProcessState;
import scheduler.Scheduler;
import visualization.SimulationPanel;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class Engine implements Runnable {

    private final List<Process> processes;
    private Scheduler scheduler;
    private final SimulationPanel panel;

    private final Clock clock = new Clock();
    private final EventManager eventManager = new EventManager();

    private volatile boolean running = true;
    private volatile boolean paused  = true;

    private final List<Integer> ganttChart = new ArrayList<>();

    private int contextSwitchCost      = 1;
    private int contextSwitchRemaining = 0;
    private Process previousProcess    = null;
    private Process pendingProcess     = null;

    private boolean convoyDetected = false;
    private String  convoyMessage  = "None";

    private int busyTime           = 0;
    private int idleTime           = 0;
    private int contextSwitchCount = 0;
    private int contextSwitchTime  = 0;

    private boolean starvationRisk    = false;
    private String  starvationMessage = "None";
    private int     maxWaitingTime    = 0;
    private double  avgWaitingTime    = 0.0;

    private int    deadlineMissCount = 0;
    private String deadlineMessage   = "No deadline misses detected";

    private int    fairnessGap     = 0;
    private String fairnessMessage = "No fairness imbalance detected";

    public Engine(List<Process> processes, Scheduler scheduler, SimulationPanel panel) {
        this.processes = processes;
        this.scheduler = scheduler;
        this.panel     = panel;
    }

    @Override
    public void run() {
        while (running) {

            if (paused) {
                sleep(100);
                continue;
            }

            int currentTime = clock.getTime();
            eventManager.updateArrivals(processes, currentTime);

            updateWaitingMetrics(currentTime);
            detectDeadlineMisses(currentTime);
            updateFairnessMetrics();

            // ── Context switch tick ────────────────────────────────────────
            if (contextSwitchRemaining > 0) {
                ganttChart.add(-2);
                contextSwitchRemaining--;
                contextSwitchTime++;
                pushUiUpdate(null);
                clock.tick();
                sleep(1000);
                continue;
            }

            if (pendingProcess != null) {
                previousProcess = pendingProcess;
                pendingProcess  = null;
            }

            // ── Ask scheduler who runs this tick ──────────────────────────
            Process current = scheduler.getNextProcess(processes, currentTime);

            // ── Context switch detection ───────────────────────────────────
            if (needsContextSwitch(previousProcess, current)) {
                pendingProcess         = current;
                contextSwitchRemaining = contextSwitchCost;
                contextSwitchCount++;
                ganttChart.add(-2);
                pushUiUpdate(null);
                clock.tick();
                sleep(1000);
                continue;
            }

            // ── Execute one tick ───────────────────────────────────────────
            if (current != null) {
                if (current.startTime == -1) current.startTime = currentTime;

                current.state = ProcessState.RUNNING;
                current.remainingTime--;
                current.cpuTimeUsed++;
                busyTime++;
                ganttChart.add(current.pid);

                if (current.remainingTime <= 0) {
                    current.state          = ProcessState.COMPLETED;
                    current.completionTime = currentTime + 1;
                    current.turnaroundTime = current.completionTime - current.arrivalTime;
                    current.waitingTime    = current.turnaroundTime - current.burstTime;
                    current.waitAge        = 0;

                    if (current.completionTime > current.deadline) {
                        current.deadlineMissed = true;
                    }
                }

                previousProcess = current;
            } else {
                ganttChart.add(-1);
                idleTime++;
                previousProcess = null;
            }

            detectConvoyEffect(current);
            updateWaitingMetrics(currentTime);
            detectDeadlineMisses(currentTime);
            updateFairnessMetrics();
            pushUiUpdate(current);

            clock.tick();
            sleep(1000);
        }
    }

    // ── Metric helpers ────────────────────────────────────────────────────────

    private void pushUiUpdate(Process current) {
        boolean      convoyNow      = convoyDetected;
        String       convoyText     = convoyMessage;
        boolean      starvationNow  = starvationRisk;
        String       starvationText = starvationMessage;
        List<Integer> snapshot      = new ArrayList<>(ganttChart);

        SwingUtilities.invokeLater(() ->
                panel.updateState(
                        current, snapshot,
                        convoyNow, convoyText,
                        getCpuUtilization(),
                        contextSwitchCount, busyTime, idleTime, contextSwitchTime,
                        starvationNow, starvationText,
                        maxWaitingTime, avgWaitingTime,
                        deadlineMissCount, deadlineMessage,
                        fairnessGap, fairnessMessage
                )
        );
    }

    private void updateFairnessMetrics() {
        int minWait    = Integer.MAX_VALUE;
        int maxWait    = Integer.MIN_VALUE;
        int activeCount = 0;

        for (Process p : processes) {
            if (p.arrivalTime <= clock.getTime()) {
                int wait = p.state == ProcessState.COMPLETED
                        ? p.waitingTime
                        : Math.max(0, clock.getTime() - p.arrivalTime - p.cpuTimeUsed);

                minWait = Math.min(minWait, wait);
                maxWait = Math.max(maxWait, wait);
                activeCount++;
            }
        }

        if (activeCount <= 1) {
            fairnessGap     = 0;
            fairnessMessage = "Insufficient data for fairness analysis";
            return;
        }

        fairnessGap = Math.max(0, maxWait - minWait);

        if      (fairnessGap <= 2) fairnessMessage = "Fair scheduling distribution";
        else if (fairnessGap <= 5) fairnessMessage = "Moderate fairness imbalance";
        else                       fairnessMessage = "Unfair scheduling distribution detected";
    }

    private void detectDeadlineMisses(int currentTime) {
        int    misses = 0;
        String msg    = "No deadline misses detected";

        for (Process p : processes) {
            if (!p.deadlineMissed && p.remainingTime > 0 && currentTime > p.deadline) {
                p.deadlineMissed = true;
            }
            if (p.deadlineMissed) {
                misses++;
                msg = "Deadline missed by P" + p.pid + " (deadline=" + p.deadline + ")";
            }
        }

        deadlineMissCount = misses;
        deadlineMessage   = msg;
    }

    private void updateWaitingMetrics(int currentTime) {
        int     totalWaiting    = 0;
        int     waitingProcs    = 0;
        int     longestWait     = 0;
        Process worstProcess    = null;

        for (Process p : processes) {
            if (p.state == ProcessState.READY
                    && p.arrivalTime <= currentTime
                    && p.remainingTime > 0) {

                int w = currentTime - p.arrivalTime;
                totalWaiting += w;
                waitingProcs++;

                if (w > longestWait) {
                    longestWait  = w;
                    worstProcess = p;
                }
            }
        }

        maxWaitingTime = longestWait;
        avgWaitingTime = waitingProcs == 0 ? 0.0 : (double) totalWaiting / waitingProcs;

        starvationRisk    = false;
        starvationMessage = "None";

        String name = scheduler.getClass().getSimpleName();

        if (worstProcess != null && longestWait >= 6) {
            starvationRisk = true;
            starvationMessage = switch (name) {
                case "AgingPriorityScheduler" ->
                        "Aging mitigating starvation for P" + worstProcess.pid
                                + " (" + longestWait + " ticks)";
                case "SRTFScheduler" ->
                        "SRTF starvation: P" + worstProcess.pid
                                + " (long job, waited " + longestWait + " ticks)";
                case "EDFScheduler" ->
                        "EDF: P" + worstProcess.pid
                                + " has far deadline, waited " + longestWait + " ticks";
                case "HRRNScheduler" ->
                    // HRRN mathematically prevents starvation — flag it differently
                        "HRRN: high wait on P" + worstProcess.pid
                                + " but ratio is rising (" + longestWait + " ticks)";
                case "MultilevelQueueScheduler" ->
                        "MLQ: low-band P" + worstProcess.pid
                                + " starved by higher queues (" + longestWait + " ticks)";
                default ->
                        "Long wait detected: P" + worstProcess.pid
                                + " waited " + longestWait + " ticks";
            };
        }
    }

    /**
     * Convoy effect: only meaningful for non-preemptive algorithms.
     * EDF and SRTF are preemptive — convoys structurally cannot form.
     * HRRN is non-preemptive but its ratio formula naturally avoids long
     * processes monopolising, so we skip it there too.
     */
    private void detectConvoyEffect(Process current) {
        convoyDetected = false;
        convoyMessage  = "None";

        if (current == null) return;

        String name = scheduler.getClass().getSimpleName();

        // Preemptive schedulers cannot produce convoy effect
        if (name.equals("SRTFScheduler")
                || name.equals("EDFScheduler")
                || name.equals("HRRNScheduler")) {
            return;
        }

        // For FCFS, RR, SJF, Priority, MLQ — check classic convoy condition
        if (!name.equals("FCFSScheduler") && !name.equals("MultilevelQueueScheduler")) return;

        int shorterCount = 0;
        for (Process p : processes) {
            if (p.state == ProcessState.READY && p.remainingTime < current.remainingTime) {
                shorterCount++;
            }
        }

        if (shorterCount >= 2 && current.remainingTime >= 5) {
            convoyDetected = true;
            convoyMessage  = "Convoy: P" + current.pid
                    + " blocking " + shorterCount + " shorter process(es)";
        }
    }

    private boolean needsContextSwitch(Process previous, Process current) {
        if (contextSwitchCost <= 0)            return false;
        if (previous == null || current == null) return false;
        if (previous.remainingTime <= 0)         return false;
        return previous.pid != current.pid;
    }

    public double getCpuUtilization() {
        int total = busyTime + idleTime + contextSwitchTime;
        return total == 0 ? 0.0 : (busyTime * 100.0) / total;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public void pause()  { paused  = true;  }
    public void resume() { paused  = false; }
    public void stop()   { running = false; }

    public void resetProcesses() {
        clock.reset();
        ganttChart.clear();

        contextSwitchRemaining = 0;
        previousProcess        = null;
        pendingProcess         = null;

        convoyDetected    = false;  convoyMessage     = "None";
        busyTime          = 0;      idleTime          = 0;
        contextSwitchCount = 0;     contextSwitchTime = 0;
        starvationRisk    = false;  starvationMessage = "None";
        maxWaitingTime    = 0;      avgWaitingTime    = 0.0;
        deadlineMissCount = 0;      deadlineMessage   = "No deadline misses detected";
        fairnessGap       = 0;      fairnessMessage   = "No fairness imbalance detected";

        for (Process p : processes) {
            p.remainingTime  = p.burstTime;
            p.state          = ProcessState.NEW;
            p.startTime      = -1;
            p.completionTime = 0;
            p.waitingTime    = 0;
            p.turnaroundTime = 0;
            p.priority       = p.originalPriority;
            p.waitAge        = 0;
            p.deadlineMissed = false;
            p.cpuTimeUsed    = 0;
        }

        SwingUtilities.invokeLater(() ->
                panel.updateState(
                        null, new ArrayList<>(),
                        false, "None",
                        0.0, 0, 0, 0, 0,
                        false, "None",
                        0, 0.0,
                        0, "No deadline misses detected",
                        0, "No fairness imbalance detected"
                )
        );
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler         = scheduler;
        previousProcess        = null;
        pendingProcess         = null;
        contextSwitchRemaining = 0;
        convoyDetected         = false;  convoyMessage  = "None";
        starvationRisk         = false;  starvationMessage = "None";
        deadlineMissCount      = 0;      deadlineMessage   = "No deadline misses detected";
        fairnessGap            = 0;      fairnessMessage   = "No fairness imbalance detected";
    }

    public void setContextSwitchCost(int cost) { this.contextSwitchCost = Math.max(0, cost); }
    public int  getContextSwitchCost()          { return contextSwitchCost; }

    private void sleep(int ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}