package builder;

import specman.model.v002.StructuredStepModel_V002;

/** Base of the builders for steps which contain sequences. */
abstract public class StructuredStepBuilder<S extends StructuredStepModel_V002, B extends StructuredStepBuilder<S, B>>
  extends AbstractStepBuilder<S, B> {

  protected StructuredStepBuilder(S step) {
    super(step);
  }

  @Override
  protected B withDefaults() {
    return super.withDefaults().withCollapsed(false);
  }

  public B withCollapsed(boolean collapsed) {
    step.collapsed = collapsed;
    return self();
  }
}
