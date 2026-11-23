package classman.editor;

import classman.model.v001.io.ClassDiagramParseException;
import org.junit.jupiter.api.Test;

import javax.swing.JMenuItem;
import java.awt.geom.Point2D;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The reasons which the editing gives for an answer no, and how the menus show them. */
class CanReasonTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private final RecordingEditorHost host = new RecordingEditorHost();
  private DiagramEditing editing;

  private void open(String source) throws ClassDiagramParseException {
    editing = new DiagramEditing(source, host);
    editing.selectAt(new Point2D.Double(editing.scene().elementAreas.get(0).area.getCenterX(),
      editing.scene().elementAreas.get(0).area.getMinY() + 5));
  }

  private JMenuItem entry(String text) {
    ClassContextMenu menu = new ClassContextMenu(editing, editing.selectedClass(), null,
      new EditorTexts(host.locale()), start -> null, () -> { });
    return Arrays.stream(menu.getComponents()).filter(c -> c instanceof JMenuItem item && text.equals(item.getText()))
      .map(c -> (JMenuItem) c).findFirst().orElseThrow();
  }

  /** The answers: yes has no reason, no has the reason it was given with, the first no of a chain wins. */
  @Test
  void answers() {
    assertTrue(Can.YES.can());
    assertEquals("", Can.YES.whyNot());
    assertFalse(Can.NOT.can());
    assertEquals("", Can.NOT.whyNot());
    assertEquals("why", Can.not("why").whyNot());
    assertTrue(Can.IF(true).can());
    assertFalse(Can.IF(false).can());
    assertTrue(Can.IF(true, "why").can());
    assertEquals("why", Can.IF(false, "why").whyNot());
    assertEquals("first", Can.not("first").and(Can.not("second")).whyNot());
    assertEquals("second", Can.YES.and(Can.not("second")).whyNot());
    assertTrue(Can.YES.and(Can.YES).can());
  }

  /** A menu entry which can't be chosen has the reason as its tooltip, one which can has none. */
  @Test
  void tooltipOfDisabledEntry() throws ClassDiagramParseException {
    open("classdiagram {\n  class A (11111111) at(10, 20) <<Entity>>\n}");

    JMenuItem stereotype = entry("Add stereotype");
    assertFalse(stereotype.isEnabled());
    assertEquals("The class has a stereotype already.", stereotype.getToolTipText());
    assertNull(entry("Add tagged value").getToolTipText());
    assertEquals("The class has the standard style already.", entry("Standard style").getToolTipText());
  }

  /** What a change set has removed offers nothing, and says so. */
  @Test
  void removedClass() throws ClassDiagramParseException {
    open("classdiagram {\n  class A (11111111) change(removed, yellow) at(10, 20)\n}");

    assertEquals("A change set has removed the class.", entry("Add stereotype").getToolTipText());
    assertEquals("A change set has removed the class.", entry("Style...").getToolTipText());
  }

  /** The reason of an attribute which can't be added follows the cause: an interface, hidden attributes. */
  @Test
  void attributeReasons() throws ClassDiagramParseException {
    open("classdiagram {\n  interface I (11111111) at(10, 20)\n}");
    assertEquals("An interface has no attributes.", editing.canAddAttributeTo(editing.selectedClass()).whyNot());

    open("classdiagram {\n  class A (11111111) at(10, 20) hideattributes {\n    -x\n  }\n}");
    assertEquals("The attributes of the class are hidden.",
      editing.canAddAttributeTo(editing.selectedClass()).whyNot());

    open("classdiagram {\n  class A (11111111) at(10, 20) {\n    -x change(added, yellow)\n  }\n}");
    assertEquals("Attributes with marks of a change set can't be hidden.",
      editing.canHideAttributesOf(editing.selectedClass()).whyNot());
  }

  /** The German texts exist for every reason. */
  @Test
  void germanTexts() {
    EditorTexts english = new EditorTexts(java.util.Locale.ENGLISH);
    EditorTexts german = new EditorTexts(java.util.Locale.GERMAN);
    for (String key : new String[] {"reason.class.removed", "reason.stereotype.exists", "reason.interface.noattributes",
      "reason.attributes.hidden", "reason.attributes.marked", "reason.style.standard", "reason.width.automatic",
      "reason.arrow.notpossible", "reason.arrow.enum", "reason.relation.removed", "reason.center.already"}) {
      assertFalse(german.get(key).isBlank(), key);
      assertFalse(german.get(key).equals(english.get(key)), key);
    }
  }
}
