package headless.changemode;

import org.junit.jupiter.api.Test;
import specman.ChangeSet;
import specman.view.EinfacherSchrittView;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ChangeSetTest extends AbstractChangemodeTest {

  /** In change mode, each change set colors the steps added while it is active: The step with its
   * panel color, the step's text with its text color. */
  @Test
  void testNewStepsAreColoredInChangeSetColors() {
    enableChangeMode(ChangeSet.DEFAULT);
    for (ChangeSet changeSet : ChangeSet.ALL.values()) {
      specman.selectChangeSet(changeSet);
      EinfacherSchrittView step = specman.clickNewSimpleStep();
      assertEquals(changeSet.panelColor(), step.getBackground(), "step background in change set " + changeSet);
      assertWholeTextMarkedAsChangedBy(step.getFirstEditArea().asTextArea(), changeSet);
    }
  }
}
