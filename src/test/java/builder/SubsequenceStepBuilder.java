package builder;

import specman.model.v002.SubsequenceStepModel_V002;

public class SubsequenceStepBuilder extends StructuredStepBuilder<SubsequenceStepModel_V002, SubsequenceStepBuilder> {
  private StepSequenceBuilder subsequence;

  public SubsequenceStepBuilder() {
    super(new SubsequenceStepModel_V002());
  }

  @Override
  protected SubsequenceStepBuilder withDefaults() {
    return super.withDefaults()
      .withSubsequence(new StepSequenceBuilder())
      .withFlatNumbering(false);
  }

  public SubsequenceStepBuilder withSubsequence(StepSequenceBuilder subsequence) {
    this.subsequence = subsequence;
    return this;
  }

  public SubsequenceStepBuilder withFlatNumbering(boolean flatNumbering) {
    step.flatNumbering = flatNumbering;
    return this;
  }

  @Override
  public SubsequenceStepModel_V002 build() {
    step.subsequence = subsequence.build();
    return step;
  }
}
