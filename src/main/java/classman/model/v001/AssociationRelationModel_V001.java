package classman.model.v001;

/** Model of an association, aggregation or composition. */
public class AssociationRelationModel_V001 extends AbstractRelationModel_V001 {
    /** Null if the source gives none. Written as in the source, for example 1..* */
    public String fromMultiplicity;
    public String toMultiplicity;
    /** True for an arrowhead at the target (written ->), false for no arrowhead (written --). */
    public boolean directed;
    /** Null if the relation has no association class. */
    public ClassModel_V001 assocClass;

    public AssociationRelationModel_V001(RelationType_V001 type, String from, String to) {
        super(type, from, to);
        if (!type.isAssociationLike()) {
            throw new IllegalArgumentException("Not an association type: " + type);
        }
    }
}
