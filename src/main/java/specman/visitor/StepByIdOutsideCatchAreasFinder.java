package specman.visitor;

import specman.editarea.EditContainer;
import specman.view.AbstractSchrittView;
import specman.view.CatchBereich;

/**
 * Finds the step with the given internal step ID, but only outside of catch areas: Steps inside catch sequences
 * are never found. In contrast to {@link StepByNumberFinder}, which finds every step of the diagram.
 * <p>
 * Used by the model loading, where the steps outside of catch areas are the ones to link:
 * the break step of a catch sequence, which always lives in the sequence owning the catch area, and, so far,
 * the source and target steps of moved steps. The traversal is stopped as soon as the step is found.
 */
public class StepByIdOutsideCatchAreasFinder implements DiagramVisitor {

  private final String stepId;
  private AbstractSchrittView foundStep;

  public StepByIdOutsideCatchAreasFinder(String stepId) {
    this.stepId = stepId;
  }

  @Override
  public void visitStep(AbstractSchrittView step) throws StopTraversalException {
    if (stepId.equals(step.getId())) {
      foundStep = step;
      throw new StopTraversalException();
    }
  }

  @Override
  public void visitCatchArea(CatchBereich catchArea) throws IgnoreChildrenException {
    throw new IgnoreChildrenException();
  }

  /** There are no steps in edit containers, so there is no need to look into the edit areas. */
  @Override
  public void visitEditContainer(EditContainer editContainer) throws IgnoreChildrenException {
    throw new IgnoreChildrenException();
  }

  /** @return the step found, or null if there is no such step outside of catch areas */
  public AbstractSchrittView step() {
    return foundStep;
  }
}
