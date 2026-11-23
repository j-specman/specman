package classman.render;

import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import classman.model.v001.io.ClassDiagramSerializer_V001;
import org.junit.jupiter.api.Test;

import static classman.render.SceneTestHelper.all;
import static classman.render.SceneTestHelper.layout;
import static classman.render.SceneTestHelper.texts;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassFillAndHiddenAttributesTest {

  private static final String ATTRIBUTES = " {\n  -first : String\n  -second\n}";

  /** The class box has the color of its own, other classes keep the standard one. */
  @Test
  void ownFill() throws ClassDiagramParseException {
    Scene scene = layout("class A (aaaaaaaa)\nclass B (bbbbbbbb) style(fill=\"#cce5ff\")");
    assertEquals(DiagramStyle.CLASS_FILL, all(scene, SceneRectangle.class).get(0).fill);
    assertEquals("#cce5ff", all(scene, SceneRectangle.class).get(1).fill);
  }

  /** The box without its attribute compartment has the height of its head and no separator line, and its attributes
   * are not drawn. */
  @Test
  void hiddenAttributes() throws ClassDiagramParseException {
    Scene shown = layout("class A (aaaaaaaa)" + ATTRIBUTES);
    Scene hidden = layout("class A (aaaaaaaa) hideattributes" + ATTRIBUTES);

    assertTrue(all(hidden, SceneRectangle.class).get(0).height < all(shown, SceneRectangle.class).get(0).height);
    assertTrue(texts(shown).stream().anyMatch(text -> text.contains("first")));
    assertTrue(texts(hidden).stream().noneMatch(text -> text.contains("first")));
    assertTrue(all(hidden, SceneLine.class).isEmpty());
    assertTrue(hidden.textAreas.stream().noneMatch(text -> text.attribute != null));
  }

  /** Attributes with a mark of a change set are shown whatever the source says, so that a change doesn't go unnoticed. */
  @Test
  void markedAttributesStayVisible() throws ClassDiagramParseException {
    Scene marked = layout("class A (aaaaaaaa) hideattributes {\n  -first change(added, yellow)\n  -second\n}");

    assertTrue(texts(marked).stream().anyMatch(text -> text.contains("first")));
    assertTrue(texts(marked).stream().anyMatch(text -> text.contains("second")));
  }

  /** The source keeps both settings, the color in lower case. */
  @Test
  void roundTrip() throws ClassDiagramParseException {
    String source = "classdiagram {\n  class A (aaaaaaaa) at(10, 20) width=100 style(fill=\"#cce5ff\") hideattributes\n}";

    String written = new ClassDiagramSerializer_V001().serialize(
      new ClassDiagramParser_V001().parse(source.replace("#cce5ff", "#CCE5FF")));

    assertTrue(written.contains("class A (aaaaaaaa) at(10, 20) width=100 style(fill=\"#cce5ff\") hideattributes\n"), written);
  }

  /** A color which isn't written #rrggbb is rejected with the line. */
  @Test
  void wrongColorRejected() {
    ClassDiagramParseException e = assertThrows(ClassDiagramParseException.class,
      () -> new ClassDiagramParser_V001().parse("classdiagram {\n  class A style(fill=\"red\")\n}"));
    assertTrue(e.getMessage().startsWith("line 2"), e.getMessage());
  }
}
