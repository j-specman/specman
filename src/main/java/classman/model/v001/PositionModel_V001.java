package classman.model.v001;

/** A point of the diagram in pixels. It is the top left corner of a class or note, or a waypoint of a route. */
public class PositionModel_V001 {
    public final int x;
    public final int y;

    public PositionModel_V001(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
