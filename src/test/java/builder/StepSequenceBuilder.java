package builder;

import specman.model.v002.StepSequenceModel_V002;

public class StepSequenceBuilder extends AbstractSequenceBuilder<StepSequenceModel_V002, StepSequenceBuilder> {

  public StepSequenceBuilder() {
    super(new StepSequenceModel_V002());
  }
}
