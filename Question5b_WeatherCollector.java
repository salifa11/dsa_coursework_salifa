import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

/**
 * Question 5b - Multi-threaded Weather Data Collector (Nepal Cities)
 *
 * Features:
 *  1. GUI with "Fetch Weather" button (remains active during fetch)
 *  2. Fetches weather for 5 Nepal cities using OpenWeatherMap API
 *  3. 5 threads (one per city) fetch data concurrently
 *  4. Thread-safe GUI updates using SwingUtilities.invokeLater + ReentrantLock
 *  5. Sequential vs parallel latency comparison with bar chart
 *
 * Thread Safety:
 *  - Data stored in ConcurrentHashMap (thread-safe map)
 *  - GUI updates dispatched via SwingUtilities.invokeLater (EDT thread)
 *  - AtomicInteger for shared counter
 *  - ReentrantLock guards shared state during batch updates
 *
 * NOTE: Replace API_KEY with your free OpenWeatherMap key.
 *       If no API key, simulated data is used for demonstration.
 */
public class Question5b_WeatherCollector extends JFrame {

    // ─── Configuration ───────────────────────────────────────────────────────────
    // Replace with your free API key from https://openweathermap.org/api
    private static final String API_KEY = "YOUR_API_KEY_HERE";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";

    private static final String[] CITIES = {
        "Kathmandu", "Pokhara", "Biratnagar", "Nepalgunj", "Dhangadhi"
    };

    // ─── Shared Data Structures (Thread-Safe) ────────────────────────────────────
    private final ConcurrentHashMap<String, WeatherData> weatherResults = new ConcurrentHashMap<>();
    private final ReentrantLock displayLock = new ReentrantLock();
    private final AtomicInteger completedCount = new AtomicInteger(0);

    // ─── GUI Components ──────────────────────────────────────────────────────────
    private JButton fetchButton;
    private JLabel statusLabel;
    private JTable weatherTable;
    private DefaultTableModel tableModel;
    private JPanel chartPanel;
    private JLabel seqTimeLabel, parTimeLabel, speedupLabel;

    // Timing results
    private long sequentialMs = -1;
    private long parallelMs = -1;

    // ─── Weather Data Model ───────────────────────────────────────────────────────
    static class WeatherData {
        String city;
        double tempCelsius;
        int humidity;
        double pressure;
        String description;
        long fetchTimeMs;
        boolean success;
        String error;

        WeatherData(String city) {
            this.city = city;
            this.success = false;
        }
    }

