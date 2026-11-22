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
}
