package specman.model.v002.io;

import specman.StepNumber;
import specman.model.v002.AbstractEditAreaModel_V002;
import specman.model.v002.AbstractStepModel_V002;
import specman.model.v002.BranchSequenceModel_V002;
import specman.model.v002.CatchSequenceModel_V002;
import specman.model.v002.CoCatchModel_V002;
import specman.model.v002.DiagramModel_V002;
import specman.model.v002.EditorContentModel_V002;
import specman.model.v002.ListItemEditAreaModel_V002;
import specman.model.v002.StepSequenceModel_V002;
import specman.model.v002.TableEditAreaModel_V002;
import specman.model.v002.TextEditAreaModel_V002;

import java.util.List;

/** Ensures the plain=`...` field of every TextEditArea is in sync with its HTML content. */
public class PlainTextSynchronizer_V002 {

  /** Re-renders plain text for every TextEditArea where the stored plain text
   * differs from a fresh rendering of its HTML content.
   * @return number of text areas updated */
  public static int updateAllPlainTexts(DiagramModel_V002 model) throws Exception {
    int[] count = {0};
    updatePlainTextsInContent(model.intro, count);
    updatePlainTextsInContent(model.outro, count);
    if (model.mainSequence != null) {
      updatePlainTextsInSequence(model.mainSequence, count);
    }
    return count[0];
  }

  private static void updatePlainTextsInSequence(StepSequenceModel_V002 seq, int[] count) throws Exception {
    if (seq == null || seq.steps == null) {
      return;
    }
    for (AbstractStepModel_V002 step : seq.steps) {
      updatePlainTextsInContent(step.content, count);
      for (NumberedSubSequence_V002 sub : step.subSequencesFor(StepNumber.EMPTY)) {
        if (sub.sequence instanceof BranchSequenceModel_V002 branch) {
          updatePlainTextsInContent(branch.heading, count);
        }
        updatePlainTextsInSequence(sub.sequence, count);
      }
    }
    if (seq.catchArea != null && seq.catchArea.catchSequences != null) {
      for (CatchSequenceModel_V002 catchSeq : seq.catchArea.catchSequences) {
        updatePlainTextsInContent(catchSeq.heading, count);
        if (catchSeq.coCatches != null) {
          for (CoCatchModel_V002 coCatch : catchSeq.coCatches) {
            updatePlainTextsInContent(coCatch.heading, count);
          }
        }
        updatePlainTextsInSequence(catchSeq, count);
      }
    }
  }

  private static void updatePlainTextsInContent(EditorContentModel_V002 content, int[] count) throws Exception {
    if (content == null || content.areas == null) {
      return;
    }
    for (int i = 0; i < content.areas.size(); i++) {
      AbstractEditAreaModel_V002 area = content.areas.get(i);
      if (area instanceof TextEditAreaModel_V002 textArea) {
        TextEditAreaModel_V002 updated = refreshPlainText(textArea);
        if (updated != textArea) {
          content.areas.set(i, updated);
          count[0]++;
        }
      } else if (area instanceof TableEditAreaModel_V002 tableArea) {
        for (List<EditorContentModel_V002> row : tableArea.cells) {
          for (EditorContentModel_V002 cell : row) {
            updatePlainTextsInContent(cell, count);
          }
        }
      } else if (area instanceof ListItemEditAreaModel_V002 listItem) {
        updatePlainTextsInContent(listItem.content, count);
      }
    }
  }

  private static TextEditAreaModel_V002 refreshPlainText(TextEditAreaModel_V002 model) throws Exception {
    if (model.text == null) {
      return model;
    }
    String freshPlainText = HtmlToPlainText.convert(model.text);
    if (freshPlainText.equals(model.plainText)) {
      return model;
    }
    return new TextEditAreaModel_V002(model.text, freshPlainText, model.markups, model.changeInfo);
  }
}
