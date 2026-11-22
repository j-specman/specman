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
     * A slanted line is not crossed by the text: the text keeps its gap to the line at the place where the line is
     * closest to it.
     * @param border where the line touches the box
     * @param inside the next point on the line, away from the box
     * @param leavesSideways true if the line leaves the box at its left or right side: the text stands above or below
     * the line, next to the border. False if it leaves at the top or bottom: the text stands right or left of the line,
     * just beyond the border. This follows the side of the box and not the direction of the line, so the text stays at
     * the box also where the line is slanted.
     * @param shift how far the text is moved along the line, away from the border
     * @return the text, null if it is moved so far that it would not be beside the part of the line any more
     */
    SceneText atEnd(String multiplicity, Point2D.Double border, Point2D.Double inside, LineSide side,
                    boolean leavesSideways, double shift) {
        double width = measurer.width(multiplicity, false);
        if (leavesSideways) {
            boolean fits = shift == 0 || MULTIPLICITY_ALONG_GAP + shift + width <= Math.abs(inside.x - border.x);
            return fits ? aboveOrBelow(multiplicity, width, border, inside, side, shift) : null;
        }
        boolean fits = shift == 0
            || MULTIPLICITY_BORDER_GAP + shift + measurer.lineHeight() <= Math.abs(inside.y - border.y);
        return fits ? rightOrLeft(multiplicity, width, border, inside, side, shift) : null;
    }

    private SceneText aboveOrBelow(String multiplicity, double width, Point2D.Double border, Point2D.Double inside,
                                   LineSide side, double shift) {
        boolean towardsRight = inside.x > border.x;
        double distance = MULTIPLICITY_ALONG_GAP + shift;
        double x = towardsRight ? border.x + distance : border.x - distance;
        double farEnd = towardsRight ? x + width : x - width;
        double yNear = PathGeometry.yOnLine(border, inside, x);
        double yFar = PathGeometry.yOnLine(border, inside, farEnd);
        double baseline = side == LineSide.USUAL
            ? Math.min(border.y, Math.min(yNear, yFar)) - MULTIPLICITY_SIDE_GAP
            : Math.max(border.y, Math.max(yNear, yFar)) + MULTIPLICITY_SIDE_GAP + measurer.ascent();
        return new SceneText(x, baseline, multiplicity, false, towardsRight ? TextAnchor.START : TextAnchor.END, width);
    }

    private SceneText rightOrLeft(String multiplicity, double width, Point2D.Double border, Point2D.Double inside,
                                  LineSide side, double shift) {
        double top = inside.y > border.y ? border.y + MULTIPLICITY_BORDER_GAP + shift
            : border.y - MULTIPLICITY_BORDER_GAP - shift - measurer.lineHeight();
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
