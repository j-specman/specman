package classman.editor;

import classman.model.v001.ClassStyleModel_V001;
import classman.render.Scene;
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

  private StylePreview preview(ClassStyleModel_V001 style, String changeSet) {
    host.changeColors.strongColors.put("yellow", STRONG);
    StylePreview preview = new StylePreview(host.renderer());
    preview.show(style, changeSet);
    return preview;
  }

  private static List<SceneRectangle> rectangles(Scene scene) {
    return scene.elements.stream().filter(SceneRectangle.class::isInstance).map(SceneRectangle.class::cast).toList();
  }

  /** Without a change set the class is drawn in its style and nothing is marked. */
  @Test
  void unmarkedClassInItsStyle() {
    ClassStyleModel_V001 style = new ClassStyleModel_V001();
    style.fill = "#336699";

    Scene scene = preview(style, null).scene();

    assertEquals(1, rectangles(scene).size());
    assertEquals("#336699", rectangles(scene).get(0).fill);
    assertTrue(scene.elements.stream().noneMatch(element -> element instanceof SceneText text
      && text.text.equals("Person") && text.fill != null));
  }

  /** With a change set the class has the frame of an added class and its three attributes are marked: a changed
   * type, an added attribute and a removed one. */
  @Test
  void markedClassHasFrameAndMarks() {
    Scene scene = preview(new ClassStyleModel_V001(), "yellow").scene();

    List<SceneRectangle> rectangles = rectangles(scene);
    assertTrue(rectangles.get(0).fill == null && STRONG.equals(rectangles.get(0).stroke), "frame comes first");
    String pale = host.renderer().paleColorOf("yellow");
    assertEquals(2, rectangles.stream().filter(rectangle -> pale.equals(rectangle.fill)).count());
    assertTrue(rectangles.stream().filter(rectangle -> STRONG.equals(rectangle.fill)).count() >= 3,
      "strong marks behind the changed type and the added and removed attribute");
  }
}
