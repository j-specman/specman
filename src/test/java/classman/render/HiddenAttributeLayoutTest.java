package classman.render;

import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import classman.model.v001.io.ClassDiagramSerializer_V001;
import org.junit.jupiter.api.Test;

import java.util.List;

import static classman.render.SceneTestHelper.all;
import static classman.render.SceneTestHelper.texts;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Attributes which the diagram doesn't show, and the line "..." which stands for them. */
class HiddenAttributeLayoutTest {
  private static final String YELLOW = "#ffff00";
  private static final String BLUE = "#0000ff";

  private final ClassDiagramLayouter layouter = new ClassDiagramLayouter(new FixedTextMeasurer(),
    changeSet -> "yellow".equals(changeSet) ? YELLOW : BLUE);

  private Scene layout(String elements) throws ClassDiagramParseException {
    return layouter.layout(new ClassDiagramParser_V001().parse("classdiagram {\n" + elements + "\n}"));
  }

  private static List<SceneRectangle> highlights(Scene scene, String color) {
    return all(scene, SceneRectangle.class).stream().filter(rectangle -> color.equals(rectangle.fill)).toList();
  }

  /** A hidden attribute is not drawn and is no line to double click, the line "..." is drawn under the others. */
  @Test
  void hiddenAttributeReplacedByEllipsis() throws ClassDiagramParseException {
    Scene scene = layout("class A (aaaaaaaa) {\n  -first\n  -second (hidden)\n  -third\n}");

    assertEquals(List.of("A", "-first", "-third", ". . ."), texts(scene));
    assertEquals(2, scene.textAreas.stream().filter(text -> text.member != null).count());
  }

  /** The line "..." is there once, however many attributes are hidden, and is the only one if all are. */
  @Test
  void oneEllipsisForAll() throws ClassDiagramParseException {
    Scene scene = layout("class A (aaaaaaaa) {\n  -first (hidden)\n  -second (hidden)\n}");

    assertEquals(List.of("A", ". . ."), texts(scene));
  }

  /** A class which hides nothing shows no "...", and a class which hides attributes is as high as its shown lines. */
  @Test
  void noEllipsisWithoutHiddenAttribute() throws ClassDiagramParseException {
    Scene shown = layout("class A (aaaaaaaa) {\n  -first\n  -second\n}");
    Scene hidden = layout("class A (aaaaaaaa) {\n  -first\n  -second (hidden)\n}");

    assertTrue(texts(shown).stream().noneMatch(". . ."::equals));
    assertEquals(all(shown, SceneRectangle.class).get(0).height, all(hidden, SceneRectangle.class).get(0).height, 0.001);
    assertTrue(texts(hidden).stream().anyMatch(". . ."::equals));
  }

  /** The width follows the shown attributes only. */
  @Test
  void hiddenAttributeDoesNotWiden() throws ClassDiagramParseException {
    Scene shown = layout("class A (aaaaaaaa) {\n  -a\n  -a_very_long_attribute_name_indeed\n}");
    Scene hidden = layout("class A (aaaaaaaa) {\n  -a\n  -a_very_long_attribute_name_indeed (hidden)\n}");

    assertTrue(all(hidden, SceneRectangle.class).get(0).width < all(shown, SceneRectangle.class).get(0).width);
  }

  /** The line "..." has the color of the first change set which marked one of the hidden attributes, in the order of
   * the attributes. Nothing is colored if none is marked. */
  @Test
  void ellipsisInColorOfFirstMark() throws ClassDiagramParseException {
    String unmarked = "  -first (hidden)\n  -second\n";
    String marked = "  -first (hidden)\n  -second change(added, yellow) (hidden)\n  -third change(added, blue) (hidden)\n";

    assertTrue(highlights(layout("class A (aaaaaaaa) {\n" + unmarked + "}"), YELLOW).isEmpty());
    Scene scene = layout("class A (aaaaaaaa) {\n" + marked + "}");
    assertEquals(1, highlights(scene, YELLOW).size());
    assertTrue(highlights(scene, BLUE).isEmpty());
    SceneText ellipsis = SceneTestHelper.textNamed(scene, ". . .");
    SceneRectangle mark = highlights(scene, YELLOW).get(0);
    assertTrue(mark.x <= ellipsis.left() && mark.x + mark.width >= ellipsis.left() + ellipsis.width);
  }

  /** Only the marks of a hidden attribute count, not those of the attributes which are shown. */
  @Test
  void shownMarksDoNotColorEllipsis() throws ClassDiagramParseException {
    Scene scene = layout("class A (aaaaaaaa) {\n  -first change(added, yellow)\n  -second (hidden)\n}");

    SceneText ellipsis = SceneTestHelper.textNamed(scene, ". . .");
    assertTrue(highlights(scene, YELLOW).stream().noneMatch(
      mark -> mark.x <= ellipsis.left() && mark.x + mark.width >= ellipsis.left() + ellipsis.width
        && mark.y <= ellipsis.y && mark.y + mark.height >= ellipsis.y));
  }

  /** A line "..." which the source has itself is the one line, the diagram adds none. */
  @Test
  void ownEllipsisIsNotDoubled() throws ClassDiagramParseException {
    Scene scene = layout("class A (aaaaaaaa) {\n  -first\n  -second (hidden)\n  ...\n}");

    assertEquals(1, texts(scene).stream().filter(". . ."::equals).count());
  }

  /** The source keeps the mark at the end of the line, behind the infos. */
  @Test
  void roundTrip() throws ClassDiagramParseException {
    String source = "classdiagram {\n  class A (aaaaaaaa) {\n    -first (22222222) change(added, yellow) : String [key] (hidden)\n    -second\n  }\n}";

    String written = new ClassDiagramSerializer_V001().serialize(new ClassDiagramParser_V001().parse(source));

    assertTrue(written.contains("-first (22222222) change(added, yellow) : String [key] (hidden)\n"), written);
    assertTrue(written.contains("-second\n"), written);
  }

  /** A name which is the word hidden is still a name. */
  @Test
  void hiddenAsName() throws ClassDiagramParseException {
    Scene scene = layout("class A (aaaaaaaa) {\n  -first\n  hidden : int\n}");

    assertEquals(List.of("A", "-first", "hidden : int"), texts(scene));
  }
}
