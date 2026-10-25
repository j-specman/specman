package builder;

import specman.model.v002.CatchAreaModel_V002;
import specman.model.v002.StepSequenceModel_V002;

import java.util.ArrayList;
import java.util.List;

import static specman.model.v002.AbstractStepModel_V002.generateId;

/**
 * Base of all sequence builders. The second type parameter is the concrete builder type
 * (see {@link AbstractStepBuilder}).
 */
abstract public class AbstractSequenceBuilder<Q extends StepSequenceModel_V002, B extends AbstractSequenceBuilder<Q, B>> {
  protected Q sequence;
  private final List<AbstractStepBuilder<?, ?>> steps = new ArrayList<>();
  private final List<CatchSequenceBuilder> catches = new ArrayList<>();

  protected AbstractSequenceBuilder(Q sequence) {
    this.sequence = sequence;
    withDefaults();
  }

  @SuppressWarnings("unchecked")
  protected B self() {
    return (B) this;
  }

  protected B withDefaults() {
    return withId(generateId());
  }

  public B withId(String id) {
    sequence.id = id;
    return self();
  }

  public B withStep(AbstractStepBuilder<?, ?> step) {
    steps.add(step);
    return self();
  }

  /** Adds a catch sequence to the catch area at the end of the sequence. */
  public B withCatch(CatchSequenceBuilder catchSequence) {
    catches.add(catchSequence);
    return self();
  }

  public Q build() {
    sequence.steps = new ArrayList<>();
    for (AbstractStepBuilder<?, ?> stepBuilder : steps) {
      sequence.steps.add(stepBuilder.build());
    }
    sequence.catchArea = buildCatchArea();
    return sequence;
  }

  private CatchAreaModel_V002 buildCatchArea() {
    if (catches.isEmpty()) {
      return null;
    }
    CatchAreaModel_V002 catchArea = new CatchAreaModel_V002(new ArrayList<>(), false);
    for (CatchSequenceBuilder catchBuilder : catches) {
      catchArea.catchSequences.add(catchBuilder.build());
    }
    return catchArea;
  }
}
