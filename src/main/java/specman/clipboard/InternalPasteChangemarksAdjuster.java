package specman.clipboard;

import specman.ChangeInfo;
import specman.EditException;
import specman.StepNumber;
import specman.model.v002.*;

import java.util.List;

/**
 * Adjusts the change-tracking markings of step models pasted from the same Specman
 * instance to match the current application changemark setting.
 * Mutates the models in-place before any view is created.
 * <p>
 * Only called when change tracking is ON. Rules:
 * <ul>
 *   <li>The top-level step's changemark is set to <em>added in the current changeset</em>,
 *       even if it carried a different changeset from the clipboard.</li>
 *   <li>All content areas directly owned by the top-level step are likewise set to
 *       <em>added in the current changeset</em>. This includes areas nested inside
 *       list items and table cells, as well as the heading areas of branch
 *       sub-sequences (if-condition, case-label, while-condition, etc.).</li>
 *   <li>Sub-steps and their content keep their original markings unchanged — the
 *       internal change history of pasted sub-steps is preserved as-is.</li>
 *   <li>No content is deleted or modified; HTML text and markup lists are not touched.</li>
 * </ul>
 */
public class InternalPasteChangemarksAdjuster implements PasteChangemarksAdjusterI {

  @Override
  public List<AbstractStepModel_V002> adjust(List<AbstractStepModel_V002> steps, boolean trackingOn) throws EditException {
    if (trackingOn) {
      steps.forEach(InternalPasteChangemarksAdjuster::adjustStep);
    }
    return steps;
  }

  /** Marks a standalone copied/cut table (not nested inside a pasted step) and all its cell
   * content as added in the current changeset - the same rule applied to tables nested inside
   * a pasted step, see {@link #adjustTableCells}. No-op when change tracking is off, mirroring
   * {@link #adjust}. */
  public static TableEditAreaModel_V002 adjustTable(TableEditAreaModel_V002 table, boolean trackingOn) {
    if (trackingOn) {
      ChangeInfoModel_V002 target = ChangeInfoModel_V002.from(ChangeInfo.added());
      table.changeInfo = target;
      adjustTableCells(table, target);
    }
    return table;
  }

  private static void adjustStep(AbstractStepModel_V002 step) {
    ChangeInfoModel_V002 target = ChangeInfoModel_V002.from(ChangeInfo.added());
    step.changeInfo = target;
    adjustContentAreas(step.content, target);
    step.subSequencesFor(StepNumber.EMPTY).forEach(ns -> {
      StepSequenceModel_V002 seq = ns.sequence;
      if (seq instanceof BranchSequenceModel_V002) {
        adjustContentAreas(((BranchSequenceModel_V002) seq).heading, target);
      }
    });
  }

  private static void adjustContentAreas(EditorContentModel_V002 content, ChangeInfoModel_V002 target) {
    if (content == null) return;
    for (AbstractEditAreaModel_V002 area : content.areas) {
      area.changeInfo = target;
      if (area instanceof ListItemEditAreaModel_V002) {
        adjustContentAreas(((ListItemEditAreaModel_V002) area).content, target);
      }
      else if (area instanceof TableEditAreaModel_V002) {
        adjustTableCells((TableEditAreaModel_V002) area, target);
      }
    }
  }

  private static void adjustTableCells(TableEditAreaModel_V002 table, ChangeInfoModel_V002 target) {
    for (List<EditorContentModel_V002> row : table.cells) {
      for (EditorContentModel_V002 cell : row) {
        adjustContentAreas(cell, target);
      }
    }
  }
}
