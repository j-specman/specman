package classman.render;

import classman.model.v001.ClassModel_V001;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import static classman.render.DiagramStyle.ASSOC_CLASS_GAP;

/**
 * Lays out the association class of a relation and the dashed line which connects it to the middle of the longest
 * part of the relation. Without a position the class stands on the side of the line where the label isn't:
 * below a horizontal line, left of a vertical one.
 */
public class AssocClassLayouter {
    private final ClassBoxLayouter classLayouter;

    public AssocClassLayouter(ClassBoxLayouter classLayouter) {
        this.classLayouter = classLayouter;
    }

    public RelationLayout layout(ClassModel_V001 assocClass, List<Point2D.Double> relationPath) {
        BoxLayout box = classLayouter.layout(assocClass, DiagramStyle.ASSOC_CLASS_FILL);
        int longest = PathGeometry.longestSegment(relationPath);
        Point2D.Double from = relationPath.get(longest);
        Point2D.Double to = relationPath.get(longest + 1);
        Point2D.Double middle = PathGeometry.attachPoint(relationPath);

        Rectangle2D.Double area;
        Point2D.Double connectorEnd;
        if (assocClass.position != null) {
            area = new Rectangle2D.Double(assocClass.position.x, assocClass.position.y, box.width, box.height);
            connectorEnd = BoxGeometry.exitPoint(area, middle);
        } else if (PathGeometry.isHorizontal(from, to)) {
            area = new Rectangle2D.Double(middle.x - box.width / 2, middle.y + ASSOC_CLASS_GAP, box.width, box.height);
            connectorEnd = new Point2D.Double(middle.x, area.y);
        } else {
            area = new Rectangle2D.Double(middle.x - ASSOC_CLASS_GAP - box.width, middle.y - box.height / 2,
                box.width, box.height);
            connectorEnd = new Point2D.Double(area.getMaxX(), middle.y);
        }

        List<AbstractSceneElement> elements = new ArrayList<>(box.placedAt(area.x, area.y));
        elements.add(new ScenePolyline(new double[]{middle.x, connectorEnd.x}, new double[]{middle.y, connectorEnd.y}, true));
        Rectangle2D.Double connectorArea = new Rectangle2D.Double(Math.min(middle.x, connectorEnd.x),
            Math.min(middle.y, connectorEnd.y), Math.abs(middle.x - connectorEnd.x), Math.abs(middle.y - connectorEnd.y));
        return new RelationLayout(elements, BoxGeometry.union(box.coveredAt(area.x, area.y), connectorArea),
            List.of(new SceneElementArea(assocClass, area)), textAreas(assocClass, box, area));
    }

    private List<SceneTextArea> textAreas(ClassModel_V001 assocClass, BoxLayout box, Rectangle2D.Double area) {
        List<SceneTextArea> textAreas = new ArrayList<>();
        box.textAreasAt(area.x, area.y).forEach((kind, textArea) ->
            textAreas.add(new SceneTextArea(assocClass, kind, textArea)));
        textAreas.addAll(box.attributeTextAreasAt(assocClass, area.x, area.y));
        return textAreas;
    }
}
