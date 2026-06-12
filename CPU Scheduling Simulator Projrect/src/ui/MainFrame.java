package ui;

import engine.Engine;
import model.Process;
import scheduler.*;
import visualization.SimulationPanel;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;

public class MainFrame extends JFrame {

    private static final Color BG0    = SimulationPanel.BG0;
    private static final Color BG1    = SimulationPanel.BG1;
    private static final Color BORDER = SimulationPanel.BORDER;
    private static final Color FG3    = SimulationPanel.FG3;

    public MainFrame(SimulationPanel panel, Engine engine, List<Process> processes) {
        setTitle("RTOS Scheduling Engine");
        setSize(1280, 720);
        setMinimumSize(new Dimension(1024, 620));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG0);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG1);
        root.setBorder(new LineBorder(BORDER, 1));
        setContentPane(root);

        // ── Header ────────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(BORDER);
                g.fillRect(0, getHeight() - 1, getWidth(), 1);
            }
        };
        header.setBackground(BG0);
        header.setPreferredSize(new Dimension(0, 40));

        JPanel titleGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleGroup.setOpaque(false);
        titleGroup.setBorder(new EmptyBorder(11, 14, 0, 0));

        // Animated pulse dot
        JLabel dot = new JLabel("●") {
            float phase = 0f;
            final Timer t = new Timer(60, e -> { phase += 0.07f; repaint(); });
            { t.start(); }

            @Override protected void paintComponent(Graphics g) {
                int alpha = (int)(160 + 95 * Math.sin(phase));
                setForeground(new Color(0, 255, 136,
                        Math.max(0, Math.min(255, alpha))));
                super.paintComponent(g);
            }
        };
        dot.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));

        titleGroup.add(dot);
        for (String word : new String[]{"RTOS", "SCHEDULING", "ENGINE"}) {
            JLabel w = new JLabel(word);
            w.setForeground(new Color(0xB0BAD0));
            w.setFont(monoFont(11f, Font.BOLD));
            titleGroup.add(w);
        }

        JLabel ver = new JLabel("v2.0  ");
        ver.setForeground(FG3);
        ver.setFont(monoFont(10f, Font.PLAIN));
        ver.setBorder(new EmptyBorder(0, 0, 0, 6));

        header.add(titleGroup, BorderLayout.WEST);
        header.add(ver,        BorderLayout.EAST);

        // ── Center: sim + input panel side by side ────────────────────────────
        JPanel center = new JPanel(new BorderLayout(0, 0));
        center.setBackground(BG1);
        center.setBorder(new EmptyBorder(10, 10, 6, 10));

        JPanel simWrap = new JPanel(new BorderLayout());
        simWrap.setBackground(BG1);
        simWrap.add(panel, BorderLayout.CENTER);

        InputPanel inputPanel = new InputPanel(processes, engine, panel);
        inputPanel.setPreferredSize(new Dimension(300, 0));

        center.add(simWrap,    BorderLayout.CENTER);
        center.add(inputPanel, BorderLayout.EAST);

        // ── Controls ──────────────────────────────────────────────────────────
        ControlPanel controls = new ControlPanel();
        panel.setActiveScheduler("Round Robin");

        controls.startBtn.addActionListener(e -> engine.resume());
        controls.pauseBtn.addActionListener(e -> engine.pause());
        controls.resetBtn.addActionListener(e -> {
            engine.pause();
            engine.resetProcesses();
        });

        controls.algoBox.addActionListener(e -> {
            String sel = (String) controls.algoBox.getSelectedItem();
            if (sel == null) return;

            engine.pause();
            engine.resetProcesses();
            panel.setActiveScheduler(sel);

            Scheduler next = switch (sel) {
                case "FCFS"             -> new FCFSScheduler();
                case "SJF"              -> new SJFScheduler();
                case "SRTF"             -> new Srtfscheduler();
                case "Priority + Aging" -> new AgingPriorityScheduler();
                case "EDF"              -> new EDFScheduler();            // NEW
                case "HRRN"             -> new HRRNScheduler();           // NEW
                case "Multilevel Queue" -> new MultilevelQueueScheduler();// NEW
                default                 -> new RoundRobinScheduler(2);
            };

            engine.setScheduler(next);
        });

        root.add(header,   BorderLayout.NORTH);
        root.add(center,   BorderLayout.CENTER);
        root.add(controls, BorderLayout.SOUTH);
    }

    private Font monoFont(float size, int style) {
        String[] candidates = {"JetBrains Mono", "Consolas", "Courier New", Font.MONOSPACED};
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] installed = ge.getAvailableFontFamilyNames();
        for (String candidate : candidates)
            for (String font : installed)
                if (font.equalsIgnoreCase(candidate))
                    return new Font(candidate, style, Math.round(size));
        return new Font(Font.MONOSPACED, style, Math.round(size));
    }
}