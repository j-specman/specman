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
    Scene scene = layout("class A at(0, 0)\nnote at(0, 100) `n` attached A");
    ScenePolyline link = all(scene, ScenePolyline.class).get(0);
    assertTrue(link.dashed);
    assertEquals(20, link.xs[0], 1e-9);
    assertEquals(100, link.ys[0], 1e-9);
    assertEquals(20, link.xs[1], 1e-9);
    assertEquals(56, link.ys[1], 1e-9);
  }

  /** A note which isn't attached has no line. */
  @Test
  void freeNoteWithoutLink() throws ClassDiagramParseException {
    assertTrue(all(layout("class A\nnote `n`"), ScenePolyline.class).isEmpty());
  }

  /** A note attached to a class which isn't in the diagram can't be drawn. */
  @Test
  void noteLinkToUnknownClassRejected() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> layout("class A\nnote `n` attached Missing"));
    assertTrue(e.getMessage().contains("Missing"), e.getMessage());
    assertFalse(e.getMessage().isEmpty());
  }
}
