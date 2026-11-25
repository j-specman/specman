package specman.cli;

import specman.ChangeSet;
import specman.model.v002.AbstractEditAreaModel_V002;
import specman.model.v002.AbstractStepModel_V002;
import specman.model.v002.DiagramModel_V002;
import specman.model.v002.Markup_V002;
import specman.model.v002.SourceStepModel_V002;
import specman.model.v002.TextEditAreaModel_V002;
import specman.model.v002.io.ModelSourcePositions;
import specman.model.v002.io.ParseOutcome;
import specman.model.v002.io.TextAreaTraversal_V002;
import specman.model.v002.io.TextAreaTraversal_V002.Site;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks the change tracking of a model, which AI agents write by hand: the markups of the texts, the change sets they
 * refer to, and the pairs of source and target steps of moved steps. None of it can be repaired automatically, because
 * only the author knows what was meant. Violations are reported as one {@link CLIException}, each with the position in
 * the file as far as it is known, ordered by position.
 * <p>
 * The markups have to be checked against the plain texts as the sanitizer regenerates them, because their offsets refer
 * to the visible text, which the plain text is the rendition of.
 */
final class SanitizerChangeTrackingValidator {

  private static final Pattern POSITION_PREFIX = Pattern.compile("^line (\\d+):(\\d+)");
  private static final long COLUMNS_PER_LINE = 1_000_000L;
  private static final String KNOWN_CHANGESETS = String.join(", ", ChangeSet.ALL.keySet());

  private SanitizerChangeTrackingValidator() {
  }

  static void assertValidChangeTracking(ParseOutcome outcome) throws CLIException {
    List<String> errors = new ArrayList<>(outcome.diagnostics());
    checkMarkups(outcome.model(), outcome.positions(), errors);
    checkMovedSteps(outcome.model(), outcome.positions(), errors);
    errors.sort(Comparator.comparing(SanitizerChangeTrackingValidator::positionOf));
    AbstractCLO.assertNoErrors("Invalid change tracking", errors);
  }

  // ---- markups ----

  private static void checkMarkups(DiagramModel_V002 model, ModelSourcePositions positions, List<String> errors) {
    TextAreaTraversal_V002.forEachContent(model, (content, site) -> {
      for (AbstractEditAreaModel_V002 area : content.areas) {
        if (area instanceof TextEditAreaModel_V002 textArea) {
          checkMarkupsOfText(textArea, site, positions, errors);
        }
      }
    });
  }

  private static void checkMarkupsOfText(TextEditAreaModel_V002 area, Site site, ModelSourcePositions positions,
                                         List<String> errors) {
    if (area.markups == null) {
      return;
    }
    String plain = area.plainText != null ? area.plainText : "";
    Markup_V002 previous = null;
    for (Markup_V002 markup : area.markups) {
      String where = located(positions.of(markup), SanitizeModelValidator.describe(site) + ": markup " + range(markup));
      if (markup.from > markup.to) {
        errors.add(where + " ends before it starts");
      }
      else if (markup.from < 0 || markup.to >= plain.length()) {
        errors.add(where + " lies outside of the " + describePlain(plain));
      }
      if (previous != null && markup.from <= previous.to) {
        errors.add(where + " overlaps or precedes the previous markup " + range(previous)
          + ", markups must be in ascending order without overlap. Offsets refer to the " + describePlain(plain));
      }
      checkMarkupChangeset(markup, where, errors);
      previous = markup;
    }
  }

  private static void checkMarkupChangeset(Markup_V002 markup, String where, List<String> errors) {
    if (markup.changeset == null) {
      if (markup.type != null && markup.type.marksChange()) {
        errors.add(where + " is of type " + markup.type + " but names no changeset. Known changesets: " + KNOWN_CHANGESETS);
      }
    }
    else if (ChangeSet.fromName(markup.changeset) == null) {
      errors.add(where + " names the unknown changeset '" + markup.changeset + "'. Known changesets: " + KNOWN_CHANGESETS);
    }
  }

