package specman.cli.sanitize;

import specman.cli.AbstractCLO;
import specman.cli.CLIException;
import specman.model.v002.AbstractStepModel_V002;
import specman.model.v002.io.TextAreaTraversal_V002.Site;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks a property of a model which can't be repaired automatically by the sanitizer.
 * A validator collects all violations it finds and reports them together as one {@link CLIException}.
 */
public abstract class AbstractModelValidator {

  private static final String UNKNOWN_STEP_NUMBER = "?";

  private final String heading;

  protected AbstractModelValidator(String heading) {
    this.heading = heading;
  }

  /** Fails with one {@link CLIException} listing all violations, if there are any. */
  public final void validate() throws CLIException {
    AbstractCLO.assertNoErrors(heading, collectErrors());
  }

  final List<String> collectErrors() {
    List<String> errors = new ArrayList<>();
    collectErrors(errors);
    return errors;
  }

  /** Adds one message for each violation to the specified list. */
  protected abstract void collectErrors(List<String> errors);

  /** @return e.g. "Step 1.2 (a1b2c3d4)" */
  protected static String describe(AbstractStepModel_V002 step) {
    return "Step " + displayedStepNumber(step) + " (" + step.id + ")";
  }

  /** @return e.g. "Step 1.2 (a1b2c3d4) text", or "intro" */
  protected static String describe(Site site) {
    return site.step() != null ? describe(site.step()) + " " + site.part() : site.part();
  }

  protected static String displayedStepNumber(AbstractStepModel_V002 step) {
    return step.stepNumber != null ? step.stepNumber : UNKNOWN_STEP_NUMBER;
  }

  protected static String located(String position, String message) {
    return position.isEmpty() ? message : position + " " + message;
  }

  protected static boolean isNotEmpty(String text) {
    return !isEmpty(text);
  }

  protected static boolean isEmpty(String text) {
    return text == null || text.isEmpty();
  }
}
