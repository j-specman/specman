package classman.render;

import classman.model.v001.FontFamily_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import classman.model.v001.io.ClassDiagramSerializer_V001;
import org.junit.jupiter.api.Test;

import static classman.render.SceneTestHelper.all;
import static classman.render.SceneTestHelper.layout;
import static classman.render.SceneTestHelper.textNamed;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassStyleTest {

  private static final String ATTRIBUTES = " {\n  -first : String\n}";

  /** The entries may come in any order, the source writes them in a fixed one, the colors in lower case. */
  @Test
  void entriesInAnyOrder() throws ClassDiagramParseException {
    String source = "classdiagram {\n  class A (aaaaaaaa) style(font=CourierPrime, fontsize=14, "
      + "linecolor=\"#112233\", textcolor=\"#AABBCC\", fill=\"#CCE5FF\")\n}";

    String written = new ClassDiagramSerializer_V001().serialize(new ClassDiagramParser_V001().parse(source));

    assertTrue(written.contains("class A (aaaaaaaa) style(fill=\"#cce5ff\", textcolor=\"#aabbcc\", "
      + "linecolor=\"#112233\", font=CourierPrime, fontsize=14)\n"), written);
  }

  /** A style which only keeps standard values isn't written. */
  @Test
  void standardStyleNotWritten() throws ClassDiagramParseException {
    String written = new ClassDiagramSerializer_V001().serialize(
      new ClassDiagramParser_V001().parse("classdiagram {\n  class A (aaaaaaaa)\n}"));

    assertFalse(written.contains("style"), written);
  }

  /** What the grammar can't tell is rejected with the line: a wrong color, an unknown font, a size which is not
   * offered, an entry which stands twice. */
  @Test
  void wrongStyleRejected() {
    for (String style : new String[] {"textcolor=\"red\"", "linecolor=\"#12345\"", "font=Arial", "fontsize=13",
        "fill=\"#ffffff\", fill=\"#000000\"", "fontsize=12, fontsize=14"}) {
      ClassDiagramParseException e = assertThrows(ClassDiagramParseException.class,
        () -> new ClassDiagramParser_V001().parse("classdiagram {\n  class A style(" + style + ")\n}"), style);
      assertTrue(e.getMessage().startsWith("line 2"), e.getMessage());
    }
  }

  /** All texts of the class get the text color, font and size, the border and the line above the attributes get the
   * line color, and a class of another style doesn't. */
  @Test
  void styleReachesTheScene() throws ClassDiagramParseException {
    Scene scene = layout("class A (aaaaaaaa) style(textcolor=\"#102030\", linecolor=\"#405060\", font=SitkaDisplay, "
      + "fontsize=16)" + ATTRIBUTES + "\nclass B (bbbbbbbb)" + ATTRIBUTES);

    for (String text : new String[] {"A", "-first : String"}) {
      SceneText styled = textNamed(scene, text);
      assertEquals("#102030", styled.fill, text);
      assertEquals(new TextFont(FontFamily_V001.SITKA, 16), styled.font, text);
    }
    assertEquals("#405060", all(scene, SceneRectangle.class).get(0).stroke);
    assertEquals("#405060", all(scene, SceneLine.class).get(0).stroke);
    SceneText other = textNamed(scene, "B");
    assertNull(other.fill);
    assertEquals(TextFont.DEFAULT, other.font);
  }

  /** A larger font makes the class larger, as the measurer of that font measures it. */
  @Test
  void fontSizeChangesTheBox() throws ClassDiagramParseException {
    SceneRectangle normal = all(layout("class A (aaaaaaaa)" + ATTRIBUTES), SceneRectangle.class).get(0);
    SceneRectangle large = all(layout("class A (aaaaaaaa) style(fontsize=20)" + ATTRIBUTES), SceneRectangle.class).get(0);

    assertTrue(large.height > normal.height);
    assertTrue(large.width > normal.width);
  }

  /** A removed class veils its own colors, only its name keeps the text color, because it stands on the strong color
   * of the change set. */
  @Test
  void removedClassVeilsOwnColors() throws ClassDiagramParseException {
    Scene scene = layout("class A (aaaaaaaa) change(removed, yellow) style(fill=\"#336699\", textcolor=\"#102030\", "
      + "linecolor=\"#102030\")" + ATTRIBUTES);

    SceneRectangle box = all(scene, SceneRectangle.class).get(1);
    assertNotEquals("#336699", box.fill);
    assertNotEquals("#102030", box.stroke);
    assertEquals("#102030", textNamed(scene, "A").fill);
    String veiled = textNamed(scene, "-first : String").fill;
    assertNotEquals("#102030", veiled);
    assertNotEquals(DiagramStyle.VEILED_TEXT, veiled);
  }

  /** The SVG names the family of the class, with what a viewer without the font falls back to, and its size. */
  @Test
  void svgNamesFont() throws ClassDiagramParseException {
    String svg = new SceneSvgWriter().write(layout("class A (aaaaaaaa) style(font=SitkaDisplay, fontsize=16)\n"
      + "class B (bbbbbbbb) style(font=CourierPrime)"));

    assertTrue(svg.contains("font-family=\"SitkaDisplay, serif\" font-size=\"16\""), svg);
    assertTrue(svg.contains("font-family=\"CourierPrime, monospace\" font-size=\"12\""), svg);
  }
}