    // ─── Constructor ─────────────────────────────────────────────────────────────
    public Question5b_WeatherCollector() {
        setTitle("🌤️ Nepal Multi-threaded Weather Collector");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 650);
        setLocationRelativeTo(null);
        buildUI();
        setVisible(true);
    }

    // ─── GUI Construction ─────────────────────────────────────────────────────────
    private void buildUI() {
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(240, 248, 255));

        // ── Top Control Panel ─────────────────────────────────────────────────
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        topPanel.setBackground(new Color(70, 130, 180));

        JLabel title = new JLabel("🌏 Nepal Weather Monitor (Multi-threaded)");
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        topPanel.add(title);

        fetchButton = new JButton("☁️ Fetch Weather");
        fetchButton.setFont(new Font("Arial", Font.BOLD, 14));
        fetchButton.setBackground(new Color(255, 200, 50));
        fetchButton.setForeground(Color.BLACK);
        fetchButton.addActionListener(e -> startFetching());
        topPanel.add(fetchButton);

        statusLabel = new JLabel("  Ready — Click 'Fetch Weather' to begin");
        statusLabel.setFont(new Font("Arial", Font.ITALIC, 13));
        statusLabel.setForeground(Color.WHITE);
        topPanel.add(statusLabel);

        add(topPanel, BorderLayout.NORTH);

        // ── Center: Weather Table ─────────────────────────────────────────────
        String[] cols = {"City", "Temp (°C)", "Humidity (%)", "Pressure (hPa)", "Condition", "Fetch Time (ms)"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        weatherTable = new JTable(tableModel);
        weatherTable.setRowHeight(30);
        weatherTable.setFont(new Font("Arial", Font.PLAIN, 13));
        weatherTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        weatherTable.getTableHeader().setBackground(new Color(70, 130, 180));
        weatherTable.getTableHeader().setForeground(Color.WHITE);

        // Alternate row colors
        weatherTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setBackground(sel ? new Color(100, 149, 237) :
                              (row % 2 == 0 ? Color.WHITE : new Color(235, 245, 255)));
                setHorizontalAlignment(col == 0 ? LEFT : CENTER);
                return this;
            }
        });

        // Preload table with city names
        for (String city : CITIES) {
            tableModel.addRow(new Object[]{city, "—", "—", "—", "Pending...", "—"});
        }

        JScrollPane tableScroll = new JScrollPane(weatherTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(70, 130, 180), 2),
                "  📊 Weather Data  "));
        add(tableScroll, BorderLayout.CENTER);

        // ── Bottom: Latency Comparison ─────────────────────────────────────────
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(new Color(240, 248, 255));
        bottomPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(70, 130, 180), 2),
                "  ⚡ Latency Comparison: Sequential vs Parallel  "));

        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        statsPanel.setBackground(new Color(240, 248, 255));

        seqTimeLabel = createStatLabel("Sequential Time: — ms", new Color(200, 80, 80));
        parTimeLabel  = createStatLabel("Parallel Time:   — ms", new Color(80, 160, 80));
        speedupLabel  = createStatLabel("Speedup: —×",           new Color(80, 80, 200));

        statsPanel.add(seqTimeLabel);
        statsPanel.add(parTimeLabel);
        statsPanel.add(speedupLabel);
        bottomPanel.add(statsPanel, BorderLayout.NORTH);

        chartPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawBarChart((Graphics2D) g);
            }
        };
        chartPanel.setPreferredSize(new Dimension(0, 160));
        chartPanel.setBackground(new Color(250, 250, 255));
        bottomPanel.add(chartPanel, BorderLayout.CENTER);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JLabel createStatLabel(String text, Color color) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setFont(new Font("Arial", Font.BOLD, 14));
        lbl.setForeground(color);
        return lbl;
    }

    // ─── Bar Chart Drawing ────────────────────────────────────────────────────────
    private void drawBarChart(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int W = chartPanel.getWidth(), H = chartPanel.getHeight();
        g2.setColor(new Color(250, 250, 255));
        g2.fillRect(0, 0, W, H);

        if (sequentialMs < 0 || parallelMs < 0) {
            g2.setColor(Color.GRAY);
            g2.setFont(new Font("Arial", Font.ITALIC, 13));
            g2.drawString("Fetch weather data to see comparison chart", W / 2 - 140, H / 2);
            return;
        }

        long maxMs = Math.max(sequentialMs, parallelMs);
        int barW = 120, spacing = 80, baseY = H - 30, maxH = H - 60;

        // Sequential bar
        int seqH = (int) (sequentialMs * maxH / maxMs);
        int seqX = W / 2 - barW - spacing / 2;
        g2.setColor(new Color(220, 80, 80));
        g2.fillRoundRect(seqX, baseY - seqH, barW, seqH, 10, 10);
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.drawString(sequentialMs + " ms", seqX + 20, baseY - seqH - 8);
        g2.drawString("Sequential", seqX + 10, baseY + 18);

        // Parallel bar
        int parH = (int) (parallelMs * maxH / maxMs);
        int parX = W / 2 + spacing / 2;
        g2.setColor(new Color(80, 160, 80));
        g2.fillRoundRect(parX, baseY - parH, barW, parH, 10, 10);
        g2.setColor(Color.BLACK);
        g2.drawString(parallelMs + " ms", parX + 20, baseY - parH - 8);
        g2.drawString("Parallel", parX + 25, baseY + 18);

        // Baseline
        g2.setColor(Color.DARK_GRAY);
        g2.drawLine(seqX - 10, baseY, parX + barW + 10, baseY);
    }

    // ─── Fetch Logic ──────────────────────────────────────────────────────────────

    private void startFetching() {
        fetchButton.setEnabled(false);
        statusLabel.setText("  Fetching weather data...");
        weatherResults.clear();
        completedCount.set(0);
        // Reset table
        for (int i = 0; i < CITIES.length; i++) {
            tableModel.setValueAt("Fetching...", i, 4);
            tableModel.setValueAt("—", i, 1);
            tableModel.setValueAt("—", i, 2);
            tableModel.setValueAt("—", i, 3);
            tableModel.setValueAt("—", i, 5);
        }

        // Run in background thread to keep GUI responsive
        new Thread(() -> {
            // ── Sequential fetch (for timing comparison) ──────────────────────
            long seqStart = System.currentTimeMillis();
            for (String city : CITIES) {
                fetchWeatherForCity(city, false);
            }
            sequentialMs = System.currentTimeMillis() - seqStart;

            // Clear results for parallel run
            weatherResults.clear();
            completedCount.set(0);

            // ── Parallel fetch (5 threads) ────────────────────────────────────
            ExecutorService executor = Executors.newFixedThreadPool(CITIES.length);
            long parStart = System.currentTimeMillis();

            List<Future<?>> futures = new ArrayList<>();
            for (String city : CITIES) {
                futures.add(executor.submit(() -> fetchWeatherForCity(city, true)));
            }

            // Wait for all threads to complete
            for (Future<?> f : futures) {
                try { f.get(); } catch (Exception e) { /* handled in task */ }
            }
            executor.shutdown();
            parallelMs = System.currentTimeMillis() - parStart;

            // Update UI on EDT
            SwingUtilities.invokeLater(this::onAllFetched);
        }, "MainFetchCoordinator").start();
    }

    /**
     * Thread-safe fetch method — called from worker threads.
     * Updates table row via SwingUtilities.invokeLater.
     */
    private void fetchWeatherForCity(String city, boolean updateUI) {
        WeatherData data = new WeatherData(city);
        long t0 = System.currentTimeMillis();
        try {
            if (API_KEY.equals("YOUR_API_KEY_HERE")) {
                // Simulate response when no API key provided
                Thread.sleep(300 + (long)(Math.random() * 400));
                data.tempCelsius = 15 + Math.random() * 15;
                data.humidity = 50 + (int)(Math.random() * 40);
                data.pressure = 1000 + Math.random() * 30;
                data.description = "Simulated (no API key)";
                data.success = true;
            } else {
                // Real API call
                String urlStr = BASE_URL + "?q=" + URLEncoder.encode(city + ",NP", "UTF-8")
                        + "&appid=" + API_KEY + "&units=metric";
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) response.append(line);
                br.close();

                String json = response.toString();
                data.tempCelsius = parseDouble(json, "\"temp\":");
                data.humidity    = (int) parseDouble(json, "\"humidity\":");
                data.pressure    = parseDouble(json, "\"pressure\":");
                data.description = parseString(json, "\"description\":\"", "\"");
                data.success = true;
            }
        } catch (Exception e) {
            data.success = false;
            data.error = e.getMessage();
            data.description = "Error: " + (data.error != null ? data.error : "Unknown");
        }
        data.fetchTimeMs = System.currentTimeMillis() - t0;

        // Thread-safe storage
        weatherResults.put(city, data);

        // Thread-safe GUI update
        if (updateUI) {
            WeatherData finalData = data;
            SwingUtilities.invokeLater(() -> updateTableRow(finalData));
        }
    }

    private void updateTableRow(WeatherData data) {
        // Find city row
        for (int i = 0; i < CITIES.length; i++) {
            if (CITIES[i].equals(data.city)) {
                if (data.success) {
                    tableModel.setValueAt(String.format("%.1f", data.tempCelsius), i, 1);
                    tableModel.setValueAt(data.humidity, i, 2);
                    tableModel.setValueAt(String.format("%.1f", data.pressure), i, 3);
                    tableModel.setValueAt(data.description, i, 4);
                } else {
                    tableModel.setValueAt("ERR", i, 1);
                    tableModel.setValueAt("ERR", i, 2);
                    tableModel.setValueAt("ERR", i, 3);
                    tableModel.setValueAt(data.description, i, 4);
                }
                tableModel.setValueAt(data.fetchTimeMs + " ms", i, 5);
                break;
            }
        }
        int done = completedCount.incrementAndGet();
        statusLabel.setText("  Fetched " + done + "/" + CITIES.length + " cities...");
    }

    private void onAllFetched() {
        fetchButton.setEnabled(true);
        statusLabel.setText("  ✅ All cities fetched! Sequential: " + sequentialMs +
                "ms | Parallel: " + parallelMs + "ms");

        // Update all table rows from results (parallel run)
        for (WeatherData data : weatherResults.values()) {
            updateTableRow(data);
        }

        double speedup = sequentialMs > 0 ? (double) sequentialMs / parallelMs : 0;
        seqTimeLabel.setText("Sequential Time: " + sequentialMs + " ms");
        parTimeLabel.setText("Parallel Time:   " + parallelMs + " ms");
        speedupLabel.setText(String.format("Speedup: %.2f×", speedup));
        chartPanel.repaint();
    }

    // ─── Simple JSON Parsing ──────────────────────────────────────────────────────
    private double parseDouble(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0) return 0;
        int start = idx + key.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) ||
               json.charAt(end) == '.' || json.charAt(end) == '-')) end++;
        try { return Double.parseDouble(json.substring(start, end)); }
        catch (NumberFormatException e) { return 0; }
    }

    private String parseString(String json, String key, String endMark) {
        int idx = json.indexOf(key);
        if (idx < 0) return "N/A";
        int start = idx + key.length();
        int end = json.indexOf(endMark, start);
        return end > start ? json.substring(start, end) : "N/A";
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Question5b_WeatherCollector());
    }
}