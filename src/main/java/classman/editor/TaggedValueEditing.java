package classman.editor;

import classman.model.v001.ClassModel_V001;
import classman.model.v001.TaggedValueModel_V001;
import classman.model.v001.change.TaggedValueChanger_V001;
import classman.model.v001.change.TaggedValueFormat_V001;
import classman.model.v001.change.TaggedValueLine_V001;
import classman.render.SceneTextArea;

import java.util.Optional;

/**
 * Changes, adds and deletes the tagged values of the classes of a diagram, one row of KEY=value at a time. A new
 * tagged value is typed into a blank row which is put after the last one while it is typed: a tagged value without key
 * and value, which is not part of the diagram until the row is committed, and goes away if it is dropped.
 */
class TaggedValueEditing {
  private final TaggedValueFormat_V001 format = new TaggedValueFormat_V001();
  private final TaggedValueChanger_V001 changer = new TaggedValueChanger_V001();
  private ClassModel_V001 pendingOwner;
  private TaggedValueModel_V001 pendingTag;

  /** @return the text which the row is shown as, empty for the blank row of a new tagged value */
  String textOf(SceneTextArea row) {
    return row.taggedValue == pendingTag ? "" : format.textOf(row.taggedValue);
  }

  /** @return false if the text is no tagged value: it needs a key, an equals sign and a value. The empty text is
   * fine, it deletes the tagged value. */
  boolean accepts(String newText) {
    return newText.isEmpty() || format.parse(newText).isPresent();
  }

  /** Gives the row its new content: the tagged value changes, a new one comes, or it is deleted if the text is empty.
   * @param changeSet the change set which tracks the changes, null if changes are not tracked
   * @return true if the diagram has changed, false if only the blank row has gone away again */
  boolean setText(SceneTextArea row, String newText, String changeSet) {
    ClassModel_V001 owner = (ClassModel_V001) row.element;
    if (row.taggedValue == pendingTag) {
      int index = owner.taggedValues.indexOf(pendingTag);
      discardPending();
      if (newText.isEmpty()) {
        return false;
      }
      owner.taggedValues.add(index, changer.create(owner, parsed(newText), changeSet));
      return true;
    }
    if (newText.isEmpty()) {
      changer.delete(owner, row.taggedValue, changeSet);
    }
    else {
      changer.apply(owner, row.taggedValue, parsed(newText), changeSet);
    }
    return true;
  }

  private TaggedValueLine_V001 parsed(String text) {
    Optional<TaggedValueLine_V001> line = format.parse(text);
    return line.orElseThrow(() -> new IllegalArgumentException("Not a tagged value: " + text));
  }

  /** Puts a blank row after the last tagged value of the class, for a new one to be typed into.
   * @return the tagged value which stands for the blank row */
  TaggedValueModel_V001 beginAtEnd(ClassModel_V001 owner) {
    discardPending();
    pendingOwner = owner;
    pendingTag = new TaggedValueModel_V001("", "");
    owner.taggedValues.add(pendingTag);
    return pendingTag;
  }

  /** Takes the blank row away again if it is still there.
   * @return true if there was one */
  boolean discardPending() {
    if (pendingTag == null) {
      return false;
    }
    pendingOwner.taggedValues.remove(pendingTag);
    pendingOwner = null;
    pendingTag = null;
    return true;
  }
}
