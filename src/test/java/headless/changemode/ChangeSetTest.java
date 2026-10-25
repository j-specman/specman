package headless.changemode;

import headless.HeadlessExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import specman.ChangeSet;
import specman.editarea.markups.MarkupType;
import specman.model.v002.Markup_V002;
import specman.view.EinfacherSchrittView;

import java.util.List;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static specman.editarea.markups.MarkupSearchPurpose.All;

@ExtendWith(HeadlessExtension.class)
class ChangeSetTest {

  /** In change mode, each change set colors the steps added while it is active: The step with its
   * panel color, the step's text with its text color. */
  @Test
  void testNewStepsAreColoredInChangeSetColors() {
    specman.toggleChangeMode();
    assertTrue(specman.aenderungenVerfolgen());

    for (ChangeSet changeSet : ChangeSet.ALL.values()) {
      specman.selectChangeSet(changeSet);
      EinfacherSchrittView step = specman.clickNewSimpleStep();
      assertEquals(changeSet.panelColor(), step.getBackground(), "step background in change set " + changeSet);
      assertWholeTextMarkedAsChangedBy(step, changeSet);
    }
  }

  private static void assertWholeTextMarkedAsChangedBy(EinfacherSchrittView step, ChangeSet changeSet) {
    List<Markup_V002> markups = step.getFirstEditArea().asTextArea().findMarkups(All);
    assertEquals(1, markups.size(), "text markups in change set " + changeSet);
    Markup_V002 markup = markups.get(0);
    assertEquals(MarkupType.Changed, markup.type);
    assertEquals(changeSet.name, markup.changeset);
    int textLength = step.getFirstEditArea().asTextArea().getPlainText().length();
    assertEquals(0, markup.from);
    assertEquals(textLength - 1, markup.to);
  }
}
