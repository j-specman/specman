package classman.editor;

import classman.model.v001.AbstractBoxModel_V001;
import classman.model.v001.AnchorModel_V001;
import classman.model.v001.PositionModel_V001;
import classman.model.v001.RouteModel_V001;

import java.util.List;

/** What a line was like when a move started, and which of the moved boxes it joins, which eat its waypoints. The route is
 * kept for a relation, the ends and waypoints for an anchor line or a link of an association class. */
class LineSnapshot {
  final RouteModel_V001 route;
  final AnchorModel_V001 startAnchor;
  final AnchorModel_V001 endAnchor;
  final List<PositionModel_V001> waypoints;
  final List<AbstractBoxModel_V001> eatingBoxes;

  LineSnapshot(RouteModel_V001 route, AnchorModel_V001 startAnchor, AnchorModel_V001 endAnchor,
               List<PositionModel_V001> waypoints, List<AbstractBoxModel_V001> eatingBoxes) {
    this.route = route;
    this.startAnchor = startAnchor;
    this.endAnchor = endAnchor;
    this.waypoints = waypoints;
    this.eatingBoxes = eatingBoxes;
  }
}
