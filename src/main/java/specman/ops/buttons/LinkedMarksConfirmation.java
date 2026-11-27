package specman.ops.buttons;

import specman.ChangeSet;
import specman.editarea.image.classman.LinkedChangesAnswer;
import specman.editarea.image.classman.LinkedClassmanImageEditArea;
import specman.ops.SpecmanOpContext;
import specman.visitor.LinkedClassmanAreaCollector;

import java.util.List;

/** Asks the user before an operation changes the marks of a change set in linked diagrams, because that changes their
 * files, which other documents may link as well. The user can have the operation leave the linked diagrams alone, until
 * {@link #finished()}. */
class LinkedMarksConfirmation {
  private final SpecmanOpContext context;
  private List<LinkedClassmanImageEditArea> leftAlone = List.of();

  LinkedMarksConfirmation(SpecmanOpContext context) {
    this.context = context;
  }

  /** @return false if the user cancels the operation. True if no linked diagram has marks of the change set, or the
   * user wants to go on, with or without changing the linked diagrams. */
  boolean confirmed(ChangeSet changeSet) {
    List<LinkedClassmanImageEditArea> affected = LinkedClassmanAreaCollector.areasOf(context::traverseDiagram).stream()
      .filter(area -> area.hasMarksToChange(changeSet))
      .toList();
    if (affected.isEmpty()) {
      return true;
    }
    LinkedChangesAnswer answer = context.askAboutLinkedChanges(affected.size(), changeSet.name);
    if (answer == LinkedChangesAnswer.LEAVE_LINKED) {
      leftAlone = affected;
      leftAlone.forEach(area -> area.setLeftAlone(true));
    }
    return answer != LinkedChangesAnswer.CANCEL;
  }

  /** The operation is over, so the linked diagrams which it left alone are changed by the next operation again. */
  void finished() {
    leftAlone.forEach(area -> area.setLeftAlone(false));
    leftAlone = List.of();
  }
}
