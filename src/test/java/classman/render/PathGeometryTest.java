package classman.render;

import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PathGeometryTest {

  private static final List<Point2D.Double> BENT = List.of(
    new Point2D.Double(0, 0), new Point2D.Double(100, 0), new Point2D.Double(100, 100));

  /** The percentage counts along the bends: 50 percent of 200 is the corner, 75 percent half of the second part. */
  @Test
  void pointAlongBentPath() {
    assertEquals(new Point2D.Double(50, 0), PathGeometry.pointAlong(BENT, 25));
    assertEquals(new Point2D.Double(100, 0), PathGeometry.pointAlong(BENT, 50));
    assertEquals(new Point2D.Double(100, 50), PathGeometry.pointAlong(BENT, 75));
  }

  /** Percentages beyond the path stay at its ends. */
  @Test
  void pointAlongIsLimited() {
    assertEquals(new Point2D.Double(0, 0), PathGeometry.pointAlong(BENT, -20));
    assertEquals(new Point2D.Double(100, 100), PathGeometry.pointAlong(BENT, 140));
  }

  /** The percentage is that of the closest point of the path. */
  @Test
  void percentNearPoint() {
    assertEquals(25, PathGeometry.percentNear(BENT, new Point2D.Double(50, 30)), 1e-9);
    assertEquals(75, PathGeometry.percentNear(BENT, new Point2D.Double(130, 50)), 1e-9);
    assertEquals(100, PathGeometry.percentNear(BENT, new Point2D.Double(100, 400)), 1e-9);
    assertEquals(0, PathGeometry.percentNear(BENT, new Point2D.Double(-50, -50)), 1e-9);
  }

  /** Left of the line is left seen in the direction of the path, also in a part which runs down. */
  @Test
  void pointBesideLine() {
    assertEquals(new Point2D.Double(50, -10), PathGeometry.pointBeside(BENT, 50, 10));
    assertEquals(new Point2D.Double(110, 50), PathGeometry.pointBeside(BENT, 150, 10));
    assertEquals(new Point2D.Double(90, 50), PathGeometry.pointBeside(BENT, 150, -10));
  }

  /** A distance beyond the path stays at its end. */
  @Test
  void pointAtDistanceIsLimited() {
    assertEquals(new Point2D.Double(100, 100), PathGeometry.pointAtDistance(BENT, 500));
    assertEquals(new Point2D.Double(0, 0), PathGeometry.pointAtDistance(BENT, -5));
  }

  /** The spot of a point is the distance along the path and the signed distance from the line. */
  @Test
  void spotNearPoint() {
    PathSpot above = PathGeometry.spotNear(BENT, new Point2D.Double(50, -10));
    assertEquals(50, above.along, 1e-9);
    assertEquals(10, above.across, 1e-9);
    PathSpot right = PathGeometry.spotNear(BENT, new Point2D.Double(130, 50));
    assertEquals(150, right.along, 1e-9);
    assertEquals(30, right.across, 1e-9);
    assertEquals(-30, PathGeometry.spotNear(BENT, new Point2D.Double(70, 50)).across, 1e-9);
  }
}
