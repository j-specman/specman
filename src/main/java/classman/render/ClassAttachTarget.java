package classman.render;

import classman.model.v001.AnchorModel_V001;
import classman.model.v001.Side_V001;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/** A class: the line ends at the border of the box, on the way to the middle of the box. */
public class ClassAttachTarget extends AttachTarget {
    private final Rectangle2D.Double classArea;

    public ClassAttachTarget(Rectangle2D.Double classArea) {
        this.classArea = classArea;
    }

    @Override
    public Point2D.Double endFacing(Point2D.Double from) {
        return BoxGeometry.exitPoint(classArea, from);
    }

    @Override
    public Double alignmentX() {
        return classArea.getCenterX();
    }

    @Override
    public Double alignmentY() {
        return classArea.getCenterY();
    }

    @Override
    public Point2D.Double anchorPoint(Side_V001 side, double offset) {
        return BoxGeometry.sidePoint(classArea, side, offset);
    }

    @Override
    public AnchorModel_V001 anchorNear(String id, Point2D point) {
        return BoxGeometry.nearestAnchor(id, classArea, point);
    }
}
