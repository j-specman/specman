package specman.visitor;

import specman.StepNumber;
import specman.view.AbstractSchrittView;

import java.util.LinkedHashMap;
import java.util.Map;

/** Collects the current step number of each step by the step's id, in document order, including the steps in catch sequences. */
public class StepNumberIndexCollector implements DiagramVisitor {

  private final Map<String, String> index = new LinkedHashMap<>();

  @Override
  public void visitStep(AbstractSchrittView step) {
    StepNumber number = step.getNumber();
    if (number != null) {
      index.put(step.getId(), number.toString());
    }
  }

  /** @return the step numbers by step id collected so far, a modifiable map the caller owns */
  public Map<String, String> index() {
    return index;
  }
}
