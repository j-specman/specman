package classman.render;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.AssociationRelationModel_V001;
import classman.model.v001.TextPlaceModel_V001;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static classman.render.DiagramStyle.TEXT_LEADER_DISTANCE;

/** The lines which join a text which the user has moved far from its line to the line, so that it is clear what the
 * text belongs to. The editor shows them while the relation is selected, they are no part of the diagram. */
public final class TextLeaders {
    private TextLeaders() {
    }

    /**
     * @param path the points of the line of the relation
     * @param textAreas where the texts of the relation are, by kind
     * @return for each text which stands more than {@link DiagramStyle#TEXT_LEADER_DISTANCE} away from the line a
     * line from the point of the line which the place of the text belongs to to the closest point of the text
     */
    public static List<Line2D.Double> of(AbstractRelationModel_V001 relation, List<? extends Point2D> path,
                                         Map<TextKind, Rectangle2D> textAreas) {
        List<Line2D.Double> leaders = new ArrayList<>();
        double length = PathGeometry.length(path);
        if (relation.labelPlace != null) {
            add(leaders, path, textAreas.get(TextKind.RELATION_LABEL), relation.labelPlace,
                length * relation.labelPlace.along / 100);
        }
        if (relation instanceof AssociationRelationModel_V001 association) {
            if (association.fromMultiplicityPlace != null) {
                add(leaders, path, textAreas.get(TextKind.FROM_MULTIPLICITY), association.fromMultiplicityPlace,
                    association.fromMultiplicityPlace.along);
            }
            if (association.toMultiplicityPlace != null) {
                add(leaders, path, textAreas.get(TextKind.TO_MULTIPLICITY), association.toMultiplicityPlace,
                    length - association.toMultiplicityPlace.along);
            }
        }
        return leaders;
    }

    private static void add(List<Line2D.Double> leaders, List<? extends Point2D> path, Rectangle2D area,
                            TextPlaceModel_V001 place, double distanceAlong) {
        if (area == null || Math.abs(place.across) <= TEXT_LEADER_DISTANCE) {
            return;
        }
        Point2D.Double onLine = PathGeometry.pointAtDistance(path, distanceAlong);
        double x = Math.max(area.getMinX(), Math.min(onLine.x, area.getMaxX()));
        double y = Math.max(area.getMinY(), Math.min(onLine.y, area.getMaxY()));
        leaders.add(new Line2D.Double(onLine.x, onLine.y, x, y));
    }
}
