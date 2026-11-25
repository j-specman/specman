package classman.render;

import classman.model.v001.AbstractRelationModel_V001;

import java.util.ArrayList;
import java.util.List;

/** The line a relation is drawn along with the marks at its ends, and the relation it belongs to. */
public class SceneRelationLine extends SceneConnectionLine {
    public final AbstractRelationModel_V001 relation;

    public SceneRelationLine(AbstractRelationModel_V001 relation, ScenePolyline line,
                             List<AbstractSceneElement> decorations) {
        super(line, decorations);
        this.relation = relation;
    }

    @Override
    public SceneRelationLine withStrokeWidth(double width) {
        List<AbstractSceneElement> widerDecorations = new ArrayList<>();
        for (AbstractSceneElement decoration : decorations) {
            widerDecorations.add(withStrokeWidth(decoration, width));
        }
        return new SceneRelationLine(relation, line.withStrokeWidth(width).withRoundCorners(), widerDecorations);
    }

    private static AbstractSceneElement withStrokeWidth(AbstractSceneElement element, double width) {
        if (element instanceof ScenePolygon polygon) {
            return polygon.withStrokeWidth(width).withRoundCorners();
        }
        if (element instanceof ScenePolyline polyline) {
            return polyline.withStrokeWidth(width).withRoundCorners();
        }
        return element;
    }
}
