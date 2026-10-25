package builder;

import specman.model.v002.IfElseStepModel_V002;

public class IfElseStepBuilder extends StructuredStepBuilder<IfElseStepModel_V002, IfElseStepBuilder> {
  private static final float DEFAULT_IF_WIDTH_RATIO = 0.5f;

  private BranchSequenceBuilder ifBranch;
  private BranchSequenceBuilder elseBranch;

  public IfElseStepBuilder() {
    super(new IfElseStepModel_V002());
  }

  @Override
  protected IfElseStepBuilder withDefaults() {
    return super.withDefaults()
      .withIfBranch(new BranchSequenceBuilder())
      .withElseBranch(new BranchSequenceBuilder())
      .withIfWidthRatio(DEFAULT_IF_WIDTH_RATIO);
  }

  public IfElseStepBuilder withIfBranch(BranchSequenceBuilder ifBranch) {
    this.ifBranch = ifBranch;
    return this;
  }

  public IfElseStepBuilder withElseBranch(BranchSequenceBuilder elseBranch) {
    this.elseBranch = elseBranch;
    return this;
  }

  public IfElseStepBuilder withIfWidthRatio(float ifWidthRatio) {
    step.ifWidthRatio = ifWidthRatio;
    return this;
  }

  @Override
  public IfElseStepModel_V002 build() {
    step.ifSequence = ifBranch.build();
    step.elseSequence = elseBranch.build();
    return step;
  }
}
