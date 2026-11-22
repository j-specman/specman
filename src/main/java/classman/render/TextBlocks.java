package classman.render;

import java.awt.geom.Rectangle2D;
import java.util.List;

/** The area of a text of several lines. */
final class TextBlocks {
    private TextBlocks() {
    }

    /** @return the smallest rectangle around the lines, null if there are none */
    static Rectangle2D.Double areaOf(List<SceneText> lines, TextMeasurer measurer) {
        Rectangle2D.Double area = null;
        for (SceneText line : lines) {
            Rectangle2D.Double lineArea = new Rectangle2D.Double(line.left(), line.y - measurer.ascent(),
                line.width, measurer.lineHeight());
            if (area == null) {
                area = lineArea;
            }
            else {
                area.add(lineArea);
            }
        }
        return area;
    }
}
