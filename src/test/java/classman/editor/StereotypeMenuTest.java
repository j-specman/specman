package classman.editor;

import classman.model.v001.ClassModel_V001;
import classman.render.StereotypeLook;
import org.junit.jupiter.api.Test;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The stereotype menu of a class: free text, the configurations of the host, removing the stereotype. */
class StereotypeMenuTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final String BARE = "classdiagram {\n  class A (11111111) at(10, 60)\n}";
  private static final String WITH_CONFIG = "classdiagram {\n  class A (11111111) at(10, 60) <<:Domain_Value>>\n}";
  private static final String WITH_FREE_TEXT = "classdiagram {\n  class A (11111111) at(10, 60) <<Entity>>\n}";

  private final RecordingEditorHost host = new RecordingEditorHost();
  private final EditorTexts texts = new EditorTexts(host.locale());
  private DiagramEditing editing;
  private TextEditor textEditor;

  private void open(String source) throws Exception {
    host.stereotypeLooks.put("Domain_Value", new StereotypeLook("Domain Value", null));
    host.stereotypeLooks.put("Icon_Only", new StereotypeLook("", null));
    SwingUtilities.invokeAndWait(() -> {
      try {
        editing = new DiagramEditing(source, host);
      }
      catch (Exception e) {
        throw new IllegalStateException(e);
      }
      textEditor = new TextEditor(new DiagramCanvas(editing.scene()), editing, () -> { });
    });
    Rectangle2D area = editing.scene().elementAreas.get(0).area;
    editing.selectAt(new Point2D.Double(area.getCenterX(), area.getMaxY() - 2));
  }

  /** The entries of the submenu in order, a separator as null. */
  private List<JMenuItem> entries() {
    ClassModel_V001 box = editing.selectedClass();
    JMenu menu = new StereotypeMenu(editing, box, textEditor, texts, () -> { });
    List<JMenuItem> items = new ArrayList<>();
    for (Component component : menu.getPopupMenu().getComponents()) {
      items.add(component instanceof JMenuItem item ? item : null);
    }
    return items;
  }

  @Test
  void freeTextThenConfigsThenRemove() throws Exception {
    open(BARE);

    List<String> labels = entries().stream().map(item -> item == null ? "-" : item.getText()).toList();

    assertEquals(List.of("Free text", "-", "Domain_Value", "Icon_Only", "-", "Remove"), labels);
  }

  @Test
  void withoutConfigsOnlyFreeTextAndRemoveRemain() throws Exception {
    open(BARE);
    host.stereotypeLooks.clear();

    List<String> labels = entries().stream().map(item -> item == null ? "-" : item.getText()).toList();

    assertEquals(List.of("Free text", "-", "Remove"), labels);
  }

  @Test
  void entryOfCurrentKindIsSelected() throws Exception {
    open(WITH_CONFIG);
    assertFalse(((javax.swing.JRadioButtonMenuItem) entries().get(0)).isSelected());
    assertTrue(((javax.swing.JRadioButtonMenuItem) entries().get(2)).isSelected());

    open(WITH_FREE_TEXT);
    assertTrue(((javax.swing.JRadioButtonMenuItem) entries().get(0)).isSelected());
    assertFalse(((javax.swing.JRadioButtonMenuItem) entries().get(2)).isSelected());
  }

  @Test
  void choosingConfigGivesReference() throws Exception {
    open(BARE);

    SwingUtilities.invokeAndWait(() -> entries().get(2).doClick(0));

    assertEquals(":Domain_Value", editing.selectedClass().stereotype);
  }

  @Test
  void freeTextOnReferenceOpensEmptyField() throws Exception {
    open(WITH_CONFIG);

    SwingUtilities.invokeAndWait(() -> entries().get(0).doClick(0));

    assertTrue(textEditor.isEditing());
    assertEquals("", editing.textOf(editing.scene().textAreas.stream()
      .filter(text -> text.kind == classman.render.TextKind.STEREOTYPE).findFirst().orElseThrow()));
  }

  @Test
  void removeIsOnlyOfferedWithStereotype() throws Exception {
    open(BARE);
    assertFalse(entries().get(entries().size() - 1).isEnabled());

    open(WITH_CONFIG);
    JMenuItem remove = entries().get(entries().size() - 1);
    assertTrue(remove.isEnabled());
    SwingUtilities.invokeAndWait(() -> remove.doClick(0));

    assertNull(editing.selectedClass().stereotype);
  }

  @Test
  void removedClassOffersNothing() throws Exception {
    open("classdiagram {\n  class A (11111111) change(removed, yellow) at(10, 60)\n}");

    ClassContextMenu menu = new ClassContextMenu(editing, editing.selectedClass(), textEditor, texts, start -> null,
      () -> { });
    JMenuItem stereotype = (JMenuItem) menu.getComponent(0);

    assertFalse(stereotype.isEnabled());
  }
}
