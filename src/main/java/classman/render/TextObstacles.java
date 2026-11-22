package classman.render;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** What the texts of a relation shouldn't cover: the boxes, the line of the relation itself and the texts of it which
 * are placed already. */
final class TextObstacles {
    private final List<Rectangle2D.Double> boxes;
    private final List<Point2D.Double> path;
    private final List<Rectangle2D.Double> texts;

    TextObstacles(Collection<Rectangle2D.Double> boxes, List<Point2D.Double> path) {
        this(List.copyOf(boxes), path, List.of());
    }

    private TextObstacles(List<Rectangle2D.Double> boxes, List<Point2D.Double> path, List<Rectangle2D.Double> texts) {
        this.boxes = boxes;
        this.path = path;
        this.texts = texts;
    }

    /** @return the same obstacles and the area of a text which is placed now */
    TextObstacles with(Rectangle2D.Double text) {
        List<Rectangle2D.Double> moreTexts = new ArrayList<>(texts);
        moreTexts.add(text);
        return new TextObstacles(boxes, path, moreTexts);
    }

    /** @return true if the area overlaps a box, a text, or the line */
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
        for (int i = 0; i < path.size() - 1; i++) {
            Point2D.Double from = path.get(i);
            Point2D.Double to = path.get(i + 1);
            if (area.intersectsLine(from.x, from.y, to.x, to.y)) {
                return true;
            }
        }
        return false;
    }
}
