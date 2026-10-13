package specman.undo;

import specman.EditException;
import specman.metatag.generic.GenericMetaTag;
import specman.view.AbstractSchrittView;

public class UndoableMetaTagAssigned extends AbstractUndoableMetaTagAssignment {

  /** @param index the position the tag got among the tags of the step */
  public UndoableMetaTagAssigned(AbstractSchrittView step, GenericMetaTag tag, int index) {
    super(step, tag, index);
  }

  @Override
  protected void undoEdit() throws EditException {
    unassign();
  }

  @Override
  protected void redoEdit() throws EditException {
    assign();
  }
}
