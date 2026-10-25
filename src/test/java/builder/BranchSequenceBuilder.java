package builder;

import specman.model.v002.BranchSequenceModel_V002;

/** Builds the branches of if, if/else and case steps. */
public class BranchSequenceBuilder extends AbstractBranchBuilder<BranchSequenceModel_V002, BranchSequenceBuilder> {

  public BranchSequenceBuilder() {
    super(new BranchSequenceModel_V002());
  }

  @Override
  protected BranchSequenceBuilder withDefaults() {
    return super.withDefaults().withHeading("");
  }
}
