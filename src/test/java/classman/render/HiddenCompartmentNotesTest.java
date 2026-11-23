package classman.render;

import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Notes are shown as long as at least one of the elements they hang on is shown. */
class HiddenCompartmentNotesTest {
  private static final String HIDDEN_A = "class A (aaaaaaaa) at(10, 10) hideattributes {\n  -x (a0000001)\n}\n";
  private static final String SHOWN_B = "class B (bbbbbbbb) at(200, 10) {\n  -y (b0000001)\n}\n";

  private final ClassDiagramLayouter layouter = new ClassDiagramLayouter(new FixedTextMeasurer());

  private Scene layout(String elements) throws ClassDiagramParseException {
    return layouter.layout(new ClassDiagramParser_V001().parse("classdiagram {\n" + elements + "\n}"));
  }

  private static long boxCount(Scene scene) {
    return scene.elementAreas.size();
  }

  /** A note which hangs only on attributes of a hidden compartment is not drawn, nor are its lines. */
  @Test
  void noteOnHiddenAttributesOnly() throws ClassDiagramParseException {
    Scene scene = layout(HIDDEN_A + "note (c0000001) at(100, 100) \"n\"\nanchor c0000001 -- a0000001");

    assertEquals(1, boxCount(scene));
    assertTrue(scene.attachLines.isEmpty());
  }

  /** A note which hangs on a hidden attribute and on a shown element stays, and only the line to the hidden one goes. */
  @Test
  void noteWithShownTargetStays() throws ClassDiagramParseException {
    Scene scene = layout(HIDDEN_A + SHOWN_B + "note (c0000001) at(100, 100) \"n\"\n"
      + "anchor c0000001 -- a0000001\nanchor c0000001 -- b0000001");

    assertEquals(3, boxCount(scene));
    assertEquals(1, scene.attachLines.size());
    assertEquals("b0000001", scene.attachLines.get(0).attachLine.targetId);
  }

  /** A note which hangs on the hidden class itself is shown: the class is. */
  @Test
  void noteOnHiddenClassTargetIsShown() throws ClassDiagramParseException {
    Scene scene = layout(HIDDEN_A + "note (c0000001) at(100, 100) \"n\"\nanchor c0000001 -- aaaaaaaa");

    assertEquals(2, boxCount(scene));
    assertEquals(1, scene.attachLines.size());
  }

  /** An attribute which is hidden on its own stays a target, as the line ". . ." which stands for it. */
  @Test
  void singleHiddenAttributeKeepsItsNote() throws ClassDiagramParseException {
    Scene scene = layout("class A (aaaaaaaa) at(10, 10) {\n  -x (a0000001) (hidden)\n}\n"
      + "note (c0000001) at(100, 100) \"n\"\nanchor c0000001 -- a0000001");

    assertEquals(2, boxCount(scene));
    assertEquals(1, scene.attachLines.size());
  }

  /** A note without any anchor line is always shown. */
  @Test
  void freeNoteIsShown() throws ClassDiagramParseException {
    assertEquals(2, boxCount(layout(HIDDEN_A + "note (c0000001) at(100, 100) \"n\"")));
  }
}
