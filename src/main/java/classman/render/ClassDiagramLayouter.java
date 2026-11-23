package classman.render;

import classman.model.v001.AbstractBoxModel_V001;
import classman.model.v001.AbstractDiagramElementModel_V001;
import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AttachLineModel_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.NoteModel_V001;
import classman.model.v001.PositionModel_V001;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static classman.render.DiagramStyle.STACK_GAP;

/**
 * Turns a class diagram model into a scene. Classes and notes with a position stand there. The others are
 * stacked from top to bottom in source order, centered on one axis, so that relations between neighbors in the
 * stack are straight vertical lines. The relations are drawn above the boxes.
 */
public class ClassDiagramLayouter {
    private final TextMeasurer measurer;
    private final ChangePalette palette;
    /** One layouter for each font which the classes use, because it measures texts in its font. */
    private final Map<TextFont, ClassBoxLayouter> classLayouters = new HashMap<>();
    private final NoteBoxLayouter noteLayouter;
    private final AttachLineLayouter attachLineLayouter = new AttachLineLayouter();
    private final AttachLineMarkLayouter attachLineMarkLayouter;
    private final RelationLayouter relationLayouter;

    /** Lays out diagrams whose change marks all get the color for unknown change sets. */
    public ClassDiagramLayouter(TextMeasurer measurer) {
        this(measurer, ChangeColors.NONE);
    }

    public ClassDiagramLayouter(TextMeasurer measurer, ChangeColors changeColors) {
        this.measurer = measurer;
        this.palette = new ChangePalette(changeColors);
        this.noteLayouter = new NoteBoxLayouter(measurer, palette);
        this.attachLineMarkLayouter = new AttachLineMarkLayouter(palette);
        this.relationLayouter = new RelationLayouter(measurer, palette);
    }

    private ClassBoxLayouter classLayouterFor(ClassModel_V001 classModel) {
        return classLayouters.computeIfAbsent(TextFont.of(classModel.style),
            font -> new ClassBoxLayouter(measurer.forFont(font), palette, font));
    }

    /** @throws IllegalArgumentException if a relation refers to a class ID which isn't in the diagram, or a note to an unknown ID */
    public Scene layout(ClassDiagramModel_V001 model) {
        HiddenElements hidden = new HiddenElements(model);
        Map<AbstractDiagramElementModel_V001, BoxLayout> boxes = layoutBoxes(model, hidden);
        Map<AbstractDiagramElementModel_V001, Rectangle2D.Double> areas = placeBoxes(boxes);
        AttachTargetIndex attachTargets = new AttachTargetIndex();

        List<AbstractSceneElement> elements = new ArrayList<>();
        List<AbstractSceneElement> underlays = new ArrayList<>();
        Rectangle2D.Double covered = null;
        for (Map.Entry<AbstractDiagramElementModel_V001, BoxLayout> box : boxes.entrySet()) {
            Rectangle2D.Double area = areas.get(box.getKey());
            elements.addAll(box.getValue().placedAt(area.x, area.y));
            covered = BoxGeometry.union(covered, box.getValue().coveredAt(area.x, area.y));
            if (box.getKey() instanceof ClassModel_V001 classModel) {
                addAttachTargets(attachTargets, classModel, area, box.getValue());
            }
        }
        List<SceneElementArea> elementAreas = new ArrayList<>();
        List<SceneTextArea> textAreas = new ArrayList<>();
        List<SceneRelationLine> relationLines = new ArrayList<>();
        areas.forEach((element, area) -> {
            elementAreas.add(new SceneElementArea(element, area));
            boxes.get(element).textAreasAt(area.x, area.y).forEach((kind, textArea) ->
                textAreas.add(new SceneTextArea(element, kind, textArea)));
            if (element instanceof ClassModel_V001 classModel) {
                textAreas.addAll(boxes.get(element).taggedValueTextAreasAt(classModel, area.x, area.y));
                textAreas.addAll(boxes.get(element).attributeTextAreasAt(classModel, area.x, area.y));
            }
        });
        Map<String, Rectangle2D.Double> classAreas = classAreas(areas);
        for (AbstractRelationModel_V001 relation : model.relations()) {
            RelationLayout relationLayout = relationLayouter.layout(relation, classAreas, areas.values());
            elements.addAll(relationLayout.elements);
            underlays.addAll(relationLayout.underlay);
            covered = BoxGeometry.union(covered, relationLayout.bounds);
            textAreas.addAll(relationLayout.textAreas);
            relationLines.add(new SceneRelationLine(relation, relationLayout.line, relationLayout.decorations));
            attachTargets.add(relation.id, new RelationAttachTarget(relationLayout.path));
        }
        List<SceneAttachLine> attachLines = attachLines(model, areas, attachTargets, hidden);
        for (SceneAttachLine attachLine : attachLines) {
            underlays.addAll(attachLineMarkLayouter.underlay(attachLine.attachLine, attachLine.line));
            elements.add(attachLine.line);
            elements.addAll(attachLineMarkLayouter.overlay(attachLine.attachLine, attachLine.line));
            covered = BoxGeometry.union(covered, attachLine.line.bounds());
        }
        elements.addAll(0, underlays);
        if (covered == null) {
            return new Scene(elements, elementAreas, textAreas, relationLines, attachLines, attachTargets, 0, 0, 0, 0);
        }
        return new Scene(elements, elementAreas, textAreas, relationLines, attachLines, attachTargets, covered.x,
            covered.y,
            covered.width, covered.height);
    }

