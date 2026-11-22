package classman.editor;

import classman.model.v001.AbstractBoxModel_V001;

import javax.swing.JCheckBoxMenuItem;

/** The entry of the menu of a class or note which switches back to the width which follows the content. It is checked
 * while the box has no width of its own, and can't be chosen then. */
class AutomaticWidthMenuItem extends JCheckBoxMenuItem {

  /** @param showDiagram shows the diagram as the editing has it now */
  AutomaticWidthMenuItem(DiagramEditing editing, AbstractBoxModel_V001 box, EditorTexts texts, Runnable showDiagram) {
    super(texts.get("menu.automatic.width"), !editing.hasFixedWidth(box));
    Can.IF(editing.hasFixedWidth(box), texts.get("reason.width.automatic")).applyTo(this);
    addActionListener(e -> {
      editing.resetWidth(box);
      showDiagram.run();
    });
  }
}
