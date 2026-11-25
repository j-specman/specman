package classman.model.v001;

import java.util.ArrayList;
import java.util.List;

/** The path of a relation: from an anchor over waypoints to an anchor. */
public class RouteModel_V001 {
    public final AnchorModel_V001 start;
    public final AnchorModel_V001 end;
    public final List<PositionModel_V001> waypoints;

    public RouteModel_V001(AnchorModel_V001 start, List<PositionModel_V001> waypoints, AnchorModel_V001 end) {
        this.start = start;
        this.waypoints = new ArrayList<>(waypoints);
        this.end = end;
    }
}
