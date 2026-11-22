package classman.editor;

import classman.model.v001.AttributeModel_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.change.AttributeChanger_V001;
import classman.model.v001.change.AttributeLineFormat_V001;
import classman.model.v001.change.AttributeLine_V001;
import classman.model.v001.change.ElementDeleter_V001;
import classman.render.SceneTextArea;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Changes, adds and deletes the attributes of the classes of a diagram, a line of text at a time. A new attribute is
 * typed into a blank line which is put at the end of the class while it is typed: an attribute without a name, which
 * is not part of the diagram until the line is committed, and goes away if it is dropped.
 */
class AttributeEditing {
  private final ClassDiagramModel_V001 model;
  private final AttributeLineFormat_V001 format = new AttributeLineFormat_V001();
  private final AttributeChanger_V001 changer = new AttributeChanger_V001();
  private final ElementDeleter_V001 deleter = new ElementDeleter_V001();
  private ClassModel_V001 pendingOwner;
  private AttributeModel_V001 pendingAttribute;

  AttributeEditing(ClassDiagramModel_V001 model) {
    this.model = model;
  }

  /** @return the line which the attribute is shown as, empty for the blank line of a class without attributes */
  String textOf(SceneTextArea line) {
    return line.attribute == null ? "" : format.textOf(line.attribute);
  }

  /** @return false if the line can't have the content: it has no name, or it would take away a visibility sign which
   * can't be marked as removed. The empty text is fine, it deletes the attribute. */
  boolean accepts(SceneTextArea line, String newText, String changeSet) {
    if (newText.isEmpty()) {
      return true;
    }
    Optional<AttributeLine_V001> parsed = parse((ClassModel_V001) line.element, newText);
    if (parsed.isEmpty()) {
      return false;
    }
    return line.attribute == null || line.attribute == pendingAttribute
      || changer.canApply((ClassModel_V001) line.element, line.attribute, parsed.get(), changeSet);
  }

  /** Gives the line its new content: the attribute changes, a new attribute comes, or the attribute is deleted if the
   * text is empty.
   * @param changeSet the change set which tracks the changes, null if changes are not tracked
   * @return true if the diagram has changed, false if only the blank line has gone away again */
  boolean setText(SceneTextArea line, String newText, String changeSet) {
    ClassModel_V001 owner = (ClassModel_V001) line.element;
    if (line.attribute != null && line.attribute == pendingAttribute) {
      return setPendingText(owner, newText, changeSet);
    }
    if (newText.isEmpty()) {
      if (line.attribute != null) {
        delete(owner, line.attribute, changeSet);
      }
    }
    else if (line.attribute == null) {
      owner.attributes.add(changer.create(owner, parsed(owner, newText), changeSet));
    }
    else {
      changer.apply(owner, line.attribute, parsed(owner, newText), changeSet);
    }
    return true;
  }

  private boolean setPendingText(ClassModel_V001 owner, String newText, String changeSet) {
    int index = owner.attributes.indexOf(pendingAttribute);
    discardPending();
    if (newText.isEmpty()) {
      return false;
    }
    owner.attributes.add(index, changer.create(owner, parsed(owner, newText), changeSet));
    return true;
  }

  private void delete(ClassModel_V001 owner, AttributeModel_V001 attribute, String changeSet) {
    if (changeSet == null || owner.change != null) {
      deleter.delete(model, attribute);
    }
    else {
      deleter.delete(model, attribute, changeSet);
    }
  }

  /** The line of an attribute, or of a value of an enum, which has no type and so no colon. */
  private Optional<AttributeLine_V001> parse(ClassModel_V001 owner, String text) {
    return format.parse(text, owner.kind.hasTypedAttributes());
  }

  private AttributeLine_V001 parsed(ClassModel_V001 owner, String text) {
    return parse(owner, text).orElseThrow(() -> new IllegalArgumentException("Not an attribute line: " + text));
  }

  /** @return true if the line is the last one of its class, or the blank line of a class without attributes */
  boolean isLastLine(SceneTextArea line) {
    if (line.attribute == null) {
      return true;
    }
    List<AttributeModel_V001> attributes = ((ClassModel_V001) line.element).attributes;
    return attributes.get(attributes.size() - 1) == line.attribute;
  }

  /** Puts a blank line at the end of the class, for a new attribute to be typed into.
   * @return the attribute which stands for the blank line */
  AttributeModel_V001 beginAtEnd(ClassModel_V001 owner) {
    discardPending();
    pendingOwner = owner;
    pendingAttribute = new AttributeModel_V001(null, "", null, new ArrayList<>());
    owner.attributes.add(pendingAttribute);
    return pendingAttribute;
  }

  /** Takes the blank line away again if it is still there.
   * @return true if there was one */
  boolean discardPending() {
    if (pendingAttribute == null) {
      return false;
    }
    pendingOwner.attributes.remove(pendingAttribute);
    pendingOwner = null;
    pendingAttribute = null;
    return true;
  }
}
