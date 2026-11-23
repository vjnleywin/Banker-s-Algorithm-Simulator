import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Banker's Algorithm dashboard (PT101 Topic 5).
 * Uses BankersAlgorithm unchanged. Every value is entered by the user
 * (or generated with Randomize). Invalid input (letters, symbols, decimals,
 * negatives, blanks) is flagged in red and explained in an error banner.
 */
public class BankersDashboard extends JFrame {

    // ---------- palette ----------
    static final Color BG = new Color(0xF1F4F9);
    static final Color CARD = Color.WHITE;
    static final Color INK = new Color(0x0F172A);
    static final Color MUTED = new Color(0x64748B);
    static final Color LINE = new Color(0xE2E8F0);
    static final Color PRIMARY = new Color(0x4F46E5);
    static final Color PRIMARY_DARK = new Color(0x4338CA);
    static final Color PRIMARY_SOFT = new Color(0xEEF2FF);
    static final Color OK = new Color(0x15803D);
    static final Color OK_SOFT = new Color(0xDCFCE7);
    static final Color BAD = new Color(0xDC2626);
    static final Color BAD_DARK = new Color(0x991B1B);
    static final Color BAD_SOFT = new Color(0xFEE2E2);
    static final Color NEUTRAL_SOFT = new Color(0xE2E8F0);

    private static String pickFamily() {
        String[] want = {"Segoe UI", "SF Pro Text", "Helvetica Neue", "Inter", "Roboto"};
        Set<String> have = new HashSet<>(Arrays.asList(
            GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String w : want) if (have.contains(w)) return w;
        return Font.SANS_SERIF;
    }

    static final String FAMILY = pickFamily();
    static final Font BASE = new Font(FAMILY, Font.PLAIN, 14);
    static final Font BOLD = BASE.deriveFont(Font.BOLD);
    static final Font SMALL = BASE.deriveFont(Font.BOLD, 11f);
    static final Font MONO = new Font(Font.MONOSPACED, Font.PLAIN, 13);

    private static final String P_MATRICES = "matrices";
    private static final String P_ANALYSIS = "analysis";
    private static final String P_TRACE = "trace";

    // ---------- state ----------
    private int n, m;
    private int[] lastTotal;
    private int[][] lastAlloc, lastMax;
    private boolean markBlanks;     // highlight blank cells after a failed Calculate
    private boolean bannerLive;     // banner currently shows a live-typing error
    private boolean suppressLive;   // skip live validation during bulk updates
    private final Random random = new Random();

    // ---------- widgets ----------
    private final Field procField = new Field(6, "e.g. 5");
    private final Field resField = new Field(6, "e.g. 3");
    private Field[] totalFields;
    private final JPanel totalsPanel = new JPanel(new BorderLayout());
    private JTable allocTable, maxTable;

    private final JPanel matricesHolder = new JPanel(new BorderLayout());
    private final JPanel analysisHolder = new JPanel(new BorderLayout());
    private final JPanel traceHolder = new JPanel(new BorderLayout());

    private PillButton createBtn, randomBtn, calcBtn, clearBtn;
    private final CardLayout pages = new CardLayout();
    private final JPanel pageHost = new JPanel(pages);
    private final ButtonGroup navGroup = new ButtonGroup();
    private NavTab navMatrices, navAnalysis;
    private JPanel topPanel;
    private ErrorBanner banner;
    private Chip stateChip;
    private final JLabel status = new JLabel();

    public BankersDashboard() {
        super("Banker's Algorithm | PT101 Topic 5");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1260, 840);
        setMinimumSize(new Dimension(1020, 700));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(18, 0));
        body.setBackground(BG);
        body.setBorder(new EmptyBorder(18, 18, 8, 18));
        body.add(buildSidebar(), BorderLayout.WEST);
        body.add(buildMainArea(), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        status.setFont(BASE);
        status.setBorder(new EmptyBorder(8, 22, 12, 22));
        add(status, BorderLayout.SOUTH);

        procField.getDocument().addDocumentListener(new Doc(this::runLiveValidation));
        resField.getDocument().addDocumentListener(new Doc(this::runLiveValidation));

        resetResults();
        setStatus("Enter the number of processes and resource types, then click Create tables.", false);
    }

