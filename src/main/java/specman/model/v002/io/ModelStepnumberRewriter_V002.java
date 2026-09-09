package specman.model.v002.io;

import specman.StepNumber;
import specman.editarea.document.WrappedDocument;
import specman.editarea.document.WrappedPosition;
import specman.model.v002.AbstractEditAreaModel_V002;
import specman.model.v002.AbstractStepModel_V002;
import specman.model.v002.BranchSequenceModel_V002;
import specman.model.v002.CatchSequenceModel_V002;
import specman.model.v002.CoCatchModel_V002;
import specman.model.v002.DiagramModel_V002;
import specman.model.v002.EditorContentModel_V002;
import specman.model.v002.ListItemEditAreaModel_V002;
import specman.model.v002.Markup_V002;
import specman.model.v002.StepSequenceModel_V002;
import specman.model.v002.TableEditAreaModel_V002;
import specman.model.v002.TextEditAreaModel_V002;

import javax.swing.*;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rewrites Steplink text in all TextEditAreaModel_V002 instances of a diagram model.
 * Uses WrappedDocument so that markup positions (stored as WrappedPosition.toModel()
 * values relative to the visible text start) are handled exactly as in the rest of
 * the Specman codebase.
 * Modifies the model in place (replacing elements in the mutable areas lists).
 * Must be called with java.awt.headless=true already set before any AWT class is loaded.
 */
public class ModelStepnumberRewriter_V002 {

  private static final HTMLEditorKit HTML_EDITOR_KIT = new HTMLEditorKit();

  /** Rewrites all stale steplink texts in the model.
   * @param numberMapping maps each step's saved number to its newly computed number
   *                      (built by ModelRenumberer_V002.buildNumberMapping)
   * @return SanitizeResult containing broken refs and steplink updates with locations */
  public static SanitizeResult rewrite(DiagramModel_V002 model, Map<String, String> numberMapping) throws Exception {
    List<String> brokenRefs = new ArrayList<>();
    // location → (oldNum → newNum)
    Map<String, Map<String, String>> locationUpdates = new LinkedHashMap<>();

    if (!numberMapping.isEmpty()) {
      rewriteContent(model.intro, "intro", numberMapping, brokenRefs, locationUpdates);
      rewriteContent(model.outro, "outro", numberMapping, brokenRefs, locationUpdates);
      rewriteSequence(model.mainSequence, numberMapping, brokenRefs, locationUpdates);
    }

    List<SteplinkUpdate> steplinkUpdates = flattenUpdates(locationUpdates);
    return new SanitizeResult(steplinkUpdates, brokenRefs);
  }

  private static List<SteplinkUpdate> flattenUpdates(Map<String, Map<String, String>> locationUpdates) {
    List<SteplinkUpdate> result = new ArrayList<>();
    for (Map.Entry<String, Map<String, String>> locEntry : locationUpdates.entrySet()) {
      String location = locEntry.getKey();
      for (Map.Entry<String, String> numEntry : locEntry.getValue().entrySet()) {
        result.add(new SteplinkUpdate(location, numEntry.getKey(), numEntry.getValue()));
      }
    }
    return result;
  }

  private static void rewriteSequence(
      StepSequenceModel_V002 seq,
      Map<String, String> numberMapping,
      List<String> brokenRefs,
      Map<String, Map<String, String>> locationUpdates) throws Exception {

    for (AbstractStepModel_V002 step : seq.steps) {
      String location = "Step " + step.stepNumber + " (" + ModelRenumberer_V002.stepType(step) + ")";
      rewriteContent(step.content, location, numberMapping, brokenRefs, locationUpdates);
      for (NumberedSubSequence_V002 sub : step.subSequencesFor(StepNumber.EMPTY)) {
        if (sub.sequence instanceof BranchSequenceModel_V002 branch) {
          rewriteContent(branch.heading, location, numberMapping, brokenRefs, locationUpdates);
        }
        rewriteSequence(sub.sequence, numberMapping, brokenRefs, locationUpdates);
      }
    }
    if (seq.catchArea != null && seq.catchArea.catchSequences != null) {
      for (CatchSequenceModel_V002 catchSeq : seq.catchArea.catchSequences) {
        // Use catch sequence's own step numbering from steps within it
        rewriteContent(catchSeq.heading, "catch", numberMapping, brokenRefs, locationUpdates);
        if (catchSeq.coCatches != null) {
          for (CoCatchModel_V002 coCatch : catchSeq.coCatches) {
            rewriteContent(coCatch.heading, "catch", numberMapping, brokenRefs, locationUpdates);
          }
        }
        rewriteSequence(catchSeq, numberMapping, brokenRefs, locationUpdates);
      }
    }
  }

