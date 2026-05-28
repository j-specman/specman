package specman.undo;

import specman.EditException;
import specman.Specman;
import specman.editarea.AbstractListItemEditArea;
import specman.editarea.EditArea;
import specman.editarea.EditContainer;
import specman.editarea.TextEditArea;
import static specman.Specman.editor;

import java.util.List;

public class UndoableListItemDissolved extends AbstractUndoableInteraction {
  private EditContainer editContainer;
  private AbstractListItemEditArea liEditArea;
  private int liEditAreaIndex;
  private List<EditArea> liftUpAreas;
  private TextEditArea followingTextEditArea;

  public UndoableListItemDissolved(EditContainer editContainer, AbstractListItemEditArea liEditArea, int liEditAreaIndex, List<EditArea> liftUpAreas, TextEditArea followingTextEditArea) {
    this.editContainer = editContainer;
    this.liEditArea = liEditArea;
    this.liEditAreaIndex = liEditAreaIndex;
    this.liftUpAreas = liftUpAreas;
    this.followingTextEditArea = followingTextEditArea;
  }

  @Override
  protected void undoEdit() throws EditException {
    editContainer.undoDissolveListItemEditArea(liEditArea, liEditAreaIndex, liftUpAreas, followingTextEditArea);
    editor().diagrammAktualisieren(liEditArea);
  }

  @Override
  protected void redoEdit() throws EditException {
    EditArea nextFocused = editContainer.redoDissolveListItemEditArea(liEditArea, liEditAreaIndex, liftUpAreas, followingTextEditArea);
    editor().diagrammAktualisieren(nextFocused);
  }
}
