package classman.render;

import classman.model.v001.io.ClassDiagramParseException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static classman.render.SceneTestHelper.all;
import static classman.render.SceneTestHelper.layout;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Association classes and note links. A and B stacked: 40 wide and 56 high, A at (0, 0), B at (0, 96), the line
 * between them runs from (20, 56) to (20, 96). An association class named link is 52 wide and 56 high.
 */
class AttachedElementsTest {

  private static final String STACKED = "class A\nclass B\n";
  private static final String SIDE_BY_SIDE = "class A at(0, 0)\nclass B at(300, 0)\n";

  private static SceneRectangle thirdBox(Scene scene) {
    return all(scene, SceneRectangle.class).get(2);
  }

  private static ScenePolyline lastLine(Scene scene) {
    List<ScenePolyline> lines = all(scene, ScenePolyline.class);
    return lines.get(lines.size() - 1);
  }

  // ---- association classes ----

  /** Beside a vertical line the association class stands left of it, centered on the middle of the line. */
  @Test
  void assocClassLeftOfVerticalLine() throws ClassDiagramParseException {
    Scene scene = layout(STACKED + "association A -- B\n  assocclass link");
    SceneRectangle box = thirdBox(scene);
    assertEquals(-62, box.x, 1e-9);
    assertEquals(48, box.y, 1e-9);
    assertEquals(52, box.width, 1e-9);
    assertEquals(56, box.height, 1e-9);
  }

  /** Beside a horizontal line it stands below the line, as the label is above. */
  @Test
  void assocClassBelowHorizontalLine() throws ClassDiagramParseException {
    Scene scene = layout(SIDE_BY_SIDE + "association A -- B\n  assocclass link");
    SceneRectangle box = thirdBox(scene);
    assertEquals(144, box.x, 1e-9);
    assertEquals(58, box.y, 1e-9);
  }

  /** A dashed line connects the association class to the middle of the relation. */
  @Test
  void assocClassConnectorDashed() throws ClassDiagramParseException {
    ScenePolyline connector = lastLine(layout(STACKED + "association A -- B\n  assocclass link"));
    assertTrue(connector.dashed);
    assertEquals(20, connector.xs[0], 1e-9);
    assertEquals(76, connector.ys[0], 1e-9);
    assertEquals(-10, connector.xs[1], 1e-9);
    assertEquals(76, connector.ys[1], 1e-9);
  }

  /** With at() the association class stands there, and the connector ends where it meets its border. */
  @Test
  void assocClassExplicitPosition() throws ClassDiagramParseException {
    Scene scene = layout(SIDE_BY_SIDE + "association A -- B\n  assocclass link at(200, 200)");
    SceneRectangle box = thirdBox(scene);
    assertEquals(200, box.x, 1e-9);
    assertEquals(200, box.y, 1e-9);
    ScenePolyline connector = lastLine(scene);
    assertEquals(170, connector.xs[0], 1e-9);
    assertEquals(28, connector.ys[0], 1e-9);
    assertEquals(200, connector.ys[1], 1e-9);
  }

  /** The scene covers an association class which stands outside of the other boxes. */
  @Test
  void sceneCoversAssocClass() throws ClassDiagramParseException {
    Scene scene = layout(SIDE_BY_SIDE + "association A -- B\n  assocclass link at(500, 400)");
    assertTrue(scene.x + scene.width >= 552);
    assertTrue(scene.y + scene.height >= 456);
  }

  /** An association class is grey, to tell it from the classes of the diagram. */
  @Test
  void assocClassGrey() throws ClassDiagramParseException {
    Scene scene = layout(STACKED + "association A -- B\n  assocclass link");
    assertEquals(DiagramStyle.ASSOC_CLASS_FILL, thirdBox(scene).fill);
  }

  /** Attributes of the association class are shown like those of any class. */
  @Test
  void assocClassWithAttributes() throws ClassDiagramParseException {
    Scene scene = layout(STACKED + "association A -- B\n  assocclass link {\n    since\n    until\n  }");
    assertTrue(SceneTestHelper.texts(scene).contains("since"));
    assertTrue(thirdBox(scene).height > 56);
  }

  // ---- note links ----

