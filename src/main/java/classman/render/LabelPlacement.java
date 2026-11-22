package classman.render;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import static classman.render.DiagramStyle.LABEL_GAP;

/** Where the label of a relation stands when it is put beside a part of the line: centered on a point of it, on one
 * side, clear of the line even where the line is slanted. */
class LabelPlacement {
    private final TextMeasurer measurer;

    LabelPlacement(TextMeasurer measurer) {
        this.measurer = measurer;
    }

    /**
     * Beside a part which runs sideways the label is centered above or below the point, beside one which runs up and
     * down it is centered vertically right or left of the point. A slanted part keeps the gap to the label at the
     * place where it is closest.
     * @param along where on the part the label is centered, 0 at its start and 1 at its end
     * @return the lines of the label
     */
    List<SceneText> beside(String label, Point2D.Double from, Point2D.Double to, double along, LineSide side) {
        String[] lines = label.split("\n", -1);
        double widest = 0;
        for (String line : lines) {
            widest = Math.max(widest, measurer.width(line, false));
        }
        double x = from.x + along * (to.x - from.x);
        double y = from.y + along * (to.y - from.y);
        if (PathGeometry.isHorizontal(from, to)) {
            return aboveOrBelow(lines, x, widest, from, to, side);
        }
        return rightOrLeft(lines, y, from, to, side);
    }

    private List<SceneText> aboveOrBelow(String[] lines, double middleX, double widest, Point2D.Double from,
                                         Point2D.Double to, LineSide side) {
        double yLeft = PathGeometry.yOnLine(from, to, middleX - widest / 2);
        double yRight = PathGeometry.yOnLine(from, to, middleX + widest / 2);
        double firstBaseline = side == LineSide.USUAL
            ? Math.min(yLeft, yRight) - LABEL_GAP - (lines.length - 1) * measurer.lineHeight()
            : Math.max(yLeft, yRight) + LABEL_GAP + measurer.ascent();
        List<SceneText> texts = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            texts.add(new SceneText(middleX, firstBaseline + i * measurer.lineHeight(), lines[i], false,
                TextAnchor.MIDDLE, measurer.width(lines[i], false)));
        }
        return texts;
    }

    private List<SceneText> rightOrLeft(String[] lines, double middleY, Point2D.Double from, Point2D.Double to,
                                        LineSide side) {
        double top = middleY - lines.length * measurer.lineHeight() / 2;
        double bottom = top + lines.length * measurer.lineHeight();
        double xTop = PathGeometry.xOnLine(from, to, top);
        double xBottom = PathGeometry.xOnLine(from, to, bottom);
        boolean right = side == LineSide.USUAL;
        double x = right ? Math.max(xTop, xBottom) + LABEL_GAP : Math.min(xTop, xBottom) - LABEL_GAP;
        List<SceneText> texts = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            texts.add(new SceneText(x, top + measurer.ascent() + i * measurer.lineHeight(), lines[i], false,
                right ? TextAnchor.START : TextAnchor.END, measurer.width(lines[i], false)));
        }
        return texts;
    }
}
