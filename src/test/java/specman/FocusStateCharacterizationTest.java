package specman;

import headless.HeadlessExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import specman.editarea.TextEditArea;
import specman.view.AbstractSchrittView;

import java.io.File;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Characterizes how Specman resolves the step belonging to the last focused text area, and what happens to
 * that text area when a diagram is created or loaded. The "after new/load" assertions document today's
 * behavior, which is a defect: the last focused text area keeps pointing into the discarded diagram. They are
 * meant to be inverted when that is fixed.
 */
@ExtendWith(HeadlessExtension.class)
class FocusStateCharacterizationTest {

  private static final File DIAGRAM = new File("doc/anagramms.nsd");

  @BeforeEach
  void loadDiagram() {
    specman.diagrammLaden(DIAGRAM);
  }

  // Loading a diagram leaves its working copy next to the file.
  @AfterEach
  void deleteWorkingCopy() {
    specman.getUndoManager().discardAllEdits();
    new File("doc/anagramms.nsd.wrk").delete();
  }

  private static TextEditArea firstStepTextArea() {
    return specman.listAllSteps().get(0).getFirstEditArea().asTextArea();
  }

  // Loading leaves the diagram marked as modified. New and Load would then ask for confirmation with a real dialog.
  private static void markDiagramAsSaved() {
    specman.getUndoManager().discardAllEdits();
  }

  @Test
  void textAreaOfStepResolvesToItsStep() {
    AbstractSchrittView step = specman.listAllSteps().get(0);
    assertSame(step, specman.findeSchritt(step.getFirstEditArea().asTextArea()));
  }

  @Test
  void textAreaOfSubstepResolvesToTheSubstepNotToItsParent() {
    AbstractSchrittView substep = specman.listAllSteps().stream()
      .filter(step -> step.getNumber().toString().contains("."))
      .findFirst().orElseThrow();
    assertSame(substep, specman.findeSchritt(substep.getFirstEditArea().asTextArea()));
  }

  @Test
  void textAreaOfIntroResolvesToNoStep() {
    assertNull(specman.findeSchritt(specman.getIntro().getFirstEditArea().asTextArea()));
  }

  @Test
  void textAreaOfOutroResolvesToNoStep() {
    assertNull(specman.findeSchritt(specman.getOutro().getFirstEditArea().asTextArea()));
  }

  @Test
  void nothingIsFocusedInAFreshlyLoadedDiagram() {
    assertNull(specman.getLastFocusedTextArea());
  }

  @Test
  void setLastFocusedTextAreaRemembersTheArea() {
    TextEditArea area = firstStepTextArea();
    specman.setLastFocusedTextArea(area);
    assertSame(area, specman.getLastFocusedTextArea());
  }

  @Test
  void focusIsResetByNewDiagram() {
    specman.setLastFocusedTextArea(firstStepTextArea());
    markDiagramAsSaved();

    specman.diagrammNeu();

    assertNull(specman.getLastFocusedTextArea());
  }

  @Test
  void focusIsResetByLoading() {
    specman.setLastFocusedTextArea(firstStepTextArea());
    markDiagramAsSaved();

    specman.diagrammLaden(DIAGRAM);

    assertNull(specman.getLastFocusedTextArea());
  }

}
