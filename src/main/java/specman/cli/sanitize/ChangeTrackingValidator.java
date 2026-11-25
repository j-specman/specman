package specman.cli.sanitize;

import specman.model.v002.io.ParseOutcome;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks the change tracking of a model, which AI agents write by hand: the markups of the texts, the change sets they
 * refer to, and the pairs of source and target steps of moved steps. None of it can be repaired automatically, because
 * only the author knows what was meant. Violations are reported as one {@link specman.cli.CLIException}, each with the
 * position in the file as far as it is known, ordered by position.
 */
public class ChangeTrackingValidator extends AbstractModelValidator {

  private static final Pattern POSITION_PREFIX = Pattern.compile("^line (\\d+):(\\d+)");
  private static final long COLUMNS_PER_LINE = 1_000_000L;

  private final ParseOutcome outcome;

  public ChangeTrackingValidator(ParseOutcome outcome) {
    super("Invalid change tracking");
    this.outcome = outcome;
  }

  @Override
  protected void collectErrors(List<String> errors) {
    errors.addAll(outcome.diagnostics());
    errors.addAll(new MarkupValidator(outcome.model(), outcome.positions()).collectErrors());
    errors.addAll(new MovedStepValidator(outcome.model(), outcome.positions()).collectErrors());
    errors.sort(Comparator.comparing(ChangeTrackingValidator::positionOf));
  }

  /** Orders messages by their position in the file. Those without position come last. */
  private static long positionOf(String message) {
    Matcher matcher = POSITION_PREFIX.matcher(message);
    if (!matcher.find()) {
      return Long.MAX_VALUE;
    }
    return Long.parseLong(matcher.group(1)) * COLUMNS_PER_LINE + Long.parseLong(matcher.group(2));
  }
}
