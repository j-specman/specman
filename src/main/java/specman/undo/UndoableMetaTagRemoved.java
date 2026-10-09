package specman.undo;

import specman.EditException;
import specman.metatag.generic.GenericMetaTag;
import specman.view.AbstractSchrittView;

public class UndoableMetaTagRemoved extends AbstractUndoableMetaTagAssignment {

  /** @param index the position the tag had among the tags of the step before it was removed */
  public UndoableMetaTagRemoved(AbstractSchrittView step, GenericMetaTag tag, int index) {
    super(step, tag, index);
  }

  @Override
  protected void undoEdit() throws EditException {
    assign();
  }

  @Override
  protected void redoEdit() throws EditException {
    unassign();
  }
}