  /** A note attached to a class gets a dashed line from its border to the border of the class. */
  @Test
  void noteLinkDashed() throws ClassDiagramParseException {
    Scene scene = layout("class A (aa000001) at(0, 0)\nnote at(0, 100) `n` attach(aa000001)");
    ScenePolyline link = all(scene, ScenePolyline.class).get(0);
    assertTrue(link.dashed);
    assertEquals(20, link.xs[0], 1e-9);
    assertEquals(100, link.ys[0], 1e-9);
    assertEquals(20, link.xs[1], 1e-9);
    assertEquals(56, link.ys[1], 1e-9);
  }

  /** A note attached to an attribute ends at the box around the attribute text on the side of the note, at half its height. */
  @Test
  void noteLinkToAttributeFromRight() throws ClassDiagramParseException {
    ScenePolyline link = lastLine(layout(
        "class A at(0, 0) {\n  first\n  second (0a0a0a0a)\n}\nnote at(200, 0) `n` attach(0a0a0a0a)"));
    assertEquals(68, link.xs[1], 1e-9);
    assertEquals(62, link.ys[1], 1e-9);
  }

  /** A note left of the attribute is linked to the left edge of its box, 2 pixels before the text. */
  @Test
  void noteLinkToAttributeFromLeft() throws ClassDiagramParseException {
    ScenePolyline link = lastLine(layout(
        "class A at(0, 0) {\n  first\n  second (0a0a0a0a)\n}\nnote at(-200, 0) `n` attach(0a0a0a0a)"));
    assertEquals(4, link.xs[1], 1e-9);
    assertEquals(62, link.ys[1], 1e-9);
  }

  /** A note attached to a relation is linked to the middle of the relation. */
  @Test
  void noteLinkToRelation() throws ClassDiagramParseException {
    ScenePolyline link = lastLine(layout(STACKED + "association (11110000) A -- B\nnote at(100, 60) `n` attach(11110000)"));
    assertEquals(20, link.xs[1], 1e-9);
    assertEquals(76, link.ys[1], 1e-9);
  }

  /** With waypoints the line leads over them, starting at the note facing the first and ending at the class facing the last. */
  @Test
  void noteLinkOverWaypoints() throws ClassDiagramParseException {
    ScenePolyline link = lastLine(layout(
        "class A (aa000001) at(0, 0)\nnote at(0, -100) `n` attach(aa000001) route (60, -40) (60, 28)"));
    assertEquals(4, link.xs.length);
    assertEquals(32.17, link.xs[0], 0.01);
    assertEquals(-72, link.ys[0], 1e-9);
    assertEquals(60, link.xs[1], 1e-9);
    assertEquals(-40, link.ys[1], 1e-9);
    assertEquals(60, link.xs[2], 1e-9);
    assertEquals(28, link.ys[2], 1e-9);
    assertEquals(40, link.xs[3], 1e-9);
    assertEquals(28, link.ys[3], 1e-9);
  }

  /** The line reaches an attribute from the side of the last waypoint, even if the note is on the other side. */
  @Test
  void attributeSideFromLastWaypoint() throws ClassDiagramParseException {
    ScenePolyline link = lastLine(layout("class A at(0, 0) {\n  first\n  second (0a0a0a0a)\n}\n"
        + "note at(200, 0) `n` attach(0a0a0a0a) route (150, -20) (-30, 62)"));
    assertEquals(4, link.xs[link.xs.length - 1], 1e-9);
    assertEquals(62, link.ys[link.ys.length - 1], 1e-9);
  }

  /** The scene covers waypoints which lie outside of the boxes. */
  @Test
  void sceneCoversWaypoints() throws ClassDiagramParseException {
    Scene scene = layout("class A (aa000001) at(0, 0)\nnote at(100, 0) `n` attach(aa000001) route (500, 400) (100, 60)");
    assertTrue(scene.x + scene.width >= 500);
    assertTrue(scene.y + scene.height >= 400);
  }

  /** A note which isn't attached has no line. */
  @Test
  void freeNoteWithoutLink() throws ClassDiagramParseException {
    assertTrue(all(layout("class A\nnote `n`"), ScenePolyline.class).isEmpty());
  }

  /** A note attached to an ID which no element of the diagram has can't be drawn. */
  @Test
  void noteLinkToUnknownIdRejected() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> layout("class A (aa000001)\nnote `n` attach(deadbeef)"));
    assertTrue(e.getMessage().contains("deadbeef"), e.getMessage());
  }

  /** An ID which two elements have can't be attached to. */
  @Test
  void duplicateIdRejected() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> layout("class A (aa000001)\nclass B (aa000001)\nnote `n` attach(aa000001)"));
    assertTrue(e.getMessage().contains("aa000001"), e.getMessage());
  }
}
