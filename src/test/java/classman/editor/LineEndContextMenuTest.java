package classman.editor;

import classman.model.v001.RelationEnd_V001;
import org.junit.jupiter.api.Test;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.awt.Component;
import java.awt.geom.Point2D;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The menu of an end handle offers the types of the end in a submenu, as far as they can be chosen. */
class LineEndContextMenuTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private static final String CLASSES = "classdiagram {\n  class A (11111111) at(10, 20)\n"
    + "  class B (22222222) at(300, 20)\n  enum E (33333333) at(300, 200)\n";

  private final RecordingEditorHost host = new RecordingEditorHost();
  private final EditorTexts texts = new EditorTexts(host.locale());
  private DiagramEditing editing;

  private void openAndSelect(String relation) throws Exception {
    editing = new DiagramEditing(CLASSES + relation + "\n}", host);
    List<Point2D> points = editing.scene().relationLines.get(0).points();
    editing.selectAt(new Point2D.Double((points.get(0).getX() + points.get(1).getX()) / 2,
      (points.get(0).getY() + points.get(1).getY()) / 2));
  }

  private LineEndContextMenu menuFor(RelationEnd_V001 end) {
    return new LineEndContextMenu(editing, end, texts, start -> null, () -> { });
  }

  private static JMenu typeMenuOf(LineEndContextMenu menu) {
    for (Component component : menu.getComponents()) {
      if (component instanceof JMenu submenu) {
        return submenu;
      }
    }
    return null;
  }

  /** The four types are in the submenu in the order navigable association, association, composition, aggregation. The
   * one which the end has is there but can't be chosen. */
  @Test
  void typesOfAnAssociationEnd() throws Exception {
    openAndSelect("association 11111111 -> 22222222");

    JMenu typeMenu = typeMenuOf(menuFor(RelationEnd_V001.TO));

    assertNotNull(typeMenu);
    assertEquals("Type", typeMenu.getText());
    assertEquals(List.of("Navigable association", "Association", "Composition", "Aggregation"),
      java.util.stream.IntStream.range(0, typeMenu.getItemCount()).mapToObj(i -> typeMenu.getItem(i).getText()).toList());
    assertFalse(typeMenu.getItem(0).isEnabled());
    assertTrue(typeMenu.getItem(1).isEnabled());
    assertTrue(typeMenu.getItem(2).isEnabled());
    assertTrue(typeMenu.getItem(3).isEnabled());
  }

  /** Choosing an entry changes the relation. A diamond can't be chosen at the end opposite of a diamond, and the
   * tooltip says why. */
  @Test
  void diamondOnlyOnce() throws Exception {
    openAndSelect("composition 11111111 -- 22222222");

    JMenu typeAtTarget = typeMenuOf(menuFor(RelationEnd_V001.TO));
    JMenuItem composition = typeAtTarget.getItem(2);

    assertFalse(composition.isEnabled());
    assertNotNull(composition.getToolTipText());
    assertTrue(typeAtTarget.getItem(0).isEnabled());

    typeAtTarget.getItem(0).doClick();
    assertEquals(1, host.reportedSources.size());
  }

  /** An enum, an inheritance and an anchor line have no types to choose, the submenu isn't there. */
  @Test
  void noSubmenuWhereNothingCanBeChosen() throws Exception {
    openAndSelect("association 11111111 -> 33333333");
    assertNull(typeMenuOf(menuFor(RelationEnd_V001.FROM)));

    openAndSelect("extends 11111111 -> 22222222");
    assertNull(typeMenuOf(menuFor(RelationEnd_V001.TO)));
  }
}
