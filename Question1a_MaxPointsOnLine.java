import java.util.HashMap;
import java.util.Map;

/**
 * Question 1a - Signal Repeater Placement (Max Points on a Line)
 * 
 * Problem: Given customer locations as 2D points, find the maximum number of
 * points that lie on the same straight line.
 * 
 * Approach: For each point, compute slopes to all other points using GCD-reduced
 * fractions to avoid floating-point precision issues. Track the maximum count.
 * 
 * Time Complexity:  O(n^2) per point => O(n^2) overall
 * Space Complexity: O(n) for the slope map per iteration
 */
public class Question1a_MaxPointsOnLine {

    public static int maxPoints(int[][] customerLocations) {
        int n = customerLocations.length;

        // Edge cases
        if (n <= 2) return n;

        int maxCount = 2;

        for (int i = 0; i < n; i++) {
            // Map from slope (as string "dy/dx") to count of points sharing that slope
            Map<String, Integer> slopeMap = new HashMap<>();
            int duplicate = 1; // count point i itself

            for (int j = i + 1; j < n; j++) {
                int dx = customerLocations[j][0] - customerLocations[i][0];
                int dy = customerLocations[j][1] - customerLocations[i][1];

                // Handle duplicate points
                if (dx == 0 && dy == 0) {
                    duplicate++;
                    continue;
                }

                // Reduce slope fraction using GCD so we avoid floating-point issues
                int gcd = gcd(Math.abs(dx), Math.abs(dy));
                dx /= gcd;
                dy /= gcd;

                // Normalize sign: keep denominator positive
                if (dx < 0) {
                    dx = -dx;
                    dy = -dy;
                }

                // Create a unique key for the slope
                String key = dy + "/" + dx;
                slopeMap.put(key, slopeMap.getOrDefault(key, 0) + 1);
            }

            // The best for point i = max slope count + duplicates
            int localMax = duplicate;
            for (int count : slopeMap.values()) {
                localMax = Math.max(localMax, count + duplicate);
            }

            maxCount = Math.max(maxCount, localMax);
        }

        return maxCount;
    }

    // Euclidean GCD
    private static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    public static void main(String[] args) {
        // Example 1: All 3 points on the same diagonal line
        int[][] locations1 = {{1, 1}, {2, 2}, {3, 3}};
        System.out.println("Example 1:");
        System.out.println("Customer Locations: [[1,1],[2,2],[3,3]]");
        System.out.println("Max customers on one line: " + maxPoints(locations1));
        System.out.println("Expected: 3");
        System.out.println();

        // Example 2: Complex placement with 4 points on same line
        int[][] locations2 = {{1, 1}, {3, 2}, {5, 3}, {4, 1}, {2, 3}, {1, 4}};
        System.out.println("Example 2:");
        System.out.println("Customer Locations: [[1,1],[3,2],[5,3],[4,1],[2,3],[1,4]]");
        System.out.println("Max customers on one line: " + maxPoints(locations2));
        System.out.println("Expected: 4");
        System.out.println();

        // Additional test: All points vertical
        int[][] locations3 = {{0, 0}, {0, 1}, {0, 2}, {0, 3}};
        System.out.println("Additional Test (vertical line):");
        System.out.println("Customer Locations: [[0,0],[0,1],[0,2],[0,3]]");
        System.out.println("Max customers on one line: " + maxPoints(locations3));
        System.out.println("Expected: 4");
        System.out.println();

        // Additional test: duplicate points
        int[][] locations4 = {{1, 1}, {1, 1}, {2, 2}};
        System.out.println("Additional Test (duplicate points):");
        System.out.println("Customer Locations: [[1,1],[1,1],[2,2]]");
        System.out.println("Max customers on one line: " + maxPoints(locations4));
        System.out.println("Expected: 3");
    }
}