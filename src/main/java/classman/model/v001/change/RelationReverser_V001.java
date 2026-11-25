package classman.model.v001.change;

import classman.model.v001.AnchorModel_V001;
import classman.model.v001.AssociationRelationModel_V001;
import classman.model.v001.AttachLineModel_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.PositionModel_V001;
import classman.model.v001.RouteModel_V001;
import classman.model.v001.TextPlaceModel_V001;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lets an association, aggregation or composition run the other way: source and target change places, and everything
 * which belongs to an end or is measured from the start of the line goes with it, so the diagram looks the same.
 * The model is changed in place.
 */
public class RelationReverser_V001 {
    private static final int FULL_LENGTH_PERCENT = 100;

    public void reverse(ClassDiagramModel_V001 model, AssociationRelationModel_V001 relation) {
        String formerSourceId = relation.fromId;
        relation.fromId = relation.toId;
        relation.toId = formerSourceId;
        reverseRoute(relation);
        reverseEnds(relation);
        relation.labelPlace = mirrored(relation.labelPlace, true);
        reverseAttachedLines(model, relation);
    }

    private void reverseRoute(AssociationRelationModel_V001 relation) {
        if (relation.route != null) {
            List<PositionModel_V001> waypoints = new ArrayList<>(relation.route.waypoints);
            Collections.reverse(waypoints);
            relation.route = new RouteModel_V001(relation.route.end, waypoints, relation.route.start);
        }
    }

    /** The multiplicities with their marks and places, and the arrowheads, belong to the ends. */
    private void reverseEnds(AssociationRelationModel_V001 relation) {
        String multiplicity = relation.fromMultiplicity;
        relation.fromMultiplicity = relation.toMultiplicity;
        relation.toMultiplicity = multiplicity;
        var multiplicityChange = relation.fromMultiplicityChange;
        relation.fromMultiplicityChange = relation.toMultiplicityChange;
        relation.toMultiplicityChange = multiplicityChange;
        TextPlaceModel_V001 place = mirrored(relation.fromMultiplicityPlace, false);
        relation.fromMultiplicityPlace = mirrored(relation.toMultiplicityPlace, false);
        relation.toMultiplicityPlace = place;
        boolean arrow = relation.fromArrow;
        relation.fromArrow = relation.toArrow;
        relation.toArrow = arrow;
    }

    /** The end of a line which is attached to a point of the relation is given in percent of its length from its start. */
    private void reverseAttachedLines(ClassDiagramModel_V001 model, AssociationRelationModel_V001 relation) {
        for (AttachLineModel_V001 attachLine : model.attachLines()) {
            AnchorModel_V001 end = attachLine.end;
            if (relation.id != null && relation.id.equals(attachLine.targetId) && end != null && end.isOnLine()) {
                attachLine.end = AnchorModel_V001.onLine(end.classId, FULL_LENGTH_PERCENT - end.offset);
            }
        }
    }

    /** The place as seen from the other direction: left and right change sides. A place along the line measured in
     * percent from its start is measured from the other end then; a distance from the end where the multiplicity
     * belongs stays, because the end goes with the multiplicity. */
    private static TextPlaceModel_V001 mirrored(TextPlaceModel_V001 place, boolean alongIsPercent) {
        if (place == null) {
            return null;
        }
        int along = alongIsPercent ? FULL_LENGTH_PERCENT - place.along : place.along;
        return new TextPlaceModel_V001(along, -place.across);
    }
}
