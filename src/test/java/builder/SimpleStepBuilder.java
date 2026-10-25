package builder;

import specman.model.v002.SimpleStepModel_V002;

public class SimpleStepBuilder extends AbstractStepBuilder<SimpleStepModel_V002, SimpleStepBuilder> {

  public SimpleStepBuilder() {
    super(new SimpleStepModel_V002());
  }
}
