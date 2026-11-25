package classman.render;

import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;

import java.util.ArrayList;
import java.util.List;

/** Parses diagram text, lays it out with fixed text widths, and picks parts out of the scene. */
final class SceneTestHelper {
  private static final ClassDiagramLayouter LAYOUTER = new ClassDiagramLayouter(new FixedTextMeasurer());

  private SceneTestHelper() {
  }

  /** Lays out the given elements as the content of a diagram. */
  static Scene layout(String elements) throws ClassDiagramParseException {
    return LAYOUTER.layout(new ClassDiagramParser_V001().parse("classdiagram {\n" + elements + "\n}"));
  }

  static <T extends AbstractSceneElement> List<T> all(Scene scene, Class<T> type) {
    List<T> result = new ArrayList<>();
    for (AbstractSceneElement element : scene.elements) {
      if (type.isInstance(element)) {
        result.add(type.cast(element));
      }
    }
    return result;
  }

  static List<String> texts(Scene scene) {
    List<String> result = new ArrayList<>();
    for (SceneText text : all(scene, SceneText.class)) {
      result.add(text.text);
    }
    return result;
  }

  static SceneText textNamed(Scene scene, String text) {
    for (SceneText candidate : all(scene, SceneText.class)) {
      if (candidate.text.equals(text)) {
        return candidate;
      }
    }
    throw new AssertionError("No text " + text + " in " + texts(scene));
  }
}
