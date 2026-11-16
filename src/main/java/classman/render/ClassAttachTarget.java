package classman.render;

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
}
