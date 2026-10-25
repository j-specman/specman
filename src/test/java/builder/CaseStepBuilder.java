package builder;

import specman.model.v002.CaseStepModel_V002;

import java.util.ArrayList;
import java.util.List;

public class CaseStepBuilder extends StructuredStepBuilder<CaseStepModel_V002, CaseStepBuilder> {
  private BranchSequenceBuilder defaultBranch;
  private final List<BranchSequenceBuilder> caseBranches = new ArrayList<>();

  public CaseStepBuilder() {
    super(new CaseStepModel_V002());
  }

  @Override
  protected CaseStepBuilder withDefaults() {
    step.columnWidthRatios = new ArrayList<>();
    return super.withDefaults().withDefaultBranch(new BranchSequenceBuilder());
  }

  public CaseStepBuilder withDefaultBranch(BranchSequenceBuilder defaultBranch) {
    this.defaultBranch = defaultBranch;
    return this;
  }

  /** Adds a case branch behind the already added ones. */
  public CaseStepBuilder withCase(BranchSequenceBuilder caseBranch) {
    caseBranches.add(caseBranch);
    return this;
  }

  @Override
  public CaseStepModel_V002 build() {
    step.defaultSequence = defaultBranch.build();
    step.caseSequences = new ArrayList<>();
    for (BranchSequenceBuilder caseBranch : caseBranches) {
      step.caseSequences.add(caseBranch.build());
    }
    return step;
  }
}
