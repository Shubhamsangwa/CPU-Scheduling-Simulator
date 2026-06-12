package ui;

import engine.Engine;
import model.Process;
import visualization.SimulationPanel;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.List;

public class InputPanel extends JPanel {

    private static final Color BG0    = SimulationPanel.BG0;
    private static final Color BG1    = SimulationPanel.BG1;
    private static final Color BG2    = SimulationPanel.BG2;
    private static final Color BG3    = SimulationPanel.BG3;
    private static final Color BORDER = SimulationPanel.BORDER;
    private static final Color FG1    = SimulationPanel.FG1;
    private static final Color FG2    = SimulationPanel.FG2;
    private static final Color FG3    = SimulationPanel.FG3;
    private static final Color GREEN  = SimulationPanel.GREEN;
    private static final Color AMBER  = SimulationPanel.AMBER;
    private static final Color BLUE   = SimulationPanel.BLUE;
    private static final Color RED    = new Color(0xFF5C5C);
    private static final Color PINK   = new Color(0xFF7A7A);

    private static final Color[] PROC_COLORS = {
            new Color(0x4C9EFF), new Color(0x00FF88), new Color(0xFFB300),
            new Color(0xFF6B6B), new Color(0xC084FC), new Color(0x34D399),
            new Color(0xFB923C), new Color(0x38BDF8)
    };

    private final List<Process> processes;
    private final Engine engine;
    private final SimulationPanel panel;

    private final JTextField pidField;
    private final JTextField burstField;
    private final JTextField arrivalField;
    private final JTextField priorityField;
    private final JTextField deadlineField;

    private final DefaultTableModel tableModel;
    private final JTable processTable;
    private final JLabel feedbackLabel;

