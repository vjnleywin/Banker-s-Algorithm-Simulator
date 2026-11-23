import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Random;

/** Java Swing UI for PT101 Topic 5. All inputs are manual. */
public class BankersApp extends JFrame {
    private final Color navy = new Color(30, 45, 66);
    private final Color blue = new Color(49, 103, 170);
    private final Color background = new Color(246, 248, 251);
    private final JTextField processCount = new JTextField(5);
    private final JTextField resourceCount = new JTextField(5);
    private final JTextArea output = new JTextArea(19, 70);
    private JTable allocationTable;
    private JTable maximumTable;
    private JTextField[] totalFields;
    private int n, m;
    private BankersAlgorithm.Result lastResult;
    private int[] lastTotal;
    private int[][] lastAllocation, lastMaximum;
    private JPanel inputPanel;
    private JButton calculateButton;
    private JButton requestButton;
    private JButton randomizeButton;
    private final Random random = new Random();

    public BankersApp() {
        super("Banker's Algorithm | PT101 Topic 5");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1060, 800);
        setMinimumSize(new Dimension(800, 650));
        setLocationRelativeTo(null);
        getContentPane().setBackground(background);
        setLayout(new BorderLayout(12, 12));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(navy);
        header.setBorder(new EmptyBorder(18, 22, 18, 22));
        JLabel title = new JLabel("BANKER'S ALGORITHM  |  DEADLOCK AVOIDANCE");
        title.setFont(new Font("SansSerif", Font.BOLD, 19));
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("PT 101  •  Topic 5  •  Enter totals, randomize matrices, and check safety");
        subtitle.setForeground(new Color(213, 224, 238));
        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBackground(background);
        content.setBorder(new EmptyBorder(12, 18, 18, 18));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        controls.setBackground(Color.WHITE);
        controls.add(new JLabel("Processes:"));
        controls.add(processCount);
        controls.add(new JLabel("Resource types:"));
        controls.add(resourceCount);
        JButton create = button("Create Empty Tables");
        create.addActionListener(e -> createTables());
        controls.add(create);
        randomizeButton = button("Randomize Matrices");
        randomizeButton.setEnabled(false);
        randomizeButton.addActionListener(e -> randomizeMatrices());
        controls.add(randomizeButton);
        calculateButton = button("Calculate & Check Safety");
        calculateButton.setEnabled(false);
        calculateButton.addActionListener(e -> calculate());
        controls.add(calculateButton);
        requestButton = button("Check Resource Request");
        requestButton.setEnabled(false);
        requestButton.addActionListener(e -> checkRequest());
        controls.add(requestButton);
        content.add(controls, BorderLayout.NORTH);

        inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(BorderFactory.createTitledBorder("1. System Inputs"));
        inputPanel.add(new JLabel("  Enter process and resource counts, then click Create Empty Tables."),
            BorderLayout.CENTER);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setTopComponent(new JScrollPane(inputPanel));
        output.setEditable(false);
        output.setFont(new Font("Monospaced", Font.PLAIN, 13));
        output.setBackground(new Color(253, 254, 255));
        output.setForeground(navy);
        output.setMargin(new Insets(12, 14, 12, 14));
        output.setText("No preloaded values. Create tables and enter your own data to begin.");
        JScrollPane results = new JScrollPane(output);
        results.setBorder(BorderFactory.createTitledBorder("2. Computation and Safety Trace"));
        split.setBottomComponent(results);
        split.setResizeWeight(0.46);
        content.add(split, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
    }

    private JButton button(String label) {
        JButton b = new JButton(label);
        b.setBackground(blue);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorder(new EmptyBorder(9, 12, 9, 12));
        return b;
    }

    private int positiveInt(String raw, String name, int limit) {
        int value;
        try {
            value = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be a whole number.");
        }
        if (value < 1 || value > limit) {
            throw new IllegalArgumentException(name + " must be between 1 and " + limit + ".");
        }
        return value;
    }

