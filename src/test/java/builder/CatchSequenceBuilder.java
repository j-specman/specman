package builder;

import specman.model.v002.CatchSequenceModel_V002;
import specman.model.v002.CoCatchModel_V002;
import specman.model.v002.EditorContentModel_V002;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a catch sequence for a break step. The catch sequence's id is the id of the break
 * step it catches, and its heading is the break step's text, unless a heading is specified
 * explicitly. Both are taken from the break step builder when the catch is built, so the
 * break step may still be changed after the catch builder has been created.
 * The same applies to the headings of co-catches.
 */
public class CatchSequenceBuilder extends AbstractBranchBuilder<CatchSequenceModel_V002, CatchSequenceBuilder> {
  private final BreakStepBuilder breakStep;
  private final List<BreakStepBuilder> coCatchedBreakSteps = new ArrayList<>();

  public CatchSequenceBuilder(BreakStepBuilder breakStep) {
    super(new CatchSequenceModel_V002(null, null, null, new ArrayList<>(), null));
    this.breakStep = breakStep;
  }

  /** Lets the catch sequence also handle the specified break step. */
  public CatchSequenceBuilder withCoCatch(BreakStepBuilder coCatchedBreakStep) {
    coCatchedBreakSteps.add(coCatchedBreakStep);
    return this;
  }

  @Override
  public CatchSequenceModel_V002 build() {
    sequence.id = breakStep.build().id;
    if (sequence.heading == null) {
      withHeading(breakStep.text());
    }
    sequence.coCatches.clear();
    for (BreakStepBuilder coCatchedBreakStep : coCatchedBreakSteps) {
      sequence.coCatches.add(buildCoCatch(coCatchedBreakStep));
    }
    return super.build();
  }

  private static CoCatchModel_V002 buildCoCatch(BreakStepBuilder coCatchedBreakStep) {
    return new CoCatchModel_V002(
      coCatchedBreakStep.build().id,
      new EditorContentModel_V002(coCatchedBreakStep.text(), null),
      null);
  }
}
