package classman.render;

/** Where a point stands in relation to the path of a relation: along it and across it. */
public final class PathSpot {
    /** How far from the start of the path, along the path, its closest point to the point is. */
    public final double along;
    /** The distance of the point from the line: positive left of the line seen in the direction of the path, negative
     * right of it. */
    public final double across;

    PathSpot(double along, double across) {
        this.along = along;
        this.across = across;
    }
}
