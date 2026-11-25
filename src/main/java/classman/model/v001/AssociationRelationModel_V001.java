package classman.model.v001;

/** Model of an association, aggregation or composition. */
public class AssociationRelationModel_V001 extends AbstractRelationModel_V001 {
    /** Null if the source gives none. Free text, written as in the source, for example 1..* */
    public String fromMultiplicity;
    public String toMultiplicity;
    /** Null if no change set changed the multiplicity. A removed multiplicity is the empty string with the mark. */
    public TextChangeModel_V001 fromMultiplicityChange;
    public TextChangeModel_V001 toMultiplicityChange;
    /** Null if the layout places the multiplicity. Only for a multiplicity which exists. */
    public TextPlaceModel_V001 fromMultiplicityPlace;
    public TextPlaceModel_V001 toMultiplicityPlace;
    /** True for an arrowhead at the source. Never for aggregation and composition, whose source has the diamond. */
    public boolean fromArrow;
    /** True for an arrowhead at the target. */
    public boolean toArrow;

    public AssociationRelationModel_V001(RelationType_V001 type, String fromId, String toId) {
        super(type, fromId, toId);
        if (!type.isAssociationLike()) {
            throw new IllegalArgumentException("Not an association type: " + type);
        }
    }
}
