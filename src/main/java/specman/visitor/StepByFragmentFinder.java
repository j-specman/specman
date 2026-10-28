package specman.visitor;

import specman.editarea.EditContainer;
import specman.editarea.InteractiveStepFragment;
import specman.view.AbstractSchrittView;
import specman.view.CatchSchrittSequenzView;
import specman.view.SchrittSequenzView;

/**
 * Finds the step owning an {@link InteractiveStepFragment}, i.e. one of its edit areas, its step number label or one
 * of its meta tags. This includes the fragments of a branch's heading, which belong to the branching step.
 * The fragments of a catch sequence's headings are owned by the catch area, which is a step as well, so the
 * catch area is found for those.
 * The traversal is stopped as soon as the owner is found.
 */
public class StepByFragmentFinder implements DiagramVisitor {

  private final InteractiveStepFragment fragment;
  private AbstractSchrittView foundStep;

  public StepByFragmentFinder(InteractiveStepFragment fragment) {
    this.fragment = fragment;
  }

  @Override
  public void visitStep(AbstractSchrittView step) throws StopTraversalException {
    if (step.enthaelt(fragment)) {
      found(step);
    }
  }

  @Override
  public void visitSequence(SchrittSequenzView sequence) throws StopTraversalException {
    if (sequence instanceof CatchSchrittSequenzView catchSequence && catchSequence.enthaelt(fragment)) {
      found(catchSequence.getParent());
    }
  }

  /** The steps know their own edit areas and headings, so there is no need to look into the edit containers. */
  @Override
  public void visitEditContainer(EditContainer editContainer) throws IgnoreChildrenException {
    throw new IgnoreChildrenException();
  }

  private void found(AbstractSchrittView step) throws StopTraversalException {
    foundStep = step;
    throw new StopTraversalException();
  }

  /** @return the step owning the fragment, or null if there is none */
  public AbstractSchrittView step() {
    return foundStep;
  }
}
