package classman.editor;

import classman.model.v001.RelationEnd_V001;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/** The menu of the handle at an end of a selected relation or anchor line. For a relation it says whether the relation
 * is navigable towards that end (the arrowhead there). For both it links the end to the center of its box. An entry
 * which doesn't fit the end right now is there but can't be chosen. */
class LineEndContextMenu extends JPopupMenu {

  /** @param showDiagram shows the diagram as the editing has it now */
  LineEndContextMenu(DiagramEditing editing, RelationEnd_V001 end, EditorTexts texts, Runnable showDiagram) {
    if (editing.isRelationSelected()) {
      JCheckBoxMenuItem navigable = new JCheckBoxMenuItem(texts.get("menu.line.end.navigable"), editing.hasArrowAt(end));
      navigable.setEnabled(editing.canSwitchArrowAt(end));
      navigable.addActionListener(e -> {
        editing.toggleArrowAt(end);
        showDiagram.run();
      });
      add(navigable);
    }
    JMenuItem center = new JMenuItem(texts.get("menu.line.end.center"));
    center.setEnabled(editing.canCenterEnd(end));
    center.addActionListener(e -> {
      editing.centerEnd(end);
      showDiagram.run();
    });
    add(center);
  }
}