    private void addAttachTargets(AttachTargetIndex attachTargets, ClassModel_V001 classModel,
                                  Rectangle2D.Double classArea, BoxLayout box) {
        attachTargets.add(classModel.id, new ClassAttachTarget(classArea));
        if (!classModel.showsAttributes()) {
            return;
        }
        box.attributeAreasAt(classArea.x, classArea.y).forEach((id, attributeArea) ->
            attachTargets.add(id, new AttributeAttachTarget(attributeArea)));
    }

    private Map<AbstractDiagramElementModel_V001, BoxLayout> layoutBoxes(ClassDiagramModel_V001 model,
                                                                         HiddenElements hidden) {
        Map<AbstractDiagramElementModel_V001, BoxLayout> boxes = new LinkedHashMap<>();
        for (AbstractDiagramElementModel_V001 element : model.elements) {
            if (element instanceof ClassModel_V001 classModel) {
                boxes.put(classModel, classLayouterFor(classModel).layout(classModel));
            } else if (element instanceof NoteModel_V001 note && !hidden.isNoteHidden(note)) {
                boxes.put(note, noteLayouter.layout(note));
            }
        }
        return boxes;
    }

    /** Boxes with a position stand there, the others are stacked. */
    private Map<AbstractDiagramElementModel_V001, Rectangle2D.Double> placeBoxes(
            Map<AbstractDiagramElementModel_V001, BoxLayout> boxes) {
        double axis = stackWidth(boxes) / 2;
        double stackY = 0;
        Map<AbstractDiagramElementModel_V001, Rectangle2D.Double> areas = new LinkedHashMap<>();
        for (Map.Entry<AbstractDiagramElementModel_V001, BoxLayout> box : boxes.entrySet()) {
            BoxLayout layout = box.getValue();
            PositionModel_V001 position = positionOf(box.getKey());
            if (position == null) {
                areas.put(box.getKey(), new Rectangle2D.Double(axis - layout.width / 2, stackY, layout.width, layout.height));
                stackY += layout.height + STACK_GAP;
            } else {
                areas.put(box.getKey(), new Rectangle2D.Double(position.x, position.y, layout.width, layout.height));
            }
        }
        return areas;
    }

    private PositionModel_V001 positionOf(AbstractDiagramElementModel_V001 element) {
        return ((AbstractBoxModel_V001) element).position;
    }

    private double stackWidth(Map<AbstractDiagramElementModel_V001, BoxLayout> boxes) {
        double widest = 0;
        for (Map.Entry<AbstractDiagramElementModel_V001, BoxLayout> box : boxes.entrySet()) {
            if (positionOf(box.getKey()) == null) {
                widest = Math.max(widest, box.getValue().width);
            }
        }
        return widest;
    }

    /** The dashed lines from the notes and classes to the elements they are attached to, except those which lead to
     * attributes in a hidden compartment.
     * @throws IllegalArgumentException if a dashed line starts at a box which the diagram doesn't have, or leads to an
     * element which it doesn't have */
    private List<SceneAttachLine> attachLines(ClassDiagramModel_V001 model,
            Map<AbstractDiagramElementModel_V001, Rectangle2D.Double> areas, AttachTargetIndex attachTargets,
            HiddenElements hidden) {
        List<SceneAttachLine> lines = new ArrayList<>();
        for (AttachLineModel_V001 attachLine : model.attachLines()) {
            if (hidden.isTargetHidden(attachLine.targetId)) {
                continue;
            }
            AbstractBoxModel_V001 box = model.boxWithId(attachLine.startId);
            if (box == null) {
                throw new IllegalArgumentException("The diagram has no box with the ID " + attachLine.startId
                    + ", a dashed line starts at it");
            }
            lines.add(new SceneAttachLine(attachLine, box, attachLineLayouter.layout(areas.get(box),
                attachTargets.find(attachLine.targetId), waypoints(attachLine), attachLine.start, attachLine.end,
                attachLineMarkLayouter.lineColor(attachLine))));
        }
        return lines;
    }

    private List<Point2D.Double> waypoints(AttachLineModel_V001 attachLine) {
        List<Point2D.Double> waypoints = new ArrayList<>();
        for (PositionModel_V001 waypoint : attachLine.waypoints) {
            waypoints.add(new Point2D.Double(waypoint.x, waypoint.y));
        }
        return waypoints;
    }

    private Map<String, Rectangle2D.Double> classAreas(Map<AbstractDiagramElementModel_V001, Rectangle2D.Double> areas) {
        Map<String, Rectangle2D.Double> classAreas = new LinkedHashMap<>();
        for (Map.Entry<AbstractDiagramElementModel_V001, Rectangle2D.Double> area : areas.entrySet()) {
            if (area.getKey() instanceof ClassModel_V001 classModel && classModel.id != null) {
                classAreas.put(classModel.id, area.getValue());
            }
        }
        return classAreas;
    }
}
