package classman.editor;

import classman.model.v001.Compartment_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.ElementStyleModel_V001;
import classman.model.v001.FontFamily_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import org.junit.jupiter.api.Test;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import java.awt.Component;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

  private List<JMenuItem> menuEntries(UnaryOperator<ElementStyleModel_V001> picker) {
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

  private static ElementStyleModel_V001 style(String fill, String textColor, FontFamily_V001 font, Integer fontSize) {
    ElementStyleModel_V001 style = new ElementStyleModel_V001();
    style.fill = fill;
    style.textColor = textColor;
    style.font = font;
    style.fontSize = fontSize;
    return style;
  }

  /** The style is one operation, written in the head of the class, and the standard style takes it away again. */
  @Test
  void styleSetAndReset() throws ClassDiagramParseException {
    open(CLASS);

    editing.setStyle(editing.selectedClass(), style("#cce5ff", "#112233", FontFamily_V001.COURIER, 16));

    ElementStyleModel_V001 written = parsedClass().style;
    assertEquals("#cce5ff", written.fill);
    assertEquals("#112233", written.textColor);
    assertEquals(FontFamily_V001.COURIER, written.font);
    assertEquals(16, written.fontSize);
    assertEquals(1, host.reportedSources.size());
    assertTrue(editing.canUndo());

    editing.setStyle(editing.selectedClass(), new ElementStyleModel_V001());

    assertTrue(parsedClass().style.isStandard());
    assertFalse(editing.source().contains("style"), editing.source());
  }

  /** The style is no change: it needs no mark while changes are tracked. */
  @Test
  void styleIsNotMarked() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    open(CLASS);

    editing.setStyle(editing.selectedClass(), style("#cce5ff", null, null, null));

    assertFalse(editing.source().contains("change("), editing.source());
    assertFalse(editing.source().contains("was("), editing.source());
  }

  /** The same style again is no operation. */
  @Test
  void sameStyleChangesNothing() throws ClassDiagramParseException {
    open(CLASS);

    editing.setStyle(editing.selectedClass(), new ElementStyleModel_V001());

    assertTrue(host.reportedSources.isEmpty());
  }

  /** The class which hides its attributes is as high as its head, and shows them again after the entry is chosen. */
  @Test
  void hidingShrinksTheClass() throws ClassDiagramParseException {
    open(CLASS);
    double heightWith = editing.selectedArea().getHeight();

    editing.setCompartmentHidden(editing.selectedClass(), Compartment_V001.ATTRIBUTES, true);

    assertTrue(parsedClass().attributesHidden);
    assertEquals(2, parsedClass().attributes.size());
    assertTrue(editing.selectedArea().getHeight() < heightWith);
    assertFalse(editing.canAddMemberTo(editing.selectedClass(), Compartment_V001.ATTRIBUTES).can());
    assertTrue(editing.scene().textAreas.stream().noneMatch(text -> text.member != null));

    editing.setCompartmentHidden(editing.selectedClass(), Compartment_V001.ATTRIBUTES, false);

    assertEquals(heightWith, editing.selectedArea().getHeight(), 0.001);
    assertFalse(editing.source().contains("hideattributes"), editing.source());
  }

  /** Attributes with marks of a change set can't be hidden: neither the operation nor the menu entry allows it. */
  @Test
  void markedAttributesCannotBeHidden() throws ClassDiagramParseException {
    open("classdiagram {\n  class A (11111111) at(10, 20) {\n    -x change(added, yellow)\n    -y\n  }\n}");

    assertFalse(editing.canHideCompartmentOf(editing.selectedClass(), Compartment_V001.ATTRIBUTES).can());
    editing.setCompartmentHidden(editing.selectedClass(), Compartment_V001.ATTRIBUTES, true);

    assertFalse(parsedClass().attributesHidden);
    assertTrue(host.reportedSources.isEmpty());
    JCheckBoxMenuItem shows = (JCheckBoxMenuItem) ((javax.swing.JMenu) menuEntries(start -> null).get(4)).getMenuComponent(0);
    assertTrue(shows.isSelected());
    assertFalse(shows.isEnabled());
  }

  /** An interface has no attribute compartment to hide, but can have a style. */
  @Test
  void interfaceCannotHideAttributes() throws ClassDiagramParseException {
    open("classdiagram {\n  interface I (11111111) at(10, 20)\n}");

    assertFalse(editing.canHideCompartmentOf(editing.selectedClass(), Compartment_V001.ATTRIBUTES).can());
    assertTrue(editing.canChangeStyle(editing.selectedClass()).can());
  }

  /** A note which hangs on an attribute of a hidden compartment only is hidden with it, with its line. */
  @Test
  void noteOnHiddenAttributeIsHidden() throws ClassDiagramParseException {
    open("classdiagram {\n  class A (11111111) at(10, 20) hideattributes {\n    -x (22222222)\n  }\n"
      + "  note (33333333) at(300, 20) \"n\"\n  anchor 33333333 -- 22222222\n}");

    assertTrue(editing.scene().attachLines.isEmpty());
    assertEquals(1, editing.scene().elementAreas.size());
  }

  /** A selected note which goes away with the compartment is not selected any more, and the anchor tool copes with
   * the note which isn't there. */
  @Test
  void hiddenNoteIsDeselected() throws ClassDiagramParseException {
    open("classdiagram {\n  class A (11111111) at(10, 20) {\n    -x (22222222)\n  }\n"
      + "  note (33333333) at(300, 20) \"n\"\n  anchor 33333333 -- 22222222\n}");
    ClassModel_V001 box = editing.selectedClass();
    Rectangle2D noteArea = editing.scene().elementAreas.get(1).area;
    editing.selectAt(new Point2D.Double(noteArea.getCenterX(), noteArea.getCenterY()));
    assertNotNull(editing.selectedNote());

    editing.setCompartmentHidden(box, Compartment_V001.ATTRIBUTES, true);
    editing.toggleAnchorTool();
    new PointerControl(editing, () -> { }, text -> { }, at -> { }, pixels -> pixels)
      .pressed(new Point2D.Double(noteArea.getCenterX(), noteArea.getCenterY()));

    assertNull(editing.selectedNote());
    assertEquals(1, editing.scene().elementAreas.size());
  }

  /** The style entry asks for a style and gives the class the one which was chosen, nothing happens when the user
   * doesn't choose one. */
  @Test
  void styleEntry() throws ClassDiagramParseException {
    open(CLASS);

    menuEntries(start -> null).get(6).doClick(0);
    assertTrue(parsedClass().style.isStandard());

    menuEntries(start -> style("#cce5ff", null, FontFamily_V001.SITKA, null)).get(6).doClick(0);
    assertEquals("#cce5ff", parsedClass().style.fill);
    assertEquals(FontFamily_V001.SITKA, parsedClass().style.font);
  }

  /** The picker gets a copy of the style of the class, so cancelling can't change the class. */
  @Test
  void stylePickerGetsCopy() throws ClassDiagramParseException {
    open(CLASS);

    menuEntries(start -> {
      start.fill = "#cce5ff";
      return null;
    }).get(6).doClick(0);

    assertTrue(parsedClass().style.isStandard());
  }

  /** The attributes entry is checked while they are shown, and switches them. */
  @Test
  void showAttributesEntry() throws ClassDiagramParseException {
    open(CLASS);
    JCheckBoxMenuItem shows = (JCheckBoxMenuItem) ((javax.swing.JMenu) menuEntries(start -> null).get(4)).getMenuComponent(0);
    assertTrue(shows.isSelected());

    shows.doClick(0);

    assertTrue(parsedClass().attributesHidden);
    assertFalse(((JCheckBoxMenuItem) ((javax.swing.JMenu) menuEntries(start -> null).get(4)).getMenuComponent(0)).isSelected());
    assertFalse(menuEntries(start -> null).get(2).isEnabled());
  }
}
