package specman.undo;

import specman.metatag.generic.GenericMetaTag;
import specman.view.AbstractSchrittView;

/** Base of the undoable assignment and removal of a tag at a step. Both keep the very tag
 * instance, so a removed and restored tag still has its freetext, and the tag's position among
 * the other tags of the step, so it returns to the place it had. */
public abstract class AbstractUndoableMetaTagAssignment extends AbstractUndoableInteraction {
  private final AbstractSchrittView step;
  private final GenericMetaTag tag;
  private final int index;

  protected AbstractUndoableMetaTagAssignment(AbstractSchrittView step, GenericMetaTag tag, int index) {
    this.step = step;
    this.tag = tag;
    this.index = index;
  }

  protected void assign() {
    step.addTag(tag, index);
  }

  protected void unassign() {
    step.removeTag(tag);
  }
}
