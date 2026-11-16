package classman.render;

import classman.model.v001.AbstractDiagramElementModel_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.NoteModel_V001;
import classman.model.v001.PositionModel_V001;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import static classman.render.DiagramStyle.STACK_GAP;

/**
 * Turns a class diagram model into a scene. Classes and notes with a position stand there. The others are
 * stacked from top to bottom in source order, centered on one axis, so that relations between neighbors in the
 * stack are straight vertical lines.
 */
public class ClassDiagramLayouter {
    private final ClassBoxLayouter classLayouter;
    private final NoteBoxLayouter noteLayouter;

    public ClassDiagramLayouter(TextMeasurer measurer) {
        this.classLayouter = new ClassBoxLayouter(measurer);
        this.noteLayouter = new NoteBoxLayouter(measurer);
    }

    public Scene layout(ClassDiagramModel_V001 model) {
        List<BoxLayout> boxes = new ArrayList<>();
        List<PositionModel_V001> positions = new ArrayList<>();
        for (AbstractDiagramElementModel_V001 element : model.elements) {
            if (element instanceof ClassModel_V001 classModel) {
                boxes.add(classLayouter.layout(classModel));
                positions.add(classModel.position);
            } else if (element instanceof NoteModel_V001 note) {
                boxes.add(noteLayouter.layout(note));
                positions.add(note.position);
            }
        }
        return placeBoxes(boxes, positions);
    }

    private Scene placeBoxes(List<BoxLayout> boxes, List<PositionModel_V001> positions) {
        double axis = stackWidth(boxes, positions) / 2;
        double stackY = 0;
        List<AbstractSceneElement> elements = new ArrayList<>();
        Rectangle2D.Double covered = null;
        for (int i = 0; i < boxes.size(); i++) {
            BoxLayout box = boxes.get(i);
            double x;
            double y;
            if (positions.get(i) == null) {
                x = axis - box.width / 2;
                y = stackY;
                stackY += box.height + STACK_GAP;
            } else {
                x = positions.get(i).x;
                y = positions.get(i).y;
            }
            elements.addAll(box.placedAt(x, y));
            covered = union(covered, new Rectangle2D.Double(x, y, box.width, box.height));
        }
        if (covered == null) {
            return new Scene(elements, 0, 0, 0, 0);
        }
        return new Scene(elements, covered.x, covered.y, covered.width, covered.height);
    }

    private double stackWidth(List<BoxLayout> boxes, List<PositionModel_V001> positions) {
        double widest = 0;
        for (int i = 0; i < boxes.size(); i++) {
            if (positions.get(i) == null) {
                widest = Math.max(widest, boxes.get(i).width);
            }
        }
        return widest;
    }

    private Rectangle2D.Double union(Rectangle2D.Double covered, Rectangle2D.Double box) {
        if (covered == null) {
            return box;
        }
        Rectangle2D.Double result = new Rectangle2D.Double();
        Rectangle2D.union(covered, box, result);
        return result;
    }
}
