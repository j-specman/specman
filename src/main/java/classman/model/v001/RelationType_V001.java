package classman.model.v001;

public enum RelationType_V001 {
    ASSOCIATION,
    AGGREGATION,
    COMPOSITION,
    EXTENDS,
    IMPLEMENTS;

    /** @return true if the relation can have an arrowhead at the end: an association at both ends, an aggregation or
     * composition only at the target, the source has the diamond, an inheritance or implementation at none, its
     * triangle is its mark */
    public boolean canHaveArrowAt(RelationEnd_V001 end) {
        return switch (this) {
            case ASSOCIATION -> true;
            case AGGREGATION, COMPOSITION -> end == RelationEnd_V001.TO;
            case EXTENDS, IMPLEMENTS -> false;
        };
    }

    /** Association, aggregation and composition have multiplicities and may have an association class. */
    public boolean isAssociationLike() {
        return this == ASSOCIATION || this == AGGREGATION || this == COMPOSITION;
    }
}
