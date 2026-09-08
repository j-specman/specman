package specman.model.v002;

import specman.StepNumber;
import specman.editarea.document.WrappedDocument;
import specman.editarea.document.WrappedPosition;

import javax.swing.*;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.io.StringWriter;
import java.util.ArrayList;
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
   * @return list of steplink texts that could not be resolved (step was deleted) */
  public static List<String> rewrite(DiagramModel_V002 model, Map<String, String> numberMapping) throws Exception {
    List<String> brokenRefs = new ArrayList<>();
    if (numberMapping.isEmpty()) {
      return brokenRefs;
    }
    rewriteContent(model.intro, numberMapping, brokenRefs);
    rewriteContent(model.outro, numberMapping, brokenRefs);
    rewriteSequence(model.mainSequence, numberMapping, brokenRefs);
    return brokenRefs;
  }

  private static void rewriteSequence(
      StepSequenceModel_V002 seq, Map<String, String> numberMapping, List<String> brokenRefs) throws Exception {

    for (AbstractStepModel_V002 step : seq.steps) {
      rewriteContent(step.content, numberMapping, brokenRefs);
      for (NumberedSubSequence_V002 sub : step.subSequencesFor(StepNumber.EMPTY)) {
        if (sub.sequence instanceof BranchSequenceModel_V002 branch) {
          rewriteContent(branch.heading, numberMapping, brokenRefs);
        }
        rewriteSequence(sub.sequence, numberMapping, brokenRefs);
      }
    }
    if (seq.catchArea != null && seq.catchArea.catchSequences != null) {
      for (CatchSequenceModel_V002 catchSeq : seq.catchArea.catchSequences) {
        rewriteContent(catchSeq.heading, numberMapping, brokenRefs);
        if (catchSeq.coCatches != null) {
          for (CoCatchModel_V002 coCatch : catchSeq.coCatches) {
            rewriteContent(coCatch.heading, numberMapping, brokenRefs);
          }
        }
        rewriteSequence(catchSeq, numberMapping, brokenRefs);
      }
    }
  }

  private static void rewriteContent(
      EditorContentModel_V002 content, Map<String, String> numberMapping, List<String> brokenRefs) throws Exception {

    if (content == null) {
      return;
    }
    for (int i = 0; i < content.areas.size(); i++) {
      AbstractEditAreaModel_V002 area = content.areas.get(i);
      if (area instanceof TextEditAreaModel_V002 textArea) {
        TextEditAreaModel_V002 updated = rewriteTextArea(textArea, numberMapping, brokenRefs);
        if (updated != textArea) {
          content.areas.set(i, updated);
        }
      }
      else if (area instanceof TableEditAreaModel_V002 tableArea) {
        for (List<EditorContentModel_V002> row : tableArea.cells) {
          for (EditorContentModel_V002 cell : row) {
            rewriteContent(cell, numberMapping, brokenRefs);
          }
        }
      }
      else if (area instanceof ListItemEditAreaModel_V002 listItem) {
        rewriteContent(listItem.content, numberMapping, brokenRefs);
      }
    }
  }

  private static TextEditAreaModel_V002 rewriteTextArea(
      TextEditAreaModel_V002 model, Map<String, String> numberMapping, List<String> brokenRefs) throws Exception {

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

    if (!rewriteSteplinksInDocument(doc, markups, numberMapping, brokenRefs)) {
      return model;
    }

    StringWriter sw = new StringWriter();
    HTML_EDITOR_KIT.write(sw, ed.getDocument(), 0, ed.getDocument().getLength());
    String newText = sw.toString();
    String newPlainText = ed.getDocument().getText(0, ed.getDocument().getLength());

    return new TextEditAreaModel_V002(newText, newPlainText, markups, model.changeInfo);
  }

  private static boolean rewriteSteplinksInDocument(
      WrappedDocument doc, List<Markup_V002> markups,
      Map<String, String> numberMapping, List<String> brokenRefs) {

    boolean anyChanged = false;
    for (int i = markups.size() - 1; i >= 0; i--) {
      anyChanged |= rewriteSteplinkAt(i, doc, markups, numberMapping, brokenRefs);
    }
    return anyChanged;
  }

  private static boolean rewriteSteplinkAt(
      int i, WrappedDocument doc, List<Markup_V002> markups,
      Map<String, String> numberMapping, List<String> brokenRefs) {

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
    return true;
  }
}
