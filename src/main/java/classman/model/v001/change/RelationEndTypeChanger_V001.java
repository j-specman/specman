package classman.model.v001.change;

import classman.model.v001.AssociationRelationModel_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.EndType_V001;
import classman.model.v001.RelationEnd_V001;
import classman.model.v001.RelationType_V001;

/**
 * Tells and changes what there is at an end of an association, aggregation or composition. The model has the diamond
 * of an aggregation or composition always at the source, so putting it at the target lets the relation run the other
 * way, see {@link RelationReverser_V001}. The model is changed in place.
 */
public class RelationEndTypeChanger_V001 {
    private final RelationReverser_V001 reverser = new RelationReverser_V001();

    /** @return what the relation has at its end */
    public EndType_V001 typeAt(AssociationRelationModel_V001 relation, RelationEnd_V001 end) {
        if (end == RelationEnd_V001.FROM && relation.type == RelationType_V001.AGGREGATION) {
            return EndType_V001.AGGREGATION;
        }
        if (end == RelationEnd_V001.FROM && relation.type == RelationType_V001.COMPOSITION) {
            return EndType_V001.COMPOSITION;
        }
        boolean hasArrow = end == RelationEnd_V001.FROM ? relation.fromArrow : relation.toArrow;
        return hasArrow ? EndType_V001.NAVIGABLE : EndType_V001.ASSOCIATION;
    }

    /** Gives the end the type. A diamond which the other end has already has to go first, as a relation has one diamond
     * at most. An end which loses its diamond makes the relation an association. */
    public void set(ClassDiagramModel_V001 model, AssociationRelationModel_V001 relation, RelationEnd_V001 end,
                    EndType_V001 type) {
        if (type.isDiamond()) {
            setDiamond(model, relation, end, type);
        }
        else {
            if (relation.type != RelationType_V001.ASSOCIATION && end == RelationEnd_V001.FROM) {
                relation.type = RelationType_V001.ASSOCIATION;
            }
            boolean hasArrow = type == EndType_V001.NAVIGABLE;
            if (end == RelationEnd_V001.FROM) {
                relation.fromArrow = hasArrow;
            }
            else {
                relation.toArrow = hasArrow;
            }
        }
    }

    private void setDiamond(ClassDiagramModel_V001 model, AssociationRelationModel_V001 relation, RelationEnd_V001 end,
                            EndType_V001 type) {
        if (end == RelationEnd_V001.TO) {
            reverser.reverse(model, relation);
        }
        relation.type = type == EndType_V001.COMPOSITION ? RelationType_V001.COMPOSITION : RelationType_V001.AGGREGATION;
        relation.fromArrow = false;
    }
}
