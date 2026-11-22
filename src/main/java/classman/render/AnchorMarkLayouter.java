package classman.render;

import classman.model.v001.AnchorLineModel_V001;
import classman.model.v001.ChangeModel_V001;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import static classman.render.DiagramStyle.CROSS_HALF_SIZE;
import static classman.render.DiagramStyle.RELATION_HIGHLIGHT_WIDTH;
import static classman.render.DiagramStyle.STRIKE_COLOR;
import static classman.render.DiagramStyle.STRIKE_WIDTH;
import static classman.render.DiagramStyle.STROKE;
import static classman.render.DiagramStyle.VEILED_LINE;

/** Makes the change marks of an anchor line like those of a relation: an added or removed line gets a thick line in the
 * color of the change set under its own dashed line. A removed one is veiled and has a cross on its line. */
public class AnchorMarkLayouter {
    private final ChangePalette palette;

    public AnchorMarkLayouter(ChangePalette palette) {
        this.palette = palette;
    }

    /** The color of the dashed line. */
    public String lineColor(AnchorLineModel_V001 anchorLine) {
        return anchorLine.isRemoved() ? VEILED_LINE : STROKE;
    }

    /** What goes below the dashed line of the anchor line. */
    public List<AbstractSceneElement> underlay(AnchorLineModel_V001 anchorLine, ScenePolyline line) {
        ChangeModel_V001 change = anchorLine.change;
        if (change == null) {
            return List.of();
        }
        return List.of(new ScenePolyline(line.xs, line.ys, false, palette.strong(change.changeSet),
            RELATION_HIGHLIGHT_WIDTH).keepingVisible());
    }

    /** What goes above the dashed line: the cross on a removed anchor line. */
    public List<AbstractSceneElement> overlay(AnchorLineModel_V001 anchorLine, ScenePolyline line) {
        if (!anchorLine.isRemoved()) {
            return List.of();
        }
        List<Point2D.Double> path = new ArrayList<>();
        for (int i = 0; i < line.xs.length; i++) {
            path.add(new Point2D.Double(line.xs[i], line.ys[i]));
        }
        Point2D.Double center = PathGeometry.quarterPoint(path);
        return List.of(
            new SceneLine(center.x - CROSS_HALF_SIZE, center.y - CROSS_HALF_SIZE, center.x + CROSS_HALF_SIZE,
                center.y + CROSS_HALF_SIZE, false, STRIKE_COLOR, 2 * STRIKE_WIDTH),
            new SceneLine(center.x - CROSS_HALF_SIZE, center.y + CROSS_HALF_SIZE, center.x + CROSS_HALF_SIZE,
                center.y - CROSS_HALF_SIZE, false, STRIKE_COLOR, 2 * STRIKE_WIDTH));
    }
}
