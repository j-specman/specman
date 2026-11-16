package classman.render;

import classman.model.v001.io.ClassDiagramParseException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static classman.render.DiagramStyle.PADDING_X;
import static classman.render.DiagramStyle.PADDING_Y;
import static classman.render.DiagramStyle.STACK_GAP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static classman.render.SceneTestHelper.all;
import static classman.render.SceneTestHelper.layout;
import static classman.render.SceneTestHelper.texts;
import static classman.render.SceneTestHelper.textNamed;

class ClassDiagramLayouterTest {

  private static final double LINE = 20;
  private static final double CHAR = 10;

  // ---- classes ----

  /** Without width, a class is as wide as its widest line plus the padding. */
  @Test
  void classWidthFromWidestLine() throws ClassDiagramParseException {
    Scene scene = layout("class ABCDEFGH { -wide_attribute_name }");
    assertEquals(20 * CHAR + 2 * PADDING_X, all(scene, SceneRectangle.class).get(0).width);
  }

  /** A very short class is still as wide as the minimum width. */
  @Test
  void minimumClassWidth() throws ClassDiagramParseException {
    assertEquals(DiagramStyle.MIN_BOX_WIDTH, all(layout("class A"), SceneRectangle.class).get(0).width);
  }

  /** The height is the lines of header and attributes plus the paddings. A class without attributes has one blank attribute line. */
  @Test
  void classHeightFromLines() throws ClassDiagramParseException {
    assertEquals(4 * PADDING_Y + LINE * (1 + 1), all(layout("class A"), SceneRectangle.class).get(0).height);
    assertEquals(4 * PADDING_Y + LINE * (1 + 2), all(layout("class A {\n  x\n  y\n}"),
        SceneRectangle.class).get(0).height);
  }

  /** The name is bold and centered, attributes start at the left padding. */
  @Test
  void nameCenteredAttributesLeft() throws ClassDiagramParseException {
    Scene scene = layout("class ABCDEFGH at(0, 0) {\n  attr\n}");
    SceneText name = textNamed(scene, "ABCDEFGH");
    assertTrue(name.bold);
    assertEquals(TextAnchor.MIDDLE, name.anchor);
    assertEquals(all(scene, SceneRectangle.class).get(0).width / 2, name.x);
    SceneText attribute = textNamed(scene, "attr");
    assertEquals(TextAnchor.START, attribute.anchor);
    assertEquals(PADDING_X, attribute.x);
  }

  /** A line divides the header from the attributes. */
  @Test
  void headerSeparatorLine() throws ClassDiagramParseException {
    Scene scene = layout("class ABCDEFGH at(0, 0) { x }");
    SceneLine separator = all(scene, SceneLine.class).get(0);
    SceneRectangle box = all(scene, SceneRectangle.class).get(0);
    assertEquals(0, separator.x1);
    assertEquals(box.width, separator.x2);
    assertEquals(separator.y1, separator.y2);
    assertEquals(PADDING_Y + LINE + PADDING_Y, separator.y1);
  }

  /** An attribute is shown as visibility sign, name, type and infos. The marker for hidden attributes is shown as three dots. */
  @Test
  void attributeTextParts() throws ClassDiagramParseException {
    Scene scene = layout("class A {\n  -name : String [Pflicht, 2..50]\n  ...\n}");
    assertTrue(texts(scene).contains("-name : String [Pflicht, 2..50]"), texts(scene).toString());
    assertTrue(texts(scene).contains("..."), texts(scene).toString());
  }

  /** The stereotype is above the name, the tagged values are below it. */
  @Test
  void stereotypeAndTaggedValuesInHeader() throws ClassDiagramParseException {
    Scene scene = layout("class A <<Entity>> {TABLENAME=helpversion, X=1}");
    double stereotype = textNamed(scene, "<<Entity>>").y;
    double name = textNamed(scene, "A").y;
    double tagged = textNamed(scene, "{TABLENAME=helpversion, X=1}").y;
    assertTrue(stereotype < name && name < tagged);
  }

  /** With a given width, attributes wrap at spaces and the following lines are indented. */
  @Test
  void attributeWrapping() throws ClassDiagramParseException {
    Scene scene = layout("class A at(0, 0) width=60 { `xx yy zz` }");
    assertEquals(60, all(scene, SceneRectangle.class).get(0).width);
    assertEquals(List.of("A", "xx", "yy", "zz"), texts(scene));
    assertEquals(PADDING_X, textNamed(scene, "xx").x);
    assertEquals(PADDING_X + DiagramStyle.CONTINUATION_INDENT, textNamed(scene, "yy").x);
    assertEquals(textNamed(scene, "yy").y + LINE, textNamed(scene, "zz").y);
  }