  /** The plain text is part of each message, because the sanitizer doesn't write the file if it finds errors, so the
   * plain text in the file may be outdated. */
  private static String describePlain(String plain) {
    String lastIndex = plain.isEmpty() ? "" : ", last index " + (plain.length() - 1);
    return "plain text (length " + plain.length() + lastIndex + "): `" + plain + "`";
  }

  private static String range(Markup_V002 markup) {
    return "(" + markup.from + "," + markup.to + ")";
  }

  // ---- moved steps ----

  private static void checkMovedSteps(DiagramModel_V002 model, ModelSourcePositions positions, List<String> errors) {
    List<AbstractStepModel_V002> steps = model.queryAllSteps();
    Map<String, AbstractStepModel_V002> stepsById = new HashMap<>();
    for (AbstractStepModel_V002 step : steps) {
      stepsById.put(AbstractStepModel_V002.normalizeId(step.id), step);
    }
    for (AbstractStepModel_V002 step : steps) {
      String where = located(positions.of(step), SanitizeModelValidator.describe(step));
      AbstractStepModel_V002 partner = stepsById.get(AbstractStepModel_V002.normalizeId(step.sourceStepId));
      if (step.isTargetStep()) {
        checkTargetStep(step, partner, where, errors);
      }
      else if (step instanceof SourceStepModel_V002) {
        checkSourceStep(step, partner, where, errors);
      }
      else if (step.sourceStepId != null) {
        errors.add(where + ": sourceStep=" + step.sourceStepId + " is only allowed at target steps, i.e. with change=(target, ...)");
      }
    }
  }

  private static void checkTargetStep(AbstractStepModel_V002 target, AbstractStepModel_V002 partner, String where,
                                      List<String> errors) {
    if (target.sourceStepId == null) {
      errors.add(where + ": target step without sourceStep= referring to its source step");
    }
    else if (partner == null) {
      errors.add(where + ": sourceStep=" + target.sourceStepId + " refers to a step which doesn't exist");
    }
    else if (!(partner instanceof SourceStepModel_V002)) {
      errors.add(where + ": sourceStep=" + target.sourceStepId + " refers to " + SanitizeModelValidator.describe(partner)
        + " which is not a source step");
    }
    else if (!refersTo(partner, target)) {
      errors.add(where + ": " + SanitizeModelValidator.describe(partner) + " is no counterpart, it refers to "
        + describeReference(partner) + " instead of this step");
    }
    else if (!Objects.equals(changesetName(partner), changesetName(target))) {
      errors.add(where + ": source and target step must belong to the same changeset, but the source "
        + SanitizeModelValidator.describe(partner) + " has '" + changesetName(partner) + "' and the target has '"
        + changesetName(target) + "'");
    }
  }

  private static void checkSourceStep(AbstractStepModel_V002 source, AbstractStepModel_V002 partner, String where,
                                      List<String> errors) {
    if (source.sourceStepId == null) {
      errors.add(where + ": source step without sourceStep= referring to its target step");
    }
    else if (partner == null) {
      errors.add(where + ": sourceStep=" + source.sourceStepId + " refers to a step which doesn't exist");
    }
    else if (!partner.isTargetStep()) {
      errors.add(where + ": sourceStep=" + source.sourceStepId + " refers to " + SanitizeModelValidator.describe(partner)
        + " which is not a target step");
    }
    else if (!refersTo(partner, source)) {
      errors.add(where + ": " + SanitizeModelValidator.describe(partner) + " is no counterpart, it refers to "
        + describeReference(partner) + " instead of this step");
    }
  }

  private static boolean refersTo(AbstractStepModel_V002 referrer, AbstractStepModel_V002 referenced) {
    return referrer.sourceStepId != null && Objects.equals(
      AbstractStepModel_V002.normalizeId(referrer.sourceStepId), AbstractStepModel_V002.normalizeId(referenced.id));
  }

  private static String describeReference(AbstractStepModel_V002 step) {
    return step.sourceStepId != null ? step.sourceStepId : "no step";
  }

  private static String changesetName(AbstractStepModel_V002 step) {
    return step.changeInfo != null ? step.changeInfo.changeset : null;
  }

  // ---- messages ----

  private static String located(String position, String message) {
    return position.isEmpty() ? message : position + " " + message;
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
