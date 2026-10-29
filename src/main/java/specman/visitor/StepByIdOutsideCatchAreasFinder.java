package specman.visitor;

import specman.view.CatchBereich;

/**
 * Finds the step with the given internal step ID, but only outside of catch areas: Steps inside catch sequences
 * are never found. In contrast to {@link StepByIdFinder}, which finds every step of the diagram.
 * <p>
 * Used by the model loading, where the steps outside of catch areas are the ones to link:
 * the break step of a catch sequence, which always lives in the sequence owning the catch area, and, so far,
 * the source and target steps of moved steps. The traversal is stopped as soon as the step is found.
 */
public class StepByIdOutsideCatchAreasFinder extends StepByIdFinder {

  public StepByIdOutsideCatchAreasFinder(String stepId) {
    super(stepId);
  }

  @Override
  public void visitCatchArea(CatchBereich catchArea) throws IgnoreChildrenException {
    throw new IgnoreChildrenException();
  }
}
