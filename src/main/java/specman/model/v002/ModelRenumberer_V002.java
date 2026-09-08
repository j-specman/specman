package specman.model.v002;

import specman.StepNumber;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Computes a fresh stepNumberIndex from the model tree without Swing.
 * Mirrors the polymorphic setNumber() dispatch in the view classes.
 * Step-type-specific logic (which sub-sequences exist, how many slots each
 * occupies in the outer sequence) lives in the model classes via
 * subSequencesFor() and nextSlotInOuterSequence().
 */
public class ModelRenumberer_V002 {

  public static Map<String, String> renumber(StepSequenceModel_V002 mainSequence) {
    Map<String, String> index = new LinkedHashMap<>();
    Map<String, StepNumber> stepNumbers = new LinkedHashMap<>();
    renumberSequence(mainSequence, StepNumber.EMPTY, index, stepNumbers);
    return index;
  }

  /** Builds a mapping from each step's saved number to its newly computed number.
   * Steps present in both indexes are included — the caller can then detect
   * changed references (saved != computed) and broken references (saved number
   * not found at all because the step was deleted). */
  public static Map<String, String> buildNumberMapping(
      Map<String, String> savedIndex, Map<String, String> computedIndex) {

    Map<String, String> result = new LinkedHashMap<>();
    for (Map.Entry<String, String> entry : computedIndex.entrySet()) {
      String savedNum = savedIndex.get(entry.getKey());
      if (savedNum != null) {
        result.put(savedNum, entry.getValue());
      }
    }
    return result;
  }

  private static void renumberSequence(
      StepSequenceModel_V002 seq,
      StepNumber base,
      Map<String, String> index,
      Map<String, StepNumber> stepNumbers) {

    StepNumber current = base;
    for (AbstractStepModel_V002 step : seq.steps) {
      StepNumber stepNum = current.naechsteID();
      index.put(step.id, stepNum.toString());
      stepNumbers.put(step.id, stepNum);
      for (NumberedSubSequence_V002 sub : step.subSequencesFor(stepNum)) {
        renumberSequence(sub.sequence, sub.base, index, stepNumbers);
      }
      current = step.nextSlotInOuterSequence(stepNum, stepNumbers);
    }

    // Catch sequences: their base is break step's number.naechsteEbene().
    // The catch sequences may be linked to break steps from this sequence OR from
    // any nested sub-sequence. Since sub-sequences are processed recursively above
    // (depth-first), all break steps are already in stepNumbers by now, regardless
    // of how deeply nested they are.
    if (seq.catchArea != null && seq.catchArea.catchSequences != null) {
      for (CatchSequenceModel_V002 catchSeq : seq.catchArea.catchSequences) {
        StepNumber breakStepNum = stepNumbers.get(catchSeq.id);
        if (breakStepNum != null) {
          renumberSequence(catchSeq, breakStepNum.naechsteEbene(), index, stepNumbers);
        }
      }
    }
  }
}
