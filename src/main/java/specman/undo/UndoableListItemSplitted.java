package specman.undo;

import specman.EditException;
import specman.Specman;
import specman.editarea.AbstractListItemEditArea;
import specman.editarea.TextEditArea;
import static specman.Specman.editor;

public class UndoableListItemSplitted extends AbstractUndoableInteraction {
  private AbstractListItemEditArea initiatingArea, splitArea;
  private TextEditArea initiatingEditArea;

  public UndoableListItemSplitted(AbstractListItemEditArea initiatingArea, TextEditArea initiatingEditArea, AbstractListItemEditArea splitArea) {
    this.initiatingArea = initiatingArea;
    this.initiatingEditArea = initiatingEditArea;
    this.splitArea = splitArea;
  }

  @Override
  protected void undoEdit() throws EditException {
    initiatingArea.getParent().mergeListItemAreasByUndoRedo(initiatingArea, splitArea);
    editor().diagrammAktualisieren(initiatingArea);
  }

  @Override
  protected void redoEdit() throws EditException {
    initiatingArea.moveEditAreas(initiatingEditArea, splitArea);
    editor().diagrammAktualisieren(splitArea);
  }

}
