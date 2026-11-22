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
 * Association class links and note links. A and B stacked: 40 wide and 56 high, A at (0, 0), B at (0, 96), the line
 * between them runs from (20, 56) to (20, 96).
 */
class AttachedElementsTest {

  private static final String STACKED = "class A (aaaaaaaa)\nclass B (bbbbbbbb)\n";
  private static final String SIDE_BY_SIDE = "class A (aaaaaaaa) at(0, 0)\nclass B (bbbbbbbb) at(300, 0)\n";

  private static ScenePolyline lastLine(Scene scene) {
    List<ScenePolyline> lines = all(scene, ScenePolyline.class);
    return lines.get(lines.size() - 1);
  }

  // ---- association class links ----

  private static final String LINKED = STACKED + "association (11110000) aaaaaaaa -- bbbbbbbb" + "\n"
      + "class L (cc000001) at(200, 200)\n";

  /** The class which is linked to a relation is drawn as any class, a dashed line leads to the middle of the relation. */
  @Test
  void assocClassLinkDashed() throws ClassDiagramParseException {
    Scene scene = layout(LINKED + "assocclass cc000001 -- 11110000");
    ScenePolyline link = lastLine(scene);
    assertTrue(link.dashed);
    assertEquals(20, link.xs[1], 1e-9);
    assertEquals(76, link.ys[1], 1e-9);
    assertTrue(link.xs[0] >= 200 && link.ys[0] >= 200);
  }

  /** The end on the relation can be fixed at a point of its line, in percent of its length. */
  @Test
  void assocClassLinkAtPercent() throws ClassDiagramParseException {
    ScenePolyline link = lastLine(layout(LINKED + "assocclass cc000001 -- 11110000 route 11110000@25%"));
    assertEquals(20, link.xs[1], 1e-9);
    assertEquals(66, link.ys[1], 1e-9);
  }

