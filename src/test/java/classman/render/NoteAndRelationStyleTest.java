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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoteAndRelationStyleTest {

  private static final String CLASSES = "class A (aaaaaaaa) at(0, 0)\nclass B (bbbbbbbb) at(0, 300)\n";
  private static final String RED = "#cc0000";
  private static final TextFont COURIER_16 = new TextFont(FontFamily_V001.COURIER, 16);

  /** The style of a note reaches its outline, its fold and its text. */
  @Test
  void noteStyleReachesTheScene() throws ClassDiagramParseException {
    Scene scene = layout("note (11111111) at(0, 0) style(fill=\"#336699\", textcolor=\"#102030\", linecolor=\"" + RED
      + "\", font=CourierPrime, fontsize=16) \"hello\"");

    ScenePolygon outline = all(scene, ScenePolygon.class).get(0);
    assertEquals("#336699", outline.fill);
    assertEquals(RED, outline.stroke);
    assertEquals(RED, all(scene, ScenePolygon.class).get(1).stroke, "the fold");
    SceneText text = textNamed(scene, "hello");
    assertEquals("#102030", text.fill);
    assertEquals(COURIER_16, text.font);
  }

  /** A larger font makes the note larger. */
  @Test
  void noteFontSizeChangesTheBox() throws ClassDiagramParseException {
    ScenePolygon normal = all(layout("note (11111111) at(0, 0) \"hello\""), ScenePolygon.class).get(0);
    ScenePolygon large = all(layout("note (11111111) at(0, 0) style(fontsize=20) \"hello\""), ScenePolygon.class).get(0);

    assertTrue(large.ys[3] > normal.ys[3]);
  }

  /** A removed note veils its own fill and line, but its text, which stands on the strong color, keeps its color. */
  @Test
  void removedNoteVeilsOwnColors() throws ClassDiagramParseException {
    Scene scene = layout("note (11111111) change(removed, yellow) at(0, 0) style(fill=\"#336699\", textcolor=\"#102030\", "
      + "linecolor=\"#102030\") \"hello\"");

    ScenePolygon outline = all(scene, ScenePolygon.class).get(1);
    assertNotEquals("#336699", outline.fill);
    assertNotEquals("#102030", outline.stroke);
    assertEquals("#102030", textNamed(scene, "hello").fill);
  }

  /** The line color of a relation reaches its line and the marks at its ends, the diamond of an aggregation stays white
   * inside, that of a composition takes the color. */
  @Test
  void relationLineColorReachesLineAndMarks() throws ClassDiagramParseException {
    Scene aggregation = layout(CLASSES + "aggregation style(linecolor=\"" + RED + "\") aaaaaaaa -> bbbbbbbb");
    Scene composition = layout(CLASSES + "composition style(linecolor=\"" + RED + "\") aaaaaaaa -> bbbbbbbb");

    assertEquals(RED, all(aggregation, ScenePolyline.class).get(0).stroke);
    ScenePolygon hollow = all(aggregation, ScenePolygon.class).get(0);
    assertEquals(DiagramStyle.WHITE, hollow.fill);
    assertEquals(RED, hollow.stroke);
    assertEquals(RED, all(composition, ScenePolygon.class).get(0).fill);
  }

  /** The text color and font of a relation reach its label and its multiplicities. */
  @Test
  void relationTextsGetColorAndFont() throws ClassDiagramParseException {
    Scene scene = layout(CLASSES + "association style(textcolor=\"#102030\", font=CourierPrime, fontsize=16) "
      + "aaaaaaaa 1 -> * bbbbbbbb \"label\"");

    for (String text : new String[] {"1", "*", "label"}) {
      assertEquals("#102030", textNamed(scene, text).fill, text);
      assertEquals(COURIER_16, textNamed(scene, text).font, text);
    }
  }

  /** A relation without a style keeps the standard look. */
  @Test
  void relationWithoutStyleIsStandard() throws ClassDiagramParseException {
    Scene scene = layout(CLASSES + "association aaaaaaaa -> bbbbbbbb \"label\"");

    assertEquals(DiagramStyle.STROKE, all(scene, ScenePolyline.class).get(0).stroke);
    assertNull(textNamed(scene, "label").fill);
    assertEquals(TextFont.DEFAULT, textNamed(scene, "label").font);
  }

  /** Notes and every kind of relation write their style at the place the grammar gives it, and read it again. */
  @Test
  void roundTripInEveryElement() throws ClassDiagramParseException {
    String source = "classdiagram {\n"
      + "    class A (aaaaaaaa)\n"
      + "    class B (bbbbbbbb)\n"
      + "    association style(linecolor=\"#cc0000\", font=Roboto) aaaaaaaa 1 -> * bbbbbbbb \"label\"\n"
      + "    aggregation (cccccccc) change(added, yellow) style(textcolor=\"#102030\") aaaaaaaa -> bbbbbbbb\n"
      + "    composition style(fontsize=16) aaaaaaaa -> bbbbbbbb\n"
      + "    extends style(linecolor=\"#cc0000\") aaaaaaaa -> bbbbbbbb\n"
      + "    implements style(linecolor=\"#cc0000\") aaaaaaaa -> bbbbbbbb\n"
      + "    note (dddddddd) at(5, 6) width=100 style(fill=\"#336699\", font=SitkaDisplay) \"hello\"\n"
      + "}\n";

    String written = new ClassDiagramSerializer_V001().serialize(new ClassDiagramParser_V001().parse(source));

    for (String line : source.split("\n")) {
      if (line.contains("style(")) {
        assertTrue(written.contains(line.strip().replace("font=Roboto", "font=Roboto") + "\n"), line + "\n---\n" + written);
      }
    }
  }

  /** A relation has no background, and every message names the kind of element. */
  @Test
  void relationFillRejected() {
    ClassDiagramParseException fill = assertThrows(ClassDiagramParseException.class, () -> new ClassDiagramParser_V001()
      .parse("classdiagram {\n  association style(fill=\"#ffffff\") aaaaaaaa -> bbbbbbbb\n}"));
    assertTrue(fill.getMessage().startsWith("line 2"), fill.getMessage());
    assertTrue(fill.getMessage().contains("A relation has no fill"), fill.getMessage());

    ClassDiagramParseException color = assertThrows(ClassDiagramParseException.class, () -> new ClassDiagramParser_V001()
      .parse("classdiagram {\n  note style(textcolor=\"red\") \"n\"\n}"));
    assertTrue(color.getMessage().contains("of a note"), color.getMessage());
  }
}
