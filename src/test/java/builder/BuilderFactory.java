package builder;

/**
 * Short factory methods for the NSD test data builders. Tests import them statically
 * to describe a diagram compactly, e.g.
 * <pre>diagramm().withMainSequence(sequence().withStep(simple("text"))).persist()</pre>
 * Names of builders for step types whose name is a Java keyword carry the suffix <i>Step</i>.
 * Every step builder is available with and without the step's primary text. A branch's
 * primary text is its heading. Catch sequences have none: the break step they catch is
 * what identifies them.
 */
public class BuilderFactory {

  private BuilderFactory() {}

  public static DiagrammBuilder diagramm() {
    return new DiagrammBuilder();
  }

  public static StepSequenceBuilder sequence() {
    return new StepSequenceBuilder();
  }

  /** A branch of an if, if/else or case step. */
  public static BranchSequenceBuilder branch() {
    return new BranchSequenceBuilder();
  }

  public static BranchSequenceBuilder branch(String heading) {
    return branch().withHeading(heading);
  }

  /** The catch sequence handling the specified break step. */
  public static CatchSequenceBuilder catchOf(BreakStepBuilder breakStep) {
    return new CatchSequenceBuilder(breakStep);
  }

  public static SimpleStepBuilder simple() {
    return new SimpleStepBuilder();
  }

  public static SimpleStepBuilder simple(String text) {
    return simple().withText(text);
  }

  public static BreakStepBuilder breakStep() {
    return new BreakStepBuilder();
  }

  public static BreakStepBuilder breakStep(String text) {
    return breakStep().withText(text);
  }

  public static WhileStepBuilder whileStep() {
    return new WhileStepBuilder();
  }

  public static WhileStepBuilder whileStep(String text) {
    return whileStep().withText(text);
  }

  public static DoWhileStepBuilder doWhileStep() {
    return new DoWhileStepBuilder();
  }

  public static DoWhileStepBuilder doWhileStep(String text) {
    return doWhileStep().withText(text);
  }

  public static IfStepBuilder ifStep() {
    return new IfStepBuilder();
  }

  public static IfStepBuilder ifStep(String text) {
    return ifStep().withText(text);
  }

  public static IfElseStepBuilder ifElseStep() {
    return new IfElseStepBuilder();
  }

  public static IfElseStepBuilder ifElseStep(String text) {
    return ifElseStep().withText(text);
  }

  public static CaseStepBuilder caseStep() {
    return new CaseStepBuilder();
  }

  public static CaseStepBuilder caseStep(String text) {
    return caseStep().withText(text);
  }

  public static SubsequenceStepBuilder subsequenceStep() {
    return new SubsequenceStepBuilder();
  }

  public static SubsequenceStepBuilder subsequenceStep(String text) {
    return subsequenceStep().withText(text);
  }
}
