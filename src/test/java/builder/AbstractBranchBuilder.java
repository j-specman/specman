package builder;

import specman.model.v002.BranchSequenceModel_V002;
import specman.model.v002.EditorContentModel_V002;

/** Base of the sequence builders for sequences with a heading, i.e. branches and catches. */
abstract public class AbstractBranchBuilder<Q extends BranchSequenceModel_V002, B extends AbstractBranchBuilder<Q, B>>
  extends AbstractSequenceBuilder<Q, B> {

  protected AbstractBranchBuilder(Q sequence) {
    super(sequence);
  }

  public B withHeading(String headingText) {
    sequence.heading = new EditorContentModel_V002(headingText, null);
    return self();
  }
}
