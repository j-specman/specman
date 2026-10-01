package specman.clipboard;

import specman.Aenderungsart;
import specman.ChangeInfo;
import specman.EditException;
import specman.StepNumber;
import specman.editarea.document.ChangemarkDecolorer;
import specman.editarea.document.WrappedDocumentUtil.DeletionRange;
import specman.editarea.document.WrappedDocument;
import specman.editarea.markups.MarkupBackgroundStyleInitializer;
import specman.editarea.markups.MarkupSearchPurpose;
import specman.model.v002.*;
import specman.model.v002.io.HtmlToPlainText;

import javax.swing.JEditorPane;
import javax.swing.text.StyledDocument;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Adjusts the change-tracking markings of step models pasted from an external Specman
 * instance (or plain text) to match the current application changemark setting.
 * Mutates the models in-place before any view is created.
 * <p>
 * Rules:
 * <ul>
 *   <li>Change tracking <b>ON</b>: every changemark at every level is set to
 *       <em>added in the current changeset</em>. The changeset background color
 *       appears on the step and area panels (driven by {@code changeInfo}), not as
 *       inline HTML coloring inside text areas.</li>
 *   <li>Change tracking <b>OFF</b>: every changemark is set to <em>untracked</em>.</li>
 *   <li>In both cases, any existing changeset coloring embedded in HTML text is
 *       removed: deleted text runs are physically removed, added/changed text runs
 *       are de-colored to the standard background.</li>
 *   <li>{@code Changed} markups are removed from the markup list; {@code ChangedSteplink}
 *       markups are converted to plain {@code Steplink} (the step reference is preserved,
 *       the change mark is dropped).</li>
 *   <li>Deleted components at every level (steps, areas, list items, catch sequences,
 *       co-catches) are physically removed from the model.</li>
 *   <li>A deleted top-level step aborts the paste entirely with an
 *       {@link EditException}.</li>
 * </ul>
 */
public class ExternalPasteChangemarksAdjuster implements PasteChangemarksAdjusterI {

  private ChangeInfoModel_V002 target;

  @Override
  public List<AbstractStepModel_V002> adjust(
      List<AbstractStepModel_V002> steps,
      boolean trackingOn) throws EditException {
    for (AbstractStepModel_V002 step : steps) {
      if (isDeleted(step.changeInfo)) {
        throw new EditException(
            "One or more steps in the clipboard are marked as deleted and cannot be pasted. " +
            "Remove deleted steps in a text editor before pasting.");
      }
    }
    target = ChangeInfoModel_V002.from(trackingOn ? ChangeInfo.added() : ChangeInfo.UNTRACKED);
    adjustSteps(steps);
    return steps;
  }

  /** Same rules as {@link #adjust} applied to a standalone copied/cut table (not nested inside
   * a pasted step): a table marked as deleted aborts the paste, otherwise every changemark in
   * the table and its cells is set to added/untracked and existing changeset HTML coloring is
   * cleaned up. */
  public TableEditAreaModel_V002 adjustTable(TableEditAreaModel_V002 table, boolean trackingOn) throws EditException {
    if (isDeleted(table.changeInfo)) {
      throw new EditException(
          "The table in the clipboard is marked as deleted and cannot be pasted. " +
          "Remove deleted content in a text editor before pasting.");
    }
    target = ChangeInfoModel_V002.from(trackingOn ? ChangeInfo.added() : ChangeInfo.UNTRACKED);
    table.changeInfo = target;
    table.cells.forEach(row -> row.forEach(this::adjustContent));
    return table;
  }

  private static boolean isDeleted(ChangeInfoModel_V002 ci) {
    return ci != null && ci.changetype == Aenderungsart.Geloescht;
  }

  // ---- Steps ----------------------------------------------------------------

  private void adjustSteps(List<AbstractStepModel_V002> steps) {
    steps.removeIf(s -> isDeleted(s.changeInfo));
    steps.forEach(this::adjustStep);
  }

  private void adjustStep(AbstractStepModel_V002 step) {
    step.changeInfo = target;
    adjustContent(step.content);
    step.subSequencesFor(StepNumber.EMPTY).forEach(ns -> adjustSequence(ns.sequence));
  }

  // ---- Sequences ------------------------------------------------------------

  private void adjustSequence(StepSequenceModel_V002 seq) {
    if (seq == null) return;
    seq.changeInfo = target;
    if (seq instanceof BranchSequenceModel_V002) {
      adjustContent(((BranchSequenceModel_V002) seq).heading);
    }
    adjustSteps(seq.steps);
    adjustCatchArea(seq.catchArea);
  }

  private void adjustCatchArea(CatchAreaModel_V002 catchArea) {
    if (catchArea == null || catchArea.catchSequences == null) return;
    catchArea.catchSequences.removeIf(cs -> isDeleted(cs.changeInfo));
    catchArea.catchSequences.forEach(cs -> {
      cs.changeInfo = target;
      adjustContent(cs.heading);
      adjustSteps(cs.steps);
      if (cs.coCatches != null) {
        cs.coCatches.removeIf(cc -> isDeleted(cc.changeInfo));
        cs.coCatches.forEach(cc -> {
          cc.changeInfo = target;
          adjustContent(cc.heading);
        });
      }
    });
  }

  // ---- Edit area content ----------------------------------------------------

  private void adjustContent(EditorContentModel_V002 content) {
    if (content == null) return;
    content.areas.removeIf(a -> isDeleted(a.changeInfo));
    content.areas.forEach(this::adjustArea);
  }

  private void adjustArea(AbstractEditAreaModel_V002 area) {
    ChangeInfoModel_V002 originalChangeInfo = area.changeInfo;
    area.changeInfo = target;
    if (area instanceof TextEditAreaModel_V002) {
      adjustTextArea((TextEditAreaModel_V002) area, originalChangeInfo);
    }
    else if (area instanceof ListItemEditAreaModel_V002) {
      adjustContent(((ListItemEditAreaModel_V002) area).content);
    }
    else if (area instanceof TableEditAreaModel_V002) {
      ((TableEditAreaModel_V002) area).cells.forEach(row -> row.forEach(this::adjustContent));
    }
  }

  // ---- Text area HTML changemarks cleanup -----------------------------------

  private void adjustTextArea(TextEditAreaModel_V002 area, ChangeInfoModel_V002 originalChangeInfo) {
    boolean hasChangeMarkups = area.markups != null && area.markups.stream().anyMatch(m -> m.type.marksChange());
    boolean hasChangeInfo = originalChangeInfo != null && originalChangeInfo.changetype != Aenderungsart.Untracked;
    if (!hasChangeMarkups && !hasChangeInfo) {
      return;
    }
    JEditorPane ed = HtmlToPlainText.fromHtml(area.text);
    WrappedDocument doc = new WrappedDocument((StyledDocument) ed.getDocument());
    new MarkupBackgroundStyleInitializer(doc, area.markups).styleChangedTextSections();
    ChangemarkDecolorer decolorer = new ChangemarkDecolorer(doc);
    List<DeletionRange> deletions = new ArrayList<>();
    decolorer.decolor(deletions, null);
    removeDeletions(deletions, doc);
    area.text = ed.getText();
    area.markups = decolorer.findMarkups(MarkupSearchPurpose.All);
  }

  private void removeDeletions(List<DeletionRange> deletions, WrappedDocument doc) {
    deletions.sort(Comparator.comparingInt(DeletionRange::start).reversed());
    for (DeletionRange range : deletions) {
      doc.remove(doc.fromModel(range.start()), range.end() - range.start());
    }
  }
}
