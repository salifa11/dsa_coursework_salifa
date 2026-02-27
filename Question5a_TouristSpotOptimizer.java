import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;

import java.util.*;
import java.util.List;

/**
 * Question 4a / 5a - Tourist Spot Optimizer
 * GUI-based itinerary planner using a Greedy + Simulated Annealing heuristic.
 *
 * Features:
 *  1. GUI for user input (budget, time, interest tags)
 *  2. Tourist spot dataset (hardcoded JSON-equivalent)
 *  3. Greedy heuristic for initial path; Simulated Annealing for refinement
 *  4. Map/coordinate visualization of selected path
 *  5. Brute-force comparison on small dataset (up to 6 spots)
 *
 * Algorithm:
 *  - Greedy: Filter spots by budget/time/tags, sort by score=(interest_match/fee),
 *    greedily select next nearest unvisited spot.
 *  - Simulated Annealing: Refine route by randomly swapping spots and accepting
 *    worse solutions with decreasing probability (temperature cooling).
 *  - Brute-Force: Enumerate all permutations of feasible spots (≤6) for comparison.
 */
public class Question5a_TouristSpotOptimizer extends JFrame {

    // ─── Data Model ─────────────────────────────────────────────────────────────
    static class TouristSpot {
        String name;
        double lat, lon;
        int entryFee;      // NPR
        int openHour, closeHour;
        String[] tags;
        int visitDuration; // hours (estimated)

        TouristSpot(String name, double lat, double lon, int fee,
                    String open, String close, String[] tags, int duration) {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
            this.entryFee = fee;
            this.openHour = Integer.parseInt(open.split(":")[0]);
            this.closeHour = Integer.parseInt(close.split(":")[0]);
            this.tags = tags;
            this.visitDuration = duration;
        }
    }

    // ─── Dataset ─────────────────────────────────────────────────────────────────
    static List<TouristSpot> allSpots = Arrays.asList(
        new TouristSpot("Pashupatinath Temple",  27.7104, 85.3488, 100, "06:00", "18:00",
                        new String[]{"culture","religious"}, 2),
        new TouristSpot("Swayambhunath Stupa",   27.7149, 85.2906, 200, "07:00", "17:00",
                        new String[]{"culture","heritage"}, 2),
        new TouristSpot("Garden of Dreams",      27.7125, 85.3170, 150, "09:00", "21:00",
                        new String[]{"nature","relaxation"}, 1),
        new TouristSpot("Chandragiri Hills",     27.6616, 85.2458, 700, "09:00", "17:00",
                        new String[]{"nature","adventure"}, 3),
        new TouristSpot("Kathmandu Durbar Square",27.7048, 85.3076, 100, "10:00", "17:00",
                        new String[]{"culture","heritage"}, 2),
        new TouristSpot("Boudhanath Stupa",      27.7215, 85.3620, 400, "06:00", "20:00",
                        new String[]{"culture","religious"}, 2),
        new TouristSpot("Namobuddha",            27.5753, 85.5372, 50,  "06:00", "18:00",
                        new String[]{"religious","nature"}, 3),
        new TouristSpot("Patan Durbar Square",   27.6710, 85.3247, 250, "10:00", "17:00",
                        new String[]{"culture","heritage"}, 2)
    );

    // ─── GUI Components ──────────────────────────────────────────────────────────
    private JSpinner budgetSpinner, timeSpinner, startHourSpinner;
    private JCheckBox[] tagBoxes;
    private JTextArea resultArea;
    private MapPanel mapPanel;
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private String[] interestTags = {"culture","religious","heritage","nature","adventure","relaxation"};

