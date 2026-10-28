package specman.visitor;

import specman.StepNumber;
import specman.editarea.EditContainer;
import specman.view.AbstractSchrittView;

/**
 * Finds the step with the given step ID, which is the step number as step links refer to it. The traversal
 * is stopped as soon as the step is found.
 */
public class StepByNumberFinder implements DiagramVisitor {

  private final String stepNumber;
  private AbstractSchrittView foundStep;

  public StepByNumberFinder(String stepNumber) {
    this.stepNumber = stepNumber;
  }

  @Override
  public void visitStep(AbstractSchrittView step) throws StopTraversalException {
    StepNumber number = step.getNumber();
    if (number != null && stepNumber.equals(number.toString())) {
      foundStep = step;
      throw new StopTraversalException();
    }
  }

  /** There are no steps in edit containers, so there is no need to look into the edit areas. */
  @Override
  public void visitEditContainer(EditContainer editContainer) throws IgnoreChildrenException {
    throw new IgnoreChildrenException();
  }

  /** @return the step found, or null if there is no step with the number */
  public AbstractSchrittView step() {
    return foundStep;
  }
}
