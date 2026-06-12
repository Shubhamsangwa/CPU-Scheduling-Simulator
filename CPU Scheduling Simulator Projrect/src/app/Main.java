package app;

import engine.Engine;
import model.Process;
import scheduler.RoundRobinScheduler;
import scheduler.Scheduler;
import ui.MainFrame;
import visualization.SimulationPanel;
import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {

            List<Process> processes = new ArrayList<>();

            SimulationPanel panel = new SimulationPanel(processes);
            Scheduler scheduler = new RoundRobinScheduler(2);
            Engine engine = new Engine(processes, scheduler, panel);

            MainFrame frame = new MainFrame(panel, engine, processes);
            frame.setVisible(true);

            Thread engineThread = new Thread(engine);
            engineThread.start();
        });
    }
}