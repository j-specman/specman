package specman.visitor;

import specman.view.AbstractSchrittView;
import specman.view.BreakSchrittView;
import specman.view.CatchBereich;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Special-purpose visitor for {@link AbstractSchrittView#queryExternallyLinkedBreakSteps()}, which finds out whether
 * cutting a step would tear apart break steps and their catch sequences. Started at the step to be cut, it
 * collects two things in one traversal of the step's subtree:
 * <ul>
 *   <li>{@link #breakSteps()}: the break steps which have a catch sequence, i.e. which are linked.</li>
 *   <li>{@link #catchAreas()}: the catch areas of all visited sequences. A catch area is part of the subtree
 *   exactly if the subtree contains the sequence owning it.</li>
 * </ul>
 * A linked break step whose catch sequence lives in none of the collected catch areas is linked to something
 * outside the subtree. The visitor doesn't draw this conclusion itself, it only delivers the two collections to
 * compare, because only the caller knows what to make of the difference.
 * <p>
 * The visitor keeps out of the catch areas, so it neither collects break steps inside catch sequences nor the
 * catch areas nested in them. Break steps in catch sequences are deliberately not considered when a step is cut,
 * which is how the query has always behaved.
 * <p>
 * Don't use it for finding break steps in general - it ignores catch sequences and skips unlinked break steps.
 * The visitor is stateful and for one traversal only.
 */
public class LinkedBreakStepCollector implements DiagramVisitor {

  private final List<BreakSchrittView> breakSteps = new ArrayList<>();
  private final Set<CatchBereich> catchAreas = new HashSet<>();

  @Override
  public void visitStep(AbstractSchrittView step) {
    if (step instanceof BreakSchrittView breakStep && breakStep.isLinked()) {
      breakSteps.add(breakStep);
    }
  }

  @Override
  public void visitCatchArea(CatchBereich catchArea) throws IgnoreChildrenException {
    catchAreas.add(catchArea);
    throw new IgnoreChildrenException();
  }

  /** @return the linked break steps collected so far, a modifiable list the caller owns */
  public List<BreakSchrittView> breakSteps() {
    return breakSteps;
  }

  /** @return the catch areas of the visited sequences, a modifiable set the caller owns */
  public Set<CatchBereich> catchAreas() {
    return catchAreas;
  }
}
