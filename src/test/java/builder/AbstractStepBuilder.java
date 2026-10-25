package builder;

import specman.model.v002.AbstractStepModel_V002;
import specman.model.v002.EditorContentModel_V002;
import specman.model.v002.TextEditAreaModel_V002;

/**
 * Base of all step builders. The second type parameter is the concrete builder type, so the
 * with-methods defined here return it and can be freely chained with the with-methods of
 * the concrete builders, e.g. {@code simple().withText("a").withId("12345678")}.
 */
abstract public class AbstractStepBuilder<S extends AbstractStepModel_V002, B extends AbstractStepBuilder<S, B>> {
  protected S step;

  protected AbstractStepBuilder(S step) {
    this.step = step;
    withDefaults();
  }

  @SuppressWarnings("unchecked")
  protected B self() {
    return (B) this;
  }

  /** Called from the constructor, so overrides must not rely on fields of the concrete builder
   *  which are initialized by an initializer: they are set only after this method has run. */
  protected B withDefaults() {
    return withId(AbstractStepModel_V002.generateId())
      .withText("");
  }

  public B withId(String id) {
    step.id = id;
    return self();
  }

  public B withStepNumber(String stepNumber) {
    step.stepNumber = stepNumber;
    return self();
  }

  public B withText(String text) {
    step.content = new EditorContentModel_V002(text, null);
    return self();
  }

  /** The primary text of the step as it was specified by {@link #withText(String)}. */
  public String text() {
    return ((TextEditAreaModel_V002) step.content.areas.get(0)).text;
  }

  public S build() {
    return step;
  }
}
