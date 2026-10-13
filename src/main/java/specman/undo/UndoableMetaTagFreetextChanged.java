package specman.undo;

import specman.EditException;
import specman.metatag.generic.GenericMetaTag;
import specman.undo.manager.UndoRedoFeedbackHider;

public class UndoableMetaTagFreetextChanged extends AbstractUndoableInteraction implements UndoRedoFeedbackHider {
  private final GenericMetaTag metaTag;
  private final String oldFreetext;
  private final String newFreetext;

  public UndoableMetaTagFreetextChanged(GenericMetaTag metaTag, String oldFreetext, String newFreetext) {
    this.metaTag = metaTag;
    this.oldFreetext = oldFreetext;
    this.newFreetext = newFreetext;
  }

  /** Shows the changed tag's freetext until the next undo/redo, so the user sees what just changed. */
  @Override
  protected void undoEdit() throws EditException {
    metaTag.setFreetext(oldFreetext);
    metaTag.showFreetextFeedback();
  }

  @Override
  protected void redoEdit() throws EditException {
    metaTag.setFreetext(newFreetext);
    metaTag.showFreetextFeedback();
  }

  @Override
  public void hideFeedback() {
    metaTag.hideFreetextFeedback();
  }
}
