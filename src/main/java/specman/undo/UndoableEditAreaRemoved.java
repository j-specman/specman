package specman.undo;

import specman.EditException;
import specman.Specman;
import specman.editarea.EditArea;
import specman.editarea.ImageEditArea;
import specman.editarea.TextEditArea;
import specman.editarea.EditContainer;
import static specman.Specman.editor;

public class UndoableEditAreaRemoved extends AbstractUndoableInteraction {
  private final EditContainer editContainer;
  private final EditArea editArea;
  private final TextEditArea leadingTextArea, trailingTextArea;

  public UndoableEditAreaRemoved(EditContainer editContainer, TextEditArea leadingTextArea, EditArea editArea, TextEditArea trailingTextArea) {
    this.editContainer = editContainer;
    this.editArea = editArea;
    this.leadingTextArea = leadingTextArea;
    this.trailingTextArea = trailingTextArea;
  }

  @Override
  protected void undoEdit() throws EditException {
    editContainer.addEditAreaByUndoRedo(leadingTextArea, editArea, trailingTextArea);
    editor().diagrammAktualisieren(null);
  }

  @Override
  protected void redoEdit() throws EditException {
    editContainer.removeEditAreaByUndoRedo(editArea, trailingTextArea);
    editor().diagrammAktualisieren(null);
  }
}