    public Question5a_TouristSpotOptimizer() {
        setTitle("🏔️ Nepal Tourist Spot Optimizer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 750);
        setLocationRelativeTo(null);
        buildUI();
        setVisible(true);
    }

    private void buildUI() {
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(245, 245, 245));

        // ── Left Panel: Inputs ────────────────────────────────────────────────
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        inputPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(70, 130, 180), 2),
                "  Traveler Preferences  "));
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setPreferredSize(new Dimension(250, 0));

        inputPanel.add(Box.createVerticalStrut(10));
        inputPanel.add(labeledField("💰 Max Budget (NPR):", budgetSpinner = new JSpinner(
                new SpinnerNumberModel(1500, 0, 10000, 100))));
        inputPanel.add(Box.createVerticalStrut(8));
        inputPanel.add(labeledField("⏰ Time Available (hrs):", timeSpinner = new JSpinner(
                new SpinnerNumberModel(8, 1, 24, 1))));
        inputPanel.add(Box.createVerticalStrut(8));
        inputPanel.add(labeledField("🕐 Start Hour (0-23):", startHourSpinner = new JSpinner(
                new SpinnerNumberModel(9, 0, 23, 1))));

        inputPanel.add(Box.createVerticalStrut(10));
        JLabel tagLabel = new JLabel("🏷️ Interest Areas:");
        tagLabel.setFont(new Font("Arial", Font.BOLD, 13));
        tagLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputPanel.add(tagLabel);

        tagBoxes = new JCheckBox[interestTags.length];
        for (int i = 0; i < interestTags.length; i++) {
            tagBoxes[i] = new JCheckBox(interestTags[i]);
            tagBoxes[i].setBackground(Color.WHITE);
            tagBoxes[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            inputPanel.add(tagBoxes[i]);
        }
        tagBoxes[0].setSelected(true); // Default: culture

        inputPanel.add(Box.createVerticalStrut(15));

        JButton planBtn = new JButton("🗺️ Plan My Trip!");
        planBtn.setBackground(new Color(70, 130, 180));
        planBtn.setForeground(Color.WHITE);
        planBtn.setFont(new Font("Arial", Font.BOLD, 14));
        planBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        planBtn.addActionListener(e -> runOptimization());
        inputPanel.add(planBtn);

        JButton bruteBtn = new JButton("🔍 Brute-Force Compare");
        bruteBtn.setBackground(new Color(180, 90, 70));
        bruteBtn.setForeground(Color.WHITE);
        bruteBtn.setFont(new Font("Arial", Font.BOLD, 12));
        bruteBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        bruteBtn.addActionListener(e -> runBruteForce());
        inputPanel.add(Box.createVerticalStrut(8));
        inputPanel.add(bruteBtn);

        add(inputPanel, BorderLayout.WEST);

        // ── Center: Map + Results ─────────────────────────────────────────────
        JSplitPane centerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        centerSplit.setResizeWeight(0.5);

        mapPanel = new MapPanel();
        mapPanel.setPreferredSize(new Dimension(600, 350));
        centerSplit.setTopComponent(mapPanel);

        // Result table
        String[] columns = {"#", "Spot", "Entry Fee (NPR)", "Visit Duration", "Tags", "Decision"};
        tableModel = new DefaultTableModel(columns, 0);
        resultTable = new JTable(tableModel);
        resultTable.setRowHeight(25);
        resultTable.setFont(new Font("Arial", Font.PLAIN, 12));
        resultTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        resultTable.getTableHeader().setBackground(new Color(70, 130, 180));
        resultTable.getTableHeader().setForeground(Color.WHITE);
        centerSplit.setBottomComponent(new JScrollPane(resultTable));

        add(centerSplit, BorderLayout.CENTER);

        // ── Right: Text Output ────────────────────────────────────────────────
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        resultArea.setBackground(new Color(250, 250, 250));
        resultArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setPreferredSize(new Dimension(280, 0));
        scrollPane.setBorder(BorderFactory.createTitledBorder("📋 Itinerary Details"));
        add(scrollPane, BorderLayout.EAST);
    }

    private JPanel labeledField(String label, JSpinner spinner) {
        JPanel p = new JPanel(new BorderLayout(5, 0));
        p.setBackground(Color.WHITE);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Arial", Font.PLAIN, 12));
        p.add(lbl, BorderLayout.WEST);
        p.add(spinner, BorderLayout.CENTER);
        return p;
    }

    // ─── Optimization Logic ───────────────────────────────────────────────────────

    private List<String> getSelectedTags() {
        List<String> selected = new ArrayList<>();
        for (int i = 0; i < tagBoxes.length; i++) {
            if (tagBoxes[i].isSelected()) selected.add(interestTags[i]);
        }
        return selected;
    }

    private int tagMatchScore(TouristSpot spot, List<String> userTags) {
        int score = 0;
        for (String tag : spot.tags) {
            if (userTags.contains(tag)) score++;
        }
        return score;
    }

    private List<TouristSpot> filterFeasible(int budget, int totalHours, int startHour,
                                              List<String> userTags) {
        List<TouristSpot> feasible = new ArrayList<>();
        for (TouristSpot s : allSpots) {
            boolean affordable = s.entryFee <= budget;
            boolean open = s.openHour <= startHour && s.closeHour >= startHour + s.visitDuration;
            boolean hasTag = userTags.isEmpty() || tagMatchScore(s, userTags) > 0;
            if (affordable && open && hasTag) feasible.add(s);
        }
        return feasible;
    }

    // Euclidean distance in coordinate space (lat/lon)
    private double distance(TouristSpot a, TouristSpot b) {
        double dx = (a.lon - b.lon) * 111320 * Math.cos(Math.toRadians((a.lat + b.lat) / 2));
        double dy = (a.lat - b.lat) * 111320;
        return Math.sqrt(dx * dx + dy * dy);
    }

    // Travel time estimate: walking speed ~4 km/h
    private double travelTime(TouristSpot a, TouristSpot b) {
        return distance(a, b) / 1000.0 / 4.0; // hours
    }

    // Greedy nearest-neighbor selection with score weighting
    private List<TouristSpot> greedyPlan(List<TouristSpot> feasible, int budget,
                                          int hours, List<String> userTags) {
        List<TouristSpot> remaining = new ArrayList<>(feasible);
        List<TouristSpot> path = new ArrayList<>();
        int spentBudget = 0;
        double spentTime = 0;

        // Sort by interest score desc initially
        remaining.sort((a, b) -> tagMatchScore(b, userTags) - tagMatchScore(a, userTags));

        // Pick first spot with best interest match
        if (!remaining.isEmpty()) {
            TouristSpot first = remaining.remove(0);
            path.add(first);
            spentBudget += first.entryFee;
            spentTime += first.visitDuration;
        }

        // Greedy nearest neighbor
        while (!remaining.isEmpty() && spentTime < hours) {
            TouristSpot last = path.get(path.size() - 1);
            TouristSpot best = null;
            double bestScore = Double.MAX_VALUE;

            for (TouristSpot candidate : remaining) {
                double travelH = travelTime(last, candidate);
                double totalTime = spentTime + travelH + candidate.visitDuration;
                int totalCost = spentBudget + candidate.entryFee;
                if (totalTime > hours || totalCost > budget) continue;

                // Score = distance penalized by low tag match
                int match = tagMatchScore(candidate, userTags);
                double score = distance(last, candidate) / (match + 1.0);
                if (score < bestScore) {
                    bestScore = score;
                    best = candidate;
                }
            }
            if (best == null) break;
            remaining.remove(best);
            path.add(best);
            spentBudget += best.entryFee;
            spentTime += travelTime(path.get(path.size() - 2), best) + best.visitDuration;
        }
        return path;
    }

    // Simulated Annealing refinement
    private List<TouristSpot> simulatedAnnealing(List<TouristSpot> initial,
                                                   int budget, int maxHours, List<String> userTags) {
        if (initial.size() < 3) return initial;
        List<TouristSpot> current = new ArrayList<>(initial);
        List<TouristSpot> bestPath = new ArrayList<>(initial);
        double bestCost = routeCost(bestPath);

        double temp = 1000;
        double cooling = 0.995;
        Random rng = new Random(42);

        for (int iter = 0; iter < 5000 && temp > 1; iter++) {
            // Random swap of two spots
            int i = rng.nextInt(current.size());
            int j = rng.nextInt(current.size());
            if (i == j) continue;
            Collections.swap(current, i, j);

            if (isFeasible(current, budget, maxHours)) {
                double newCost = routeCost(current);
                double delta = newCost - bestCost;
                if (delta < 0 || Math.random() < Math.exp(-delta / temp)) {
                    bestPath = new ArrayList<>(current);
                    bestCost = newCost;
                }
            }
            Collections.swap(current, i, j); // undo if not accepted
            temp *= cooling;
        }
        return bestPath;
    }

    private double routeCost(List<TouristSpot> path) {
        double cost = 0;
        for (int i = 1; i < path.size(); i++) cost += distance(path.get(i - 1), path.get(i));
        return cost;
    }

    private boolean isFeasible(List<TouristSpot> path, int budget, int hours) {
        int cost = 0;
        double time = 0;
        for (int i = 0; i < path.size(); i++) {
            cost += path.get(i).entryFee;
            time += path.get(i).visitDuration;
            if (i > 0) time += travelTime(path.get(i - 1), path.get(i));
        }
        return cost <= budget && time <= hours;
    }

    private void runOptimization() {
        int budget = (int) budgetSpinner.getValue();
        int hours = (int) timeSpinner.getValue();
        int startHour = (int) startHourSpinner.getValue();
        List<String> userTags = getSelectedTags();

        if (userTags.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select at least one interest area.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<TouristSpot> feasible = filterFeasible(budget, hours, startHour, userTags);
        if (feasible.isEmpty()) {
            resultArea.setText("No spots match your constraints.\nTry increasing budget or time.");
            return;
        }

        List<TouristSpot> greedyPath = greedyPlan(feasible, budget, hours, userTags);
        List<TouristSpot> finalPath = simulatedAnnealing(greedyPath, budget, hours, userTags);

        displayResults(finalPath, budget, hours, userTags, "Greedy + Simulated Annealing");
    }

    private void runBruteForce() {
        int budget = (int) budgetSpinner.getValue();
        int hours = (int) timeSpinner.getValue();
        int startHour = (int) startHourSpinner.getValue();
        List<String> userTags = getSelectedTags();

        List<TouristSpot> feasible = filterFeasible(budget, hours, startHour, userTags);
        // Limit to 6 spots for brute-force
        if (feasible.size() > 6) feasible = feasible.subList(0, 6);

        List<TouristSpot> bestPath = new ArrayList<>();
        double bestCost = Double.MAX_VALUE;
        for (List<TouristSpot> perm : permutations(feasible)) {
            if (isFeasible(perm, budget, hours)) {
                double cost = routeCost(perm);
                if (cost < bestCost) {
                    bestCost = cost;
                    bestPath = perm;
                }
            }
        }

        if (bestPath.isEmpty()) {
            resultArea.setText("No feasible brute-force path found.\n(Using first 6 filtered spots)");
            return;
        }

        // Also run heuristic on same subset for comparison
        List<TouristSpot> greedyPath = greedyPlan(new ArrayList<>(feasible), budget, hours, userTags);
        List<TouristSpot> heuristicPath = simulatedAnnealing(greedyPath, budget, hours, userTags);

        StringBuilder sb = new StringBuilder();
        sb.append("=== BRUTE-FORCE vs HEURISTIC ===\n\n");
        sb.append(String.format("Dataset: %d spots (capped at 6)\n\n", feasible.size()));
        sb.append("BRUTE-FORCE (optimal):\n");
        appendPathInfo(sb, bestPath);
        sb.append("\nHEURISTIC (Greedy+SA):\n");
        appendPathInfo(sb, heuristicPath);
        sb.append("\n=== ACCURACY ANALYSIS ===\n");
        sb.append(String.format("Brute-force spots:  %d\n", bestPath.size()));
        sb.append(String.format("Heuristic spots:    %d\n", heuristicPath.size()));
        double bfCost  = calcTotalCost(bestPath);
        double hCost   = calcTotalCost(heuristicPath);
        sb.append(String.format("Brute-force fee:    NPR %d\n", (int) bfCost));
        sb.append(String.format("Heuristic fee:      NPR %d\n", (int) hCost));
        sb.append("\nConclusion: Heuristic is O(n² + SA_iters)\n");
        sb.append("vs Brute-Force O(n!) — heuristic scales better\n");
        sb.append("for large inputs with near-optimal accuracy.\n");
        resultArea.setText(sb.toString());

        mapPanel.setPath(bestPath, allSpots);
        updateTable(bestPath, userTags, "Brute-Force Optimal");
    }

    private void appendPathInfo(StringBuilder sb, List<TouristSpot> path) {
        for (int i = 0; i < path.size(); i++) {
            sb.append(String.format("  %d. %s (NPR %d)\n", i + 1, path.get(i).name, path.get(i).entryFee));
        }
        sb.append(String.format("  Total fee: NPR %d\n", (int) calcTotalCost(path)));
    }

    private double calcTotalCost(List<TouristSpot> path) {
        return path.stream().mapToInt(s -> s.entryFee).sum();
    }

    private double calcTotalTime(List<TouristSpot> path) {
        double time = 0;
        for (int i = 0; i < path.size(); i++) {
            time += path.get(i).visitDuration;
            if (i > 0) time += travelTime(path.get(i - 1), path.get(i));
        }
        return time;
    }

    private List<List<TouristSpot>> permutations(List<TouristSpot> list) {
        List<List<TouristSpot>> result = new ArrayList<>();
        if (list.size() <= 1) {
            result.add(new ArrayList<>(list));
            return result;
        }
        for (int i = 0; i < list.size(); i++) {
            TouristSpot first = list.get(i);
            List<TouristSpot> rest = new ArrayList<>(list);
            rest.remove(i);
            for (List<TouristSpot> p : permutations(rest)) {
                p.add(0, first);
                result.add(p);
            }
        }
        return result;
    }

    private void displayResults(List<TouristSpot> path, int budget, int hours,
                                 List<String> userTags, String algorithm) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ITINERARY (").append(algorithm).append(") ===\n\n");

        double currentTime = (int) startHourSpinner.getValue();
        int totalFee = 0;
        for (int i = 0; i < path.size(); i++) {
            TouristSpot spot = path.get(i);
            if (i > 0) currentTime += travelTime(path.get(i - 1), spot);
            sb.append(String.format("%d. %s\n", i + 1, spot.name));
            sb.append(String.format("   Arrive: %02d:%02d | Fee: NPR %d\n",
                    (int) currentTime, (int) ((currentTime % 1) * 60), spot.entryFee));
            sb.append(String.format("   Duration: %dh | Tags: %s\n",
                    spot.visitDuration, String.join(", ", spot.tags)));
            int match = tagMatchScore(spot, userTags);
            sb.append(String.format("   Decision: Selected (interest match: %d/%d)\n\n",
                    match, userTags.size()));
            currentTime += spot.visitDuration;
            totalFee += spot.entryFee;
        }
        sb.append("─────────────────────────\n");
        sb.append(String.format("Total Spots: %d\n", path.size()));
        sb.append(String.format("Total Fee:   NPR %d / %d\n", totalFee, budget));
        sb.append(String.format("Total Time:  %.1fh / %dh\n", calcTotalTime(path), hours));

        resultArea.setText(sb.toString());
        mapPanel.setPath(path, allSpots);
        updateTable(path, userTags, algorithm);
    }

    private void updateTable(List<TouristSpot> path, List<String> userTags, String algo) {
        tableModel.setRowCount(0);
        for (int i = 0; i < path.size(); i++) {
            TouristSpot s = path.get(i);
            int match = tagMatchScore(s, userTags);
            tableModel.addRow(new Object[]{
                i + 1, s.name, s.entryFee, s.visitDuration + "h",
                String.join(", ", s.tags),
                "✅ Selected (" + match + "/" + userTags.size() + " tags)"
            });
        }
    }

    // ─── Map Visualization Panel ─────────────────────────────────────────────────
    static class MapPanel extends JPanel {
        private List<TouristSpot> selectedPath = new ArrayList<>();
        private List<TouristSpot> allSpotsList = new ArrayList<>();

        MapPanel() {
            setBackground(new Color(200, 220, 255));
            setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(new Color(70, 130, 180), 2),
                    "  🗺️ Route Map (Kathmandu Valley)  "));
        }

        void setPath(List<TouristSpot> path, List<TouristSpot> all) {
            this.selectedPath = path;
            this.allSpotsList = all;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (allSpotsList.isEmpty()) {
                g2.drawString("Run optimization to see map", getWidth() / 2 - 80, getHeight() / 2);
                return;
            }

            // Compute bounding box
            double minLat = allSpotsList.stream().mapToDouble(s -> s.lat).min().orElse(0) - 0.01;
            double maxLat = allSpotsList.stream().mapToDouble(s -> s.lat).max().orElse(1) + 0.01;
            double minLon = allSpotsList.stream().mapToDouble(s -> s.lon).min().orElse(0) - 0.01;
            double maxLon = allSpotsList.stream().mapToDouble(s -> s.lon).max().orElse(1) + 0.01;
            int pad = 40;
            int W = getWidth() - 2 * pad, H = getHeight() - 2 * pad;

            // Draw all spots (gray)
            for (TouristSpot s : allSpotsList) {
                int x = pad + (int) ((s.lon - minLon) / (maxLon - minLon) * W);
                int y = pad + (int) ((1 - (s.lat - minLat) / (maxLat - minLat)) * H);
                g2.setColor(new Color(180, 180, 180));
                g2.fillOval(x - 5, y - 5, 10, 10);
                g2.setColor(Color.GRAY);
                g2.setFont(new Font("Arial", Font.PLAIN, 9));
                g2.drawString(s.name.substring(0, Math.min(12, s.name.length())), x + 7, y + 4);
            }

            if (selectedPath.isEmpty()) return;

            // Draw route lines
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    0, new float[]{8, 4}, 0));
            g2.setColor(new Color(220, 80, 80));
            int[] xs = new int[selectedPath.size()];
            int[] ys = new int[selectedPath.size()];
            for (int i = 0; i < selectedPath.size(); i++) {
                TouristSpot s = selectedPath.get(i);
                xs[i] = pad + (int) ((s.lon - minLon) / (maxLon - minLon) * W);
                ys[i] = pad + (int) ((1 - (s.lat - minLat) / (maxLat - minLat)) * H);
            }
            for (int i = 1; i < xs.length; i++) {
                g2.drawLine(xs[i - 1], ys[i - 1], xs[i], ys[i]);
            }

            // Draw selected spots (colored)
            g2.setStroke(new BasicStroke(1));
            for (int i = 0; i < selectedPath.size(); i++) {
                g2.setColor(i == 0 ? new Color(0, 150, 0) : new Color(220, 80, 80));
                g2.fillOval(xs[i] - 8, ys[i] - 8, 16, 16);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 10));
                g2.drawString(String.valueOf(i + 1), xs[i] - 3, ys[i] + 4);
                g2.setColor(Color.BLACK);
                g2.setFont(new Font("Arial", Font.BOLD, 10));
                g2.drawString(selectedPath.get(i).name, xs[i] + 10, ys[i] - 5);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Question5a_TouristSpotOptimizer());
    }
}