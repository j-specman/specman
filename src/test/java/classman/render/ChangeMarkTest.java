package classman.render;

import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import org.junit.jupiter.api.Test;

import java.util.List;

import static classman.render.SceneTestHelper.all;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** How added, removed and changed elements and texts are drawn. Yellow is the only change set the test knows. */
class ChangeMarkTest {
  private static final String YELLOW = "#ffff00";
  private static final String PALE_YELLOW = "#ffff99";
  private static final double HALF_FRAME = DiagramStyle.FRAME_STROKE_WIDTH / 2;

  private final ClassDiagramLayouter layouter = new ClassDiagramLayouter(new FixedTextMeasurer(),
      changeSet -> "yellow".equals(changeSet) ? YELLOW : null);

  private Scene layout(String elements) throws ClassDiagramParseException {
    return layouter.layout(new ClassDiagramParser_V001().parse("classdiagram {\n" + elements + "\n}"));
  }

  private static List<SceneRectangle> rectanglesFilled(Scene scene, String fill) {
    return all(scene, SceneRectangle.class).stream().filter(rectangle -> fill.equals(rectangle.fill)).toList();
  }

  /** An added class has a frame in the strong color below its box, and the scene has room for the frame. */
  @Test
  void addedClassFrame() throws ClassDiagramParseException {
    Scene scene = layout("class A (aaaaaaaa) change(added, yellow) at(0, 0)");
    List<SceneRectangle> rectangles = all(scene, SceneRectangle.class);
    assertEquals(YELLOW, rectangles.get(0).stroke);
    assertEquals(DiagramStyle.FRAME_STROKE_WIDTH, rectangles.get(0).strokeWidth);
    assertEquals(null, rectangles.get(0).fill);
    assertEquals(DiagramStyle.CLASS_FILL, rectangles.get(1).fill);
    assertEquals(-HALF_FRAME, scene.x);
    assertEquals(-HALF_FRAME, scene.y);
    assertEquals(rectangles.get(1).width + 2 * HALF_FRAME, scene.width);
  }

  /** A removed class is veiled, and a line goes through its name. */
  @Test
  void removedClassVeiled() throws ClassDiagramParseException {
    Scene scene = layout("class Aa (aaaaaaaa) change(removed, yellow) at(0, 0)");
    assertNotEquals(DiagramStyle.CLASS_FILL, all(scene, SceneRectangle.class).get(1).fill);
    SceneText name = SceneTestHelper.textNamed(scene, "Aa");
    assertEquals(DiagramStyle.VEILED_MARKED_TEXT, name.fill);
    SceneLine strike = strikes(scene).get(0);
    assertEquals(name.left() - 1, strike.x1);
    assertEquals(name.left() + name.width + 1, strike.x2);
  }

  /** The name of a removed class has the strong color of the change set behind it, like a removed text in Specman. */
  @Test
  void removedClassNameHighlighted() throws ClassDiagramParseException {
    Scene scene = layout("class Aa (aaaaaaaa) change(removed, yellow) at(0, 0)");
    SceneText name = SceneTestHelper.textNamed(scene, "Aa");
    List<SceneRectangle> highlights = rectanglesFilled(scene, YELLOW);
    assertEquals(1, highlights.size());
    assertTrue(highlights.get(0).x <= name.left() && highlights.get(0).x + highlights.get(0).width >= name.left() + name.width);
  }

  /** The text of a removed note has the strong color behind it. */
  @Test
  void removedNoteTextHighlighted() throws ClassDiagramParseException {
    Scene scene = layout("note change(removed, yellow) at(0, 0) \"gone\"");
    assertEquals(1, rectanglesFilled(scene, YELLOW).size());
  }

  private static List<SceneLine> strikes(Scene scene) {
    return all(scene, SceneLine.class).stream().filter(line -> DiagramStyle.STRIKE_COLOR.equals(line.stroke)).toList();
  }

  /** A change set which the host doesn't know gets the color for unknown change sets. */
  @Test
  void unknownChangeSetColor() throws ClassDiagramParseException {
    Scene scene = layout("class A (aaaaaaaa) change(added, nobody) at(0, 0)");
    assertEquals(DiagramStyle.UNKNOWN_CHANGE_COLOR, all(scene, SceneRectangle.class).get(0).stroke);
  }

  /** A removed attribute has a pale row, the strong color behind its text and a line through it. */
  @Test
  void removedAttributeRow() throws ClassDiagramParseException {
    Scene scene = layout("class A (aaaaaaaa) at(0, 0) { x change(removed, yellow) }");
    SceneRectangle box = all(scene, SceneRectangle.class).get(0);
    SceneRectangle row = rectanglesFilled(scene, PALE_YELLOW).get(0);
    assertEquals(box.width - 2, row.width);
    SceneText text = SceneTestHelper.textNamed(scene, "x");
    SceneRectangle highlight = rectanglesFilled(scene, YELLOW).get(0);
    assertEquals(text.left() - DiagramStyle.HIGHLIGHT_MARGIN, highlight.x);
    assertEquals(text.width + 2 * DiagramStyle.HIGHLIGHT_MARGIN, highlight.width);
    assertEquals(1, strikes(scene).size());
  }

