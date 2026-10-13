package specman.undo;

import specman.EditException;
import specman.metatag.generic.GenericMetaTag;

import java.util.List;

import static specman.Specman.editor;

/** The result of a "replace all" in the freetexts of the meta tags. Undoing or redoing it reports
 * the number of replacements by a single toast, instead of showing the freetext of every changed tag. */
public class UndoableMetaTagFreetextReplaceAll extends AbstractUndoableInteraction {
  public static class FreetextChange {
    private final GenericMetaTag tag;
    private final String oldFreetext;
    private final String newFreetext;

    public FreetextChange(GenericMetaTag tag, String oldFreetext, String newFreetext) {
      this.tag = tag;
      this.oldFreetext = oldFreetext;
      this.newFreetext = newFreetext;
    }
  }

  private final List<FreetextChange> changes;
  private final int replacements;

  public UndoableMetaTagFreetextReplaceAll(List<FreetextChange> changes, int replacements) {
    this.changes = changes;
    this.replacements = replacements;
  }

  @Override
  protected void undoEdit() throws EditException {
    changes.forEach(change -> change.tag.setFreetext(change.oldFreetext));
    editor().showToast("Undo: " + replacementsText() + " in tag freetexts reverted");
  }

  @Override
  protected void redoEdit() throws EditException {
    changes.forEach(change -> change.tag.setFreetext(change.newFreetext));
    editor().showToast("Redo: " + replacementsText() + " in tag freetexts redone");
  }

  private String replacementsText() {
    return replacements == 1 ? "1 replacement" : replacements + " replacements";
  }
}
