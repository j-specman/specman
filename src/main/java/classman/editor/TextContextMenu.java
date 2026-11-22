package classman.editor;

import classman.render.SceneTextArea;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;

/** The menu of the label or a multiplicity of a relation, which only gives the text back to the layout. The entry is
 * checked while the layout places the text, and can't be chosen then. */
class TextContextMenu extends JPopupMenu {

  /** @param showDiagram shows the diagram as the editing has it now */
  TextContextMenu(DiagramEditing editing, SceneTextArea text, EditorTexts texts, Runnable showDiagram) {
    JCheckBoxMenuItem automatic = new JCheckBoxMenuItem(texts.get("menu.text.position.automatic"),
      !editing.hasFixedPlace(text));
    Can.IF(editing.hasFixedPlace(text), texts.get("reason.text.position.automatic")).applyTo(automatic);
    automatic.addActionListener(e -> {
      editing.resetPlace(text);
      showDiagram.run();
    });
    add(automatic);
  }
}
