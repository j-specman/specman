package classman.model.v001;

/** Where a route starts or ends: on a side of a class, with a pixel offset from the start of that side. */
public class AnchorModel_V001 {
    public final String className;
    public final Side_V001 side;
    /** From the left end of the top and bottom side, from the upper end of the left and right side. */
    public final int offset;

    public AnchorModel_V001(String className, Side_V001 side, int offset) {
        this.className = className;
        this.side = side;
        this.offset = offset;
    }
}