    // =====================================================================
    //  Layout builders
    // =====================================================================

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, new Color(0x1E1B4B), getWidth(), 0, new Color(0x6D28D9)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(255, 255, 255, 22));
                g2.fillOval(getWidth() - 220, -90, 260, 260);
                g2.fillOval(getWidth() - 360, 20, 140, 140);
                g2.dispose();
            }
        };
        header.setBorder(new EmptyBorder(18, 26, 18, 26));

        header.add(new Logo(), BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Banker's Algorithm");
        title.setFont(BASE.deriveFont(Font.BOLD, 24f));
        title.setForeground(Color.WHITE);
        JLabel sub = new JLabel("Deadlock avoidance simulator  •  PT 101  •  Topic 5");
        sub.setFont(BASE);
        sub.setForeground(new Color(0xDDD6FE));
        text.add(title);
        text.add(Box.createVerticalStrut(3));
        text.add(sub);
        header.add(text, BorderLayout.CENTER);

        stateChip = new Chip("NOT ANALYZED", NEUTRAL_SOFT, MUTED);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 10));
        right.setOpaque(false);
        right.add(stateChip);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JComponent buildSidebar() {
        Card card = new Card(new BorderLayout(0, 12));
        card.setBorder(new EmptyBorder(20, 20, 20, 24));
        card.setPreferredSize(new Dimension(318, 0));

        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

        col.add(stepTitle(1, "System size"));
        col.add(Box.createVerticalStrut(10));
        col.add(labeledRow("Processes", procField));
        col.add(Box.createVerticalStrut(8));
        col.add(labeledRow("Resource types", resField));
        col.add(Box.createVerticalStrut(12));
        createBtn = new PillButton("Create tables", PRIMARY, Color.WHITE, true);
        createBtn.addActionListener(e -> createTables());
        col.add(fullWidth(createBtn));

        col.add(Box.createVerticalStrut(24));
        col.add(stepTitle(2, "Total resources"));
        col.add(Box.createVerticalStrut(10));
        totalsPanel.setOpaque(false);
        totalsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel hint = new JLabel("Create the tables to enter totals.");
        hint.setFont(BASE);
        hint.setForeground(MUTED);
        totalsPanel.add(hint, BorderLayout.CENTER);
        col.add(totalsPanel);

        col.add(Box.createVerticalStrut(24));
        col.add(stepTitle(3, "Actions"));
        col.add(Box.createVerticalStrut(10));
        randomBtn = new PillButton("Randomize matrices", PRIMARY, PRIMARY, false);
        randomBtn.addActionListener(e -> randomizeMatrices());
        col.add(fullWidth(randomBtn));
        col.add(Box.createVerticalStrut(8));
        calcBtn = new PillButton("Calculate & check safety", PRIMARY, Color.WHITE, true);
        calcBtn.addActionListener(e -> calculate());
        col.add(fullWidth(calcBtn));
        col.add(Box.createVerticalStrut(8));
        clearBtn = new PillButton("Clear all values", MUTED, MUTED, false);
        clearBtn.addActionListener(e -> clearValues());
        col.add(fullWidth(clearBtn));

        randomBtn.setEnabled(false);
        calcBtn.setEnabled(false);
        clearBtn.setEnabled(false);

        card.add(col, BorderLayout.NORTH);
        card.add(buildTip(), BorderLayout.SOUTH);
        return card;
    }

    private JComponent buildTip() {
        JPanel tip = new JPanel(new BorderLayout(0, 4)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(PRIMARY_SOFT);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        tip.setOpaque(false);
        tip.setBorder(new EmptyBorder(12, 14, 12, 14));
        JLabel t = new JLabel("Input rules");
        t.setFont(BOLD);
        t.setForeground(PRIMARY_DARK);
        JTextArea body = new JTextArea(
            "• Whole numbers only (0, 1, 2 …)\n"
          + "• No letters, symbols or negatives\n"
          + "• Allocation ≤ Maximum ≤ Total\n"
          + "• Use 0 where a value is none");
        body.setEditable(false);
        body.setOpaque(false);
        body.setFont(BASE.deriveFont(13f));
        body.setForeground(INK);
        tip.add(t, BorderLayout.NORTH);
        tip.add(body, BorderLayout.CENTER);
        return tip;
    }

    private JComponent buildMainArea() {
        JPanel main = new JPanel(new BorderLayout(0, 14));
        main.setOpaque(false);

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        nav.setOpaque(false);
        navMatrices = nav(P_MATRICES, "1   Matrices", nav);
        navAnalysis = nav(P_ANALYSIS, "2   Analysis", nav);
        nav(P_TRACE, "3   Safety trace", nav);
        navMatrices.setSelected(true);

        banner = new ErrorBanner(this::hideError);

        topPanel = new JPanel(new BorderLayout(0, 12));
        topPanel.setOpaque(false);
        topPanel.add(nav, BorderLayout.NORTH);
        topPanel.add(banner, BorderLayout.SOUTH);
        main.add(topPanel, BorderLayout.NORTH);

        for (JPanel p : new JPanel[]{matricesHolder, analysisHolder, traceHolder}) p.setOpaque(false);
        pageHost.setOpaque(false);
        pageHost.add(matricesHolder, P_MATRICES);
        pageHost.add(analysisHolder, P_ANALYSIS);
        pageHost.add(traceHolder, P_TRACE);
        main.add(pageHost, BorderLayout.CENTER);
        return main;
    }

    private NavTab nav(String page, String text, JPanel parent) {
        NavTab tab = new NavTab(text);
        tab.addActionListener(e -> pages.show(pageHost, page));
        navGroup.add(tab);
        parent.add(tab);
        return tab;
    }

    private void showPage(String page, NavTab tab) {
        tab.setSelected(true);
        pages.show(pageHost, page);
    }

    // =====================================================================
    //  Validation
    // =====================================================================

    /** Returns null if the text is empty or a valid non-negative whole number; otherwise a reason. */
    static String problem(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) return null;
        String shown = s.length() > 14 ? s.substring(0, 14) + "…" : s;
        if (s.startsWith("-") && s.substring(1).matches("\\d*")) {
            return "cannot be negative (found \"" + shown + "\"). Use 0 or a positive whole number.";
        }
        if (s.matches("\\d+")) {
            return s.length() > 9 ? "is too large (maximum 9 digits)." : null;
        }
        if (s.matches("[-+]?\\d*[.,]\\d+")) {
            return "must be a whole number, without decimals (found \"" + shown + "\").";
        }
        return "must contain digits only — letters and symbols are not allowed (found \"" + shown + "\").";
    }

    private static String titleFor(String msg) {
        if (msg.contains("blank")) return "Missing values";
        if (msg.contains("negative")) return "Negative number not allowed";
        if (msg.contains("digits only") || msg.contains("without decimals") || msg.contains("too large")) {
            return "Not a valid number";
        }
        return "Check your input";
    }

    private int parseSize(Field f, String name, int max) {
        String s = f.getText().trim();
        if (s.isEmpty()) {
            f.requireFilled = true;
            throw new IllegalArgumentException(name + " is blank. Enter a whole number from 1 to " + max + ".");
        }
        String p = problem(s);
        if (p != null) throw new IllegalArgumentException(name + " " + p);
        int v = Integer.parseInt(s);
        if (v < 1 || v > max) {
            throw new IllegalArgumentException(name + " must be between 1 and " + max + ".");
        }
        return v;
    }

    private int[] readTotals() {
        int[] t = new int[m];
        for (int j = 0; j < m; j++) {
            String nm = "Total " + BankersAlgorithm.resourceName(j);
            String s = totalFields[j].getText().trim();
            if (s.isEmpty()) {
                markBlanks = true;
                refreshFlags();
                throw new IllegalArgumentException(nm + " is blank. Enter 0 if the value is zero.");
            }
            String p = problem(s);
            if (p != null) {
                refreshFlags();
                throw new IllegalArgumentException(nm + " " + p);
            }
            t[j] = Integer.parseInt(s);
        }
        return t;
    }

    private void readInputs() {
        stopEditing();
        int[] total = readTotals();
        int[][] a = new int[n][m];
        int[][] mx = new int[n][m];
        int blanks = 0;
        String firstBlank = null;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                String label = "P" + i + " " + BankersAlgorithm.resourceName(j);
                String sa = cell(allocTable, i, j);
                String sm = cell(maxTable, i, j);
                String pa = problem(sa);
                if (pa != null) { refreshFlags(); throw new IllegalArgumentException("Allocation " + label + " " + pa); }
                String pm = problem(sm);
                if (pm != null) { refreshFlags(); throw new IllegalArgumentException("Maximum " + label + " " + pm); }
                if (sa.isEmpty()) { blanks++; if (firstBlank == null) firstBlank = "Allocation " + label; }
                else a[i][j] = Integer.parseInt(sa);
                if (sm.isEmpty()) { blanks++; if (firstBlank == null) firstBlank = "Maximum " + label; }
                else mx[i][j] = Integer.parseInt(sm);
            }
        }
        if (blanks > 0) {
            markBlanks = true;
            refreshFlags();
            throw new IllegalArgumentException(blanks + (blanks == 1 ? " cell is" : " cells are")
                + " still blank (first: " + firstBlank + "). Enter 0 where the value is zero.");
        }
        lastTotal = total;
        lastAlloc = a;
        lastMax = mx;
    }

    private static String cell(JTable t, int i, int j) {
        Object v = t.getValueAt(i, j + 1);
        return v == null ? "" : v.toString().trim();
    }

    /** Re-applies red styling to every invalid / blank-required field. */
    private void refreshFlags() {
        flag(procField);
        flag(resField);
        if (totalFields != null) {
            for (Field f : totalFields) {
                f.setInvalid(problem(f.getText()) != null || (markBlanks && f.getText().trim().isEmpty()));
            }
        }
        if (allocTable != null) allocTable.repaint();
        if (maxTable != null) maxTable.repaint();
    }

    private void flag(Field f) {
        f.setInvalid(problem(f.getText()) != null || (f.requireFilled && f.getText().trim().isEmpty()));
    }

    /** Runs while the user types: flags bad values immediately. */
    private void runLiveValidation() {
        if (suppressLive) return;
        refreshFlags();
        String msg = firstLiveError();
        if (msg != null) showError(titleFor(msg), msg, true);
        else if (bannerLive) hideError();
    }

    private String firstLiveError() {
        String p;
        if ((p = problem(procField.getText())) != null) return "Processes " + p;
        if ((p = problem(resField.getText())) != null) return "Resource types " + p;
        if (totalFields != null) {
            for (int j = 0; j < totalFields.length; j++) {
                if ((p = problem(totalFields[j].getText())) != null) {
                    return "Total " + BankersAlgorithm.resourceName(j) + " " + p;
                }
            }
        }
        String s = scanTable(allocTable, "Allocation");
        return s != null ? s : scanTable(maxTable, "Maximum");
    }

    private String scanTable(JTable t, String kind) {
        if (t == null) return null;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                String p = problem(cell(t, i, j));
                if (p != null) return kind + " P" + i + " " + BankersAlgorithm.resourceName(j) + " " + p;
            }
        }
        return null;
    }

    private void showError(String title, String msg, boolean live) {
        bannerLive = live;
        banner.showMessage(title, msg);
        topPanel.revalidate();
        topPanel.repaint();
        setStatus("Fix the highlighted input and try again.", true);
    }

    private void hideError() {
        bannerLive = false;
        if (banner.isVisible()) {
            banner.hideMessage();
            topPanel.revalidate();
            topPanel.repaint();
        }
    }

    // =====================================================================
    //  Actions
    // =====================================================================

    private void createTables() {
        int newN, newM;
        try {
            newN = parseSize(procField, "Processes", 30);
            newM = parseSize(resField, "Resource types", 10);
        } catch (IllegalArgumentException ex) {
            refreshFlags();
            showError(titleFor(ex.getMessage()), ex.getMessage(), false);
            return;
        }
        n = newN;
        m = newM;
        procField.requireFilled = false;
        resField.requireFilled = false;
        markBlanks = false;

        // totals
        totalFields = new Field[m];
        JPanel grid = new JPanel(new GridLayout(0, 2, 12, 10));
        grid.setOpaque(false);
        for (int j = 0; j < m; j++) {
            JPanel cell = new JPanel(new BorderLayout(8, 0));
            cell.setOpaque(false);
            JLabel name = new JLabel(BankersAlgorithm.resourceName(j));
            name.setFont(BOLD);
            name.setForeground(PRIMARY_DARK);
            totalFields[j] = new Field(4, "—");
            totalFields[j].getDocument().addDocumentListener(new Doc(this::runLiveValidation));
            cell.add(name, BorderLayout.WEST);
            cell.add(totalFields[j], BorderLayout.CENTER);
            grid.add(cell);
        }
        totalsPanel.removeAll();
        totalsPanel.add(grid, BorderLayout.CENTER);
        totalsPanel.revalidate();
        totalsPanel.repaint();

        // matrices
        String[] headers = new String[m + 1];
        headers[0] = "Process";
        for (int j = 0; j < m; j++) headers[j + 1] = BankersAlgorithm.resourceName(j);
        allocTable = inputTable("Allocation", headers);
        maxTable = inputTable("Maximum", headers);

        JPanel tables = new JPanel(new GridLayout(1, 2, 16, 0));
        tables.setOpaque(false);
        tables.add(tableCard("Allocation matrix", "What each process currently holds.", allocTable));
        tables.add(tableCard("Maximum matrix", "The most each process may ever request.", maxTable));
        matricesHolder.removeAll();
        matricesHolder.add(tables, BorderLayout.CENTER);
        matricesHolder.revalidate();
        matricesHolder.repaint();

        hideError();
        resetResults();
        refreshFlags();

        randomBtn.setEnabled(true);
        calcBtn.setEnabled(true);
        clearBtn.setEnabled(true);
        showPage(P_MATRICES, navMatrices);
        setStatus("Tables created. Enter the totals and every matrix value (use 0 for none), or click Randomize.", false);
    }

    private void randomizeMatrices() {
        try {
            int[] totals = readTotals();
            int[][] a = new int[n][m];
            int[][] mx = new int[n][m];
            for (int j = 0; j < m; j++) {
                int target = random.nextInt(totals[j] + 1);
                for (int u = 0; u < target; u++) a[random.nextInt(n)][j]++;
                for (int i = 0; i < n; i++) {
                    mx[i][j] = a[i][j] + random.nextInt(totals[j] - a[i][j] + 1);
                }
            }
            stopEditing();
            suppressLive = true;
            try {
                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < m; j++) {
                        allocTable.setValueAt(a[i][j], i, j + 1);
                        maxTable.setValueAt(mx[i][j], i, j + 1);
                    }
                }
            } finally {
                suppressLive = false;
            }
            markBlanks = false;
            hideError();
            refreshFlags();
            resetResults();
            showPage(P_MATRICES, navMatrices);
            setStatus("Random matrices generated. The state may be SAFE or UNSAFE — click Calculate to find out. You can still edit any cell.", false);
        } catch (IllegalArgumentException ex) {
            showError(titleFor(ex.getMessage()), ex.getMessage(), false);
        }
    }

    private void clearValues() {
        stopEditing();
        suppressLive = true;
        try {
            for (Field f : totalFields) f.setText("");
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < m; j++) {
                    allocTable.setValueAt(null, i, j + 1);
                    maxTable.setValueAt(null, i, j + 1);
                }
            }
        } finally {
            suppressLive = false;
        }
        markBlanks = false;
        hideError();
        refreshFlags();
        resetResults();
        showPage(P_MATRICES, navMatrices);
        setStatus("All values cleared.", false);
    }

    private void calculate() {
        try {
            readInputs();
            BankersAlgorithm.Result r = BankersAlgorithm.analyze(lastTotal, lastAlloc, lastMax);
            markBlanks = false;
            hideError();
            refreshFlags();
            showAnalysis(r);
            showTrace(r);
            if (r.safe) stateChip.set("SAFE STATE", OK_SOFT, OK);
            else stateChip.set("UNSAFE STATE", BAD_SOFT, BAD);
            showPage(P_ANALYSIS, navAnalysis);
            setStatus(r.safe
                ? "System is SAFE. Safe sequence: " + sequenceText(r.sequence)
                : "System is UNSAFE. No complete safe sequence exists (this does not necessarily mean a deadlock).",
                !r.safe);
        } catch (IllegalArgumentException ex) {
            showError(titleFor(ex.getMessage()), ex.getMessage(), false);
            showPage(P_MATRICES, navMatrices);
        }
    }

    // =====================================================================
    //  Result pages
    // =====================================================================

    private void resetResults() {
        analysisHolder.removeAll();
        analysisHolder.add(emptyState("No analysis yet",
            "Fill in the matrices and click \"Calculate & check safety\"."), BorderLayout.CENTER);
        traceHolder.removeAll();
        traceHolder.add(emptyState("No safety trace yet",
            "The step-by-step Work / Need / Allocation trace appears here."), BorderLayout.CENTER);
        if (allocTable == null) {
            matricesHolder.removeAll();
            matricesHolder.add(emptyState("Start by creating tables",
                "Type the number of processes and resource types in the sidebar."), BorderLayout.CENTER);
        }
        if (stateChip != null) stateChip.set("NOT ANALYZED", NEUTRAL_SOFT, MUTED);
        for (JPanel p : new JPanel[]{analysisHolder, traceHolder, matricesHolder}) {
            p.revalidate();
            p.repaint();
        }
    }

    private void showAnalysis(BankersAlgorithm.Result r) {
        analysisHolder.removeAll();

        JPanel stats = new JPanel(new GridLayout(1, 3, 16, 0));
        stats.setOpaque(false);
        stats.setBorder(new EmptyBorder(0, 0, 16, 0));
        stats.add(statCard("TOTAL", BankersAlgorithm.vector(lastTotal), INK));
        stats.add(statCard("ALLOCATED", BankersAlgorithm.vector(r.totalAllocated), INK));
        stats.add(statCard("AVAILABLE", BankersAlgorithm.vector(r.available), PRIMARY));

        DefaultTableModel resModel = readOnlyModel(new String[]{"Resource", "Total", "Allocated", "Available"});
        for (int j = 0; j < m; j++) {
            resModel.addRow(new Object[]{BankersAlgorithm.resourceName(j), lastTotal[j],
                r.totalAllocated[j], r.available[j]});
        }
        JTable resTable = styledTable(new JTable(resModel));

        DefaultTableModel procModel = readOnlyModel(new String[]{"Process", "Allocation", "Maximum", "Need"});
        for (int i = 0; i < n; i++) {
            procModel.addRow(new Object[]{"P" + i, BankersAlgorithm.vector(lastAlloc[i]),
                BankersAlgorithm.vector(lastMax[i]), BankersAlgorithm.vector(r.need[i])});
        }
        JTable procTable = styledTable(new JTable(procModel));
        procTable.getColumnModel().getColumn(3).setCellRenderer(new AccentRenderer());

        JPanel lower = new JPanel(new GridLayout(1, 2, 16, 0));
        lower.setOpaque(false);
        lower.add(tableCard("Resource computation", "Available = Total − Allocated", resTable));
        lower.add(tableCard("Need computation", "Need = Maximum − Allocation", procTable));

        analysisHolder.add(stats, BorderLayout.NORTH);
        analysisHolder.add(lower, BorderLayout.CENTER);
        analysisHolder.revalidate();
        analysisHolder.repaint();
    }

    private void showTrace(BankersAlgorithm.Result r) {
        traceHolder.removeAll();

        Card banner = new Card(new FlowLayout(FlowLayout.LEFT, 10, 14));
        if (r.safe) {
            banner.add(new Chip("SAFE STATE", OK_SOFT, OK));
            JLabel l = new JLabel("Safe sequence:");
            l.setFont(BOLD);
            l.setForeground(MUTED);
            banner.add(l);
            for (int k = 0; k < r.sequence.length; k++) {
                banner.add(new Chip("P" + r.sequence[k], PRIMARY_SOFT, PRIMARY_DARK));
                if (k < r.sequence.length - 1) {
                    JLabel arrow = new JLabel("→");
                    arrow.setFont(BASE.deriveFont(Font.BOLD, 16f));
                    arrow.setForeground(MUTED);
                    banner.add(arrow);
                }
            }
        } else {
            banner.add(new Chip("UNSAFE STATE", BAD_SOFT, BAD));
            JLabel l = new JLabel("Cannot finish:");
            l.setFont(BOLD);
            l.setForeground(MUTED);
            banner.add(l);
            for (int i = 0; i < r.finish.length; i++) {
                if (!r.finish[i]) banner.add(new Chip("P" + i, BAD_SOFT, BAD));
            }
        }

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(new EmptyBorder(16, 0, 0, 4));

        JLabel init = new JLabel("Initial Work = Available = " + BankersAlgorithm.vector(r.available)
            + "   (all Finish flags start FALSE)");
        init.setFont(BASE);
        init.setForeground(MUTED);
        init.setAlignmentX(Component.LEFT_ALIGNMENT);
        list.add(init);
        list.add(Box.createVerticalStrut(12));

        int k = 1;
        for (BankersAlgorithm.Step s : r.steps) {
            list.add(stepCard(k++, s));
            list.add(Box.createVerticalStrut(12));
        }
        if (!r.safe) {
            JLabel stuck = new JLabel(r.steps.isEmpty()
                ? "No process has Need ≤ Work, so nothing can run."
                : "No remaining process satisfies Need ≤ Work — the search stops here.");
            stuck.setFont(BOLD);
            stuck.setForeground(BAD);
            stuck.setAlignmentX(Component.LEFT_ALIGNMENT);
            list.add(stuck);
        }

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(list, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(wrap);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        traceHolder.add(banner, BorderLayout.NORTH);
        traceHolder.add(scroll, BorderLayout.CENTER);
        traceHolder.revalidate();
        traceHolder.repaint();
    }

    private JComponent stepCard(int k, BankersAlgorithm.Step s) {
        Card c = new Card(new BorderLayout(16, 0));
        c.setBorder(new EmptyBorder(14, 16, 16, 18));
        c.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        JLabel step = new JLabel("STEP " + k);
        step.setFont(SMALL);
        step.setForeground(MUTED);
        step.setAlignmentX(Component.LEFT_ALIGNMENT);
        Chip chip = new Chip("P" + s.process, PRIMARY, Color.WHITE);
        chip.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(step);
        left.add(Box.createVerticalStrut(4));
        left.add(chip);
        c.add(left, BorderLayout.WEST);

        String text =
            "Need " + BankersAlgorithm.vector(s.need) + " <= Work " + BankersAlgorithm.vector(s.before) + "   TRUE\n"
          + "Work = " + BankersAlgorithm.vector(s.before) + " + " + BankersAlgorithm.vector(s.released)
          + " = " + BankersAlgorithm.vector(s.after) + "\n"
          + "Finish[P" + s.process + "] = TRUE";
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setOpaque(false);
        area.setFont(MONO);
        area.setForeground(INK);
        c.add(area, BorderLayout.CENTER);

        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        return c;
    }

    // =====================================================================
    //  Helpers
    // =====================================================================

    private void stopEditing() {
        if (allocTable != null && allocTable.isEditing()) allocTable.getCellEditor().stopCellEditing();
        if (maxTable != null && maxTable.isEditing()) maxTable.getCellEditor().stopCellEditing();
    }

    private static String sequenceText(int[] seq) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < seq.length; i++) {
            if (i > 0) sb.append(" → ");
            sb.append("P").append(seq[i]);
        }
        return sb.toString();
    }

    private void setStatus(String msg, boolean error) {
        status.setText((error ? "⚠  " : "") + msg);
        status.setForeground(error ? BAD : MUTED);
    }

    // ---------- component factories ----------

    private JComponent stepTitle(int number, String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.setOpaque(false);
        p.add(new Badge(number));
        JLabel l = new JLabel(text);
        l.setFont(BASE.deriveFont(Font.BOLD, 15f));
        l.setForeground(INK);
        l.setBorder(new EmptyBorder(0, 10, 0, 0));
        p.add(l);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        return p;
    }

    private JComponent labeledRow(String label, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(BASE);
        l.setForeground(INK);
        field.setPreferredSize(new Dimension(96, 36));
        row.add(l, BorderLayout.CENTER);
        row.add(field, BorderLayout.EAST);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        return row;
    }

    private JComponent fullWidth(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        c.setPreferredSize(new Dimension(100, 40));
        return c;
    }

    private JComponent emptyState(String title, String msg) {
        Card c = new Card(new GridBagLayout());
        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        IconCircle icon = new IconCircle();
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel t = new JLabel(title);
        t.setFont(BASE.deriveFont(Font.BOLD, 18f));
        t.setForeground(INK);
        t.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel d = new JLabel(msg);
        d.setFont(BASE);
        d.setForeground(MUTED);
        d.setAlignmentX(Component.CENTER_ALIGNMENT);
        inner.add(icon);
        inner.add(Box.createVerticalStrut(14));
        inner.add(t);
        inner.add(Box.createVerticalStrut(6));
        inner.add(d);
        c.add(inner);
        return c;
    }

    private JComponent statCard(String label, String value, Color valueColor) {
        Card c = new Card(new BorderLayout(0, 4));
        c.setBorder(new EmptyBorder(16, 20, 18, 20));
        JLabel l = new JLabel(label);
        l.setFont(SMALL);
        l.setForeground(MUTED);
        JLabel v = new JLabel(value);
        v.setFont(new Font(Font.MONOSPACED, Font.BOLD, 24));
        v.setForeground(valueColor);
        c.add(l, BorderLayout.NORTH);
        c.add(v, BorderLayout.CENTER);
        return c;
    }

    private JComponent tableCard(String title, String caption, JTable table) {
        Card c = new Card(new BorderLayout(0, 12));
        c.setBorder(new EmptyBorder(16, 18, 20, 20));
        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(title);
        t.setFont(BASE.deriveFont(Font.BOLD, 16f));
        t.setForeground(INK);
        JLabel d = new JLabel(caption);
        d.setFont(BASE);
        d.setForeground(MUTED);
        head.add(t);
        head.add(Box.createVerticalStrut(2));
        head.add(d);
        c.add(head, BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(LINE));
        sp.getViewport().setBackground(Color.WHITE);
        c.add(sp, BorderLayout.CENTER);
        return c;
    }

    // ---------- tables ----------

    private DefaultTableModel readOnlyModel(String[] headers) {
        return new DefaultTableModel(headers, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private JTable inputTable(String kind, String[] headers) {
        DefaultTableModel model = new DefaultTableModel(headers, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c != 0; }
        };
        for (int i = 0; i < n; i++) {
            Object[] row = new Object[m + 1];
            row[0] = "P" + i;
            model.addRow(row);
        }
        model.addTableModelListener(e -> runLiveValidation());

        final JTable t = styledTable(new JTable(model));
        t.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        t.setDefaultRenderer(Object.class, new InputRenderer());

        // The editor flags bad input while the user is still typing.
        final JTextField ed = new JTextField();
        ed.setHorizontalAlignment(JTextField.CENTER);
        ed.setFont(BASE);
        final Border okB = BorderFactory.createLineBorder(PRIMARY, 2);
        final Border badB = BorderFactory.createLineBorder(BAD, 2);
        ed.setBorder(okB);
        ed.getDocument().addDocumentListener(new Doc(() -> {
            int r = t.getEditingRow(), c = t.getEditingColumn();
            if (r < 0 || c < 1) return;
            String pr = problem(ed.getText());
            ed.setBorder(pr != null ? badB : okB);
            ed.setBackground(pr != null ? BAD_SOFT : Color.WHITE);
            if (pr != null) {
                String msg = kind + " P" + r + " " + BankersAlgorithm.resourceName(c - 1) + " " + pr;
                showError(titleFor(msg), msg, true);
            } else {
                runLiveValidation();
            }
        }));
        t.setDefaultEditor(Object.class, new DefaultCellEditor(ed));
        return t;
    }

    private JTable styledTable(JTable t) {
        t.setRowHeight(36);
        t.setFont(BASE);
        t.setForeground(INK);
        t.setGridColor(LINE);
        t.setShowVerticalLines(false);
        t.setShowHorizontalLines(true);
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setSelectionBackground(PRIMARY_SOFT);
        t.setSelectionForeground(INK);
        t.setFillsViewportHeight(true);
        t.setDefaultRenderer(Object.class, new CellRenderer());
        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setPreferredSize(new Dimension(100, 36));
        h.setDefaultRenderer(new HeaderRenderer());
        return t;
    }

    /** Centered cells; first column bold as the row label. */
    private static class CellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        CellRenderer() { setHorizontalAlignment(CENTER); }
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                                 boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, false, row, col);
            setFont(col == 0 ? BOLD : BASE);
            setForeground(col == 0 ? PRIMARY_DARK : INK);
            if (!sel) setBackground(row % 2 == 0 ? Color.WHITE : new Color(0xFAFBFD));
            return this;
        }
    }

    /** Input cells: invalid values (letters, negatives, decimals) and flagged blanks turn red. */
    private class InputRenderer extends CellRenderer {
        private static final long serialVersionUID = 1L;
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                                 boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, focus, row, col);
            setBorder(BorderFactory.createEmptyBorder());
            if (col == 0) return this;
            String s = v == null ? "" : v.toString().trim();
            boolean bad = problem(s) != null;
            boolean blank = s.isEmpty() && markBlanks;
            if (bad) {
                setBackground(BAD_SOFT);
                setForeground(BAD);
                setFont(BOLD);
                setBorder(BorderFactory.createLineBorder(BAD, 2));
            } else if (blank) {
                setBackground(BAD_SOFT);
                setBorder(BorderFactory.createLineBorder(new Color(0xFCA5A5), 1));
            }
            return this;
        }
    }

    private static class AccentRenderer extends CellRenderer {
        private static final long serialVersionUID = 1L;
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                                 boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, focus, row, col);
            setFont(MONO.deriveFont(Font.BOLD));
            setForeground(PRIMARY_DARK);
            return this;
        }
    }

    private static class HeaderRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        HeaderRenderer() { setHorizontalAlignment(CENTER); setOpaque(true); }
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                                 boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, v, false, false, row, col);
            setBackground(PRIMARY_SOFT);
            setForeground(PRIMARY_DARK);
            setFont(BOLD);
            setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, LINE));
            return this;
        }
    }

    // =====================================================================
    //  Custom-painted components
    // =====================================================================

    static Color mix(Color a, Color b, double t) {
        return new Color(
            (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
            (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    /** Simple adapter: run something on every document change. */
    static class Doc implements DocumentListener {
        private final Runnable r;
        Doc(Runnable r) { this.r = r; }
        @Override public void insertUpdate(DocumentEvent e) { r.run(); }
        @Override public void removeUpdate(DocumentEvent e) { r.run(); }
        @Override public void changedUpdate(DocumentEvent e) { r.run(); }
    }

    /** White rounded panel with a soft shadow. */
    static class Card extends JPanel {
        private static final long serialVersionUID = 1L;
        Card(LayoutManager lm) {
            super(lm);
            setOpaque(false);
            setBorder(new EmptyBorder(8, 12, 12, 16));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth() - 4, h = getHeight() - 4;
            for (int i = 4; i >= 1; i--) {
                g2.setColor(new Color(15, 23, 42, 9));
                g2.fillRoundRect(i, i, w, h, 20, 20);
            }
            g2.setColor(CARD);
            g2.fillRoundRect(0, 0, w, h, 20, 20);
            g2.setColor(LINE);
            g2.drawRoundRect(0, 0, w - 1, h - 1, 20, 20);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Small rounded status label. */
    static class Chip extends JLabel {
        private static final long serialVersionUID = 1L;
        private Color bg;
        Chip(String text, Color bg, Color fg) {
            super(text);
            setOpaque(false);
            setFont(BOLD);
            setBorder(new EmptyBorder(5, 13, 5, 13));
            set(text, bg, fg);
        }
        void set(String text, Color bg, Color fg) {
            this.bg = bg;
            setText(text);
            setForeground(fg);
            repaint();
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Rounded text field with hint text, focus ring and red invalid state. */
    static class Field extends JTextField {
        private static final long serialVersionUID = 1L;
        private final String hint;
        private boolean invalid, focused;
        boolean requireFilled;

        Field(int cols, String hint) {
            super(cols);
            this.hint = hint;
            setFont(BASE);
            setHorizontalAlignment(CENTER);
            setOpaque(false);
            setForeground(INK);
            setCaretColor(PRIMARY);
            setBorder(new EmptyBorder(7, 8, 7, 8));
            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { focused = true; repaint(); }
                @Override public void focusLost(FocusEvent e) { focused = false; repaint(); }
            });
        }

        void setInvalid(boolean b) {
            if (invalid != b) {
                invalid = b;
                setForeground(b ? BAD : INK);
                repaint();
            }
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(invalid ? BAD_SOFT : Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.dispose();
            super.paintComponent(g);

            Graphics2D h = (Graphics2D) g.create();
            h.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (getText().isEmpty() && !focused && hint != null) {
                h.setColor(new Color(0x94A3B8));
                h.setFont(getFont());
                FontMetrics fm = h.getFontMetrics();
                h.drawString(hint, (getWidth() - fm.stringWidth(hint)) / 2,
                    (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            }
            boolean thick = invalid || focused;
            h.setStroke(new BasicStroke(thick ? 2f : 1f));
            h.setColor(invalid ? BAD : focused ? PRIMARY : new Color(0xCBD5E1));
            h.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);
            h.dispose();
        }
    }

    /** Rounded button: filled (gradient) or outlined. */
    static class PillButton extends JButton {
        private static final long serialVersionUID = 1L;
        private final Color accent;
        private final Color textColor;
        private final boolean filled;
        PillButton(String text, Color accent, Color textColor, boolean filled) {
            super(text);
            this.accent = accent;
            this.textColor = textColor;
            this.filled = filled;
            setFont(BOLD);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(8, 16, 8, 16));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth() - 1, h = getHeight() - 1;
            boolean on = isEnabled();
            if (filled) {
                if (!on) {
                    g2.setColor(new Color(0xCBD5E1));
                } else {
                    Color top = mix(accent, Color.WHITE, getModel().isRollover() ? 0.22 : 0.1);
                    Color bottom = getModel().isPressed() ? accent.darker() : accent;
                    g2.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
                }
                g2.fillRoundRect(0, 0, w, h, 14, 14);
                setForeground(on ? textColor : Color.WHITE);
            } else {
                g2.setColor(getModel().isRollover() && on ? PRIMARY_SOFT : Color.WHITE);
                g2.fillRoundRect(0, 0, w, h, 14, 14);
                g2.setColor(on ? accent : LINE);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, w, h, 14, 14);
                setForeground(on ? textColor : new Color(0x94A3B8));
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Segmented page tab. */
    static class NavTab extends JToggleButton {
        private static final long serialVersionUID = 1L;
        NavTab(String text) {
            super(text);
            setFont(BOLD);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(10, 20, 10, 20));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (isSelected()) {
                g2.setPaint(new GradientPaint(0, 0, PRIMARY, getWidth(), 0, new Color(0x7C3AED)));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                setForeground(Color.WHITE);
            } else {
                g2.setColor(getModel().isRollover() ? NEUTRAL_SOFT : new Color(0xE7EBF3));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                setForeground(MUTED);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Numbered circle for sidebar steps. */
    static class Badge extends JComponent {
        private static final long serialVersionUID = 1L;
        private final int number;
        Badge(int number) {
            this.number = number;
            setPreferredSize(new Dimension(26, 26));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, PRIMARY, 26, 26, new Color(0x7C3AED)));
            g2.fillOval(0, 0, 25, 25);
            g2.setColor(Color.WHITE);
            g2.setFont(BOLD.deriveFont(13f));
            FontMetrics fm = g2.getFontMetrics();
            String s = String.valueOf(number);
            g2.drawString(s, (25 - fm.stringWidth(s)) / 2, (25 - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    /** App logo in the header. */
    static class Logo extends JComponent {
        private static final long serialVersionUID = 1L;
        Logo() { setPreferredSize(new Dimension(48, 48)); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(new Color(255, 255, 255, 45));
            g2.fillRoundRect(0, 0, 47, 47, 16, 16);
            g2.setColor(new Color(255, 255, 255, 110));
            g2.drawRoundRect(0, 0, 47, 47, 16, 16);
            g2.setColor(Color.WHITE);
            g2.setFont(BASE.deriveFont(Font.BOLD, 26f));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString("B", (48 - fm.stringWidth("B")) / 2, (48 - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    /** Decorative icon for empty states. */
    static class IconCircle extends JComponent {
        private static final long serialVersionUID = 1L;
        IconCircle() { setPreferredSize(new Dimension(76, 76)); setMaximumSize(new Dimension(76, 76)); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(PRIMARY_SOFT);
            g2.fillOval(0, 0, 75, 75);
            g2.setColor(PRIMARY);
            g2.fillRoundRect(22, 24, 32, 6, 6, 6);
            g2.fillRoundRect(22, 35, 24, 6, 6, 6);
            g2.fillRoundRect(22, 46, 28, 6, 6, 6);
            g2.dispose();
        }
    }

    /** Large, hard-to-miss red error banner with a short attention flash. */
    static class ErrorBanner extends JPanel {
        private static final long serialVersionUID = 1L;
        private final JLabel title = new JLabel();
        private final JTextArea msg = new JTextArea();
        private boolean flash;
        private int ticks;
        private Timer timer;
        private String lastText = "";

        ErrorBanner(Runnable onClose) {
            super(new BorderLayout(14, 0));
            setOpaque(false);
            setBorder(new EmptyBorder(14, 22, 14, 14));
            setVisible(false);

            JComponent icon = new JComponent() {
                private static final long serialVersionUID = 1L;
                { setPreferredSize(new Dimension(38, 38)); }
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(BAD);
                    g2.fillOval(0, 0, 37, 37);
                    g2.setColor(Color.WHITE);
                    g2.setFont(BASE.deriveFont(Font.BOLD, 24f));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString("!", (37 - fm.stringWidth("!")) / 2, (37 - fm.getHeight()) / 2 + fm.getAscent());
                    g2.dispose();
                }
            };
            JPanel iconWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            iconWrap.setOpaque(false);
            iconWrap.add(icon);
            add(iconWrap, BorderLayout.WEST);

            title.setFont(BASE.deriveFont(Font.BOLD, 16f));
            title.setForeground(BAD_DARK);
            msg.setEditable(false);
            msg.setOpaque(false);
            msg.setLineWrap(true);
            msg.setWrapStyleWord(true);
            msg.setFont(BASE.deriveFont(14f));
            msg.setForeground(BAD_DARK);
            msg.setBorder(null);
            JPanel center = new JPanel(new BorderLayout(0, 3));
            center.setOpaque(false);
            center.add(title, BorderLayout.NORTH);
            center.add(msg, BorderLayout.CENTER);
            add(center, BorderLayout.CENTER);

            JButton close = new JButton("×");
            close.setFont(BASE.deriveFont(Font.BOLD, 24f));
            close.setForeground(BAD);
            close.setContentAreaFilled(false);
            close.setBorderPainted(false);
            close.setFocusPainted(false);
            close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            close.setToolTipText("Dismiss");
            close.addActionListener(e -> onClose.run());
            JPanel closeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            closeWrap.setOpaque(false);
            closeWrap.add(close);
            add(closeWrap, BorderLayout.EAST);
        }

        void showMessage(String t, String m) {
            String text = t + "|" + m;
            boolean same = isVisible() && text.equals(lastText);
            lastText = text;
            title.setText(t);
            msg.setText(m);
            setVisible(true);
            if (!same) startFlash();
        }

        void hideMessage() {
            if (timer != null) timer.stop();
            flash = false;
            lastText = "";
            setVisible(false);
        }

        private void startFlash() {
            if (timer != null) timer.stop();
            ticks = 0;
            timer = new Timer(130, e -> {
                flash = !flash;
                repaint();
                if (++ticks >= 7) {
                    flash = false;
                    repaint();
                    timer.stop();
                }
            });
            timer.start();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth() - 1, h = getHeight() - 1;
            g2.setColor(flash ? new Color(0xFECACA) : BAD_SOFT);
            g2.fillRoundRect(0, 0, w, h, 18, 18);
            g2.setColor(BAD);
            g2.fillRoundRect(0, 0, 9, h, 9, 9);
            g2.setColor(flash ? BAD_DARK : BAD);
            g2.setStroke(new BasicStroke(flash ? 3f : 2f));
            g2.drawRoundRect(1, 1, w - 2, h - 2, 18, 18);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // =====================================================================

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) { }
        SwingUtilities.invokeLater(() -> new BankersDashboard().setVisible(true));
    }
}
