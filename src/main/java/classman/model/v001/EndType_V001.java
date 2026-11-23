package classman.model.v001;

/** What there is at one end of an association, aggregation or composition: the arrowhead of a navigable association,
 * nothing, or the diamond of an aggregation or composition. */
public enum EndType_V001 {
    /** An association which can be navigated towards this end: it has an arrowhead there. */
    NAVIGABLE,
    /** An association without an arrowhead at this end. */
    ASSOCIATION,
    /** The filled diamond: this end is the whole of a composition. */
    COMPOSITION,
    /** The hollow diamond: this end is the whole of an aggregation. */
    AGGREGATION;

    /** @return true for the diamonds, of which a relation has one at most */
    public boolean isDiamond() {
        return this == COMPOSITION || this == AGGREGATION;
    }
}
