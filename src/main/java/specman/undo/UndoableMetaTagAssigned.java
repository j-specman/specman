package specman.undo;

import specman.EditException;
import specman.metatag.generic.GenericMetaTag;
import specman.view.AbstractSchrittView;

public class UndoableMetaTagAssigned extends AbstractUndoableInteraction {
  private final AbstractSchrittView step;
  private final GenericMetaTag tag;

  public UndoableMetaTagAssigned(AbstractSchrittView step, GenericMetaTag tag) {
    this.step = step;
    this.tag = tag;
  }

  @Override
  protected void undoEdit() throws EditException {
    step.removeTag(tag);
  }

  @Override
  protected void redoEdit() throws EditException {
    step.addTag(tag);
  }
}