  /** A link to a relation which the diagram doesn't have can't be drawn. */
  @Test
  void assocClassLinkToUnknownRelationRejected() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> layout(LINKED + "assocclass cc000001 -- deadbeef"));
    assertTrue(e.getMessage().contains("deadbeef"), e.getMessage());
  }

  // ---- note links ----

  /** A note attached to a class gets a dashed line from its border to the border of the class. */
  @Test
  void noteLinkDashed() throws ClassDiagramParseException {
    Scene scene = layout("class A (aa000001) at(0, 0)\nnote (bb000001) at(0, 100) \"n\"\nanchor bb000001 -- aa000001");
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
        "class A (aaaaaaaa) at(0, 0) {\n  first\n  second (0a0a0a0a)\n}\nnote (bb000001) at(200, 0) \"n\"\n"
        + "anchor bb000001 -- 0a0a0a0a"));
    assertEquals(68, link.xs[1], 1e-9);
    assertEquals(62, link.ys[1], 1e-9);
  }

  /** A note left of the attribute is linked to the left edge of its box, 2 pixels before the text. */
  @Test
  void noteLinkToAttributeFromLeft() throws ClassDiagramParseException {
    ScenePolyline link = lastLine(layout(
        "class A (aaaaaaaa) at(0, 0) {\n  first\n  second (0a0a0a0a)\n}\nnote (bb000001) at(-200, 0) \"n\"\n"
        + "anchor bb000001 -- 0a0a0a0a"));
    assertEquals(4, link.xs[1], 1e-9);
    assertEquals(62, link.ys[1], 1e-9);
  }

  /** A note attached to a relation is linked to the middle of the relation. */
  @Test
  void noteLinkToRelation() throws ClassDiagramParseException {
    ScenePolyline link = lastLine(layout(STACKED + "association (11110000) aaaaaaaa -- bbbbbbbb\n"
        + "note (bb000001) at(100, 60) \"n\"\nanchor bb000001 -- 11110000"));
    assertEquals(20, link.xs[1], 1e-9);
    assertEquals(76, link.ys[1], 1e-9);
  }

  /** An anchor on an attribute fixes the end at a point of the border of the box around its text, like on a class. */
  @Test
  void noteLinkToAttributeAnchored() throws ClassDiagramParseException {
    String diagram = "class A (aaaaaaaa) at(0, 0) {\n  first\n  second (0a0a0a0a)\n}\n"
        + "note (bb000001) at(200, 0) \"n\"\nanchor bb000001 -- 0a0a0a0a route 0a0a0a0a.right@%d";
    ScenePolyline top = lastLine(layout(String.format(diagram, 0)));
    ScenePolyline lower = lastLine(layout(String.format(diagram, 10)));
    assertEquals(68, top.xs[1], 1e-9);
    assertEquals(68, lower.xs[1], 1e-9);
    assertEquals(10, lower.ys[1] - top.ys[1], 1e-9);
  }

  /** The end of the line on a relation is a point of the relation line, in percent of its length. */
  @Test
  void noteLinkToRelationAtPercent() throws ClassDiagramParseException {
    ScenePolyline link = lastLine(layout(STACKED + "association (11110000) aaaaaaaa -- bbbbbbbb\n"
        + "note (bb000001) at(100, 60) \"n\"\nanchor bb000001 -- 11110000 route (150, 0) 11110000@25%"));
    assertEquals(20, link.xs[2], 1e-9);
    assertEquals(66, link.ys[2], 1e-9);
  }

  /** A point on a line which doesn't fit the target is ignored: the end is computed as without it. */
  @Test
  void percentOnBoxIsIgnored() throws ClassDiagramParseException {
    ScenePolyline link = lastLine(layout("class A (aa000001) at(0, 0)\nnote (bb000001) at(0, 100) \"n\"\n"
        + "anchor bb000001 -- aa000001 route aa000001@50%"));
    assertEquals(20, link.xs[1], 1e-9);
    assertEquals(56, link.ys[1], 1e-9);
  }

  /** An anchor fixes an end of the line at a point of the border: the start on the note, the end on the class. */
  @Test
  void noteLinkWithAnchors() throws ClassDiagramParseException {
    ScenePolyline link = lastLine(layout("class A (aa000001) at(0, 0)\nnote (bb000001) at(0, 100) \"n\"\n"
        + "anchor bb000001 -- aa000001 route bb000001.top@10 aa000001.bottom@15"));
    assertEquals(2, link.xs.length);
    assertEquals(10, link.xs[0], 1e-9);
    assertEquals(100, link.ys[0], 1e-9);
    assertEquals(15, link.xs[1], 1e-9);
    assertEquals(56, link.ys[1], 1e-9);
  }

  /** With waypoints the line leads over them, starting at the note facing the first and ending at the class facing the last. */
  @Test
  void noteLinkOverWaypoints() throws ClassDiagramParseException {
    ScenePolyline link = lastLine(layout("class A (aa000001) at(0, 0)\nnote (bb000001) at(0, -100) \"n\"\n"
        + "anchor bb000001 -- aa000001 route (60, -40) (60, 28)"));
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
    ScenePolyline link = lastLine(layout("class A (aaaaaaaa) at(0, 0) {\n  first\n  second (0a0a0a0a)\n}\n"
        + "note (bb000001) at(200, 0) \"n\"\nanchor bb000001 -- 0a0a0a0a route (150, -20) (-30, 62)"));
    assertEquals(4, link.xs[link.xs.length - 1], 1e-9);
    assertEquals(62, link.ys[link.ys.length - 1], 1e-9);
  }

  /** The scene covers waypoints which lie outside of the boxes. */
  @Test
  void sceneCoversWaypoints() throws ClassDiagramParseException {
    Scene scene = layout("class A (aa000001) at(0, 0)\nnote (bb000001) at(100, 0) \"n\"\n"
        + "anchor bb000001 -- aa000001 route (500, 400) (100, 60)");
    assertTrue(scene.x + scene.width >= 500);
    assertTrue(scene.y + scene.height >= 400);
  }

  /** A note which isn't attached has no line. */
  @Test
  void freeNoteWithoutLink() throws ClassDiagramParseException {
    assertTrue(all(layout("class A (aaaaaaaa)\nnote \"n\""), ScenePolyline.class).isEmpty());
  }

  /** An anchor line to an ID which no element of the diagram has can't be drawn. */
  @Test
  void noteLinkToUnknownIdRejected() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> layout("class A (aa000001)\nnote (bb000001) \"n\"\nanchor bb000001 -- deadbeef"));
    assertTrue(e.getMessage().contains("deadbeef"), e.getMessage());
  }

  /** An anchor line from a note which the diagram doesn't have can't be drawn either. */
  @Test
  void noteLinkFromUnknownNoteRejected() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> layout("class A (aa000001)\nanchor deadbeef -- aa000001"));
    assertTrue(e.getMessage().contains("deadbeef"), e.getMessage());
  }

  /** An ID which two elements have can't be attached to. */
  @Test
  void duplicateIdRejected() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> layout("class A (aa000001)\nclass B (aa000001)\nnote (bb000001) \"n\"\nanchor bb000001 -- aa000001"));
    assertTrue(e.getMessage().contains("aa000001"), e.getMessage());
  }
}
