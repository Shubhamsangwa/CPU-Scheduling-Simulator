package ui;

import visualization.SimulationPanel;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

public class ControlPanel extends JPanel {

    public JButton startBtn;
    public JButton pauseBtn;
    public JButton resetBtn;
    public JComboBox<String> algoBox;

    private static final Color BG0    = SimulationPanel.BG0;
    private static final Color BG2    = SimulationPanel.BG2;
    private static final Color BORDER = SimulationPanel.BORDER;
    private static final Color GREEN  = SimulationPanel.GREEN;
    private static final Color AMBER  = SimulationPanel.AMBER;
    private static final Color RED    = new Color(0xFF4C4C);
    private static final Color FG1    = SimulationPanel.FG1;
    private static final Color FG3    = SimulationPanel.FG3;

    // All nine algorithms — original five + four new ones
    public static final String[] ALGORITHMS = {
            "Round Robin",           // original
            "FCFS",                  // original
            "SJF",                   // original (non-preemptive)
            "SRTF",                  // NEW — preemptive SJF
            "Priority + Aging",      // original
            "EDF",                   // NEW — Earliest Deadline First
            "HRRN",                  // NEW — Highest Response Ratio Next
            "Multilevel Queue"       // NEW — MLQ with 3 priority bands
    };

    public ControlPanel() {
        setLayout(new BorderLayout());
        setBackground(BG0);

        // Glow separator at top
        JPanel glowLine = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(0, 255, 136, 0),
                        getWidth() / 2f, 0, new Color(0, 255, 136, 90), true
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        glowLine.setPreferredSize(new Dimension(0, 1));
        glowLine.setOpaque(false);

        JPanel inner = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 10));
        inner.setBackground(BG0);

        JLabel label = new JLabel("SCHEDULER");
        label.setForeground(FG3);
        label.setFont(mono(10f, Font.BOLD));

        algoBox = buildComboBox();

        JPanel div = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(BORDER);
                g.fillRect(0, 0, 1, getHeight());
            }
        };
        div.setOpaque(false);
        div.setPreferredSize(new Dimension(1, 28));

        startBtn = makeButton("▶  START", GREEN);
        pauseBtn = makeButton("⏸  PAUSE", AMBER);
        resetBtn = makeButton("↺  RESET", RED);

        inner.add(label);
        inner.add(algoBox);
        inner.add(Box.createHorizontalStrut(4));
        inner.add(div);
        inner.add(Box.createHorizontalStrut(4));
        inner.add(startBtn);
        inner.add(pauseBtn);
        inner.add(resetBtn);

        add(glowLine, BorderLayout.NORTH);
        add(inner,    BorderLayout.CENTER);
    }

    private JComboBox<String> buildComboBox() {
        JComboBox<String> box = new JComboBox<>(ALGORITHMS);

        box.setUI(new BasicComboBoxUI());
        box.setBackground(BG2);
        box.setForeground(FG1);
        box.setFont(mono(12f, Font.PLAIN));
        // Wider to fit "Multilevel Queue"
        box.setPreferredSize(new Dimension(200, 32));
        box.setFocusable(false);
        box.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, false),
                new EmptyBorder(0, 8, 0, 8)
        ));

        for (Component comp : box.getComponents()) {
            if (comp instanceof JButton) {
                ((JButton) comp).setBackground(BG2);
                ((JButton) comp).setBorder(BorderFactory.createEmptyBorder());
            }
        }

        box.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object val, int idx, boolean sel, boolean focus) {
                JLabel c = (JLabel) super.getListCellRendererComponent(
                        list, val, idx, sel, focus);

                String text = val == null ? "" : val.toString();

                if (idx == -1) {
                    c.setBackground(BG2);
                    c.setForeground(FG1);
                } else {
                    c.setBackground(sel ? SimulationPanel.BG3 : BG2);

                    // Colour-code new algorithms so they stand out in the dropdown
                    boolean isNew = text.equals("SRTF")
                            || text.equals("EDF")
                            || text.equals("HRRN")
                            || text.equals("Multilevel Queue");

                    if (sel) {
                        c.setForeground(GREEN);
                    } else if (isNew) {
                        c.setForeground(SimulationPanel.BLUE);   // blue tint for new entries
                    } else {
                        c.setForeground(FG1);
                    }
                }

                c.setFont(mono(12f, Font.PLAIN));
                c.setBorder(new EmptyBorder(6, 10, 6, 10));
                return c;
            }
        });

        return box;
    }

    private JButton makeButton(String text, Color accent) {
        JButton btn = new JButton(text) {
            boolean hov = false;

            {
                setContentAreaFilled(false);
                setBorderPainted(false);
                setFocusPainted(false);
                setOpaque(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hov = true;  repaint(); }
                    @Override public void mouseExited (MouseEvent e) { hov = false; repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int w = getWidth(), h = getHeight();
                boolean pressed = getModel().isPressed();

                Color bg = pressed
                        ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 55)
                        : hov
                        ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 28)
                        : BG2;
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 6, 6));

                g2.setColor(hov || pressed
                        ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 200)
                        : BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(.5f, .5f, w - 1, h - 1, 6, 6));

                if (hov || pressed) {
                    g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 180));
                    g2.fillRect(0, 0, w, 2);
                }

                g2.setFont(getFont());
                g2.setColor(accent);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(getText())) / 2;
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(getText(), tx, ty);

                g2.dispose();
            }
        };

        btn.setFont(mono(11f, Font.BOLD));
        btn.setForeground(accent);
        btn.setPreferredSize(new Dimension(124, 32));
        return btn;
    }

    private Font mono(float size, int style) {
        String[] fc = {"JetBrains Mono", "Consolas", "Courier New", Font.MONOSPACED};
        for (String f : fc) return new Font(f, style, (int) size);
        return new Font(Font.MONOSPACED, style, (int) size);
    }
}