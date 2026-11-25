package specman.cli.sanitize;

import specman.model.v002.AbstractStepModel_V002;
import specman.model.v002.DiagramModel_V002;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Checks that each step ID is used by one step only. */
public class DuplicateIdValidator extends AbstractModelValidator {

  private final DiagramModel_V002 model;

  public DuplicateIdValidator(DiagramModel_V002 model) {
    super("Duplicate step IDs");
    this.model = model;
  }

  @Override
  protected void collectErrors(List<String> errors) {
    for (Map.Entry<String, List<String>> entry : collectStepNumbersById().entrySet()) {
      if (entry.getValue().size() > 1) {
        errors.add("ID " + entry.getKey() + " used by steps: " + entry.getValue());
      }
    }
  }

  private Map<String, List<String>> collectStepNumbersById() {
    Map<String, List<String>> stepNumbersById = new LinkedHashMap<>();
    for (AbstractStepModel_V002 step : model.queryAllSteps()) {
      stepNumbersById.computeIfAbsent(step.id, id -> new ArrayList<>()).add(displayedStepNumber(step));
    }
    return stepNumbersById;
  }
}
