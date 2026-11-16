package classman.render;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Map;

/** Lays out the dashed line from a note to the class it is attached to. */
public class NoteLinkLayouter {

    /**
     * @param noteArea where the note is
     * @param className the class the note is attached to
     * @param classAreas where the classes are, by name
     * @throws IllegalArgumentException if the diagram has no such class
     */
    public ScenePolyline layout(Rectangle2D.Double noteArea, String className, Map<String, Rectangle2D.Double> classAreas) {
        Rectangle2D.Double classArea = classAreas.get(className);
        if (classArea == null) {
            throw new IllegalArgumentException("The diagram has no class " + className + ", a note is attached to it");
        }
        Point2D.Double fromNote = BoxGeometry.exitPoint(noteArea, BoxGeometry.center(classArea));
        Point2D.Double toClass = BoxGeometry.exitPoint(classArea, BoxGeometry.center(noteArea));
        return new ScenePolyline(new double[]{fromNote.x, toClass.x}, new double[]{fromNote.y, toClass.y}, true);
    }
}
