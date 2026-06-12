package model;

public class Process {

    public int pid;
    public int burstTime;
    public int remainingTime;
    public int arrivalTime;
    public int priority;

    public int waitAge = 0;
    public int originalPriority;

    public int startTime = -1;
    public int completionTime = 0;
    public int waitingTime = 0;
    public int turnaroundTime = 0;

    public int deadline;
    public boolean deadlineMissed = false;

    public int cpuTimeUsed = 0;

    public ProcessState state;

    public Process(int pid, int burstTime, int arrivalTime, int priority) {
        this(pid, burstTime, arrivalTime, priority, Integer.MAX_VALUE);
    }

    public Process(int pid, int burstTime, int arrivalTime, int priority, int deadline) {
        this.pid = pid;
        this.burstTime = burstTime;
        this.remainingTime = burstTime;
        this.arrivalTime = arrivalTime;
        this.priority = priority;
        this.originalPriority = priority;
        this.deadline = deadline;
        this.state = ProcessState.NEW;
    }
}