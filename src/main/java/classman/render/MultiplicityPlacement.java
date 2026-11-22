package classman.render;

import java.awt.geom.Point2D;

import static classman.render.DiagramStyle.MULTIPLICITY_ALONG_GAP;
import static classman.render.DiagramStyle.MULTIPLICITY_BORDER_GAP;
import static classman.render.DiagramStyle.MULTIPLICITY_SIDE_GAP;

/** Where a multiplicity stands at the end of a relation: next to the border where the line touches the box, on one
 * side of the line, clear of the line even where it is slanted. */
class MultiplicityPlacement {
    private final TextMeasurer measurer;

    MultiplicityPlacement(TextMeasurer measurer) {
        this.measurer = measurer;
    }

    /**
     * Beside a line which runs sideways from the box the text stands above or below it, next to the border. Beside a
     * line which runs up or down it stands right or left of it, just beyond the border. A slanted line is not crossed
     * by the text: the text keeps its gap to the line at the place where the line is closest to it.
     * @param border where the line touches the box
     * @param inside the next point on the line, away from the box
     */
    SceneText atEnd(String multiplicity, Point2D.Double border, Point2D.Double inside, LineSide side) {
        double width = measurer.width(multiplicity, false);
        if (PathGeometry.isHorizontal(border, inside)) {
            return aboveOrBelow(multiplicity, width, border, inside, side);
        }
        return rightOrLeft(multiplicity, width, border, inside, side);
    }

    private SceneText aboveOrBelow(String multiplicity, double width, Point2D.Double border, Point2D.Double inside,
                                   LineSide side) {
        boolean towardsRight = inside.x > border.x;
        double x = towardsRight ? border.x + MULTIPLICITY_ALONG_GAP : border.x - MULTIPLICITY_ALONG_GAP;
        double farEnd = towardsRight ? x + width : x - width;
        double yNear = PathGeometry.yOnLine(border, inside, x);
        double yFar = PathGeometry.yOnLine(border, inside, farEnd);
        double baseline = side == LineSide.USUAL
            ? Math.min(border.y, Math.min(yNear, yFar)) - MULTIPLICITY_SIDE_GAP
            : Math.max(border.y, Math.max(yNear, yFar)) + MULTIPLICITY_SIDE_GAP + measurer.ascent();
        return new SceneText(x, baseline, multiplicity, false, towardsRight ? TextAnchor.START : TextAnchor.END, width);
    }

    private SceneText rightOrLeft(String multiplicity, double width, Point2D.Double border, Point2D.Double inside,
                                  LineSide side) {
        double top = inside.y > border.y ? border.y + MULTIPLICITY_BORDER_GAP
            : border.y - MULTIPLICITY_BORDER_GAP - measurer.lineHeight();
        double xTop = PathGeometry.xOnLine(border, inside, top);
        double xBottom = PathGeometry.xOnLine(border, inside, top + measurer.lineHeight());
        double baseline = top + measurer.ascent();
        if (side == LineSide.USUAL) {
            double lineRight = Math.max(border.x, Math.max(xTop, xBottom));
            return new SceneText(lineRight + MULTIPLICITY_SIDE_GAP, baseline, multiplicity, false, TextAnchor.START,
                width);
        }
        double lineLeft = Math.min(border.x, Math.min(xTop, xBottom));
        return new SceneText(lineLeft - MULTIPLICITY_SIDE_GAP, baseline, multiplicity, false, TextAnchor.END, width);
    }
}
