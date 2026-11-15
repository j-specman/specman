package specman.undo;

import specman.EditException;
import specman.editarea.EditArea;
import specman.editarea.TextEditArea;
import specman.editarea.EditContainer;
import static specman.Specman.editor;

public class UndoableEditAreaRemoved extends AbstractUndoableInteraction {
  private final EditContainer editContainer;
  private final EditArea editArea;
  private final EditArea precedingArea;
  private final TextEditArea trailingTextArea;

  public UndoableEditAreaRemoved(EditContainer editContainer, EditArea precedingArea, EditArea editArea, TextEditArea trailingTextArea) {
    this.editContainer = editContainer;
    this.editArea = editArea;
    this.precedingArea = precedingArea;
    this.trailingTextArea = trailingTextArea;
  }

  @Override
  protected void undoEdit() throws EditException {
    editContainer.addEditAreaByUndoRedo(precedingArea, editArea, trailingTextArea);
    editor().diagrammAktualisieren(null);
  }

  @Override
  protected void redoEdit() throws EditException {
    editContainer.removeEditAreaByUndoRedo(editArea, trailingTextArea);
    editor().diagrammAktualisieren(null);
  }
}