  private static void rewriteContent(
      EditorContentModel_V002 content,
      String location,
      Map<String, String> numberMapping,
      List<String> brokenRefs,
      Map<String, Map<String, String>> locationUpdates) throws Exception {

    if (content == null) {
      return;
    }
    for (int i = 0; i < content.areas.size(); i++) {
      AbstractEditAreaModel_V002 area = content.areas.get(i);
      if (area instanceof TextEditAreaModel_V002 textArea) {
        TextEditAreaModel_V002 updated = rewriteTextArea(textArea, location, numberMapping, brokenRefs, locationUpdates);
        if (updated != textArea) {
          content.areas.set(i, updated);
        }
      }
      else if (area instanceof TableEditAreaModel_V002 tableArea) {
        for (List<EditorContentModel_V002> row : tableArea.cells) {
          for (EditorContentModel_V002 cell : row) {
            rewriteContent(cell, location, numberMapping, brokenRefs, locationUpdates);
          }
        }
      }
      else if (area instanceof ListItemEditAreaModel_V002 listItem) {
        rewriteContent(listItem.content, location, numberMapping, brokenRefs, locationUpdates);
      }
    }
  }

  private static TextEditAreaModel_V002 rewriteTextArea(
      TextEditAreaModel_V002 model,
      String location,
      Map<String, String> numberMapping,
      List<String> brokenRefs,
      Map<String, Map<String, String>> locationUpdates) throws Exception {

    if (model.markups == null || model.markups.isEmpty()) {
      return model;
    }
    boolean hasSteplink = model.markups.stream().anyMatch(m -> m.type.isSteplink());
    if (!hasSteplink) {
      return model;
    }

    JEditorPane ed = new JEditorPane();
    ed.setEditorKit(HTML_EDITOR_KIT);
    ed.setText(model.text);
    SwingUtilities.invokeAndWait(() -> {});

    WrappedDocument doc = new WrappedDocument((StyledDocument) ed.getDocument());
    List<Markup_V002> markups = new ArrayList<>(model.markups);

    if (!rewriteSteplinksInDocument(doc, markups, location, numberMapping, brokenRefs, locationUpdates)) {
      return model;
    }

    StringWriter sw = new StringWriter();
    HTML_EDITOR_KIT.write(sw, ed.getDocument(), 0, ed.getDocument().getLength());
    String newText = sw.toString();
    String newPlainText = ed.getDocument().getText(0, ed.getDocument().getLength());

    return new TextEditAreaModel_V002(newText, newPlainText, markups, model.changeInfo);
  }

  private static boolean rewriteSteplinksInDocument(
      WrappedDocument doc,
      List<Markup_V002> markups,
      String location,
      Map<String, String> numberMapping,
      List<String> brokenRefs,
      Map<String, Map<String, String>> locationUpdates) {

    boolean anyChanged = false;
    for (int i = markups.size() - 1; i >= 0; i--) {
      anyChanged |= rewriteSteplinkAt(i, doc, markups, location, numberMapping, brokenRefs, locationUpdates);
    }
    return anyChanged;
  }

  private static boolean rewriteSteplinkAt(
      int i,
      WrappedDocument doc,
      List<Markup_V002> markups,
      String location,
      Map<String, String> numberMapping,
      List<String> brokenRefs,
      Map<String, Map<String, String>> locationUpdates) {

    Markup_V002 m = markups.get(i);
    if (!m.type.isSteplink()) {
      return false;
    }
    int currentNumLength = m.to - m.from + 1;
    WrappedPosition pos = doc.fromModel(m.from);
    String currentNum = doc.getText(pos, currentNumLength);
    String newNum = numberMapping.get(currentNum);

    if (newNum == null) {
      brokenRefs.add(currentNum);
      return false;
    }
    if (newNum.equals(currentNum)) {
      return false;
    }
    doc.remove(pos, currentNumLength);
    doc.insertString(pos, newNum, doc.getCharacterElement(pos).getAttributes());

    int delta = newNum.length() - currentNum.length();
    markups.set(i, new Markup_V002(m.from, m.from + newNum.length() - 1, m.type, m.changeset));
    for (int j = i + 1; j < markups.size(); j++) {
      Markup_V002 mj = markups.get(j);
      markups.set(j, new Markup_V002(mj.from + delta, mj.to + delta, mj.type, mj.changeset));
    }

    // Record the successful update grouped by location
    locationUpdates.computeIfAbsent(location, k -> new LinkedHashMap<>()).put(currentNum, newNum);
    return true;
  }
}
