import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Question 1b - Marketing Keyword Segmentation (Word Break II)
 *
 * Problem: Given a user search string and a marketing keyword dictionary,
 * return all possible ways to segment the string so each word is in the dictionary.
 *
 * Approach: Top-down dynamic programming (memoization) with recursive backtracking.
 * - For each starting index, try every prefix substring.
 * - If prefix is in dictionary, recurse on the remainder.
 * - Memoize results for each starting index to avoid redundant work.
 *
 * Time Complexity:  O(n^2 * 2^n) worst case (for highly ambiguous strings)
 * Space Complexity: O(n * 2^n) for memo storage
 */
public class Question1b_KeywordSegmentation {

    // Memoization cache: starting index -> list of valid sentences from that index
    private static Map<Integer, List<String>> memo = new HashMap<>();

    /**
     * Main method to find all keyword segmentations of user_query.
     */
    public static List<String> wordBreak(String userQuery, List<String> dictionary) {
        memo.clear(); // reset for fresh run
        Set<String> wordSet = new HashSet<>(dictionary);
        List<String> results = new ArrayList<>();

        // Get all sentence fragments starting from index 0
        List<String> sentences = backtrack(userQuery, wordSet, 0);
        results.addAll(sentences);
        return results;
    }

    /**
     * Recursive helper with memoization.
     * Returns all valid sentence suffixes starting at index 'start'.
     */
    private static List<String> backtrack(String query, Set<String> wordSet, int start) {
        // Return memoized result if available
        if (memo.containsKey(start)) {
            return memo.get(start);
        }

        List<String> results = new ArrayList<>();

        // Base case: reached end of string, return empty string as valid ending
        if (start == query.length()) {
            results.add("");
            return results;
        }

        // Try every possible end index for the current word
        for (int end = start + 1; end <= query.length(); end++) {
            String word = query.substring(start, end);

            if (wordSet.contains(word)) {
                // Recurse on the remainder of the string
                List<String> remainingResults = backtrack(query, wordSet, end);

                for (String remaining : remainingResults) {
                    // Combine current word with remaining sentence
                    if (remaining.isEmpty()) {
                        results.add(word); // last word in the sentence
                    } else {
                        results.add(word + " " + remaining);
                    }
                }
            }
        }

        // Memoize and return
        memo.put(start, results);
        return results;
    }

    public static void main(String[] args) {
        // Example 1: Basic segmentation
        String query1 = "nepaltrekkingguide";
        List<String> dict1 = List.of("nepal", "trekking", "guide", "nepaltrekking");
        List<String> result1 = wordBreak(query1, dict1);
        System.out.println("Example 1:");
        System.out.println("User Query: " + query1);
        System.out.println("Dictionary: " + dict1);
        System.out.println("Segmentations: " + result1);
        System.out.println("Expected: [nepal trekking guide, nepaltrekking guide]");
        System.out.println();

        // Example 2: Complex segmentation
        String query2 = "visitkathmandunepal";
        List<String> dict2 = List.of("visit", "kathmandu", "nepal", "visitkathmandu", "kathmandunepal");
        List<String> result2 = wordBreak(query2, dict2);
        System.out.println("Example 2:");
        System.out.println("User Query: " + query2);
        System.out.println("Dictionary: " + dict2);
        System.out.println("Segmentations: " + result2);
        System.out.println("Expected: [visit kathmandu nepal, visitkathmandu nepal, visit kathmandunepal]");
        System.out.println();

        // Example 3: No valid segmentation
        String query3 = "everesthikingtrail";
        List<String> dict3 = List.of("everest", "hiking", "trek");
        List<String> result3 = wordBreak(query3, dict3);
        System.out.println("Example 3:");
        System.out.println("User Query: " + query3);
        System.out.println("Dictionary: " + dict3);
        System.out.println("Segmentations: " + result3);
        System.out.println("Expected: []");
        System.out.println();

        // Example 4: Reusing keywords
        String query4 = "catcat";
        List<String> dict4 = List.of("cat");
        List<String> result4 = wordBreak(query4, dict4);
        System.out.println("Additional Test (keyword reuse):");
        System.out.println("User Query: " + query4);
        System.out.println("Dictionary: " + dict4);
        System.out.println("Segmentations: " + result4);
        System.out.println("Expected: [cat cat]");
    }
}