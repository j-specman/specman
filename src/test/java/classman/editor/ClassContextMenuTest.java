package classman.editor;

import classman.model.v001.RelationType_V001;
import classman.model.v001.ClassModel_V001;
import classman.render.TextKind;
import org.junit.jupiter.api.Test;

import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassContextMenuTest {
  static {
    // The scene is measured with the real fonts, which touch AWT and read this property once.
    System.setProperty("java.awt.headless", "true");
  }

  private static final String BARE = "classdiagram {\n  class A (11111111) at(10, 60)\n  note (22222222) at(300, 60) \"n\"\n}";
  private static final String WITH_STEREOTYPE = "classdiagram {\n  class A (11111111) at(10, 60) <<Entity>>\n}";

  private final RecordingEditorHost host = new RecordingEditorHost();
  private final EditorTexts texts = new EditorTexts(host.locale());
  private final List<Point2D> menusRequestedAt = new ArrayList<>();
  private DiagramEditing editing;
  private DiagramCanvas canvas;
  private TextEditor textEditor;
  private PointerControl control;

  private void open(String source) throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      try {
        editing = new DiagramEditing(source, host);
      }
      catch (Exception e) {
        throw new IllegalStateException(e);
      }
      canvas = new DiagramCanvas(editing.scene());
      textEditor = new TextEditor(canvas, editing, () -> canvas.setScene(editing.scene()));
      control = new PointerControl(editing, () -> { }, text -> { }, menusRequestedAt::add, pixels -> pixels);
    });
  }

  private Point2D insideClass() {
    Rectangle2D area = editing.scene().elementAreas.get(0).area;
    return new Point2D.Double(area.getCenterX(), area.getMaxY() - 2);
  }

  private List<JMenuItem> entries() {
    ClassContextMenu menu = new ClassContextMenu(editing, editing.selectedClass(), textEditor, texts, start -> null, () -> { });
    List<JMenuItem> items = new ArrayList<>();
    for (java.awt.Component component : menu.getComponents()) {
      if (component instanceof JMenuItem item) {
        items.add(item);
      }
    }
    return items;
  }

  /** The menu request on a class selects it and shows the menu there. */
  @Test
  void requestOnClassSelectsAndShows() throws Exception {
    open(BARE);

    control.contextMenuRequested(insideClass());

    assertNotNull(editing.selectedClass());
    assertEquals(List.of(insideClass()), menusRequestedAt);
  }

  /** Where there is no class or note, there is no menu: on empty space. The selection follows the click, as with any
   * press. */
  @Test
  void noMenuElsewhere() throws Exception {
    open(BARE);
    control.contextMenuRequested(insideClass());
    menusRequestedAt.clear();

    control.contextMenuRequested(new Point2D.Double(2000, 2000));
    assertFalse(editing.hasSelection());
  }

  /** A note has a menu with its style and the switch back to the automatic width, which is not offered as long as it
   * is on. */
  @Test
  void noteMenuHasStyleAndAutomaticWidth() throws Exception {
    open(BARE);

    control.contextMenuRequested(new Point2D.Double(310, 70));

    assertNotNull(editing.selectedNote());
    assertEquals(1, menusRequestedAt.size());
    NoteContextMenu menu = new NoteContextMenu(editing, editing.selectedNote(), texts, start -> null, () -> { });
    assertEquals(2, menu.getComponentCount());
    assertEquals("Style...", ((JMenuItem) menu.getComponent(0)).getText());
    assertTrue(((JMenuItem) menu.getComponent(0)).isEnabled());
    JMenuItem width = (JMenuItem) menu.getComponent(1);
    assertEquals("Automatic width", width.getText());
    assertFalse(width.isEnabled());
  }

  /** While the tool for relations is armed, a click on a class means something else. */
  @Test
  void noMenuWhileToolIsArmed() throws Exception {
    open(BARE);
    editing.toggleRelationTool(RelationType_V001.ASSOCIATION);

    control.contextMenuRequested(insideClass());

    assertTrue(menusRequestedAt.isEmpty());
    assertFalse(editing.hasSelection());
  }

  /** A class without parts can get a stereotype and an attribute. */
  @Test
  void bareClassCanGetBoth() throws Exception {
    open(BARE);
    editing.selectAt(insideClass());

    List<JMenuItem> entries = entries();

    assertEquals(List.of("Add stereotype", "Add tagged value", "Add attribute", "Hide / show attribute", "Style...",
      "Show attributes", "Automatic width"),
      entries.stream().map(JMenuItem::getText).toList());
    assertTrue(entries.get(0).isEnabled());
    assertTrue(entries.get(1).isEnabled());
    assertTrue(entries.get(2).isEnabled());
  }

  /** A class which has a stereotype can't get a second one, but can get attributes. */
  @Test
  void secondStereotypeIsNotOffered() throws Exception {
    open(WITH_STEREOTYPE);
    editing.selectAt(insideClass());

    List<JMenuItem> entries = entries();

    assertFalse(entries.get(0).isEnabled());
    assertTrue(entries.get(1).isEnabled());
  }

  /** The entry for the automatic width is checked and can't be chosen while the class has none of its own. When it
   * has, the entry takes it away. */
  @Test
  void automaticWidthEntry() throws Exception {
    open(BARE);
    editing.selectAt(insideClass());
    JMenuItem automatic = entries().get(6);
    assertTrue(((javax.swing.JCheckBoxMenuItem) automatic).isSelected());
    assertFalse(automatic.isEnabled());

    open("classdiagram {\n  class A (11111111) at(10, 60) width=200\n}");
    editing.selectAt(insideClass());
    automatic = entries().get(6);
    assertFalse(((javax.swing.JCheckBoxMenuItem) automatic).isSelected());
    assertTrue(automatic.isEnabled());

    automatic.doClick(0);

    assertFalse(editing.source().contains("width"), editing.source());
  }

  /** The stereotype entry opens a field in the row which the class got for it. */
  @Test
  void stereotypeEntryStartsField() throws Exception {
    open(BARE);
    editing.selectAt(insideClass());

    SwingUtilities.invokeAndWait(() -> entries().get(0).doClick(0));

    assertTrue(textEditor.isEditing());
    assertEquals(1, canvas.getComponentCount());
    assertEquals("", ((JTextComponent) canvas.getComponent(0)).getText());
    assertTrue(editing.scene().textAreas.stream().anyMatch(text -> text.kind == TextKind.STEREOTYPE));
  }

  /** The tagged value entry opens a field in a new row after the tagged values, with the braces around it. */
  @Test
  void taggedValueEntryStartsField() throws Exception {
    open(BARE);
    editing.selectAt(insideClass());

    SwingUtilities.invokeAndWait(() -> entries().get(1).doClick(0));

    assertTrue(textEditor.isEditing());
    assertEquals("", ((JTextComponent) canvas.getComponent(0)).getText());
    assertTrue(editing.scene().textAreas.stream().anyMatch(text -> text.kind == TextKind.TAGGED_VALUE));
  }

  /** The attribute entry opens a field in a new line at the end of the class. */
  @Test
  void attributeEntryStartsField() throws Exception {
    open(BARE);
    editing.selectAt(insideClass());

    SwingUtilities.invokeAndWait(() -> entries().get(2).doClick(0));

    assertTrue(textEditor.isEditing());
    ClassModel_V001 box = editing.selectedClass();
    assertEquals(1, box.attributes.size());
  }
}
