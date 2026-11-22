package classman.model.v001;

/**
 * Where a route, or the dashed line of a note, starts or ends. Either on a side of the box with this ID, with a pixel
 * offset from the start of that side, or, for the end of the dashed line to a relation, on the line of that relation at
 * a percentage of its length.
 */
public class AnchorModel_V001 {
    /** The ID of the class, attribute, note or relation the anchor is on. */
    public final String classId;
    /** Null for a point on the line of a relation. */
    public final Side_V001 side;
    /** From the left end of the top and bottom side, from the upper end of the left and right side. For a point on a
     * line the percent of the length of the line, counted from its start. */
    public final int offset;
    /** True for the end of a route which is linked to the center of the class: it leaves the class where a line from
     * the center to the next point crosses its border. Then there is no side and no offset. */
    public final boolean center;

    public AnchorModel_V001(String classId, Side_V001 side, int offset) {
        this(classId, side, offset, false);
    }

    private AnchorModel_V001(String classId, Side_V001 side, int offset, boolean center) {
        this.classId = classId;
        this.side = side;
        this.offset = offset;
        this.center = center;
    }

    /** @return the end of a route which is linked to the center of the class */
    public static AnchorModel_V001 center(String classId) {
        return new AnchorModel_V001(classId, null, 0, true);
    }

    /** @param percent how many percent of the length of the line of the relation lie before the point */
    public static AnchorModel_V001 onLine(String relationId, int percent) {
        return new AnchorModel_V001(relationId, null, percent);
    }

    /** @return true if the anchor is a point on the line of a relation, false if it is a point on the side of a box */
    public boolean isOnLine() {
        return side == null && !center;
    }
}
