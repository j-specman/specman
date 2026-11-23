package classman.model.v001;

public enum RelationType_V001 {
    ASSOCIATION,
    AGGREGATION,
    COMPOSITION,
    EXTENDS,
    IMPLEMENTS,
    /** Dashed line with an arrowhead at the target, between classes and attributes. */
    DEPENDENCY;

    /** @return true if the relation can have an arrowhead which the user can switch at the end: an association at both
     * ends, an aggregation or composition only at the target, the source has the diamond, an inheritance or
     * implementation at none, its triangle is its mark, and neither has a dependency, whose arrowhead is always there */
    public boolean canHaveArrowAt(RelationEnd_V001 end) {
        return switch (this) {
            case ASSOCIATION -> true;
            case AGGREGATION, COMPOSITION -> end == RelationEnd_V001.TO;
            case EXTENDS, IMPLEMENTS, DEPENDENCY -> false;
        };
    }

    /** @return true if the line of the relation is dashed */
    public boolean isDashed() {
        return this == IMPLEMENTS || this == DEPENDENCY;
    }

    /** Association, aggregation and composition have multiplicities and may have an association class. */
    public boolean isAssociationLike() {
        return this == ASSOCIATION || this == AGGREGATION || this == COMPOSITION;
    }
}
