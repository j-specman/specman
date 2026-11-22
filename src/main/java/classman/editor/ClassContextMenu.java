package classman.editor;

import classman.model.v001.ClassModel_V001;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/** The menu of a class: it adds the parts which a class can have, each typed into its place in the class right away,
 * and switches back to the width which follows the content. An entry which doesn't fit the class right now, like a
 * second stereotype, is there but can't be chosen. */
class ClassContextMenu extends JPopupMenu {

  /** @param showDiagram shows the diagram as the editing has it now */
  ClassContextMenu(DiagramEditing editing, ClassModel_V001 box, TextEditor textEditor, EditorTexts texts,
                   Runnable showDiagram) {
    add(entry(texts.get("menu.class.add.stereotype"), editing.canAddStereotypeTo(box),
      () -> textEditor.startNewStereotype(box)));
    add(entry(texts.get("menu.class.add.taggedvalue"), editing.canAddPartsTo(box),
      () -> textEditor.startNewTaggedValue(box)));
    add(entry(texts.get("menu.class.add.attribute"), editing.canAddPartsTo(box),
      () -> textEditor.startNewAttribute(box)));
    addSeparator();
    JCheckBoxMenuItem automaticWidth = new JCheckBoxMenuItem(texts.get("menu.class.automatic.width"),
      !editing.hasFixedWidth(box));
    automaticWidth.setEnabled(editing.hasFixedWidth(box));
    automaticWidth.addActionListener(e -> {
      editing.resetWidth(box);
      showDiagram.run();
    });
    add(automaticWidth);
  }

  private static JMenuItem entry(String label, boolean enabled, Runnable action) {
    JMenuItem item = new JMenuItem(label);
    item.setEnabled(enabled);
    item.addActionListener(e -> action.run());
    return item;
  }
}
