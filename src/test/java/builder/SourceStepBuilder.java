package builder;

import specman.Aenderungsart;
import specman.model.v002.SourceStepModel_V002;

/** Builds the source step of a moved step, tracked in the default change set unless specified otherwise. */
public class SourceStepBuilder extends AbstractStepBuilder<SourceStepModel_V002, SourceStepBuilder> {
  private static final String DEFAULT_CHANGESET = "yellow";

  public SourceStepBuilder() {
    super(new SourceStepModel_V002());
    withChange(Aenderungsart.Quellschritt, DEFAULT_CHANGESET);
  }
}
