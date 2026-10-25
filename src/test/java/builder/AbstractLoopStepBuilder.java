package builder;

import specman.model.v002.WhileStepModel_V002;

/** Base of the while and do-while step builders. */
abstract public class AbstractLoopStepBuilder<S extends WhileStepModel_V002, B extends AbstractLoopStepBuilder<S, B>>
  extends StructuredStepBuilder<S, B> {
  private static final int DEFAULT_BAR_WIDTH = 18;

  private StepSequenceBuilder loopSequence;

  protected AbstractLoopStepBuilder(S step) {
    super(step);
  }

  @Override
  protected B withDefaults() {
    return super.withDefaults()
      .withLoopSequence(new StepSequenceBuilder())
      .withBarWidth(DEFAULT_BAR_WIDTH);
  }

  public B withLoopSequence(StepSequenceBuilder loopSequence) {
    this.loopSequence = loopSequence;
    return self();
  }

  public B withBarWidth(int barWidth) {
    step.barWidth = barWidth;
    return self();
  }

  @Override
  public S build() {
    step.loopSequence = loopSequence.build();
    return step;
  }
}
