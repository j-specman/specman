package builder;

import specman.model.v002.WhileStepModel_V002;

public class WhileStepBuilder extends AbstractLoopStepBuilder<WhileStepModel_V002, WhileStepBuilder> {

  public WhileStepBuilder() {
    super(new WhileStepModel_V002());
  }
}
