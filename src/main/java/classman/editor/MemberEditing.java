package classman.editor;

import classman.model.v001.AbstractMemberModel_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.change.AbstractMemberChanger_V001;
import classman.model.v001.change.ElementDeleter_V001;
import classman.model.v001.change.MemberLine_V001;
import classman.render.SceneTextArea;

import java.util.List;
import java.util.Optional;

/**
 * Changes, adds and deletes the attributes, or the methods, of the classes of a diagram, a line of text at a time. A
 * new member is typed into a blank line which is put at the end of the compartment while it is typed: a member without
 * a name, which is not part of the diagram until the line is committed, and goes away if it is dropped.
 *
 * @param <M> the kind of member
 * @param <L> the line of the kind of member
 */
abstract class MemberEditing<M extends AbstractMemberModel_V001, L extends MemberLine_V001> {
  private final ClassDiagramModel_V001 model;
  private final Class<M> memberType;
  private final AbstractMemberChanger_V001<M, L> changer;
  private final ElementDeleter_V001 deleter = new ElementDeleter_V001();
  private ClassModel_V001 pendingOwner;
  private M pendingMember;

  MemberEditing(ClassDiagramModel_V001 model, Class<M> memberType, AbstractMemberChanger_V001<M, L> changer) {
    this.model = model;
    this.memberType = memberType;
    this.changer = changer;
  }

  /** @return the members of the kind which the class has, the list itself */
  protected abstract List<M> membersOf(ClassModel_V001 owner);

  /** @return a member without a name, which stands for the blank line */
  protected abstract M blank();

  /** @return the line which the member is shown as, and edited in */
  protected abstract String format(M member);

  /** @return the parts of the line, none if it is no line of the kind of member the class can have */
  protected abstract Optional<L> parse(ClassModel_V001 owner, String text);

  /** @return the line which the member is shown as, empty for a blank line: the one of a class without members of the
   * kind, and the one for a new member which is typed */
  String textOf(SceneTextArea line) {
    return line.member == null || line.member == pendingMember ? "" : format(memberType.cast(line.member));
  }

  /** @return false if the line can't have the content: it has no name, or it would take away a visibility sign which
   * can't be marked as removed. The empty text is fine, it deletes the member. */
  boolean accepts(SceneTextArea line, String newText, String changeSet) {
    if (newText.isEmpty()) {
      return true;
    }
    ClassModel_V001 owner = (ClassModel_V001) line.element;
    Optional<L> parsed = parse(owner, newText);
    if (parsed.isEmpty()) {
      return false;
    }
    return line.member == null || line.member == pendingMember
      || changer.canApply(owner, memberType.cast(line.member), parsed.get(), changeSet);
  }

  /** Gives the line its new content: the member changes, a new member comes, or the member is deleted if the text is
   * empty.
   * @param changeSet the change set which tracks the changes, null if changes are not tracked
   * @return true if the diagram has changed, false if only the blank line has gone away again */
  boolean setText(SceneTextArea line, String newText, String changeSet) {
    ClassModel_V001 owner = (ClassModel_V001) line.element;
    if (line.member != null && line.member == pendingMember) {
      return setPendingText(owner, newText, changeSet);
    }
    if (newText.isEmpty()) {
      if (line.member != null) {
        delete(owner, line.member, changeSet);
      }
    }
    else if (line.member == null) {
      membersOf(owner).add(changer.create(owner, parsed(owner, newText), changeSet));
    }
    else {
      changer.apply(owner, memberType.cast(line.member), parsed(owner, newText), changeSet);
    }
    return true;
  }

  private boolean setPendingText(ClassModel_V001 owner, String newText, String changeSet) {
    int index = membersOf(owner).indexOf(pendingMember);
    discardPending();
    if (newText.isEmpty()) {
      return false;
    }
    membersOf(owner).add(index, changer.create(owner, parsed(owner, newText), changeSet));
    return true;
  }

  private void delete(ClassModel_V001 owner, AbstractMemberModel_V001 member, String changeSet) {
    if (changeSet == null || owner.change != null) {
      deleter.delete(model, member);
    }
    else {
      deleter.delete(model, member, changeSet);
    }
  }

  private L parsed(ClassModel_V001 owner, String text) {
    return parse(owner, text).orElseThrow(() -> new IllegalArgumentException("Not a line of a member: " + text));
  }

  /** @return true if the line is the last one of its compartment, or the blank line of a class without members of
   * the kind */
  boolean isLastLine(SceneTextArea line) {
    if (line.member == null) {
      return true;
    }
    List<M> members = membersOf((ClassModel_V001) line.element);
    return members.get(members.size() - 1) == line.member;
  }

  /** Puts a blank line at the end of the compartment, for a new member to be typed into.
   * @return the member which stands for the blank line */
  M beginAtEnd(ClassModel_V001 owner) {
    discardPending();
    pendingOwner = owner;
    pendingMember = blank();
    membersOf(owner).add(pendingMember);
    return pendingMember;
  }

  /** Takes the blank line away again if it is still there.
   * @return true if there was one */
  boolean discardPending() {
    if (pendingMember == null) {
      return false;
    }
    membersOf(pendingOwner).remove(pendingMember);
    pendingOwner = null;
    pendingMember = null;
    return true;
  }
}
