package specman.cli.sanitize;

import specman.model.v002.AbstractStepModel_V002;
import specman.model.v002.DiagramModel_V002;
import specman.model.v002.SourceStepModel_V002;
import specman.model.v002.io.ModelSourcePositions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Checks the pairs of source and target steps of moved steps. */
final class MovedStepValidator extends AbstractModelValidator {

  private static final String SOURCE_STEP = ": sourceStep=";

  private final DiagramModel_V002 model;
  private final ModelSourcePositions positions;

  MovedStepValidator(DiagramModel_V002 model, ModelSourcePositions positions) {
    super("Invalid moved steps");
    this.model = model;
    this.positions = positions;
  }

  @Override
  protected void collectErrors(List<String> errors) {
    List<AbstractStepModel_V002> steps = model.queryAllSteps();
    Map<String, AbstractStepModel_V002> stepsById = indexById(steps);
    for (AbstractStepModel_V002 step : steps) {
      String where = located(positions.of(step), describe(step));
      AbstractStepModel_V002 partner = stepsById.get(AbstractStepModel_V002.normalizeId(step.sourceStepId));
      if (step.isTargetStep()) {
        checkTargetStep(step, partner, where, errors);
      }
      else if (isSourceStep(step)) {
        checkSourceStep(step, partner, where, errors);
      }
      else if (refersToAStep(step)) {
        errors.add(where + SOURCE_STEP + step.sourceStepId + " is only allowed at target steps, i.e. with change=(target, ...)");
      }
    }
  }

  private static Map<String, AbstractStepModel_V002> indexById(List<AbstractStepModel_V002> steps) {
    Map<String, AbstractStepModel_V002> stepsById = new HashMap<>();
    for (AbstractStepModel_V002 step : steps) {
      stepsById.put(AbstractStepModel_V002.normalizeId(step.id), step);
    }
    return stepsById;
  }

  private static void checkTargetStep(AbstractStepModel_V002 target, AbstractStepModel_V002 partner, String where,
                                      List<String> errors) {
    if (!refersToAStep(target)) {
      errors.add(where + ": target step without sourceStep= referring to its source step");
    }
    else if (partner == null) {
      errors.add(where + SOURCE_STEP + target.sourceStepId + " refers to a step which doesn't exist");
    }
    else if (!isSourceStep(partner)) {
      errors.add(where + SOURCE_STEP + target.sourceStepId + " refers to " + describe(partner)
        + " which is not a source step");
    }
    else if (!refersTo(partner, target)) {
      errors.add(noCounterpart(where, partner));
    }
    else if (!Objects.equals(changesetName(partner), changesetName(target))) {
      errors.add(where + ": source and target step must belong to the same changeset, but the source "
        + describe(partner) + " has '" + changesetName(partner) + "' and the target has '"
        + changesetName(target) + "'");
    }
  }

  private static void checkSourceStep(AbstractStepModel_V002 source, AbstractStepModel_V002 partner, String where,
                                      List<String> errors) {
    if (!refersToAStep(source)) {
      errors.add(where + ": source step without sourceStep= referring to its target step");
    }
    else if (partner == null) {
      errors.add(where + SOURCE_STEP + source.sourceStepId + " refers to a step which doesn't exist");
    }
    else if (!partner.isTargetStep()) {
      errors.add(where + SOURCE_STEP + source.sourceStepId + " refers to " + describe(partner)
        + " which is not a target step");
    }
    else if (!refersTo(partner, source)) {
      errors.add(noCounterpart(where, partner));
    }
  }

  private static String noCounterpart(String where, AbstractStepModel_V002 partner) {
    return where + ": " + describe(partner) + " is no counterpart, it refers to " + describeReference(partner)
      + " instead of this step";
  }

  private static boolean isSourceStep(AbstractStepModel_V002 step) {
    return step instanceof SourceStepModel_V002;
  }

  private static boolean refersToAStep(AbstractStepModel_V002 step) {
    return step.sourceStepId != null;
  }

  private static boolean refersTo(AbstractStepModel_V002 referrer, AbstractStepModel_V002 referenced) {
    return refersToAStep(referrer) && Objects.equals(
      AbstractStepModel_V002.normalizeId(referrer.sourceStepId), AbstractStepModel_V002.normalizeId(referenced.id));
  }

  private static String describeReference(AbstractStepModel_V002 step) {
    return refersToAStep(step) ? step.sourceStepId : "no step";
  }

  private static String changesetName(AbstractStepModel_V002 step) {
    return step.changeInfo != null ? step.changeInfo.changeset : null;
  }
}
