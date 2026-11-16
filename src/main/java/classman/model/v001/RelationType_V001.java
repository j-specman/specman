package classman.model.v001;

public enum RelationType_V001 {
    ASSOCIATION,
    AGGREGATION,
    COMPOSITION,
    EXTENDS,
    IMPLEMENTS;

    /** Association, aggregation and composition have multiplicities and may have an association class. */
    public boolean isAssociationLike() {
        return this == ASSOCIATION || this == AGGREGATION || this == COMPOSITION;
    }
}
