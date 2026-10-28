package specman.visitor;

import specman.view.AbstractSchrittView;

import java.util.ArrayList;
import java.util.List;

/** Collects all steps of the diagram in document order, including the steps in catch sequences. */
public class StepCollector implements DiagramVisitor {

  private final List<AbstractSchrittView> steps = new ArrayList<>();

  @Override
  public void visitStep(AbstractSchrittView step) {
    steps.add(step);
  }

  /** @return the steps collected so far, a modifiable list the caller owns */
  public List<AbstractSchrittView> steps() {
    return steps;
  }
}
