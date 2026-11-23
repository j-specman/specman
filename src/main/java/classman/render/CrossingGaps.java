package classman.render;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static classman.render.DiagramStyle.CROSSING_GAP;
import static classman.render.DiagramStyle.TRIANGLE_LENGTH;

/**
 * Lets lines which cross be seen as crossing and not as meeting: where a line is crossed by a line which is drawn
 * later, it gets a small gap, so the later line runs over it. Crossings near the ends of a line are left alone, the
 * marks at the ends are there.
 */
class CrossingGaps {
    /** How far from the ends of both lines a crossing must be to get a gap. */
    private static final double END_ZONE = TRIANGLE_LENGTH;
    /** Steep crossings get the gap as it is, for flat ones it grows along the line, but only up to this share. */
    private static final double MIN_SINE = 0.4;

    /**
     * @param elements the elements of a scene
     * @param lines the lines of the relations in the order they are drawn, each of them is one of the elements
     * @return the elements, each line which is crossed replaced by its pieces
     */
    List<AbstractSceneElement> withGaps(List<AbstractSceneElement> elements, List<ScenePolyline> lines) {
        Map<ScenePolyline, List<ScenePolyline>> pieces = new IdentityHashMap<>();
        for (int i = 0; i < lines.size(); i++) {
            List<double[]> gaps = gapsOf(lines.get(i), lines.subList(i + 1, lines.size()));
            if (!gaps.isEmpty()) {
                pieces.put(lines.get(i), piecesOf(lines.get(i), gaps));
            }
        }
        List<AbstractSceneElement> result = new ArrayList<>();
        for (AbstractSceneElement element : elements) {
            List<ScenePolyline> cut = element instanceof ScenePolyline line ? pieces.get(line) : null;
            if (cut == null) {
                result.add(element);
            } else {
                result.addAll(cut);
            }
        }
        return result;
    }

    /** @return the gaps as pairs of distances from the start of the line, not sorted, maybe overlapping */
    private List<double[]> gapsOf(ScenePolyline line, List<ScenePolyline> linesAbove) {
        List<double[]> gaps = new ArrayList<>();
        for (ScenePolyline above : linesAbove) {
            addGapsUnder(gaps, line, above);
        }
        return gaps;
    }

    private void addGapsUnder(List<double[]> gaps, ScenePolyline line, ScenePolyline above) {
        double[] starts = distancesOfPoints(line);
        double[] aboveStarts = distancesOfPoints(above);
        for (int i = 1; i < line.xs.length; i++) {
            for (int j = 1; j < above.xs.length; j++) {
                double[] crossing = crossing(line, i, above, j);
                if (crossing == null) {
                    continue;
                }
                double at = starts[i - 1] + crossing[0] * (starts[i] - starts[i - 1]);
                double aboveAt = aboveStarts[j - 1] + crossing[1] * (aboveStarts[j] - aboveStarts[j - 1]);
                if (isAwayFromEnds(at, starts) && isAwayFromEnds(aboveAt, aboveStarts)) {
                    double half = (above.strokeWidth / 2 + CROSSING_GAP) / Math.max(crossing[2], MIN_SINE);
                    gaps.add(new double[] {at - half, at + half});
                }
            }
        }
    }

    private boolean isAwayFromEnds(double at, double[] distancesOfPoints) {
        return at >= END_ZONE && distancesOfPoints[distancesOfPoints.length - 1] - at >= END_ZONE;
    }

    /** The distance from the start of the line to each of its points along the line. */
    private double[] distancesOfPoints(ScenePolyline line) {
        double[] distances = new double[line.xs.length];
        for (int i = 1; i < distances.length; i++) {
            distances[i] = distances[i - 1] + distance(line, i - 1, i);
        }
        return distances;
    }

    /**
     * @param i the segment of the line which ends at the point with this index
     * @param j the same for the line above
     * @return where on the segment of the line (0 to 1), where on the segment of the line above (0 to 1) and the sine of
     * the angle between them, null if they don't cross
     */
    private double[] crossing(ScenePolyline line, int i, ScenePolyline above, int j) {
        double dx = line.xs[i] - line.xs[i - 1];
        double dy = line.ys[i] - line.ys[i - 1];
        double aboveDx = above.xs[j] - above.xs[j - 1];
        double aboveDy = above.ys[j] - above.ys[j - 1];
        double denominator = dx * aboveDy - dy * aboveDx;
        double lengths = Math.hypot(dx, dy) * Math.hypot(aboveDx, aboveDy);
        if (lengths == 0 || Math.abs(denominator) < 1e-9 * lengths) {
            return null;
        }
        double toX = above.xs[j - 1] - line.xs[i - 1];
        double toY = above.ys[j - 1] - line.ys[i - 1];
        double onLine = (toX * aboveDy - toY * aboveDx) / denominator;
        double onAbove = (toX * dy - toY * dx) / denominator;
        if (onLine < 0 || onLine > 1 || onAbove < 0 || onAbove > 1) {
            return null;
        }
        return new double[] {onLine, onAbove, Math.abs(denominator) / lengths};
    }

    /** The parts of the line which are left when the gaps are taken out. */
    private List<ScenePolyline> piecesOf(ScenePolyline line, List<double[]> gaps) {
        gaps.sort(Comparator.comparingDouble(gap -> gap[0]));
        List<ScenePolyline> pieces = new ArrayList<>();
        double pieceStart = 0;
        for (double[] gap : gaps) {
            if (gap[0] > pieceStart) {
                pieces.add(piece(line, pieceStart, gap[0]));
            }
            pieceStart = Math.max(pieceStart, gap[1]);
        }
        if (pieceStart < length(line)) {
            pieces.add(piece(line, pieceStart, length(line)));
        }
        return pieces;
    }

    /** The part of the line between two distances from its start. */
    private ScenePolyline piece(ScenePolyline line, double from, double to) {
        List<Point2D.Double> points = new ArrayList<>();
        points.add(pointAt(line, from));
        double walked = 0;
        for (int i = 1; i < line.xs.length; i++) {
            walked += distance(line, i - 1, i);
            if (walked > from && walked < to) {
                points.add(new Point2D.Double(line.xs[i], line.ys[i]));
            }
        }
        points.add(pointAt(line, to));
        double[] xs = new double[points.size()];
        double[] ys = new double[points.size()];
        for (int i = 0; i < xs.length; i++) {
            xs[i] = points.get(i).x;
            ys[i] = points.get(i).y;
        }
        return line.withPoints(xs, ys);
    }

    private Point2D.Double pointAt(ScenePolyline line, double distanceFromStart) {
        double walked = 0;
        for (int i = 1; i < line.xs.length; i++) {
            double segmentLength = distance(line, i - 1, i);
            if (walked + segmentLength >= distanceFromStart || i == line.xs.length - 1) {
                double share = segmentLength == 0 ? 0 : (distanceFromStart - walked) / segmentLength;
                return new Point2D.Double(line.xs[i - 1] + share * (line.xs[i] - line.xs[i - 1]),
                    line.ys[i - 1] + share * (line.ys[i] - line.ys[i - 1]));
            }
            walked += segmentLength;
        }
        return new Point2D.Double(line.xs[0], line.ys[0]);
    }

    private double length(ScenePolyline line) {
        double length = 0;
        for (int i = 1; i < line.xs.length; i++) {
            length += distance(line, i - 1, i);
        }
        return length;
    }

    private double distance(ScenePolyline line, int from, int to) {
        return Math.hypot(line.xs[to] - line.xs[from], line.ys[to] - line.ys[from]);
    }
}
