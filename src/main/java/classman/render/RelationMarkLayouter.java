package classman.render;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AssociationRelationModel_V001;
import classman.model.v001.ChangeModel_V001;
import classman.model.v001.ChangeType_V001;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import static classman.render.DiagramStyle.CROSS_HALF_SIZE;
import static classman.render.DiagramStyle.RELATION_HIGHLIGHT_WIDTH;
import static classman.render.DiagramStyle.STRIKE_COLOR;
import static classman.render.DiagramStyle.STRIKE_WIDTH;
import static classman.render.DiagramStyle.STROKE;
import static classman.render.DiagramStyle.VEILED_LINE;

/**
 * Makes the change marks of a relation. An added or removed relation gets a thick line in the color of the change
 * set under its own line. A removed one is veiled, with a cross on its line and a label which is struck through.
 * Texts which a change set changed get a colored background. The marks of the texts of an added or removed relation
 * are not shown.
 */
public class RelationMarkLayouter {
    private final TextHighlighter highlighter;
    private final ChangePalette palette;

    public RelationMarkLayouter(TextMeasurer measurer, ChangePalette palette) {
        this.highlighter = new TextHighlighter(measurer, palette);
        this.palette = palette;
    }

    /** The color of the line and the marks at its ends. */
    public String lineColor(AbstractRelationModel_V001 relation) {
        return isRemoved(relation) ? VEILED_LINE : STROKE;
    }

    /** What goes below the line of the relation. */
    public List<AbstractSceneElement> underlay(AbstractRelationModel_V001 relation, List<Point2D.Double> path,
                                               RelationTexts texts) {
        List<AbstractSceneElement> underlay = new ArrayList<>();
        ChangeModel_V001 change = relation.change;
        if (change == null) {
            underlay.addAll(textMarks(relation, texts));
            return underlay;
        }
        underlay.add(thickLine(path, palette.strong(change.changeSet)));
        underlay.addAll(highlighter.wholeHighlights(change.changeSet, texts.labelLines));
        return underlay;
    }

    /** What goes above the line and the texts of the relation: the strike through the label, and the cross. */
    public List<AbstractSceneElement> overlay(AbstractRelationModel_V001 relation, RelationTexts texts,
                                              Point2D.Double crossCenter) {
        List<AbstractSceneElement> overlay = new ArrayList<>();
        if (isRemoved(relation)) {
            overlay.addAll(highlighter.strikes(texts.labelLines));
            overlay.add(new SceneLine(crossCenter.x - CROSS_HALF_SIZE, crossCenter.y - CROSS_HALF_SIZE,
                crossCenter.x + CROSS_HALF_SIZE, crossCenter.y + CROSS_HALF_SIZE, false, STRIKE_COLOR, 2 * STRIKE_WIDTH));
            overlay.add(new SceneLine(crossCenter.x - CROSS_HALF_SIZE, crossCenter.y + CROSS_HALF_SIZE,
                crossCenter.x + CROSS_HALF_SIZE, crossCenter.y - CROSS_HALF_SIZE, false, STRIKE_COLOR, 2 * STRIKE_WIDTH));
        }
        return overlay;
    }

    private boolean isRemoved(AbstractRelationModel_V001 relation) {
        return relation.change != null && relation.change.type == ChangeType_V001.REMOVED;
    }

    private List<SceneRectangle> textMarks(AbstractRelationModel_V001 relation, RelationTexts texts) {
        List<SceneRectangle> marks = new ArrayList<>();
        if (relation instanceof AssociationRelationModel_V001 association) {
            marks.addAll(highlighter.changeHighlights(association.fromMultiplicityChange, association.fromMultiplicity,
                listOf(texts.fromMultiplicity)));
            marks.addAll(highlighter.changeHighlights(association.toMultiplicityChange, association.toMultiplicity,
                listOf(texts.toMultiplicity)));
        }
        marks.addAll(highlighter.changeHighlights(relation.labelChange, relation.label, texts.labelLines));
        return marks;
    }

    private List<SceneText> listOf(SceneText text) {
        return text == null ? List.of() : List.of(text);
    }

    private ScenePolyline thickLine(List<Point2D.Double> path, String color) {
        double[] xs = new double[path.size()];
        double[] ys = new double[path.size()];
        for (int i = 0; i < path.size(); i++) {
            xs[i] = path.get(i).x;
            ys[i] = path.get(i).y;
        }
        return new ScenePolyline(xs, ys, false, color, RELATION_HIGHLIGHT_WIDTH).keepingVisible();
    }
}