  /** An added attribute is not struck through. */
  @Test
  void addedAttributeWithoutStrike() throws ClassDiagramParseException {
    Scene scene = layout("class A (aaaaaaaa) at(0, 0) { x change(added, yellow) }");
    assertEquals(1, rectanglesFilled(scene, YELLOW).size());
    assertTrue(strikes(scene).isEmpty());
  }

  /** The background of a changed text is exactly as wide as that text, not as the whole attribute line. */
  @Test
  void changedTextHighlight() throws ClassDiagramParseException {
    Scene scene = layout("class A (aaaaaaaa) at(0, 0) { -name was(yellow, nick) }");
    SceneText line = SceneTestHelper.textNamed(scene, "-name");
    SceneRectangle highlight = rectanglesFilled(scene, YELLOW).get(0);
    assertEquals(line.left() + 10 - DiagramStyle.HIGHLIGHT_MARGIN, highlight.x);
    assertEquals(40 + 2 * DiagramStyle.HIGHLIGHT_MARGIN, highlight.width);
    assertTrue(highlight.keepsVisible, "the highlight of a change must stay noticeable when the diagram is small");
    assertEquals(1, rectanglesFilled(scene, YELLOW).size());
  }

  /** A text change in the class name marks all of the name, the box keeps its size. */
  @Test
  void changedNameKeepsBoxSize() throws ClassDiagramParseException {
    Scene marked = layout("class Hello was(yellow, \"A much longer old name\") (aaaaaaaa) at(0, 0)");
    Scene plain = layout("class Hello (aaaaaaaa) at(0, 0)");
    assertEquals(all(plain, SceneRectangle.class).get(0).width, all(marked, SceneRectangle.class).get(0).width);
    assertEquals(1, rectanglesFilled(marked, YELLOW).size());
  }

  /** The marks inside an added class are not drawn: its frame says it all. */
  @Test
  void marksInAddedClassIgnored() throws ClassDiagramParseException {
    Scene scene = layout("class A was(yellow, B) (aaaaaaaa) change(added, yellow) at(0, 0) { x change(removed, yellow) }");
    assertTrue(rectanglesFilled(scene, YELLOW).isEmpty());
    assertTrue(strikes(scene).isEmpty());
  }

  /** An added relation has a thick line in the strong color below its own line, and its label is highlighted. */
  @Test
  void addedRelationHighlight() throws ClassDiagramParseException {
    Scene scene = layout("""
        class A (aaaaaaaa) at(0, 0)
        class B (bbbbbbbb) at(200, 0)
        association change(added, yellow) aaaaaaaa -- bbbbbbbb "x"
        """);
    List<ScenePolyline> lines = all(scene, ScenePolyline.class);
    assertEquals(YELLOW, lines.get(0).stroke);
    assertEquals(DiagramStyle.RELATION_HIGHLIGHT_WIDTH, lines.get(0).strokeWidth);
    assertEquals(DiagramStyle.STROKE, lines.get(1).stroke);
    assertEquals(1, rectanglesFilled(scene, YELLOW).size());
  }

  /** A removed relation is veiled, has a cross on its line and a struck through label on the strong color of the change set. */
  @Test
  void removedRelationCross() throws ClassDiagramParseException {
    Scene scene = layout("""
        class A (aaaaaaaa) at(0, 0)
        class B (bbbbbbbb) at(200, 0)
        association change(removed, yellow) aaaaaaaa -- bbbbbbbb "x"
        """);
    List<ScenePolyline> lines = all(scene, ScenePolyline.class);
    assertEquals(DiagramStyle.VEILED_LINE, lines.get(1).stroke);
    assertEquals(3, strikes(scene).size());
    assertEquals(1, rectanglesFilled(scene, YELLOW).size());
  }

  /** A changed multiplicity and a changed label are highlighted, the line is left alone. */
  @Test
  void changedRelationTexts() throws ClassDiagramParseException {
    Scene scene = layout("""
        class A (aaaaaaaa) at(0, 0)
        class B (bbbbbbbb) at(200, 0)
        association aaaaaaaa 1 was(yellow, 0..1) -> * bbbbbbbb "x" was(yellow)
        """);
    assertEquals(2, rectanglesFilled(scene, YELLOW).size());
    assertEquals(DiagramStyle.STROKE_WIDTH, all(scene, ScenePolyline.class).get(0).strokeWidth);
  }

  /** An added note has a frame around its outline, and the scene has room for it. */
  @Test
  void addedNoteFrame() throws ClassDiagramParseException {
    Scene scene = layout("note change(added, yellow) at(0, 0) \"hi\"");
    ScenePolygon frame = all(scene, ScenePolygon.class).get(0);
    assertEquals(YELLOW, frame.stroke);
    assertEquals(null, frame.fill);
    assertEquals(DiagramStyle.FRAME_STROKE_WIDTH, frame.strokeWidth);
    assertEquals(-HALF_FRAME, scene.x);
  }

  /** A diagram without marks has no frames, highlights or strikes. */
  @Test
  void unmarkedDiagramPlain() throws ClassDiagramParseException {
    Scene scene = layout("class A (aaaaaaaa) at(0, 0) { x }\nnote at(100, 0) \"n\"");
    assertEquals(0, scene.x);
    assertEquals(1, all(scene, SceneRectangle.class).size());
    assertFalse(all(scene, SceneText.class).stream().anyMatch(text -> text.fill != null));
  }
}
