package classman.editor;

import classman.model.v001.ElementStyleModel_V001;
import classman.model.v001.NoteModel_V001;

import javax.swing.JPopupMenu;
import java.util.function.UnaryOperator;

/** The menu of a note: its style, and switching back to the width which follows the text. */
class NoteContextMenu extends JPopupMenu {

  /** @param stylePicker asks the user for a style, given a copy of the one to start from, and gives null if the user
   * doesn't choose one
   * @param showDiagram shows the diagram as the editing has it now */
  NoteContextMenu(DiagramEditing editing, NoteModel_V001 note, EditorTexts texts,
                  UnaryOperator<ElementStyleModel_V001> stylePicker, Runnable showDiagram) {
    add(new StyleMenuEntry(editing, note, texts, stylePicker, showDiagram));
    add(new AutomaticWidthMenuItem(editing, note, texts, showDiagram));
  }
}
