package classman.editor;

import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import org.junit.jupiter.api.Test;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Hiding single attributes, and showing them again, from the context menu of the class. */
class HiddenAttributeEditingTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final String CLASS = "classdiagram {\n  class A (11111111) at(10, 20) {\n    -x\n    -y\n    -z\n  }\n}";

  private final RecordingEditorHost host = new RecordingEditorHost();
  private DiagramEditing editing;

  private void open(String source) throws ClassDiagramParseException {
    editing = new DiagramEditing(source, host);
    editing.selectAt(new Point2D.Double(editing.scene().elementAreas.get(0).area.getCenterX(),
      editing.scene().elementAreas.get(0).area.getMinY() + 5));
  }

  private ClassModel_V001 parsedClass() throws ClassDiagramParseException {
    return new ClassDiagramParser_V001().parse(editing.source()).classes().get(0);
  }

  private AttributeModel_V001 attribute(String name) {
    return editing.selectedClass().attributes.stream().filter(a -> name.equals(a.name)).findFirst().orElseThrow();
  }

  private JMenu submenu(String text) {
    ClassContextMenu menu = new ClassContextMenu(editing, editing.selectedClass(), null,
      new EditorTexts(host.locale()), start -> null, () -> { });
    return Arrays.stream(menu.getComponents()).filter(c -> c instanceof JMenu item && text.equals(item.getText()))
      .map(c -> (JMenu) c).findFirst().orElse(null);
  }

  /** Hiding is one operation which writes the mark at the end of the line, and leaves a box which is as high as
   * before when the ". . ." replaces the attribute. */
  @Test
  void hideOneAttribute() throws ClassDiagramParseException {
    open(CLASS);
    double heightWith = editing.selectedArea().getHeight();

    editing.hideMember(editing.selectedClass(), attribute("y"));

    assertTrue(parsedClass().attributes.get(1).hidden);
    assertFalse(parsedClass().attributes.get(0).hidden);
    assertEquals(3, parsedClass().attributes.size());
    assertEquals(1, host.reportedSources.size());
    assertTrue(editing.canUndo());
    assertEquals(heightWith, editing.selectedArea().getHeight(), 0.001);
    assertTrue(editing.scene().textAreas.stream().noneMatch(text -> text.member != null && "y".equals(text.member.name)));
  }

  /** Hiding is no change: nothing is marked while changes are tracked, and an attribute which carries marks can be
   * hidden, its marks are kept. */
  @Test
  void hidingIsNotMarked() throws ClassDiagramParseException {
    host.trackedChangeSet = "yellow";
    open("classdiagram {\n  class A (11111111) at(10, 20) {\n    -x change(added, blue)\n    -y\n  }\n}");

    editing.hideMember(editing.selectedClass(), attribute("x"));
    editing.hideMember(editing.selectedClass(), attribute("y"));

    assertTrue(parsedClass().attributes.get(0).hidden);
    assertEquals("blue", parsedClass().attributes.get(0).change.changeSet);
    assertFalse(editing.source().contains("yellow"), editing.source());
  }

  /** The line "...", a hidden attribute and an attribute of a removed class can't be hidden. */
  @Test
  void whatCannotBeHidden() throws ClassDiagramParseException {
    open("classdiagram {\n  class A (11111111) at(10, 20) {\n    -x (hidden)\n    ...\n  }\n}");

    assertFalse(editing.canHideMember(editing.selectedClass(), attribute("x")).can());
    assertFalse(editing.canHideMember(editing.selectedClass(), editing.selectedClass().attributes.get(1)).can());
    assertFalse(editing.canHideMember(editing.selectedClass(), null).can());

    open("classdiagram {\n  class A (11111111) change(removed, yellow) at(10, 20) {\n    -x\n  }\n}");

    assertFalse(editing.canHideMember(editing.selectedClass(), attribute("x")).can());
  }

  /** Showing takes the mark away, the attribute is back in its place. */
  @Test
  void showAgain() throws ClassDiagramParseException {
    open(CLASS);
    editing.hideMember(editing.selectedClass(), attribute("y"));

    editing.showMember(editing.selectedClass(), attribute("y"));

    assertFalse(editing.source().contains("hidden"), editing.source());
    assertTrue(editing.scene().textAreas.stream().anyMatch(text -> text.member == attribute("y")));
  }

  private static List<String> checked(JMenu menu) {
    return Arrays.stream(menu.getMenuComponents()).map(c -> (JCheckBoxMenuItem) c)
      .map(item -> item.getText() + (item.isSelected() ? "+" : "-")).toList();
  }

  /** One submenu holds a check item for each attribute, checked while it is shown. Choosing an item hides or shows
   * that attribute, and the item shows the new state. */
  @Test
  void visibleSubmenuSwitchesAttributes() throws ClassDiagramParseException {
    open(CLASS);
    assertEquals(List.of("x+", "y+", "z+"), checked(submenu("Hide / show attribute")));

    ((JMenuItem) submenu("Hide / show attribute").getMenuComponents()[1]).doClick(0);

    assertTrue(parsedClass().attributes.get(1).hidden);
    assertEquals(List.of("x+", "y-", "z+"), checked(submenu("Hide / show attribute")));

    JMenu menu = submenu("Hide / show attribute");
    ((JMenuItem) menu.getMenuComponents()[1]).doClick(0);

    assertFalse(editing.source().contains("hidden"), editing.source());
    assertEquals(List.of("x+", "y+", "z+"), checked(menu));
  }

  /** Several items can be switched with the same menu, and a hidden attribute is hidden for the others. */
  @Test
  void severalSwitchedInOneMenu() throws ClassDiagramParseException {
    open(CLASS);
    JMenu menu = submenu("Hide / show attribute");

    ((JMenuItem) menu.getMenuComponents()[0]).doClick(0);
    ((JMenuItem) menu.getMenuComponents()[2]).doClick(0);

    assertEquals(List.of("x-", "y+", "z-"), checked(menu));
    assertTrue(parsedClass().attributes.get(0).hidden);
    assertTrue(parsedClass().attributes.get(2).hidden);
    assertEquals(2, host.reportedSources.size());
  }

  /** The submenu is there on every class and can't be opened if nothing can be switched: a class without
   * attributes. The line ". . ." has no item, and the items of a class which a change set has removed can't be chosen. */
  @Test
  void visibleSubmenuWithoutAttributes() throws ClassDiagramParseException {
    open("classdiagram {\n  class A (11111111) at(10, 20)\n}");
    assertFalse(submenu("Hide / show attribute").isEnabled());

    open("classdiagram {\n  class A (11111111) at(10, 20) {\n    -x\n    ...\n  }\n}");
    assertEquals(List.of("x+"), checked(submenu("Hide / show attribute")));

    open("classdiagram {\n  class A (11111111) change(removed, yellow) at(10, 20) {\n    -x\n  }\n}");
    assertEquals(List.of("x+"), checked(submenu("Hide / show attribute")));
    assertFalse(submenu("Hide / show attribute").getMenuComponent(0).isEnabled());

    open(CLASS);
    assertTrue(submenu("Hide / show attribute").isEnabled());
  }

  /** For an enum the entries talk about values. */
  @Test
  void enumTalksAboutValues() throws ClassDiagramParseException {
    open("classdiagram {\n  enum E (11111111) at(10, 20) {\n    RED (hidden)\n    GREEN\n  }\n}");

    assertEquals(List.of("RED-", "GREEN+"), checked(submenu("Hide / show value")));
  }
}