    public InputPanel(List<Process> processes, Engine engine, SimulationPanel panel) {
        this.processes = processes;
        this.engine    = engine;
        this.panel     = panel;

        setLayout(new BorderLayout(0, 0));
        setBackground(BG0);
        setBorder(new MatteBorder(0, 1, 0, 0, BORDER));

        // ── Header ────────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(BORDER);
                g.fillRect(0, getHeight() - 1, getWidth(), 1);
            }
        };
        header.setBackground(BG0);
        header.setPreferredSize(new Dimension(0, 52));
        header.setBorder(new EmptyBorder(10, 14, 10, 14));

        JLabel title = new JLabel("PROCESS  INPUT");
        title.setForeground(FG1);
        title.setFont(mono(11f, Font.BOLD));

        JLabel subtitle = new JLabel("Add workloads or load demo test cases");
        subtitle.setForeground(FG3);
        subtitle.setFont(mono(9f, Font.PLAIN));

        JPanel titleStack = new JPanel();
        titleStack.setLayout(new BoxLayout(titleStack, BoxLayout.Y_AXIS));
        titleStack.setOpaque(false);
        titleStack.add(title);
        titleStack.add(Box.createVerticalStrut(3));
        titleStack.add(subtitle);
        header.add(titleStack, BorderLayout.WEST);

        // ── Scrollable content ────────────────────────────────────────────────
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG1);

        // Glow accent line
        content.add(makeGlowLine());

        // ── Form section ──────────────────────────────────────────────────────
        JPanel formSection = new JPanel();
        formSection.setLayout(new BoxLayout(formSection, BoxLayout.Y_AXIS));
        formSection.setBackground(BG1);
        formSection.setBorder(new EmptyBorder(12, 12, 8, 12));
        formSection.setAlignmentX(LEFT_ALIGNMENT);

        formSection.add(makeSectionTitle("NEW  PROCESS"));
        formSection.add(Box.createVerticalStrut(10));

        pidField      = buildField("e.g. 1");
        burstField    = buildField("e.g. 8");
        arrivalField  = buildField("e.g. 0");
        priorityField = buildField("e.g. 2");
        deadlineField = buildField("e.g. 15");

        formSection.add(makeFieldBlock("PID",      "Process identifier",         pidField));
        formSection.add(Box.createVerticalStrut(8));
        formSection.add(makeFieldBlock("BURST",    "CPU time required (> 0)",    burstField));
        formSection.add(Box.createVerticalStrut(8));
        formSection.add(makeFieldBlock("ARRIVAL",  "Time enters queue (≥ 0)",    arrivalField));
        formSection.add(Box.createVerticalStrut(8));
        formSection.add(makeFieldBlock("PRIORITY", "Lower = higher priority",    priorityField));
        formSection.add(Box.createVerticalStrut(8));

        // Deadline field with deadline-specific accent color
        formSection.add(makeFieldBlock("DEADLINE", "Absolute deadline (≥ burst)", deadlineField, PINK));

        content.add(formSection);

        // ── Feedback label ────────────────────────────────────────────────────
        feedbackLabel = new JLabel(" ");
        feedbackLabel.setFont(mono(9f, Font.BOLD));
        feedbackLabel.setForeground(RED);
        feedbackLabel.setAlignmentX(LEFT_ALIGNMENT);
        feedbackLabel.setBorder(new EmptyBorder(2, 14, 4, 14));
        feedbackLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        content.add(feedbackLabel);

        // ── Primary buttons ───────────────────────────────────────────────────
        JPanel primaryBtns = new JPanel(new GridLayout(1, 2, 8, 0));
        primaryBtns.setBackground(BG1);
        primaryBtns.setBorder(new EmptyBorder(0, 12, 12, 12));
        primaryBtns.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        primaryBtns.setAlignmentX(LEFT_ALIGNMENT);

        JButton addBtn   = makeButton("＋  ADD", GREEN);
        JButton clearBtn = makeButton("✕  CLEAR ALL", RED);
        primaryBtns.add(addBtn);
        primaryBtns.add(clearBtn);
        content.add(primaryBtns);

        content.add(makeSeparatorPanel());

        // ── Demo section ──────────────────────────────────────────────────────
        JPanel demoSection = new JPanel();
        demoSection.setLayout(new BoxLayout(demoSection, BoxLayout.Y_AXIS));
        demoSection.setBackground(BG1);
        demoSection.setBorder(new EmptyBorder(10, 12, 10, 12));
        demoSection.setAlignmentX(LEFT_ALIGNMENT);

        demoSection.add(makeSectionTitle("DEMO  SCENARIOS"));
        demoSection.add(Box.createVerticalStrut(8));

        JButton convoyBtn     = makeButton("LOAD  CONVOY  EFFECT",    AMBER);
        JButton starvationBtn = makeButton("LOAD  STARVATION  DEMO",  BLUE);
        JButton deadlineBtn   = makeButton("LOAD  DEADLINE  DEMO",    PINK);

        for (JButton b : new JButton[]{convoyBtn, starvationBtn, deadlineBtn}) {
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            b.setAlignmentX(LEFT_ALIGNMENT);
        }

        demoSection.add(convoyBtn);
        demoSection.add(Box.createVerticalStrut(6));
        demoSection.add(starvationBtn);
        demoSection.add(Box.createVerticalStrut(6));
        demoSection.add(deadlineBtn);
        content.add(demoSection);

        content.add(makeSeparatorPanel());

        // ── Table section ─────────────────────────────────────────────────────
        tableModel = new DefaultTableModel(
                new Object[]{"PID", "BURST", "ARR", "PRI", "DL"}, 0
        ) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        processTable = new JTable(tableModel);
        styleTable(processTable);

        JScrollPane tableScroll = new JScrollPane(processTable);
        tableScroll.setBorder(new LineBorder(BORDER, 1));
        tableScroll.getViewport().setBackground(BG2);
        tableScroll.setBackground(BG2);
        tableScroll.setPreferredSize(new Dimension(0, 180));

        JPanel tableSection = new JPanel(new BorderLayout(0, 6));
        tableSection.setBackground(BG1);
        tableSection.setBorder(new EmptyBorder(10, 12, 12, 12));
        tableSection.setAlignmentX(LEFT_ALIGNMENT);
        tableSection.add(makeSectionTitle("PROCESS  QUEUE"), BorderLayout.NORTH);
        tableSection.add(tableScroll, BorderLayout.CENTER);

        JPanel tableSectionWrap = new JPanel(new BorderLayout());
        tableSectionWrap.setBackground(BG1);
        tableSectionWrap.setAlignmentX(LEFT_ALIGNMENT);
        tableSectionWrap.add(tableSection, BorderLayout.CENTER);
        content.add(tableSectionWrap);

        // ── Outer scroll ──────────────────────────────────────────────────────
        JScrollPane outerScroll = new JScrollPane(content);
        outerScroll.setBorder(BorderFactory.createEmptyBorder());
        outerScroll.getViewport().setBackground(BG1);
        outerScroll.setBackground(BG1);
        outerScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        outerScroll.getVerticalScrollBar().setUnitIncrement(12);

        add(header,      BorderLayout.NORTH);
        add(outerScroll, BorderLayout.CENTER);

        // ── Wire actions ──────────────────────────────────────────────────────
        addBtn.addActionListener(e -> addProcessFromFields());
        clearBtn.addActionListener(e -> clearAllProcesses());
        convoyBtn.addActionListener(e -> loadConvoyDemo());
        starvationBtn.addActionListener(e -> loadStarvationDemo());
        deadlineBtn.addActionListener(e -> loadDeadlineDemo());

        KeyAdapter enterKey = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) addProcessFromFields();
            }
        };
        pidField.addKeyListener(enterKey);
        burstField.addKeyListener(enterKey);
        arrivalField.addKeyListener(enterKey);
        priorityField.addKeyListener(enterKey);
        deadlineField.addKeyListener(enterKey);

        refreshTable();
    }

    // ── Layout helpers ────────────────────────────────────────────────────────

    private JPanel makeFieldBlock(String name, String hint, JTextField field) {
        return makeFieldBlock(name, hint, field, FG2);
    }

    private JPanel makeFieldBlock(String name, String hint, JTextField field, Color nameColor) {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setOpaque(false);
        block.setAlignmentX(LEFT_ALIGNMENT);

        JLabel nameLabel = new JLabel(name);
        nameLabel.setForeground(nameColor);
        nameLabel.setFont(mono(10f, Font.BOLD));
        nameLabel.setAlignmentX(LEFT_ALIGNMENT);

        JLabel hintLabel = new JLabel(hint);
        hintLabel.setForeground(FG3);
        hintLabel.setFont(mono(8f, Font.PLAIN));
        hintLabel.setAlignmentX(LEFT_ALIGNMENT);

        field.setAlignmentX(LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        block.add(nameLabel);
        block.add(Box.createVerticalStrut(2));
        block.add(hintLabel);
        block.add(Box.createVerticalStrut(4));
        block.add(field);

        return block;
    }

    private JLabel makeSectionTitle(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(FG3);
        lbl.setFont(mono(9f, Font.BOLD));
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        return lbl;
    }

    private JPanel makeGlowLine() {
        JPanel line = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(0, 255, 136, 0),
                        getWidth() / 2f, 0, new Color(0, 255, 136, 70), true
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        line.setPreferredSize(new Dimension(0, 1));
        line.setOpaque(false);
        line.setAlignmentX(LEFT_ALIGNMENT);
        return line;
    }

    private JPanel makeSeparatorPanel() {
        JPanel sep = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(BORDER);
                g.fillRect(0, 0, getWidth(), 1);
            }
        };
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setPreferredSize(new Dimension(0, 1));
        sep.setOpaque(false);
        sep.setAlignmentX(LEFT_ALIGNMENT);
        return sep;
    }

    // ── Component builders ────────────────────────────────────────────────────

    private JTextField buildField(String placeholder) {
        JTextField field = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(FG3);
                    g2.setFont(getFont().deriveFont(Font.ITALIC));
                    Insets ins = getInsets();
                    g2.drawString(placeholder, ins.left + 2,
                            getHeight() / 2 + g2.getFontMetrics().getAscent() / 2 - 2);
                    g2.dispose();
                }
            }
        };
        field.setBackground(BG2);
        field.setForeground(FG1);
        field.setCaretColor(GREEN);
        field.setFont(mono(11f, Font.PLAIN));
        field.setSelectionColor(new Color(0, 255, 136, 50));
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1),
                new EmptyBorder(6, 8, 6, 8)
        ));

        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(new Color(0, 255, 136, 120), 1),
                        new EmptyBorder(6, 8, 6, 8)
                ));
            }
            @Override public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(BORDER, 1),
                        new EmptyBorder(6, 8, 6, 8)
                ));
            }
        });

        return field;
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
        btn.setFont(mono(10f, Font.BOLD));
        btn.setForeground(accent);
        btn.setPreferredSize(new Dimension(0, 34));
        return btn;
    }

    // ── Table styling ─────────────────────────────────────────────────────────

    private void styleTable(JTable table) {
        table.setBackground(BG2);
        table.setForeground(FG1);
        table.setGridColor(BORDER);
        table.setRowHeight(26);
        table.setFont(mono(10f, Font.PLAIN));
        table.setSelectionBackground(BG3);
        table.setSelectionForeground(FG1);
        table.setShowVerticalLines(true);
        table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new Dimension(0, 0));

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean focus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(
                        t, val, sel, focus, row, col);

                lbl.setBackground(sel ? BG3 : (row % 2 == 0 ? BG2 : new Color(0x161921)));
                lbl.setFont(mono(10f, Font.PLAIN));
                lbl.setBorder(new EmptyBorder(0, 6, 0, 6));
                lbl.setHorizontalAlignment(CENTER);

                if (col == 0 && val != null) {
                    // PID column — colored per process
                    try {
                        int pid = Integer.parseInt(val.toString());
                        lbl.setForeground(PROC_COLORS[Math.abs(pid) % PROC_COLORS.length]);
                        lbl.setFont(mono(10f, Font.BOLD));
                    } catch (NumberFormatException ignored) {
                        lbl.setForeground(FG1);
                    }
                } else if (col == 4 && val != null) {
                    // Deadline column — check if deadline < burst (tight/impossible)
                    try {
                        int dl    = Integer.parseInt(val.toString());
                        int burst = Integer.parseInt(tableModel.getValueAt(row, 1).toString());
                        if (dl < burst) {
                            // impossible deadline — red
                            lbl.setForeground(RED);
                            lbl.setFont(mono(10f, Font.BOLD));
                        } else if (dl == burst) {
                            // very tight — amber
                            lbl.setForeground(AMBER);
                            lbl.setFont(mono(10f, Font.BOLD));
                        } else {
                            lbl.setForeground(PINK);
                        }
                    } catch (Exception ignored) {
                        lbl.setForeground(PINK);
                    }
                } else {
                    lbl.setForeground(sel ? FG1 : FG2);
                }
                return lbl;
            }
        });

        // Header
        JTableHeader hdr = table.getTableHeader();
        hdr.setBackground(BG0);
        hdr.setForeground(FG3);
        hdr.setFont(mono(9f, Font.BOLD));
        hdr.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));
        hdr.setReorderingAllowed(false);
        hdr.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean focus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(
                        t, val, sel, focus, row, col);
                lbl.setBackground(BG0);
                // Deadline header gets pink accent
                lbl.setForeground(col == 4 ? PINK : FG3);
                lbl.setFont(mono(9f, Font.BOLD));
                lbl.setBorder(new EmptyBorder(4, 6, 4, 6));
                lbl.setHorizontalAlignment(CENTER);
                return lbl;
            }
        });

        // Equal column widths for narrow panel
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(52);
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void addProcessFromFields() {
        clearFeedback();
        try {
            int pid      = Integer.parseInt(pidField.getText().trim());
            int burst    = Integer.parseInt(burstField.getText().trim());
            int arrival  = Integer.parseInt(arrivalField.getText().trim());
            int priority = Integer.parseInt(priorityField.getText().trim());
            int deadline = Integer.parseInt(deadlineField.getText().trim());

            if (burst <= 0)    { showFeedback("Burst must be > 0", RED);          return; }
            if (arrival < 0)   { showFeedback("Arrival must be ≥ 0", RED);        return; }
            if (priority <= 0) { showFeedback("Priority must be > 0", RED);       return; }
            if (deadline < 0)  { showFeedback("Deadline must be ≥ 0", RED);       return; }
            if (deadline < burst) {
                // Warn but still allow — user may intend to study missed deadlines
                showFeedback("⚠ Deadline < Burst — will miss deadline", AMBER);
            }

            for (Process p : processes) {
                if (p.pid == pid) {
                    showFeedback("PID " + pid + " already exists", RED);
                    return;
                }
            }

            engine.pause();
            processes.add(new Process(pid, burst, arrival, priority, deadline));
            engine.resetProcesses();
            panel.repaint();
            refreshTable();
            clearFields();

            if (deadline >= burst) {
                showFeedback("P" + pid + " added", GREEN);
            }

        } catch (NumberFormatException ex) {
            showFeedback("All fields must be integers", RED);
        }
    }

    private void clearAllProcesses() {
        engine.pause();
        processes.clear();
        engine.resetProcesses();
        panel.repaint();
        refreshTable();
        clearFields();
        showFeedback("All processes cleared", AMBER);
    }

    private void loadConvoyDemo() {
        engine.pause();
        processes.clear();
        processes.add(new Process(1, 12, 0, 3, 20));
        processes.add(new Process(2,  2, 0, 2,  8));
        processes.add(new Process(3,  1, 0, 1,  6));
        engine.resetProcesses();
        panel.repaint();
        refreshTable();
        clearFields();
        showFeedback("Convoy demo loaded", AMBER);
    }

    private void loadStarvationDemo() {
        engine.pause();
        processes.clear();
        processes.add(new Process(1, 10, 0, 5, 18));
        processes.add(new Process(2,  3, 0, 1,  8));
        processes.add(new Process(3,  3, 1, 1, 10));
        processes.add(new Process(4,  2, 2, 1, 12));
        engine.resetProcesses();
        panel.repaint();
        refreshTable();
        clearFields();
        showFeedback("Starvation demo loaded", BLUE);
    }

    private void loadDeadlineDemo() {
        engine.pause();
        processes.clear();
        processes.add(new Process(1,  7, 0, 2,  5));  // impossible — DL < burst
        processes.add(new Process(2,  3, 0, 1,  6));  // tight
        processes.add(new Process(3,  2, 1, 3,  4));  // tight
        processes.add(new Process(4,  4, 2, 2, 10));  // feasible
        engine.resetProcesses();
        panel.repaint();
        refreshTable();
        clearFields();
        showFeedback("Deadline demo loaded — check DL column", PINK);
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Process p : processes) {
            tableModel.addRow(new Object[]{
                    p.pid, p.burstTime, p.arrivalTime, p.priority, p.deadline
            });
        }
    }

    private void clearFields() {
        pidField.setText("");
        burstField.setText("");
        arrivalField.setText("");
        priorityField.setText("");
        deadlineField.setText("");
        pidField.requestFocusInWindow();
    }

    private void showFeedback(String message, Color color) {
        feedbackLabel.setText("  " + message);
        feedbackLabel.setForeground(color);
    }

    private void clearFeedback() {
        feedbackLabel.setText(" ");
    }

    private Font mono(float size, int style) {
        String[] candidates = {"JetBrains Mono", "Consolas", "Courier New", Font.MONOSPACED};
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] installed = ge.getAvailableFontFamilyNames();
        for (String candidate : candidates) {
            for (String font : installed) {
                if (font.equalsIgnoreCase(candidate)) {
                    return new Font(candidate, style, Math.round(size));
                }
            }
        }
        return new Font(Font.MONOSPACED, style, Math.round(size));
    }
}