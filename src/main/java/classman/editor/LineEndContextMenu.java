package classman.editor;

import classman.model.v001.ElementStyleModel_V001;
import classman.model.v001.RelationEnd_V001;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.util.function.UnaryOperator;

/** The menu of the handle at an end of a selected relation or anchor line. For a relation it says whether the relation
 * is navigable towards that end (the arrowhead there), and opens the dialog for the style of the relation, so that
 * there is one menu less to know: it is the only menu on a relation. For both it links the end to the center of its
 * box. An entry which doesn't fit the end right now is there but can't be chosen, and its tooltip says why. */
class LineEndContextMenu extends JPopupMenu {

  /** @param stylePicker asks the user for the style of a relation, given a copy of the one to start from, and gives
   * null if the user doesn't choose one
   * @param showDiagram shows the diagram as the editing has it now */
  LineEndContextMenu(DiagramEditing editing, RelationEnd_V001 end, EditorTexts texts,
                     UnaryOperator<ElementStyleModel_V001> stylePicker, Runnable showDiagram) {
    if (editing.isRelationSelected()) {
      JCheckBoxMenuItem navigable = new JCheckBoxMenuItem(texts.get("menu.line.end.navigable"), editing.hasArrowAt(end));
      editing.canSwitchArrowAt(end).applyTo(navigable);
      navigable.addActionListener(e -> {
        editing.toggleArrowAt(end);
        showDiagram.run();
      });
      add(navigable);
    }
    JMenuItem center = new JMenuItem(texts.get("menu.line.end.center"));
    editing.canCenterEnd(end).applyTo(center);
    center.addActionListener(e -> {
      editing.centerEnd(end);
      showDiagram.run();
    });
    add(center);
    if (editing.isRelationSelected()) {
      addSeparator();
      add(new StyleMenuEntry(editing, editing.selectedRelation(), texts, stylePicker, showDiagram));
    }
  }
}