  /** A word wider than the given width widens the box. */
  @Test
  void longWordWidensBox() throws ClassDiagramParseException {
    Scene scene = layout("class A width=30 { abcdefgh }");
    assertEquals(8 * CHAR + 2 * PADDING_X, all(scene, SceneRectangle.class).get(0).width);
  }

  // ---- placing ----

  /** A class with at() is placed there. */
  @Test
  void explicitPosition() throws ClassDiagramParseException {
    Scene scene = layout("class A at(100, 50)");
    SceneRectangle box = all(scene, SceneRectangle.class).get(0);
    assertEquals(100, box.x);
    assertEquals(50, box.y);
  }

  /** Classes without at() are stacked from the top, with a gap, centered on one axis. */
  @Test
  void stackedClassesCentered() throws ClassDiagramParseException {
    Scene scene = layout("class Wide12345\nclass B");
    List<SceneRectangle> boxes = all(scene, SceneRectangle.class);
    SceneRectangle first = boxes.get(0);
    SceneRectangle second = boxes.get(1);
    assertEquals(0, first.y);
    assertEquals(first.y + first.height + STACK_GAP, second.y);
    assertEquals(first.x + first.width / 2, second.x + second.width / 2, 1e-9);
  }

  /** Classes with at() don't take part in the stack. */
  @Test
  void placedClassesOutsideStack() throws ClassDiagramParseException {
    Scene scene = layout("class A at(500, 500)\nclass B");
    assertEquals(0, all(scene, SceneRectangle.class).get(1).y);
  }

  /** The scene covers all boxes. */
  @Test
  void sceneBounds() throws ClassDiagramParseException {
    Scene scene = layout("class A at(100, 50)\nclass B at(300, 200)");
    List<SceneRectangle> boxes = all(scene, SceneRectangle.class);
    assertEquals(100, scene.x);
    assertEquals(50, scene.y);
    assertEquals(boxes.get(1).x + boxes.get(1).width - 100, scene.width);
    assertEquals(boxes.get(1).y + boxes.get(1).height - 50, scene.height);
  }

  // ---- notes ----

  /** A note with a given width wraps its text, and its outline has a folded corner. */
  @Test
  void noteGivenWidth() throws ClassDiagramParseException {
    Scene scene = layout("note at(0, 0) width=120 `aa bb cc dd`");
    assertEquals(List.of("aa bb cc", "dd"), texts(scene));
    ScenePolygon outline = all(scene, ScenePolygon.class).get(0);
    assertEquals(DiagramStyle.NOTE_FILL, outline.fill);
    assertEquals(120, outline.xs[2]);
    assertEquals(DiagramStyle.NOTE_FOLD, outline.ys[2]);
    assertEquals(2 * PADDING_Y + 2 * LINE, outline.ys[3]);
  }

  /** A note without width is as wide as its longest line, and its text keeps clear of the folded corner. */
  @Test
  void noteWidthFromLongestLine() throws ClassDiagramParseException {
    Scene scene = layout("note at(0, 0) `abc\\nabcdefgh`");
    assertEquals(List.of("abc", "abcdefgh"), texts(scene));
    double width = all(scene, ScenePolygon.class).get(0).xs[2];
    assertEquals(8 * CHAR + PADDING_X + DiagramStyle.NOTE_RIGHT_PADDING, width);
    SceneText longest = textNamed(scene, "abcdefgh");
    assertTrue(longest.left() + longest.width <= width - DiagramStyle.NOTE_FOLD);
  }

  /** A note without width wraps its text when it would be wider than the maximum. */
  @Test
  void noteWrapsAtMaxWidth() throws ClassDiagramParseException {
    Scene scene = layout("note `aaaaa bbbbb ccccc ddddd eeeee fffff`");
    assertEquals(List.of("aaaaa bbbbb ccccc", "ddddd eeeee fffff"), texts(scene));
  }

  /** Notes without at() are stacked together with the classes. */
  @Test
  void notesInStack() throws ClassDiagramParseException {
    Scene scene = layout("class AAAAAAAA\nnote `n`");
    SceneRectangle box = all(scene, SceneRectangle.class).get(0);
    ScenePolygon note = all(scene, ScenePolygon.class).get(0);
    assertEquals(box.y + box.height + STACK_GAP, note.ys[0]);
  }
}
