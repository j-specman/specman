package specman.editarea.focusmover;

import specman.editarea.EditArea;
import specman.editarea.ImageEditArea;
import specman.editarea.TextEditArea;
import specman.visitor.DiagramVisitor;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Collects the edit areas the caret can be moved to, together with their bounds in the coordinates of the
 * working area. The area the caret is moving away from is not a candidate.
 */
class FocusCandidateCollector implements DiagramVisitor {

  record Candidate(EditArea area, Rectangle bounds) {}

  private final Container workingArea;
  private final EditArea source;
  private final List<Candidate> candidates = new ArrayList<>();

  FocusCandidateCollector(Container workingArea, EditArea source) {
    this.workingArea = workingArea;
    this.source = source;
  }

  @Override
  public void visitTextArea(TextEditArea textArea) {
    // Areas the user can't edit show no caret
    addIfCandidate(textArea, textArea.isEditable());
  }

  @Override
  public void visitImageArea(ImageEditArea imageArea) {
    // Deleted images don't react to keys, so one could never leave them again
    addIfCandidate(imageArea, !imageArea.getChangeInfo().isDeleted());
  }

  List<Candidate> candidates() {
    return candidates;
  }

  static Rectangle boundsInWorkingArea(Container workingArea, Component component) {
    return SwingUtilities.convertRectangle(component.getParent(), component.getBounds(), workingArea);
  }

  private void addIfCandidate(EditArea<?> editArea, boolean canTakeCaret) {
    if (editArea == source || !canTakeCaret || !isDisplayed(editArea.asComponent())) {
      return;
    }
    Rectangle bounds = boundsInWorkingArea(workingArea, editArea.asComponent());
    if (!bounds.isEmpty()) {
      candidates.add(new Candidate(editArea, bounds));
    }
  }

  /**
   * Edit areas in collapsed sections have no place on the screen, so the user can't navigate to them. Instead of
   * deriving this from the model, i.e. from the collapsed state of steps and catch areas, the question is put to
   * Swing: Is every component between the edit area and the working area visible?
   * It is not enough to look at the edit area itself, as it is a panel further up which is hidden, not the area.
   */
  private boolean isDisplayed(Component component) {
    for (Component current = component; current != workingArea; current = current.getParent()) {
      if (current == null || !current.isVisible()) {
        return false;
      }
    }
    return true;
  }
}
