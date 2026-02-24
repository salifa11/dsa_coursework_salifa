/**
 * Question 3 - Agricultural Commodity Trading (Max Profit with K Transactions)
 *
 * Problem: Given daily commodity prices and a max number of allowed buy-sell
 * transactions, find the maximum achievable profit. You cannot hold multiple
 * lots simultaneously (must sell before buying again).
 *
 * Approach: Dynamic Programming (O(k * n) solution)
 * - dp[t][d] = max profit using at most t transactions up to day d
 * - Transition: dp[t][d] = max(dp[t][d-1], prices[d] - prices[m] + dp[t-1][m])
 *   for all m < d (day we bought)
 * - Optimized by tracking maxSoFar = max(dp[t-1][m] - prices[m]) as we scan.
 *
 * Special case: if max_trades >= n/2, unlimited transactions (take every profit).
 *
 * Time Complexity:  O(k * n) where k = max_trades, n = number of days
 * Space Complexity: O(k * n) for DP table (can be reduced to O(n) with two arrays)
 */
public class Question3_CommodityTrading {

    /**
     * Returns the maximum profit achievable with at most maxTrades transactions.
     */
    public static int maxProfit(int maxTrades, int[] dailyPrices) {
        int n = dailyPrices.length;
        if (n <= 1 || maxTrades == 0) return 0;

        // Special case: if we can make unlimited trades, sum up every positive delta
        if (maxTrades >= n / 2) {
            int profit = 0;
            for (int i = 1; i < n; i++) {
                if (dailyPrices[i] > dailyPrices[i - 1]) {
                    profit += dailyPrices[i] - dailyPrices[i - 1];
                }
            }
            return profit;
        }

        // dp[t][d] = max profit using at most t transactions up to day d
        int[][] dp = new int[maxTrades + 1][n];

        for (int t = 1; t <= maxTrades; t++) {
            // maxSoFar tracks max(dp[t-1][m] - prices[m]) for all m < current day
            int maxSoFar = -dailyPrices[0]; // buying on day 0

            for (int d = 1; d < n; d++) {
                // Option 1: do nothing on day d (carry forward previous best)
                // Option 2: sell on day d, having bought on best day m < d
                dp[t][d] = Math.max(dp[t][d - 1], dailyPrices[d] + maxSoFar);

                // Update maxSoFar to include buying on day d for future selling
                maxSoFar = Math.max(maxSoFar, dp[t - 1][d] - dailyPrices[d]);
            }
        }

        return dp[maxTrades][n - 1];
    }

    public static void main(String[] args) {
        // Example 1: max_trades=2, prices=[2000, 4000, 1000]
        // Buy at 2000, sell at 4000 => profit = 2000
        int[] prices1 = {2000, 4000, 1000};
        System.out.println("Example 1:");
        System.out.println("Max Trades: 2");
        System.out.println("Daily Prices (NPR): [2000, 4000, 1000]");
        System.out.println("Max Profit: NPR " + maxProfit(2, prices1));
        System.out.println("Expected: NPR 2000");
        System.out.println();

        // Additional Test: 2 trades possible
        int[] prices2 = {3, 2, 6, 5, 0, 3};
        System.out.println("Additional Test 1:");
        System.out.println("Max Trades: 2");
        System.out.println("Daily Prices: [3, 2, 6, 5, 0, 3]");
        System.out.println("Max Profit: " + maxProfit(2, prices2));
        System.out.println("Expected: 7 (buy@2 sell@6) + (buy@0 sell@3)");
        System.out.println();

        // Additional Test: Only 1 trade
        int[] prices3 = {100, 180, 260, 310, 40, 535, 695};
        System.out.println("Additional Test 2:");
        System.out.println("Max Trades: 1");
        System.out.println("Daily Prices: [100, 180, 260, 310, 40, 535, 695]");
        System.out.println("Max Profit: " + maxProfit(1, prices3));
        System.out.println("Expected: 595 (buy@100 sell@695)");
        System.out.println();

        // Additional Test: Unlimited trades (k >= n/2)
        int[] prices4 = {100, 180, 260, 310, 40, 535, 695};
        System.out.println("Additional Test 3 (unlimited trades):");
        System.out.println("Max Trades: 10");
        System.out.println("Daily Prices: [100, 180, 260, 310, 40, 535, 695]");
        System.out.println("Max Profit: " + maxProfit(10, prices4));
        System.out.println("Expected: 865");
        System.out.println();

        // Additional Test: Decreasing prices - no profit possible
        int[] prices5 = {7, 6, 4, 3, 1};
        System.out.println("Additional Test 4 (decreasing prices):");
        System.out.println("Max Trades: 2");
        System.out.println("Daily Prices: [7, 6, 4, 3, 1]");
        System.out.println("Max Profit: " + maxProfit(2, prices5));
        System.out.println("Expected: 0 (no profitable trade possible)");
    }
}