package specman.undo;

import specman.EditException;
import specman.Specman;
import specman.editarea.EditArea;
import specman.editarea.EditContainer;
import specman.editarea.TableEditArea;
import specman.editarea.TextEditArea;
import static specman.Specman.editor;

public class UndoableEditAreaAdded extends AbstractUndoableInteraction {
  private final EditContainer editContainer;
  private final EditArea editArea;
  private final EditArea initiatingEditArea;
  private final TextEditArea cutOffTextArea;

  public UndoableEditAreaAdded(EditContainer editContainer, EditArea initiatingEditArea, EditArea editArea, TextEditArea cutOffTextArea) {
    this.editContainer = editContainer;
    this.editArea = editArea;
    this.initiatingEditArea = initiatingEditArea;
    this.cutOffTextArea = cutOffTextArea;
  }

  @Override
  protected void undoEdit() throws EditException {
    editContainer.removeEditAreaByUndoRedo(editArea, cutOffTextArea);
    editor().diagrammAktualisieren(initiatingEditArea);
  }

  @Override
  protected void redoEdit() throws EditException {
    editContainer.addEditAreaByUndoRedo(initiatingEditArea, editArea, cutOffTextArea);
    editor().diagrammAktualisieren(editArea);
  }
}
