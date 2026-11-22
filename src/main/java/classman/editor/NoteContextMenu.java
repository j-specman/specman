package classman.editor;

import classman.model.v001.NoteModel_V001;

import javax.swing.JPopupMenu;

/** The menu of a note, which only switches back to the width which follows the text. */
class NoteContextMenu extends JPopupMenu {

  /** @param showDiagram shows the diagram as the editing has it now */
  NoteContextMenu(DiagramEditing editing, NoteModel_V001 note, EditorTexts texts, Runnable showDiagram) {
    add(new AutomaticWidthMenuItem(editing, note, texts, showDiagram));
  }
}
