package specman.undo;

import specman.EditException;
import specman.metatag.TagMoveDirection;
import specman.metatag.generic.GenericMetaTag;
import specman.view.AbstractSchrittView;

public class UndoableMetaTagMoved extends AbstractUndoableInteraction {
  private final AbstractSchrittView step;
  private final GenericMetaTag tag;
  private final TagMoveDirection direction;

  public UndoableMetaTagMoved(AbstractSchrittView step, GenericMetaTag tag, TagMoveDirection direction) {
    this.step = step;
    this.tag = tag;
    this.direction = direction;
  }

  @Override
  protected void undoEdit() throws EditException {
    step.moveTag(tag, direction.opposite());
  }

  @Override
  protected void redoEdit() throws EditException {
    step.moveTag(tag, direction);
  }
}
