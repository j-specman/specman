package specman.model.v002.io;

import specman.StepNumber;
import specman.model.v002.AbstractStepModel_V002;
import specman.model.v002.BreakStepModel_V002;
import specman.model.v002.CaseStepModel_V002;
import specman.model.v002.CatchSequenceModel_V002;
import specman.model.v002.DoWhileStepModel_V002;
import specman.model.v002.IfElseStepModel_V002;
import specman.model.v002.IfStepModel_V002;
import specman.model.v002.SimpleStepModel_V002;
import specman.model.v002.StepSequenceModel_V002;
import specman.model.v002.SubsequenceStepModel_V002;
import specman.model.v002.TextEditAreaModel_V002;
import specman.model.v002.WhileStepModel_V002;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Assigns step numbers directly to each step's {@code stepNumber} field without Swing.
 * Mirrors the polymorphic setNumber() dispatch in the view classes.
 * Step-type-specific logic (which sub-sequences exist, how many slots each
 * occupies in the outer sequence) lives in the model classes via
 * subSequencesFor() and nextSlotInOuterSequence().
 */
public class ModelRenumberer_V002 {

  private static final int PLAIN_TEXT_MAX = 60;

  /** Sets {@code step.stepNumber} on every step in the tree in-place. */
  public static void renumber(StepSequenceModel_V002 mainSequence) {
    Map<String, StepNumber> stepNumbers = new LinkedHashMap<>();
    renumberSequence(mainSequence, StepNumber.EMPTY, stepNumbers);
  }

  /** Collects the current {@code stepNumber} of every step in the tree as id → stepNumber map.
   *  Steps with a null stepNumber are omitted. */
  public static Map<String, String> collectNumbers(StepSequenceModel_V002 seq) {
    Map<String, String> result = new LinkedHashMap<>();
    collectNumbersRecursively(seq, result);
    return result;
  }

  private static void collectNumbersRecursively(StepSequenceModel_V002 seq, Map<String, String> result) {
    if (seq == null || seq.steps == null) {
      return;
    }
    for (AbstractStepModel_V002 step : seq.steps) {
      if (step.stepNumber != null) {
        result.put(step.id, step.stepNumber);
      }
      for (NumberedSubSequence_V002 sub : step.subSequencesFor(StepNumber.EMPTY)) {
        collectNumbersRecursively(sub.sequence, result);
      }
    }
    if (seq.catchArea != null && seq.catchArea.catchSequences != null) {
      for (CatchSequenceModel_V002 catchSeq : seq.catchArea.catchSequences) {
        collectNumbersRecursively(catchSeq, result);
      }
    }
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

  /** Builds the list of step number changes by comparing saved and computed numbers.
   *  Includes step type and truncated plain text for human/agent orientation. */
  public static List<StepNumberChange> buildStepNumberChanges(
      StepSequenceModel_V002 mainSequence,
      Map<String, String> savedNumbers,
      Map<String, String> computedNumbers) {

    List<StepNumberChange> changes = new ArrayList<>();
    List<AbstractStepModel_V002> allSteps = new ArrayList<>();
    collectAllSteps(mainSequence, allSteps);

    for (AbstractStepModel_V002 step : allSteps) {
      String oldNum = savedNumbers.get(step.id);
      String newNum = computedNumbers.get(step.id);
      if (oldNum != null && newNum != null && !oldNum.equals(newNum)) {
        changes.add(new StepNumberChange(oldNum, newNum, stepType(step), extractPlainText(step)));
      }
    }
    return changes;
  }

  /** Returns the grammar keyword for this step type. */
  public static String stepType(AbstractStepModel_V002 step) {
    if (step instanceof DoWhileStepModel_V002)      { return "doWhile"; }
    if (step instanceof WhileStepModel_V002)         { return "while"; }
    if (step instanceof IfElseStepModel_V002)        { return "ifElse"; }
    if (step instanceof IfStepModel_V002)            { return "if"; }
    if (step instanceof CaseStepModel_V002)          { return "case"; }
    if (step instanceof SubsequenceStepModel_V002)   { return "subsequence"; }
    if (step instanceof BreakStepModel_V002)         { return "break"; }
    if (step instanceof SimpleStepModel_V002)        { return "simple"; }
    return "step";
  }

  private static String extractPlainText(AbstractStepModel_V002 step) {
    if (step.content == null || step.content.areas == null || step.content.areas.isEmpty()) {
      return "";
    }
    if (!(step.content.areas.get(0) instanceof TextEditAreaModel_V002 textArea)) {
      return "";
    }
    String plain = textArea.plainText;
    if (plain == null || plain.isEmpty()) {
      return "";
    }
    plain = plain.replace("\n", " ").trim();
    if (plain.length() > PLAIN_TEXT_MAX) {
      return plain.substring(0, PLAIN_TEXT_MAX) + "...";
    }
    return plain;
  }

  private static void collectAllSteps(StepSequenceModel_V002 seq, List<AbstractStepModel_V002> result) {
    if (seq == null || seq.steps == null) {
      return;
    }
    for (AbstractStepModel_V002 step : seq.steps) {
      result.add(step);
      for (NumberedSubSequence_V002 sub : step.subSequencesFor(StepNumber.EMPTY)) {
        collectAllSteps(sub.sequence, result);
      }
    }
    if (seq.catchArea != null && seq.catchArea.catchSequences != null) {
      for (CatchSequenceModel_V002 catchSeq : seq.catchArea.catchSequences) {
        collectAllSteps(catchSeq, result);
      }
    }
  }

  private static void renumberSequence(
      StepSequenceModel_V002 seq,
      StepNumber base,
      Map<String, StepNumber> stepNumbers) {

    StepNumber current = base;
    for (AbstractStepModel_V002 step : seq.steps) {
      StepNumber stepNum = current.naechsteID();
      step.stepNumber = stepNum.toString();
      stepNumbers.put(step.id, stepNum);
      for (NumberedSubSequence_V002 sub : step.subSequencesFor(stepNum)) {
        renumberSequence(sub.sequence, sub.base, stepNumbers);
      }
      current = step.nextSlotInOuterSequence(stepNum, stepNumbers);
    }

    // Catch sequences: their base is break step's number.naechsteEbene().
    // All break steps are already in stepNumbers by now (depth-first traversal above).
    if (seq.catchArea != null && seq.catchArea.catchSequences != null) {
      for (CatchSequenceModel_V002 catchSeq : seq.catchArea.catchSequences) {
        StepNumber breakStepNum = stepNumbers.get(catchSeq.id);
        if (breakStepNum != null) {
          renumberSequence(catchSeq, breakStepNum.naechsteEbene(), stepNumbers);
        }
      }
    }
  }
}

