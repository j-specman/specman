package builder;

import specman.model.v002.DoWhileStepModel_V002;

public class DoWhileStepBuilder extends AbstractLoopStepBuilder<DoWhileStepModel_V002, DoWhileStepBuilder> {

  public DoWhileStepBuilder() {
    super(new DoWhileStepModel_V002());
  }
}
