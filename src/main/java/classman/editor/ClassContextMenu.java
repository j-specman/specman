package classman.editor;

import classman.model.v001.ClassModel_V001;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/** The menu of a class: it adds the parts which a class can have. An entry which doesn't fit the class right now, like
 * a second stereotype, is there but can't be chosen. Each part is typed into its place in the class right away. */
class ClassContextMenu extends JPopupMenu {

  ClassContextMenu(DiagramEditing editing, ClassModel_V001 box, TextEditor textEditor, EditorTexts texts) {
    add(entry(texts.get("menu.class.add.stereotype"), editing.canAddStereotypeTo(box),
      () -> textEditor.startNewStereotype(box)));
    add(entry(texts.get("menu.class.add.attribute"), editing.canAddPartsTo(box),
      () -> textEditor.startNewAttribute(box)));
  }

  private static JMenuItem entry(String label, boolean enabled, Runnable action) {
    JMenuItem item = new JMenuItem(label);
    item.setEnabled(enabled);
    item.addActionListener(e -> action.run());
    return item;
  }
}
