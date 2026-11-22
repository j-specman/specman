package classman.render;

import classman.model.v001.Side_V001;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** What the texts of a relation shouldn't cover: the boxes, the lines of relations, and texts which are placed
 * already. */
public final class TextObstacles {
    /** How far from the edge of a box a point may be and still lie on that side, in pixels. */
    private static final double ON_SIDE = 0.5;

    private final List<Rectangle2D.Double> boxes;
    private final List<List<Point2D.Double>> paths;
    private final List<Rectangle2D.Double> texts;

    /**
     * @param boxes the areas of all boxes of the diagram
     * @param paths the lines of the relations which have to be kept clear of, the line of the relation whose texts are
     * placed not among them, see {@link #withPath}
     * @param texts the areas of the texts which are placed already
     */
    public TextObstacles(Collection<Rectangle2D.Double> boxes, List<List<Point2D.Double>> paths,
                         Collection<Rectangle2D.Double> texts) {
        this.boxes = List.copyOf(boxes);
        this.paths = List.copyOf(paths);
        this.texts = List.copyOf(texts);
    }

    /** @return the same obstacles and the line of one more relation */
    TextObstacles withPath(List<Point2D.Double> path) {
        List<List<Point2D.Double>> morePaths = new ArrayList<>(paths);
        morePaths.add(path);
        return new TextObstacles(boxes, morePaths, texts);
    }

    /** @return the same obstacles and the area of a text which is placed now */
    TextObstacles with(Rectangle2D.Double text) {
        List<Rectangle2D.Double> moreTexts = new ArrayList<>(texts);
        moreTexts.add(text);
        return new TextObstacles(boxes, paths, moreTexts);
    }

    /** @return the side of a box which the point lies on, null if it lies on the corner of a box or on no box */
    Side_V001 boxSideAt(Point2D.Double point) {
        for (Rectangle2D.Double box : boxes) {
            boolean withinX = point.x >= box.getMinX() - ON_SIDE && point.x <= box.getMaxX() + ON_SIDE;
            boolean withinY = point.y >= box.getMinY() - ON_SIDE && point.y <= box.getMaxY() + ON_SIDE;
            boolean left = withinY && Math.abs(point.x - box.getMinX()) <= ON_SIDE;
            boolean right = withinY && Math.abs(point.x - box.getMaxX()) <= ON_SIDE;
            boolean top = withinX && Math.abs(point.y - box.getMinY()) <= ON_SIDE;
            boolean bottom = withinX && Math.abs(point.y - box.getMaxY()) <= ON_SIDE;
            int sides = (left ? 1 : 0) + (right ? 1 : 0) + (top ? 1 : 0) + (bottom ? 1 : 0);
            if (sides == 1) {
                return left ? Side_V001.LEFT : right ? Side_V001.RIGHT : top ? Side_V001.TOP : Side_V001.BOTTOM;
            }
        }
        return null;
    }

    /** @return true if the area overlaps a box or a text, or a line runs through it */
    boolean covers(Rectangle2D.Double area) {
        return overlapsOne(boxes, area) || overlapsOne(texts, area) || crossedByPath(area);
    }

    private static boolean overlapsOne(List<Rectangle2D.Double> others, Rectangle2D.Double area) {
        for (Rectangle2D.Double other : others) {
            if (other.intersects(area)) {
                return true;
            }
        }
        return false;
    }

    private boolean crossedByPath(Rectangle2D.Double area) {
        for (List<Point2D.Double> path : paths) {
            for (int i = 0; i < path.size() - 1; i++) {
                Point2D.Double from = path.get(i);
                Point2D.Double to = path.get(i + 1);
                if (area.intersectsLine(from.x, from.y, to.x, to.y)) {
                    return true;
                }
            }
        }
        return false;
    }
}
