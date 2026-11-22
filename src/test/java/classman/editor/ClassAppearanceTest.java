package classman.editor;

import classman.model.v001.ClassModel_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import org.junit.jupiter.api.Test;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import java.awt.Color;
import java.awt.Component;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassAppearanceTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final String CLASS = "classdiagram {\n  class A (11111111) at(10, 20) {\n    -x\n    -y\n  }\n}";

  private final RecordingEditorHost host = new RecordingEditorHost();
  private DiagramEditing editing;

  private void open(String source) throws ClassDiagramParseException {
    editing = new DiagramEditing(source, host);
    editing.selectAt(inClass());
  }

  private Point2D inClass() {
    Rectangle2D area = editing.scene().elementAreas.get(0).area;
    return new Point2D.Double(area.getCenterX(), area.getMinY() + 5);
  }

  private ClassModel_V001 parsedClass() throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(editing.source()).classes().get(0);
  }

  private List<JMenuItem> menuEntries(UnaryOperator<Color> picker) {
    ClassContextMenu menu = new ClassContextMenu(editing, editing.selectedClass(), null, new EditorTexts(host.locale()),
      picker, () -> { });
    List<JMenuItem> items = new ArrayList<>();
    for (Component component : menu.getComponents()) {
      if (component instanceof JMenuItem item) {
        items.add(item);
      }
    }
    return items;
  }

  /** The color is one operation, written in the head of the class, and the standard color takes it away again. */
  @Test
  void fillSetAndReset() throws ClassDiagramParseException {
    open(CLASS);

    editing.setFill(editing.selectedClass(), "#CCE5FF");

    assertEquals("#cce5ff", parsedClass().fill);
    assertEquals(1, host.reportedSources.size());
    assertTrue(editing.canUndo());

    editing.setFill(editing.selectedClass(), null);

    assertNull(parsedClass().fill);
    assertFalse(editing.source().contains("fill"), editing.source());
  }

  /** The color is no change: it needs no mark while changes are tracked. */
  @Test
  void fillIsNotMarked() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    open(CLASS);

    editing.setFill(editing.selectedClass(), "#cce5ff");

    assertFalse(editing.source().contains("change("), editing.source());
    assertFalse(editing.source().contains("was("), editing.source());
  }

  /** The class which hides its attributes is as high as its head, and shows them again after the entry is chosen. */
  @Test
  void hidingShrinksTheClass() throws ClassDiagramParseException {
    open(CLASS);
    double heightWith = editing.selectedArea().getHeight();

    editing.setAttributesHidden(editing.selectedClass(), true);

    assertTrue(parsedClass().attributesHidden);
    assertEquals(2, parsedClass().attributes.size());
    assertTrue(editing.selectedArea().getHeight() < heightWith);
    assertFalse(editing.canAddAttributeTo(editing.selectedClass()));
    assertTrue(editing.scene().textAreas.stream().noneMatch(text -> text.attribute != null));

    editing.setAttributesHidden(editing.selectedClass(), false);

    assertEquals(heightWith, editing.selectedArea().getHeight(), 0.001);
    assertFalse(editing.source().contains("hideattributes"), editing.source());
  }

  /** Attributes with marks of a change set can't be hidden: neither the operation nor the menu entry allows it. */
  @Test
  void markedAttributesCannotBeHidden() throws ClassDiagramParseException {
    open("classdiagram {\n  class A (11111111) at(10, 20) {\n    -x change(added, yellow)\n    -y\n  }\n}");

    assertFalse(editing.canHideAttributesOf(editing.selectedClass()));
    editing.setAttributesHidden(editing.selectedClass(), true);

    assertFalse(parsedClass().attributesHidden);
    assertTrue(host.reportedSources.isEmpty());
    JCheckBoxMenuItem shows = (JCheckBoxMenuItem) menuEntries(start -> null).get(6);
    assertTrue(shows.isSelected());
    assertFalse(shows.isEnabled());
  }

  /** An interface has no attribute compartment to hide, but can have a color. */
  @Test
  void interfaceCannotHideAttributes() throws ClassDiagramParseException {
    open("classdiagram {\n  interface I (11111111) at(10, 20)\n}");

    assertFalse(editing.canHideAttributesOf(editing.selectedClass()));
    assertTrue(editing.canChangeFill(editing.selectedClass()));
  }

  /** What is attached to a hidden attribute is attached to the class. */
  @Test
  void anchorToHiddenAttributeStaysDrawable() throws ClassDiagramParseException {
    open("classdiagram {\n  class A (11111111) at(10, 20) hideattributes {\n    -x (22222222)\n  }\n"
      + "  note (33333333) at(300, 20) \"n\"\n  anchor 33333333 -- 22222222\n}");

    assertEquals(1, editing.scene().attachLines.size());
  }

  /** The color entry asks for a color and gives the class the one which was chosen, nothing happens when the user
   * doesn't choose one. */
  @Test
  void colorEntry() throws ClassDiagramParseException {
    open(CLASS);

    menuEntries(start -> null).get(4).doClick(0);
    assertNull(parsedClass().fill);

    menuEntries(start -> new Color(0xCC, 0xE5, 0xFF)).get(4).doClick(0);
    assertEquals("#cce5ff", parsedClass().fill);
  }

  /** The standard color entry can only be chosen while the class has a color of its own. */
  @Test
  void standardColorEntry() throws ClassDiagramParseException {
    open(CLASS);
    assertFalse(menuEntries(start -> null).get(5).isEnabled());

    editing.setFill(editing.selectedClass(), "#cce5ff");
    JMenuItem standard = menuEntries(start -> null).get(5);
    assertTrue(standard.isEnabled());

    standard.doClick(0);
    assertNull(parsedClass().fill);
  }

  /** The attributes entry is checked while they are shown, and switches them. */
  @Test
  void showAttributesEntry() throws ClassDiagramParseException {
    open(CLASS);
    JCheckBoxMenuItem shows = (JCheckBoxMenuItem) menuEntries(start -> null).get(6);
    assertTrue(shows.isSelected());

    shows.doClick(0);

    assertTrue(parsedClass().attributesHidden);
    assertFalse(((JCheckBoxMenuItem) menuEntries(start -> null).get(6)).isSelected());
    assertFalse(menuEntries(start -> null).get(2).isEnabled());
  }
}
