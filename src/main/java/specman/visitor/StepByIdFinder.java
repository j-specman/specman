package specman.visitor;

import specman.editarea.EditContainer;
import specman.view.AbstractSchrittView;

/**
 * Finds the step with the given internal step ID, wherever it lives, catch sequences included. In contrast to
 * {@link StepByNumberFinder}, the ID doesn't change when steps are moved. The traversal is stopped as soon as
 * the step is found.
 */
public class StepByIdFinder implements DiagramVisitor {

  private final String stepId;
  private AbstractSchrittView foundStep;

  public StepByIdFinder(String stepId) {
    this.stepId = stepId;
  }

  @Override
  public void visitStep(AbstractSchrittView step) throws StopTraversalException {
    if (stepId.equals(step.getId())) {
      foundStep = step;
      throw new StopTraversalException();
    }
  }

  /** There are no steps in edit containers, so there is no need to look into the edit areas. */
  @Override
  public void visitEditContainer(EditContainer editContainer) throws IgnoreChildrenException {
    throw new IgnoreChildrenException();
  }

  /** @return the step found, or null if there is no such step */
  public AbstractSchrittView step() {
    return foundStep;
  }
}
