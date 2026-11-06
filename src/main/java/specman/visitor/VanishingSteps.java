package specman.visitor;

import specman.view.AbstractSchrittView;
import specman.view.BreakSchrittView;
import specman.view.CatchSchrittSequenzView;
import specman.view.QuellSchrittView;
import specman.view.SchrittSequenzView;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The steps which vanish from the diagram when the user deletes a part of it, and what this means for the moves
 * recorded by source and target steps: A target step doesn't make sense without its source step and vice versa.
 * <ul>
 *   <li>The source steps of vanishing target steps have to vanish as well, see {@link #externalSourceSteps()}.</li>
 *   <li>The target steps of vanishing source steps become ordinary steps, see {@link #externalTargetSteps()}.</li>
 * </ul>
 * Ties between steps which both vanish don't matter.
 */
public class VanishingSteps {

  private final Set<AbstractSchrittView> steps = new LinkedHashSet<>();

  private VanishingSteps() {}

  /** The step with its subtree. A break step takes the catch sequence it is the primary break step of along, so
   * the steps in there vanish too. */
  public static VanishingSteps ofStep(AbstractSchrittView step) {
    VanishingSteps vanishing = new VanishingSteps();
    vanishing.collect(step::traverse);
    return vanishing;
  }

  /** The steps of the sequence, e.g. a case branch or a catch sequence, with their subtrees. */
  public static VanishingSteps ofSequence(SchrittSequenzView sequence) {
    VanishingSteps vanishing = new VanishingSteps();
    vanishing.collect(sequence::traverse);
    return vanishing;
  }

  private void collect(Consumer<DiagramVisitor> traversal) {
    StepCollector collector = new StepCollector();
    traversal.accept(collector);
    for (AbstractSchrittView step : collector.steps()) {
      if (steps.add(step) && step instanceof BreakSchrittView breakStep) {
        CatchSchrittSequenzView catchSequence = breakStep.vanishingCatchSequence();
        if (catchSequence != null) {
          collect(catchSequence::traverse);
        }
      }
    }
  }

  /** The source steps of the vanishing target steps whose own place is not vanishing. */
  public List<QuellSchrittView> externalSourceSteps() {
    return steps.stream()
      .map(AbstractSchrittView::getQuellschritt)
      .filter(Objects::nonNull)
      .filter(sourceStep -> !steps.contains(sourceStep))
      .toList();
  }

  /** The target steps of the vanishing source steps whose own place is not vanishing. */
  public List<AbstractSchrittView> externalTargetSteps() {
    return steps.stream()
      .filter(QuellSchrittView.class::isInstance)
      .map(step -> ((QuellSchrittView) step).getZielschritt())
      .filter(Objects::nonNull)
      .filter(targetStep -> !steps.contains(targetStep))
      .toList();
  }

  /** Whether the external source steps can be removed without emptying a sequence, which is not allowed.
   * @param sequenceLosingStep a sequence losing a step in addition, e.g. the one the deleted step is in, or null */
  public boolean allowsRemovalOfExternalSourceSteps(SchrittSequenzView sequenceLosingStep) {
    Map<SchrittSequenzView, Integer> numRemovalsPerSequence = new HashMap<>();
    if (sequenceLosingStep != null) {
      numRemovalsPerSequence.merge(sequenceLosingStep, 1, Integer::sum);
    }
    for (QuellSchrittView sourceStep : externalSourceSteps()) {
      numRemovalsPerSequence.merge(sourceStep.getParent(), 1, Integer::sum);
    }
    return numRemovalsPerSequence.entrySet().stream()
      .allMatch(entry -> entry.getKey().allowsStepsDeletion(entry.getValue()));
  }
}
