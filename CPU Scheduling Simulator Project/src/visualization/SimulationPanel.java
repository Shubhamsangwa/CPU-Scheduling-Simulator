package visualization;

import model.Process;
import model.ProcessState;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SimulationPanel extends JPanel {

    public static final Color BG0        = new Color(0x0A0C10);
    public static final Color BG1        = new Color(0x0D0F14);
    public static final Color BG2        = new Color(0x13161D);
    public static final Color BG3        = new Color(0x1A1D26);
    public static final Color BORDER     = new Color(0x1E2330);
    public static final Color GREEN      = new Color(0x00FF88);
    public static final Color AMBER      = new Color(0xFFB300);
    public static final Color MAGENTA_FX = new Color(0xFF4CF7);
    public static final Color BLUE       = new Color(0x4C9EFF);
    public static final Color FG1        = new Color(0xE8EDF5);
    public static final Color FG2        = new Color(0x8A95AA);
    public static final Color FG3        = new Color(0x3A4255);
    public static final Color RED_WARN   = new Color(0xFF5C5C);

    private static final Color[] PROC_COLORS = {
            new Color(0x4C9EFF), new Color(0x00FF88), new Color(0xFFB300),
            new Color(0xFF6B6B), new Color(0xC084FC), new Color(0x34D399),
            new Color(0xFB923C), new Color(0x38BDF8)
    };

    // Fixed structural constants
    private static final int MARGIN      = 20;
    private static final int SIDEBAR_W   = 72;
    private static final int CPU_BOX_W   = 128;
    private static final int CPU_BOX_H   = 128;
    private static final int PROC_PILL_W = 52;
    private static final int PROC_PILL_H = 52;

    // Gantt + legend occupy the bottom 120px
    private static final int GANTT_RESERVE = 120;

    private final List<Process> processes;
    private Process currentProcess;
    private List<Integer> ganttChart = new ArrayList<>();
    private String activeScheduler = "Round Robin";

    private boolean convoyDetected    = false;
    private String  convoyMessage     = "None";
    private double  cpuUtilization    = 0.0;
    private int     contextSwitchCount = 0;
    private int     busyTime          = 0;
    private int     idleTime          = 0;
    private int     contextSwitchTime = 0;
    private boolean starvationRisk    = false;
    private String  starvationMessage = "None";
    private int     maxWaitingTime    = 0;
    private double  avgWaitingTime    = 0.0;
    private int     deadlineMissCount = 0;
    private String  deadlineMessage   = "No deadline misses detected";
    private int     fairnessGap       = 0;
    private String  fairnessMessage   = "No fairness imbalance detected";

    public SimulationPanel(List<Process> processes) {
        this.processes = processes;
        setBackground(BG1);
    }

    public void updateState(Process current,
                            List<Integer> gantt,
                            boolean convoyDetected,
                            String convoyMessage,
                            double cpuUtilization,
                            int contextSwitchCount,
                            int busyTime,
                            int idleTime,
                            int contextSwitchTime,
                            boolean starvationRisk,
                            String starvationMessage,
                            int maxWaitingTime,
                            double avgWaitingTime,
                            int deadlineMissCount,
                            String deadlineMessage,
                            int fairnessGap,
                            String fairnessMessage) {
        this.currentProcess       = current;
        this.ganttChart           = gantt;
        this.convoyDetected       = convoyDetected;
        this.convoyMessage        = convoyMessage;
        this.cpuUtilization       = cpuUtilization;
        this.contextSwitchCount   = contextSwitchCount;
        this.busyTime             = busyTime;
        this.idleTime             = idleTime;
        this.contextSwitchTime    = contextSwitchTime;
        this.starvationRisk       = starvationRisk;
        this.starvationMessage    = starvationMessage;
        this.maxWaitingTime       = maxWaitingTime;
        this.avgWaitingTime       = avgWaitingTime;
        this.deadlineMissCount    = deadlineMissCount;
        this.deadlineMessage      = deadlineMessage;
        this.fairnessGap          = fairnessGap;
        this.fairnessMessage      = fairnessMessage;
        repaint();
    }

    public void setActiveScheduler(String name) {
        this.activeScheduler = name;
        repaint();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Color procColor(int pid) {
        return PROC_COLORS[Math.abs(pid) % PROC_COLORS.length];
    }

    private Font mono(float size, int style) {
        String[] candidates = {"JetBrains Mono", "Consolas", "Courier New", Font.MONOSPACED};
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] installed = ge.getAvailableFontFamilyNames();
        for (String c : candidates)
            for (String f : installed)
                if (f.equalsIgnoreCase(c))
                    return new Font(c, style, Math.round(size));
        return new Font(Font.MONOSPACED, style, Math.round(size));
    }

    private void fillCard(Graphics2D g2, int x, int y, int w, int h, Color fill) {
        g2.setColor(fill);
        g2.fillRoundRect(x, y, w, h, 6, 6);
        g2.setColor(BORDER);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(x, y, w, h, 6, 6);
    }

    private void drawProcessPill(Graphics2D g2, int x, int y, int w, int h, int pid) {
        Color c = procColor(pid);
        fillCard(g2, x, y, w, h, BG2);
        g2.setColor(c);
        g2.fillRoundRect(x, y, w, 4, 3, 3);
        g2.setFont(mono(11f, Font.BOLD));
        FontMetrics fm = g2.getFontMetrics();
        String lbl = "P" + pid;
        g2.setColor(c);
        g2.drawString(lbl, x + (w - fm.stringWidth(lbl)) / 2,
                y + h / 2 + fm.getAscent() / 2 - 2);
    }

    private void sectionLabel(Graphics2D g2, String text, int x, int y) {
        g2.setFont(mono(10f, Font.BOLD));
        g2.setColor(FG3);
        g2.drawString(text, x, y);
    }

    private void hRule(Graphics2D g2, int x, int y, int w) {
        g2.setColor(BORDER);
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(x, y, x + w, y);
    }

    // ── Paint ─────────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        final int W = getWidth();
        final int H = getHeight();

        g2.setColor(BG1);
        g2.fillRect(0, 0, W, H);

        // Centre column between the two sidebars
        int leftEdge  = MARGIN + SIDEBAR_W + 8;
        int rightEdge = W - MARGIN - SIDEBAR_W - 8;
        int colW      = rightEdge - leftEdge;
        int colCX     = leftEdge + colW / 2;

        // ── Fixed vertical regions ─────────────────────────────────────────
        // Title band: 0..58
        final int TITLE_BOTTOM  = 58;
        // Gantt+legend: (H - GANTT_RESERVE)..H
        final int GANTT_TOP     = H - GANTT_RESERVE;
        // Metrics card sits just below title
        final int METRICS_H     = 52;
        final int GAP           = 6;

        int metricsTop = TITLE_BOTTOM + GAP;
        int metricsBot = metricsTop + METRICS_H;

        // CPU row: fixed height, placed just below metrics
        final int CPU_ROW_LABEL = 14; // space above box for "CPU" label
        int cpuRowTop = metricsBot + GAP;
        int cpuBoxY   = cpuRowTop + CPU_ROW_LABEL;
        int cpuRowBot = cpuBoxY + CPU_BOX_H;

        // Remaining vertical space for 4 analysis cards + their gaps
        // Available = GANTT_TOP - GAP - cpuRowBot
        int analysisTop = cpuRowBot + GAP;
        int analysisBot = GANTT_TOP - GAP;
        int analysisH   = Math.max(0, analysisBot - analysisTop);

        // 4 cards: convoy(~30), starvation(~46), deadline(~46), fairness(~46)
        // gap between each = GAP
        // Total: convoyH + card3H*3 + GAP*3 = analysisH
        // convoy is ~38% of a card3, so weight convoy as 0.6, others as 1.0 each → total weight 3.6
        float totalWeight   = 0.65f + 1.0f + 1.0f + 1.0f; // convoy + 3 full cards
        int   gapsTotal     = GAP * 3;
        int   spaceForCards = Math.max(0, analysisH - gapsTotal);
        int   convoyH       = Math.max(20, Math.round(spaceForCards * (0.65f / totalWeight)));
        int   card3H        = Math.max(26, Math.round(spaceForCards * (1.0f  / totalWeight)));

        // ── Draw everything ────────────────────────────────────────────────
        drawTitle(g2, colCX, W);
        drawMetricsPanel(g2, leftEdge, metricsTop, colW, METRICS_H);
        drawCPUAndQueues(g2, leftEdge, cpuRowTop, colW, colCX, W, cpuBoxY);

        int ay = analysisTop;
        drawConvoyWarning(g2,    leftEdge, ay, colW, convoyH); ay += convoyH + GAP;
        drawStarvationPanel(g2,  leftEdge, ay, colW, card3H);  ay += card3H  + GAP;
        drawDeadlinePanel(g2,    leftEdge, ay, colW, card3H);  ay += card3H  + GAP;
        drawFairnessPanel(g2,    leftEdge, ay, colW, card3H);

        drawGanttChart(g2, W, H, GANTT_TOP);
        drawLegend(g2, H);
    }

    // ── Section renderers ─────────────────────────────────────────────────────

    private void drawTitle(Graphics2D g2, int colCX, int W) {
        g2.setFont(mono(13f, Font.BOLD));
        g2.setColor(FG1);
        FontMetrics fm = g2.getFontMetrics();
        String t = "CPU  SCHEDULING  SIMULATION";
        g2.drawString(t, colCX - fm.stringWidth(t) / 2, 24);

        g2.setFont(mono(10f, Font.BOLD));
        FontMetrics fmS = g2.getFontMetrics();
        String badge = "▸  " + activeScheduler.toUpperCase();
        int bw = fmS.stringWidth(badge) + 20;
        int bx = colCX - bw / 2;
        g2.setColor(new Color(GREEN.getRed(), GREEN.getGreen(), GREEN.getBlue(), 18));
        g2.fillRoundRect(bx, 30, bw, 18, 4, 4);
        g2.setColor(new Color(GREEN.getRed(), GREEN.getGreen(), GREEN.getBlue(), 60));
        g2.drawRoundRect(bx, 30, bw, 18, 4, 4);
        g2.setColor(GREEN);
        g2.drawString(badge, bx + 10, 43);

        hRule(g2, MARGIN, 54, W - 2 * MARGIN);
    }

    private void drawMetricsPanel(Graphics2D g2, int x, int y, int w, int h) {
        fillCard(g2, x, y, w, h, BG2);
        sectionLabel(g2, "PERFORMANCE  METRICS", x + 12, y - 5);

        g2.setFont(mono(10f, Font.BOLD));
        String[] labels = {"CPU UTIL:", "CS COUNT:", "BUSY:", "IDLE:", "CS TIME:"};
        String[] values = {
                String.format("%.1f%%", cpuUtilization),
                String.valueOf(contextSwitchCount),
                String.valueOf(busyTime),
                String.valueOf(idleTime),
                String.valueOf(contextSwitchTime)
        };
        Color[] colors = {GREEN, MAGENTA_FX, BLUE, AMBER, FG2};

        FontMetrics fm = g2.getFontMetrics();
        int cols = labels.length;
        int cw   = w / cols;
        int ty   = y + h / 2 + fm.getAscent() / 2 - 1;

        for (int i = 0; i < cols; i++) {
            String full = labels[i] + " " + values[i];
            g2.setColor(colors[i]);
            g2.drawString(full, x + i * cw + (cw - fm.stringWidth(full)) / 2, ty);
        }
    }

    private void drawCPUAndQueues(Graphics2D g2,
                                  int leftEdge, int rowTop, int colW, int colCX,
                                  int W, int boxY) {
        // Section label above CPU box
        sectionLabel(g2, "CPU", colCX - 14, rowTop + 12);

        int boxX = colCX - CPU_BOX_W / 2;

        if (currentProcess != null && currentProcess.state != ProcessState.COMPLETED) {
            Color c = procColor(currentProcess.pid);
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 30));
            g2.fillRoundRect(boxX - 4, boxY - 4, CPU_BOX_W + 8, CPU_BOX_H + 8, 10, 10);
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 80));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(boxX - 4, boxY - 4, CPU_BOX_W + 8, CPU_BOX_H + 8, 10, 10);
        }

        fillCard(g2, boxX, boxY, CPU_BOX_W, CPU_BOX_H, BG2);

        if (currentProcess != null && currentProcess.state != ProcessState.COMPLETED) {
            Color c = procColor(currentProcess.pid);
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 25));
            g2.fillRoundRect(boxX + 6, boxY + 6, CPU_BOX_W - 12, CPU_BOX_H - 12, 4, 4);
            g2.setColor(c);
            g2.fillRoundRect(boxX + 6, boxY + 6, CPU_BOX_W - 12, 3, 2, 2);
            g2.setFont(mono(20f, Font.BOLD));
            g2.setColor(c);
            FontMetrics fm = g2.getFontMetrics();
            String lbl = "P" + currentProcess.pid;
            g2.drawString(lbl, boxX + (CPU_BOX_W - fm.stringWidth(lbl)) / 2,
                    boxY + CPU_BOX_H / 2 + 8);
            g2.setFont(mono(9f, Font.BOLD));
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 180));
            FontMetrics fms = g2.getFontMetrics();
            String st = "RUNNING";
            g2.drawString(st, boxX + (CPU_BOX_W - fms.stringWidth(st)) / 2,
                    boxY + CPU_BOX_H - 10);
        } else {
            g2.setFont(mono(11f, Font.PLAIN));
            g2.setColor(FG3);
            FontMetrics fm = g2.getFontMetrics();
            String idle = "IDLE";
            g2.drawString(idle, boxX + (CPU_BOX_W - fm.stringWidth(idle)) / 2,
                    boxY + CPU_BOX_H / 2 + 5);
        }

        // Ready queue — left sidebar, vertically aligned with CPU box
        int qx = MARGIN;
        sectionLabel(g2, "READY  QUEUE", qx, rowTop + 12);
        int py = boxY;
        int count = 0;
        for (Process p : processes) {
            if (p.state == ProcessState.READY) {
                drawProcessPill(g2, qx, py, PROC_PILL_W, PROC_PILL_H, p.pid);
                py += PROC_PILL_H + 6;
                if (++count >= 7) break;
            }
        }
        if (count == 0) {
            g2.setFont(mono(9f, Font.PLAIN));
            g2.setColor(FG3);
            g2.drawString("empty", qx + 4, boxY + 20);
        }

        // Completed — right sidebar
        int cx = W - MARGIN - PROC_PILL_W;
        sectionLabel(g2, "COMPLETED", cx - 10, rowTop + 12);
        py = boxY;
        count = 0;
        for (Process p : processes) {
            if (p.state == ProcessState.COMPLETED) {
                fillCard(g2, cx, py, PROC_PILL_W, PROC_PILL_H, BG2);
                g2.setColor(FG3);
                g2.fillRoundRect(cx, py, PROC_PILL_W, 4, 3, 3);
                g2.setFont(mono(11f, Font.BOLD));
                g2.setColor(FG3);
                FontMetrics fm = g2.getFontMetrics();
                String lbl = "P" + p.pid;
                g2.drawString(lbl, cx + (PROC_PILL_W - fm.stringWidth(lbl)) / 2,
                        py + PROC_PILL_H / 2 + fm.getAscent() / 2 - 2);
                g2.setColor(GREEN);
                g2.setFont(mono(9f, Font.BOLD));
                g2.drawString("✓", cx + PROC_PILL_W - 14, py + 12);
                py += PROC_PILL_H + 6;
                if (++count >= 7) break;
            }
        }
        if (count == 0) {
            g2.setFont(mono(9f, Font.PLAIN));
            g2.setColor(FG3);
            g2.drawString("none", cx + 4, boxY + 20);
        }
    }

    private void drawConvoyWarning(Graphics2D g2, int x, int y, int w, int h) {
        fillCard(g2, x, y, w, h, BG2);
        Color accent = convoyDetected ? RED_WARN : GREEN;
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 180));
        g2.fillRoundRect(x, y, w, 3, 3, 3);

        g2.setFont(mono(10f, Font.BOLD));
        FontMetrics fm = g2.getFontMetrics();
        int ty = y + (h + fm.getAscent() - fm.getDescent()) / 2;

        if (convoyDetected) {
            g2.setColor(RED_WARN);
            g2.drawString("WARNING:", x + 12, ty);
            g2.setColor(FG1);
            g2.drawString(convoyMessage, x + 100, ty);
        } else {
            g2.setColor(GREEN);
            g2.drawString("STATUS:", x + 12, ty);
            g2.setColor(FG2);
            g2.drawString("No convoy effect detected", x + 84, ty);
        }
    }

    private void drawStarvationPanel(Graphics2D g2, int x, int y, int w, int h) {
        fillCard(g2, x, y, w, h, BG2);
        sectionLabel(g2, "STARVATION  ANALYSIS", x + 12, y - 4);

        g2.setFont(mono(10f, Font.BOLD));
        FontMetrics fm = g2.getFontMetrics();
        int row1Y = y + h / 3 + fm.getAscent() / 2;
        int row2Y = y + 2 * h / 3 + fm.getAscent() / 2 + 2;

        if (starvationRisk) {
            g2.setColor(RED_WARN);  g2.drawString("RISK: HIGH", x + 12, row1Y);
            g2.setColor(FG1);       g2.drawString(starvationMessage, x + 110, row1Y);
        } else {
            g2.setColor(GREEN);     g2.drawString("RISK: LOW",  x + 12, row1Y);
            g2.setColor(FG2);       g2.drawString("No major starvation signal detected", x + 104, row1Y);
        }
        g2.setColor(BLUE);  g2.drawString("MAX WAIT: " + maxWaitingTime, x + 12, row2Y);
        g2.setColor(AMBER); g2.drawString(String.format("AVG WAIT: %.1f", avgWaitingTime), x + 170, row2Y);
    }

    private void drawDeadlinePanel(Graphics2D g2, int x, int y, int w, int h) {
        fillCard(g2, x, y, w, h, BG2);
        sectionLabel(g2, "DEADLINE  ANALYSIS", x + 12, y - 4);

        g2.setFont(mono(10f, Font.BOLD));
        FontMetrics fm = g2.getFontMetrics();
        int row1Y = y + h / 3 + fm.getAscent() / 2;
        int row2Y = y + 2 * h / 3 + fm.getAscent() / 2 + 2;

        if (deadlineMissCount > 0) {
            g2.setColor(RED_WARN); g2.drawString("MISSES: " + deadlineMissCount, x + 12, row1Y);
            g2.setColor(FG1);      g2.drawString(deadlineMessage, x + 120, row1Y);
        } else {
            g2.setColor(GREEN);    g2.drawString("MISSES: 0", x + 12, row1Y);
            g2.setColor(FG2);      g2.drawString("All current deadlines are being met", x + 104, row1Y);
        }
        g2.setColor(AMBER); g2.drawString("Real-time status monitoring enabled", x + 12, row2Y);
    }

    private void drawFairnessPanel(Graphics2D g2, int x, int y, int w, int h) {
        fillCard(g2, x, y, w, h, BG2);
        sectionLabel(g2, "FAIRNESS  ANALYSIS", x + 12, y - 4);

        g2.setFont(mono(10f, Font.BOLD));
        FontMetrics fm = g2.getFontMetrics();
        int row1Y = y + h / 3 + fm.getAscent() / 2;
        int row2Y = y + 2 * h / 3 + fm.getAscent() / 2 + 2;

        Color gc = fairnessGap <= 2 ? GREEN : fairnessGap <= 5 ? AMBER : RED_WARN;
        g2.setColor(gc);   g2.drawString("FAIRNESS GAP: " + fairnessGap, x + 12, row1Y);
        g2.setColor(FG1);  g2.drawString(fairnessMessage, x + 160, row1Y);
        g2.setColor(BLUE); g2.drawString("Tracks imbalance in wait distribution", x + 12, row2Y);
    }

    private void drawGanttChart(Graphics2D g2, int W, int H, int ganttTop) {
        hRule(g2, MARGIN, ganttTop - 10, W - 2 * MARGIN);
        sectionLabel(g2, "GANTT  CHART", MARGIN, ganttTop + 4);

        int x      = MARGIN;
        int barY   = ganttTop + 14;
        int slotW  = 30;
        int slotH  = 36;
        int maxSlots = (W - 2 * MARGIN) / slotW;

        List<Integer> visible = ganttChart.size() > maxSlots
                ? ganttChart.subList(ganttChart.size() - maxSlots, ganttChart.size())
                : ganttChart;

        for (int i = 0; i < visible.size(); i++) {
            int   slot  = visible.get(i);
            Color fill;
            String label;

            if      (slot == -1) { fill = BG3;        label = "—";       }
            else if (slot == -2) { fill = MAGENTA_FX; label = "CS";      }
            else                 { fill = procColor(slot); label = "P" + slot; }

            g2.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(),
                    slot == -1 ? 60 : 180));
            g2.fillRect(x, barY, slotW - 1, slotH);
            g2.setColor(fill);
            g2.fillRect(x, barY, slotW - 1, 2);

            g2.setFont(mono(9f, Font.BOLD));
            if      (slot == -1) g2.setColor(FG3);
            else if (slot == -2) g2.setColor(Color.WHITE);
            else                 g2.setColor(BG0);

            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(label,
                    x + (slotW - 1 - fm.stringWidth(label)) / 2,
                    barY + slotH / 2 + fm.getAscent() / 2 - 2);

            if (i % 5 == 0) {
                g2.setColor(FG3);
                g2.setFont(mono(8f, Font.PLAIN));
                int globalIdx = ganttChart.size() > maxSlots
                        ? (ganttChart.size() - maxSlots + i) : i;
                g2.drawString(String.valueOf(globalIdx), x, barY + slotH + 12);
            }
            x += slotW;
        }

        if (ganttChart.isEmpty()) {
            g2.setFont(mono(10f, Font.PLAIN));
            g2.setColor(FG3);
            g2.drawString("simulation not started", MARGIN, barY + 22);
        }
    }

    private void drawLegend(Graphics2D g2, int H) {
        int y = H - 20;
        int x = MARGIN;

        Object[][] items = {
                {PROC_COLORS[0], "Process"},
                {MAGENTA_FX,     "Context Switch"},
                {BG3,            "Idle"}
        };

        sectionLabel(g2, "LEGEND", x, y - 1);
        x += 58;

        for (Object[] item : items) {
            Color  c   = (Color)  item[0];
            String lbl = (String) item[1];

            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 180));
            g2.fillRect(x, y - 10, 10, 10);
            g2.setColor(c);
            g2.fillRect(x, y - 10, 10, 2);

            g2.setFont(mono(9f, Font.PLAIN));
            g2.setColor(FG2);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(lbl, x + 14, y);
            x += 14 + fm.stringWidth(lbl) + 20;
        }
    }
}