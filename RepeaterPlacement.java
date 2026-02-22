// You're an engineer working for a telecommunications company in Kathmandu, Nepal, tasked with 
// optimizing the placement of new signal repeaters. Your goal is to maximize network coverage efficiency by 
// identifying locations where a single, powerful repeater can serve the largest number of existing customer 
// households, which are represented as points on a 2D map. (Question No 1)

import java.util.*;

class Point {
    double x, y;
    Point(double x, double y) {
        this.x = x;
        this.y = y;
    }
}

public class RepeaterPlacement {

    static double dist(Point a, Point b) {
        return Math.hypot(a.x - b.x, a.y - b.y);
    }

    static int countCovered(List<Point> pts, Point center, double R) {
        int count = 0;
        for (Point p : pts) {
            if (dist(p, center) <= R + 1e-9) {
                count++;
            }
        }
        return count;
    }

    // Return possible circle centers of radius R passing through p1 & p2
    static List<Point> getCenters(Point p1, Point p2, double R) {
        List<Point> centers = new ArrayList<>();

        double d = dist(p1, p2);
        if (d > 2 * R) return centers;

        double midX = (p1.x + p2.x) / 2.0;
        double midY = (p1.y + p2.y) / 2.0;

        double h = Math.sqrt(R * R - (d / 2) * (d / 2));

        double dx = (p2.x - p1.x) / d;
        double dy = (p2.y - p1.y) / d;

        // perpendicular vector
        double px = -dy;
        double py = dx;

        centers.add(new Point(midX + px * h, midY + py * h));
        centers.add(new Point(midX - px * h, midY - py * h));

        return centers;
    }

    static void findBestLocation(List<Point> houses, double R) {
        int bestCount = 0;
        Point bestCenter = null;

        // Check centers at each house (covers single cluster cases)
        for (Point p : houses) {
            int c = countCovered(houses, p, R);
            if (c > bestCount) {
                bestCount = c;
                bestCenter = p;
            }
        }

        // Check circle centers formed by pairs
        for (int i = 0; i < houses.size(); i++) {
            for (int j = i + 1; j < houses.size(); j++) {
                List<Point> centers = getCenters(houses.get(i), houses.get(j), R);
                for (Point center : centers) {
                    int c = countCovered(houses, center, R);
                    if (c > bestCount) {
                        bestCount = c;
                        bestCenter = center;
                    }
                }
            }
        }

        System.out.println("Maximum households covered: " + bestCount);
        if (bestCenter != null) {
            System.out.printf("Best repeater location: (%.3f, %.3f)%n", bestCenter.x, bestCenter.y);
        }
    }

    public static void main(String[] args) {
        List<Point> houses = Arrays.asList(
                new Point(1, 2),
                new Point(2, 3),
                new Point(5, 4),
                new Point(3, 1),
                new Point(6, 2)
        );

        double R = 2.5; // repeater range (km, or map units)

        findBestLocation(houses, R);
    }
}