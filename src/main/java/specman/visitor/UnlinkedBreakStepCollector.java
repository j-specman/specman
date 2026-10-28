package specman.visitor;

import specman.view.AbstractSchrittView;
import specman.view.BreakSchrittView;
import specman.view.CatchBereich;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects the break steps which don't have a catch sequence yet. Break steps in catch sequences are not
 * collected, as the visitor keeps out of the catch areas.
 */
public class UnlinkedBreakStepCollector implements DiagramVisitor {

  private final List<BreakSchrittView> breakSteps = new ArrayList<>();

  @Override
  public void visitStep(AbstractSchrittView step) {
    if (step instanceof BreakSchrittView breakStep && !breakStep.isLinked()) {
      breakSteps.add(breakStep);
    }
  }

  @Override
  public void visitCatchArea(CatchBereich catchArea) throws IgnoreChildrenException {
    throw new IgnoreChildrenException();
  }

  /** @return the break steps collected so far, a modifiable list the caller owns */
  public List<BreakSchrittView> breakSteps() {
    return breakSteps;
  }
}
