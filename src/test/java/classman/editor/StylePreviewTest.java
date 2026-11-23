package classman.editor;

import classman.model.v001.ElementStyleModel_V001;
import classman.render.DiagramRenderer;
import classman.render.DiagramStyle;
import classman.render.Scene;
import classman.render.ScenePolygon;
import classman.render.ScenePolyline;
import classman.render.SceneRectangle;
import classman.render.SceneText;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StylePreviewTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final String STRONG = "#ffee00";

  private final RecordingEditorHost host = new RecordingEditorHost();

  private static ElementStyleModel_V001 style(String fill, String lineColor) {
    ElementStyleModel_V001 style = new ElementStyleModel_V001();
    style.fill = fill;
    style.lineColor = lineColor;
    return style;
  }

  private DiagramRenderer renderer() {
    host.changeColors.strongColors.put("yellow", STRONG);
    return host.renderer();
  }

  private static <T> List<T> all(Scene scene, Class<T> type) {
    return scene.elements.stream().filter(type::isInstance).map(type::cast).toList();
  }

  private static long marks(Scene scene) {
    return all(scene, SceneRectangle.class).stream().filter(rectangle -> STRONG.equals(rectangle.fill)).count();
  }

  /** Without a change set the class is drawn in its style and nothing is marked. */
  @Test
  void unmarkedClassInItsStyle() {
    Scene scene = ClassPreview.scene(renderer(), style("#336699", null), null);

    assertEquals(1, all(scene, SceneRectangle.class).size());
    assertEquals("#336699", all(scene, SceneRectangle.class).get(0).fill);
    assertTrue(all(scene, SceneText.class).stream().anyMatch(text -> text.text.equals("Person")));
  }

  /** With a change set the class has the frame of an added class and its three attributes are marked: a changed
   * type, an added attribute and a removed one. */
  @Test
  void markedClassHasFrameAndMarks() {
    DiagramRenderer renderer = renderer();

    Scene scene = ClassPreview.scene(renderer, new ElementStyleModel_V001(), "yellow");

    List<SceneRectangle> rectangles = all(scene, SceneRectangle.class);
    assertTrue(rectangles.get(0).fill == null && STRONG.equals(rectangles.get(0).stroke), "frame comes first");
    String pale = renderer.paleColorOf("yellow");
    assertEquals(2, rectangles.stream().filter(rectangle -> pale.equals(rectangle.fill)).count());
    assertTrue(marks(scene) >= 3, "strong marks behind the changed type and the added and removed attribute");
  }

  /** The note is drawn in its style, and with a change set it has the frame of an added note and a marked text. */
  @Test
  void notePreviewInItsStyleAndMarked() {
    Scene unmarked = NotePreview.scene(renderer(), style("#336699", "#cc0000"), null);
    Scene marked = NotePreview.scene(renderer(), new ElementStyleModel_V001(), "yellow");

    ScenePolygon outline = all(unmarked, ScenePolygon.class).get(0);
    assertEquals("#336699", outline.fill);
    assertEquals("#cc0000", outline.stroke);
    assertEquals(0, marks(unmarked));
    ScenePolygon frame = all(marked, ScenePolygon.class).get(0);
    assertTrue(frame.fill == null && STRONG.equals(frame.stroke), "frame comes first");
    assertTrue(marks(marked) >= 1, "a strong mark behind the changed text");
  }

  /** The relation is drawn in its style, and with a change set it has the thick line of an added relation and a marked
   * label. */
  @Test
  void relationPreviewInItsStyleAndMarked() {
    Scene unmarked = RelationPreview.scene(renderer(), style(null, "#cc0000"), null);
    Scene marked = RelationPreview.scene(renderer(), new ElementStyleModel_V001(), "yellow");

    assertTrue(all(unmarked, ScenePolyline.class).stream().anyMatch(line -> "#cc0000".equals(line.stroke)));
    assertEquals(0, marks(unmarked));
    assertTrue(all(marked, ScenePolyline.class).stream().anyMatch(line -> STRONG.equals(line.stroke)
      && line.strokeWidth == DiagramStyle.RELATION_HIGHLIGHT_WIDTH), "the thick line below the relation");
    assertTrue(marks(marked) >= 1, "a strong mark behind the label");
    assertTrue(all(marked, SceneText.class).stream().anyMatch(text -> text.text.equals("label")));
  }
}
