package headless;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import specman.ops.buttons.CreateSimpleStepOpButton;
import specman.ops.buttons.CreateSubsequenceStepOpButton;
import specman.ops.buttons.CreateWhileStepOpButton;
import specman.view.EinfacherSchrittView;
import specman.view.SubsequenzSchrittView;
import specman.view.WhileSchrittView;

import static headless.HeadlessExtension.specman;
import static specman.metatag.stepnumberlabel.StepnumberContextMenu.FLAT_NUMBERING_ITEM;

/**
 * Flat numbering of a subsequence step: Its substeps continue the numbering of the surrounding
 * sequence instead of starting a numbering level of their own. Inserting steps ahead of the
 * subsequence step must keep that numbering consistent in both modes.
 */
@ExtendWith(HeadlessExtension.class)
class FlatNumberingTest {

  @Test
  void testFlatNumberingToggleAndInsertion() {
    SubsequenzSchrittView subsequence = createInitialDiagram();
    specman.assertStepNumbers("1", "2", "2.1", "2.2", "2.2.1", "3");

    insertSimpleStepAfterFirstStep();
    specman.assertStepNumbers("1", "2", "3", "3.1", "3.2", "3.2.1", "4");

    specman.clickContextMenuItem(subsequence, FLAT_NUMBERING_ITEM);
    specman.assertStepNumbers("1", "2", "3", "4", "5", "5.1", "6");

    insertSimpleStepAfterFirstStep();
    specman.assertStepNumbers("1", "2", "3", "4", "5", "6", "6.1", "7");

    specman.clickContextMenuItem(subsequence, FLAT_NUMBERING_ITEM);
    specman.assertStepNumbers("1", "2", "3", "4", "4.1", "4.2", "4.2.1", "5");
  }

  @Test
  void testFlatNumberingUndoRedo() {
    SubsequenzSchrittView subsequence = createInitialDiagram();

    specman.clickContextMenuItem(subsequence, FLAT_NUMBERING_ITEM);
    specman.assertStepNumbers("1", "2", "3", "4", "4.1", "5");

    specman.undo();
    specman.assertStepNumbers("1", "2", "2.1", "2.2", "2.2.1", "3");

    specman.redo();
    specman.assertStepNumbers("1", "2", "3", "4", "4.1", "5");
  }

  /**
   * Creates the diagram 1 (simple), 2 (subsequence) containing 2.1 (simple) and 2.2 (while)
   * containing 2.2.1 (simple), 3 (simple). A new step is always inserted behind the step in focus.
   */
  private SubsequenzSchrittView createInitialDiagram() {
    specman.click(CreateSimpleStepOpButton.class);
    specman.click(CreateSubsequenceStepOpButton.class);
    SubsequenzSchrittView subsequence = specman.findStepByStepID("2", SubsequenzSchrittView.class);

    specman.focus(specman.findSubstep(subsequence, "2.1", EinfacherSchrittView.class));
    specman.click(CreateWhileStepOpButton.class);
    WhileSchrittView whileStep = specman.findSubstep(subsequence, "2.2", WhileSchrittView.class);
    specman.findSubstep(whileStep, "2.2.1", EinfacherSchrittView.class);

    specman.focus(subsequence);
    specman.click(CreateSimpleStepOpButton.class);
    return subsequence;
  }

  private void insertSimpleStepAfterFirstStep() {
    specman.focus(specman.findStepByStepID("1", EinfacherSchrittView.class));
    specman.click(CreateSimpleStepOpButton.class);
  }

}
