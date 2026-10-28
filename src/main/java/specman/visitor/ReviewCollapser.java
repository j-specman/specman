package specman.visitor;

import specman.view.AbstractSchrittView;
import specman.view.CatchBereich;

/**
 * Prepares the diagram for reviewing changes: Collapses every collapsible step and catch area which contains no
 * change marks, so that the reviewer gets to see the changes only. Each node decides for itself whether it is
 * collapsible and unchanged, see {@link AbstractSchrittView#zusammenklappenFuerReview()}. The visitor only reports
 * the nodes to them.
 */
public class ReviewCollapser implements DiagramVisitor {

  @Override
  public void visitStep(AbstractSchrittView step) {
    step.zusammenklappenFuerReview();
  }

  @Override
  public void visitCatchArea(CatchBereich catchArea) {
    catchArea.zusammenklappenFuerReview();
  }
}