    private void createTables() {
        try {
            n = positiveInt(processCount.getText(), "Processes", 30);
            m = positiveInt(resourceCount.getText(), "Resource types", 10);
            lastResult = null;
            requestButton.setEnabled(false);
            totalFields = new JTextField[m];
            JPanel totals = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 7));
            totals.setBackground(Color.WHITE);
            totals.add(new JLabel("Total resources:"));
            for (int j = 0; j < m; j++) {
                totals.add(new JLabel(BankersAlgorithm.resourceName(j)));
                totalFields[j] = new JTextField(4);
                totals.add(totalFields[j]);
            }
            String[] headers = new String[m + 1];
            headers[0] = "Process";
            for (int j = 0; j < m; j++) headers[j + 1] = BankersAlgorithm.resourceName(j);
            allocationTable = new JTable(model(headers));
            maximumTable = new JTable(model(headers));
            allocationTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
            maximumTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
            JPanel tables = new JPanel(new GridLayout(1, 2, 10, 0));
            tables.setBackground(Color.WHITE);
            tables.add(titledTable("Allocation Matrix", allocationTable));
            tables.add(titledTable("Maximum Matrix", maximumTable));
            inputPanel.removeAll();
            inputPanel.add(totals, BorderLayout.NORTH);
            inputPanel.add(tables, BorderLayout.CENTER);
            inputPanel.revalidate();
            inputPanel.repaint();
            calculateButton.setEnabled(true);
            randomizeButton.setEnabled(true);
            output.setText("Tables created. Enter every value (including zero), then click Calculate & Check Safety.\n");
        } catch (IllegalArgumentException e) {
            error(e.getMessage());
        }
    }

    // Generate valid random matrices from the user's process count,
    // resource count, and total resources. Users can still edit the tables.
    private void randomizeMatrices() {
        try {
            int[] totals = new int[m];

            for (int j = 0; j < m; j++) {
                totals[j] = parseCell(
                    totalFields[j].getText(),
                    "Total " + BankersAlgorithm.resourceName(j)
                );
            }

            int[][] generatedAllocation = new int[n][m];
            int[][] generatedMaximum = new int[n][m];

            // Pick a fresh, random allocated total for each resource type.
            // Distribute individual units among randomly chosen processes.
            // This avoids favoring the first process and uses no fixed reserve.
            for (int j = 0; j < m; j++) {
                int targetAllocation = random.nextInt(totals[j] + 1);

                for (int unit = 0; unit < targetAllocation; unit++) {
                    int process = random.nextInt(n);
                    generatedAllocation[process][j]++;
                }

                // Each process's maximum claim is independently randomized.
                // Allocation <= Maximum <= Total for each resource type.
                for (int i = 0; i < n; i++) {
                    int allocated = generatedAllocation[i][j];
                    int extraClaim = random.nextInt(totals[j] - allocated + 1);
                    generatedMaximum[i][j] = allocated + extraClaim;
                }
            }

            if (allocationTable.isEditing()) {
                allocationTable.getCellEditor().stopCellEditing();
            }
            if (maximumTable.isEditing()) {
                maximumTable.getCellEditor().stopCellEditing();
            }

            for (int i = 0; i < n; i++) {
                for (int j = 0; j < m; j++) {
                    allocationTable.setValueAt(generatedAllocation[i][j], i, j + 1);
                    maximumTable.setValueAt(generatedMaximum[i][j], i, j + 1);
                }
            }

            lastResult = null;
            requestButton.setEnabled(false);
            output.setText(
                "Random Allocation and Maximum matrices generated.\n" +
                "These are valid resource counts, but the resulting state " +
                "may be SAFE or UNSAFE.\n" +
                "Click Calculate & Check Safety to see the computations.\n" +
                "You can also edit any matrix value manually."
            );

        } catch (IllegalArgumentException exception) {
            error(exception.getMessage());
        }
    }

    private DefaultTableModel model(String[] headers) {
        DefaultTableModel model = new DefaultTableModel(headers, 0) {
            @Override public boolean isCellEditable(int row, int col) {
                return col != 0;
            }
        };
        for (int i = 0; i < n; i++) {
            Object[] row = new Object[m + 1];
            row[0] = "P" + i;
            model.addRow(row);
        }
        return model;
    }

    private JPanel titledTable(String name, JTable table) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(name));
        table.setRowHeight(27);
        table.getTableHeader().setBackground(new Color(227, 236, 248));
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private int parseCell(Object cell, String name) {
        if (cell == null || cell.toString().trim().isEmpty()) {
            throw new IllegalArgumentException(name + " is blank. Enter 0 if the value is zero.");
        }
        try {
            int v = Integer.parseInt(cell.toString().trim());
            if (v < 0) throw new NumberFormatException();
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be a nonnegative whole number.");
        }
    }

    private void readInputs() {
        if (allocationTable.isEditing()) allocationTable.getCellEditor().stopCellEditing();
        if (maximumTable.isEditing()) maximumTable.getCellEditor().stopCellEditing();
        int[] total = new int[m];
        int[][] allocation = new int[n][m];
        int[][] maximum = new int[n][m];
        for (int j = 0; j < m; j++) {
            total[j] = parseCell(totalFields[j].getText(), "Total " + BankersAlgorithm.resourceName(j));
        }
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                String label = "P" + i + " " + BankersAlgorithm.resourceName(j);
                allocation[i][j] = parseCell(allocationTable.getValueAt(i, j + 1), "Allocation " + label);
                maximum[i][j] = parseCell(maximumTable.getValueAt(i, j + 1), "Maximum " + label);
            }
        }
        lastTotal = total;
        lastAllocation = allocation;
        lastMaximum = maximum;
    }

    private void calculate() {
        try {
            requestButton.setEnabled(false);
            lastResult = null;
            readInputs();
            lastResult = BankersAlgorithm.analyze(lastTotal, lastAllocation, lastMaximum);
            output.setText(formatReport(lastResult, lastTotal, lastAllocation, lastMaximum));
            output.setCaretPosition(0);
            requestButton.setEnabled(true);
        } catch (IllegalArgumentException e) {
            error(e.getMessage());
        }
    }

    private String formatReport(BankersAlgorithm.Result result, int[] total,
                                int[][] allocation, int[][] maximum) {
        StringBuilder sb = new StringBuilder();
        sb.append("BANKER'S ALGORITHM — STEP-BY-STEP REPORT\n");
        sb.append("============================================================\n\n");
        sb.append("TOTAL RESOURCES: ").append(BankersAlgorithm.vector(total)).append("\n\n");
        sb.append("PROCESS    ALLOCATION       MAXIMUM          NEED\n");
        for (int i = 0; i < allocation.length; i++) {
            sb.append(String.format("%-10s %-16s %-16s %s%n", "P" + i,
                BankersAlgorithm.vector(allocation[i]), BankersAlgorithm.vector(maximum[i]),
                BankersAlgorithm.vector(result.need[i])));
        }
        sb.append("\nRESOURCE ALLOCATION AND AVAILABLE COMPUTATION\n");
        sb.append("Available = Total - sum(Allocation column)\n\n");
        for (int j = 0; j < total.length; j++) {
            sb.append(BankersAlgorithm.resourceName(j)).append(": Allocated = ");
            for (int i = 0; i < allocation.length; i++) {
                if (i > 0) sb.append(" + ");
                sb.append(allocation[i][j]);
            }
            sb.append(" = ").append(result.totalAllocated[j]).append("\n");
            sb.append("   Available = ").append(total[j]).append(" - ")
                .append(result.totalAllocated[j]).append(" = ")
                .append(result.available[j]).append("\n");
        }
        sb.append("\nAVAILABLE VECTOR: ").append(BankersAlgorithm.vector(result.available)).append("\n");
        sb.append("\nNEED COMPUTATION: Need = Maximum - Allocation\n");
        for (int i = 0; i < allocation.length; i++) {
            sb.append("P").append(i).append(": ");
            for (int j = 0; j < total.length; j++) {
                if (j > 0) sb.append(" | ");
                sb.append(maximum[i][j]).append(" - ").append(allocation[i][j])
                    .append(" = ").append(result.need[i][j]);
            }
            sb.append("\n");
        }
        sb.append("\nSAFETY ALGORITHM\nInitial Work = Available = ")
            .append(BankersAlgorithm.vector(result.available)).append("\n");
        sb.append("Initially Finish = FALSE for all processes.\n");
        for (int k = 0; k < result.steps.size(); k++) {
            BankersAlgorithm.Step step = result.steps.get(k);
            sb.append("\nIteration ").append(k + 1).append(" — P").append(step.process).append("\n");
            sb.append("  Need ").append(BankersAlgorithm.vector(step.need))
                .append(" <= Work ").append(BankersAlgorithm.vector(step.before)).append(" : TRUE\n");
            sb.append("  Work After = Work Before + Allocation = ")
                .append(BankersAlgorithm.vector(step.before)).append(" + ")
                .append(BankersAlgorithm.vector(step.released)).append(" = ")
                .append(BankersAlgorithm.vector(step.after)).append("\n");
            sb.append("  Finish[P").append(step.process).append("] = TRUE\n");
        }
        sb.append("\nFINAL FINISH FLAGS\n");
        for (int i = 0; i < result.finish.length; i++) {
            sb.append("P").append(i).append(" = ").append(result.finish[i]).append("\n");
        }
        sb.append("\nRESULT: ").append(result.safe ? "SAFE STATE" : "UNSAFE STATE").append("\n");
        if (result.safe) {
            sb.append("Safe sequence: ");
            for (int i = 0; i < result.sequence.length; i++) {
                if (i > 0) sb.append(" -> ");
                sb.append("P").append(result.sequence[i]);
            }
            sb.append("\n");
        } else {
            sb.append("No complete safe sequence exists. Unfinished: ");
            for (int i = 0; i < result.finish.length; i++) {
                if (!result.finish[i]) sb.append("P").append(i).append(" ");
            }
            sb.append("\nAn unsafe state does not necessarily mean a deadlock exists.\n");
        }
        return sb.toString();
    }

    private void checkRequest() {
        try {
            // Re-read current tables to avoid checking an outdated allocation state.
            readInputs();
            lastResult = BankersAlgorithm.analyze(lastTotal, lastAllocation, lastMaximum);
            String p = JOptionPane.showInputDialog(this, "Requesting process number (0 to " + (n - 1) + "): ");
            if (p == null) return;
            int process = Integer.parseInt(p.trim());
            String raw = JOptionPane.showInputDialog(this,
                "Enter " + m + " requested resource values separated by spaces (e.g. 1 0 2):");
            if (raw == null) return;
            String[] parts = raw.trim().split("[\\s,]+");
            if (parts.length != m) throw new IllegalArgumentException("Enter exactly " + m + " values.");
            int[] request = new int[m];
            for (int j = 0; j < m; j++) request[j] = parseCell(parts[j], "Request " + j);
            BankersAlgorithm.RequestResult verdict = BankersAlgorithm.checkRequest(
                lastTotal, lastAllocation, lastMaximum, process, request);
            StringBuilder message = new StringBuilder("\nRESOURCE REQUEST CHECK\n");
            message.append("Process P").append(process).append(" requests ")
                .append(BankersAlgorithm.vector(request)).append("\n");
            message.append("Current Need: ").append(BankersAlgorithm.vector(lastResult.need[process])).append("\n");
            message.append("Current Available: ").append(BankersAlgorithm.vector(lastResult.available)).append("\n");
            message.append(verdict.explanation).append("\n");
            if (verdict.tentativeResult != null) {
                message.append("Tentative Available: ")
                    .append(BankersAlgorithm.vector(verdict.tentativeResult.available)).append("\n");
                message.append("Tentative state: ")
                    .append(verdict.tentativeResult.safe ? "SAFE" : "UNSAFE").append("\n");
                message.append("Tentative safety trace:\n");
                for (BankersAlgorithm.Step step : verdict.tentativeResult.steps) {
                    message.append("P").append(step.process).append(": ")
                        .append(BankersAlgorithm.vector(step.before)).append(" + ")
                        .append(BankersAlgorithm.vector(step.released)).append(" = ")
                        .append(BankersAlgorithm.vector(step.after)).append("\n");
                }
            }
            message.append("This check does not change your input tables.\n");
            output.append("\n" + message);
            output.setCaretPosition(output.getDocument().getLength());
        } catch (NumberFormatException e) {
            error("Process number must be a whole number.");
        } catch (IllegalArgumentException e) {
            error(e.getMessage());
        }
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(this, message, "Input error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BankersApp().setVisible(true));
    }
}
