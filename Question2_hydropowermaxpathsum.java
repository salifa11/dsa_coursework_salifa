/**
 * Question 2 - Hydropower Plant Cascade Efficiency (Binary Tree Maximum Path Sum)
 *
 * Problem: Given a binary tree where each node represents a hydropower plant
 * with a net power value (positive = generation, negative = environmental cost),
 * find the maximum sum of any path through the tree.
 * A path can go through any connected sequence of nodes (can bend at any node).
 *
 * Approach: Post-order DFS recursion.
 * - For each node, compute the best single-arm extension (left or right, not both).
 * - At each node, consider the path that "bends" here: left arm + node + right arm.
 * - Track global maximum across all such candidate paths.
 *
 * Time Complexity:  O(n) — visits each node once
 * Space Complexity: O(h) — recursion stack depth equals tree height h
 */
public class Question2_hydropowermaxpathsum {

    // TreeNode definition for the hydropower plant tree
    static class TreeNode {
        int val;
        TreeNode left, right;

        TreeNode(int val) {
            this.val = val;
        }

        TreeNode(int val, TreeNode left, TreeNode right) {
            this.val = val;
            this.left = left;
            this.right = right;
        }
    }

    // Global variable to track maximum path sum found so far
    private static int globalMax;

    /**
     * Returns the maximum net power generation from any valid path in the tree.
     */
    public static int maxPowerGeneration(TreeNode root) {
        globalMax = Integer.MIN_VALUE; // initialize to smallest possible value
        dfs(root);
        return globalMax;
    }

    /**
     * DFS helper that returns the best single-sided path sum starting at 'node'.
     * Internally updates globalMax when a better "bent" path is found.
     */
    private static int dfs(TreeNode node) {
        if (node == null) return 0;

        // Compute max contribution from left and right subtrees
        // If negative, we skip that subtree (take 0 instead)
        int leftMax  = Math.max(0, dfs(node.left));
        int rightMax = Math.max(0, dfs(node.right));

        // Path sum when the path "bends" at this node (uses both arms)
        int bentPathSum = node.val + leftMax + rightMax;

        // Update global maximum with this bent path
        globalMax = Math.max(globalMax, bentPathSum);

        // Return the best single-arm extension upward to the parent
        return node.val + Math.max(leftMax, rightMax);
    }

    // Helper to build a tree from a level-order array (null = missing node)
    private static TreeNode buildTree(Integer[] values) {
        if (values == null || values.length == 0 || values[0] == null) return null;

        TreeNode[] nodes = new TreeNode[values.length];
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null) nodes[i] = new TreeNode(values[i]);
        }

        for (int i = 0; i < values.length; i++) {
            if (nodes[i] != null) {
                int leftIdx  = 2 * i + 1;
                int rightIdx = 2 * i + 2;
                if (leftIdx  < values.length) nodes[i].left  = nodes[leftIdx];
                if (rightIdx < values.length) nodes[i].right = nodes[rightIdx];
            }
        }
        return nodes[0];
    }

    public static void main(String[] args) {
        // Example 1: root = [1, 2, 3]
        //       1
        //      / \
        //     2   3
        // Optimal path: 2 -> 1 -> 3 = 6
        TreeNode root1 = buildTree(new Integer[]{1, 2, 3});
        System.out.println("Example 1:");
        System.out.println("Tree: [1, 2, 3]");
        System.out.println("Max Power Generation: " + maxPowerGeneration(root1));
        System.out.println("Expected: 6");
        System.out.println();

        // Example 2: root = [-10, 9, 20, null, null, 15, 7]
        //         -10
        //         /  \
        //        9   20
        //           /  \
        //          15    7
        // Optimal path: 15 -> 20 -> 7 = 42
        TreeNode root2 = buildTree(new Integer[]{-10, 9, 20, null, null, 15, 7});
        System.out.println("Example 2:");
        System.out.println("Tree: [-10, 9, 20, null, null, 15, 7]");
        System.out.println("Max Power Generation: " + maxPowerGeneration(root2));
        System.out.println("Expected: 42");
        System.out.println();

        // Additional test: Single node
        TreeNode root3 = new TreeNode(-3);
        System.out.println("Additional Test (single negative node):");
        System.out.println("Tree: [-3]");
        System.out.println("Max Power Generation: " + maxPowerGeneration(root3));
        System.out.println("Expected: -3");
        System.out.println();

        // Additional test: All negative except one
        //      -1
        //      / \
        //    -2  -3
        TreeNode root4 = buildTree(new Integer[]{-1, -2, -3});
        System.out.println("Additional Test (all negative):");
        System.out.println("Tree: [-1, -2, -3]");
        System.out.println("Max Power Generation: " + maxPowerGeneration(root4));
        System.out.println("Expected: -1");
    }
}