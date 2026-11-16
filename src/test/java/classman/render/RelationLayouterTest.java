package classman.render;

import classman.model.v001.io.ClassDiagramParseException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static classman.render.SceneTestHelper.all;
import static classman.render.SceneTestHelper.layout;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two classes A and B without position are 40 wide and 56 high and stacked: A at (0, 0), B at (0, 96), both centered
 * on x = 20. Placed at (0, 0) and (200, 100) they are diagonal, with centers (20, 28) and (220, 128).
 */
class RelationLayouterTest {

  private static final String STACKED = "class A\nclass B\n";
  private static final String DIAGONAL = "class A at(0, 0)\nclass B at(200, 100)\n";

  private static ScenePolyline relationLine(Scene scene) {
    return all(scene, ScenePolyline.class).get(0);
  }

  private static void assertPoint(ScenePolyline line, int index, double x, double y) {
    assertEquals(x, line.xs[index], 1e-9, "x of point " + index);
    assertEquals(y, line.ys[index], 1e-9, "y of point " + index);
  }

  // ---- paths ----

  /** Between stacked classes the line is vertical, from the bottom border of A to the top border of B. */
  @Test
  void straightLineBetweenStackedClasses() throws ClassDiagramParseException {
    ScenePolyline line = relationLine(layout(STACKED + "association A -- B"));
    assertEquals(2, line.xs.length);
    assertPoint(line, 0, 20, 56);
    assertPoint(line, 1, 20, 96);
  }

  /** A line from center to center is cut where it leaves the box of A and where it enters the box of B. */
  @Test
  void diagonalLineClippedAtBorders() throws ClassDiagramParseException {
    ScenePolyline line = relationLine(layout(DIAGONAL + "association A -- B"));
    assertPoint(line, 0, 40, 38);
    assertPoint(line, 1, 200, 118);
  }

  /** A route starts at its anchor on a side of A, goes over the waypoints and ends at its anchor on B. */
  @Test
  void routeWithAnchorsAndWaypoints() throws ClassDiagramParseException {
    ScenePolyline line = relationLine(layout(DIAGONAL + "association A -- B\n  route A.bottom@10 (10, 150) B.left@20"));
    assertEquals(3, line.xs.length);
    assertPoint(line, 0, 10, 56);
    assertPoint(line, 1, 10, 150);
    assertPoint(line, 2, 200, 120);
  }

  /** An offset beyond the length of the side puts the anchor at the end of the side. */
  @Test
  void anchorOffsetClamped() throws ClassDiagramParseException {
    ScenePolyline line = relationLine(layout(DIAGONAL + "association A -- B\n  route A.bottom@999 B.top@-5"));
    assertPoint(line, 0, 40, 56);
    assertPoint(line, 1, 200, 100);
  }

  /** A relation from a class to itself without route goes as a loop over the top right corner. */
  @Test
  void loopForRelationToSameClass() throws ClassDiagramParseException {
    ScenePolyline line = relationLine(layout("class A at(0, 0)\nassociation A -- A"));
    assertEquals(5, line.xs.length);
    assertPoint(line, 0, 30, 0);
    assertPoint(line, 1, 30, -20);
    assertPoint(line, 2, 60, -20);
    assertPoint(line, 3, 60, 14);
    assertPoint(line, 4, 40, 14);
  }

  /** A relation to a class which isn't in the diagram can't be drawn. */
  @Test
  void unknownClassRejected() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> layout("class A\nassociation A -- Missing"));
    assertTrue(e.getMessage().contains("Missing"), e.getMessage());
  }

  /** The scene also covers waypoints outside of all boxes. */
  @Test
  void sceneBoundsIncludeWaypoints() throws ClassDiagramParseException {
    Scene scene = layout(DIAGONAL + "association A -- B\n  route A.bottom@10 (500, 600) B.left@20");
    assertTrue(scene.x + scene.width >= 500);
    assertTrue(scene.y + scene.height >= 600);
  }

  // ---- end markings ----

  /** A composition has a filled diamond at the source. Its tip is at the border, it is 14 long and 8 wide. */
  @Test
  void compositionFilledDiamond() throws ClassDiagramParseException {
    ScenePolygon diamond = all(layout(STACKED + "composition A -- B"), ScenePolygon.class).get(0);
    assertEquals(DiagramStyle.STROKE, diamond.fill);
    assertEquals(4, diamond.xs.length);
    assertEquals(56, diamond.ys[0], 1e-9);
    assertEquals(70, diamond.ys[2], 1e-9);
    assertEquals(16, Math.min(diamond.xs[1], diamond.xs[3]), 1e-9);
    assertEquals(24, Math.max(diamond.xs[1], diamond.xs[3]), 1e-9);
  }

  /** An aggregation has a hollow diamond, white inside so that the line doesn't show through. */
  @Test
  void aggregationHollowDiamond() throws ClassDiagramParseException {
    ScenePolygon diamond = all(layout(STACKED + "aggregation A -- B"), ScenePolygon.class).get(0);
    assertEquals(DiagramStyle.WHITE, diamond.fill);
    assertEquals(DiagramStyle.STROKE, diamond.stroke);
  }

  /** A directed association has an open arrowhead with its tip at the border of the target. */
  @Test
  void directedAssociationArrowhead() throws ClassDiagramParseException {
    Scene scene = layout(STACKED + "association A -> B");
    List<ScenePolyline> lines = all(scene, ScenePolyline.class);
    assertEquals(2, lines.size());
    ScenePolyline arrow = lines.get(1);
    assertEquals(3, arrow.xs.length);
    assertPoint(arrow, 0, 15, 84);
    assertPoint(arrow, 1, 20, 96);
    assertPoint(arrow, 2, 25, 84);
  }

  /** Two dashes instead of an arrow mean no arrowhead at all. */
  @Test
  void undirectedAssociationWithoutArrowhead() throws ClassDiagramParseException {
    Scene scene = layout(STACKED + "association A -- B");
    assertEquals(1, all(scene, ScenePolyline.class).size());
    assertTrue(all(scene, ScenePolygon.class).stream().noneMatch(polygon -> polygon.xs.length == 3));
  }

  /** Extends has a white triangle at the superclass, and a solid line. */
  @Test
  void extendsTriangle() throws ClassDiagramParseException {
    Scene scene = layout(STACKED + "extends A -> B");
    ScenePolygon triangle = all(scene, ScenePolygon.class).get(0);
    assertEquals(3, triangle.xs.length);
    assertEquals(DiagramStyle.WHITE, triangle.fill);
    assertEquals(96, triangle.ys[0], 1e-9);
    assertEquals(82, triangle.ys[1], 1e-9);
    assertFalse(relationLine(scene).dashed);
  }

  /** Implements looks like extends, but the line is dashed. */
  @Test
  void implementsDashedLine() throws ClassDiagramParseException {
    Scene scene = layout(STACKED + "implements A -> B");
    assertTrue(relationLine(scene).dashed);
    assertEquals(3, all(scene, ScenePolygon.class).get(0).xs.length);
  }
}
