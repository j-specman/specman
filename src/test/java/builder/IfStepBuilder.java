package builder;

import specman.model.v002.IfStepModel_V002;

public class IfStepBuilder extends StructuredStepBuilder<IfStepModel_V002, IfStepBuilder> {
  private static final int DEFAULT_EMPTY_WIDTH = 20;

  private BranchSequenceBuilder ifBranch;

  public IfStepBuilder() {
    super(new IfStepModel_V002());
  }

  @Override
  protected IfStepBuilder withDefaults() {
    return super.withDefaults()
      .withIfBranch(new BranchSequenceBuilder())
      .withEmptyWidth(DEFAULT_EMPTY_WIDTH);
  }

  public IfStepBuilder withIfBranch(BranchSequenceBuilder ifBranch) {
    this.ifBranch = ifBranch;
    return this;
  }

  public IfStepBuilder withEmptyWidth(int emptyWidth) {
    step.emptyWidth = emptyWidth;
    return this;
  }

  @Override
  public IfStepModel_V002 build() {
    step.ifSequence = ifBranch.build();
    return step;
  }
}
