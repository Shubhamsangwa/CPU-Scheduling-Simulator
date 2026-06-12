package engine;

public class Clock {

    private int time = 0;

    public void tick() {
        time++;
    }

    public int getTime() {
        return time;
    }

    public void reset() {
        time = 0;
    }
}