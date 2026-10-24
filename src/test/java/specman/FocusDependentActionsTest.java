package specman;

import headless.HeadlessExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import specman.editarea.TextEditArea;
import specman.ops.buttons.CreateCaseBranchADBLOpButton;
import specman.ops.buttons.CreateCatchSequenceADBLOpButton;
import specman.ops.buttons.CreateSimpleStepOpButton;
import specman.ops.buttons.DeleteStepOpButton;
import specman.ops.buttons.ToggleBorderTypeOpButton;
import specman.ops.buttons.ToneOpButton;

import javax.swing.AbstractButton;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.util.List;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step buttons need a focused text area that belongs to a step, insert buttons need any focused text area
 * (also in intro and outro). Without focus, e.g. after New, everything is disabled.
 */
@ExtendWith(HeadlessExtension.class)
class FocusDependentActionsTest {

  private static final File DIAGRAM = new File("doc/anagramms.nsd");

  @BeforeEach
  void loadDiagram() {
    specman.diagrammLaden(DIAGRAM);
  }

  @AfterEach
  void deleteWorkingCopy() {
    specman.getUndoManager().discardAllEdits();
    new File("doc/anagramms.nsd.wrk").delete();
  }

  private static TextEditArea firstStepTextArea() {
    return specman.listAllSteps().get(0).getFirstEditArea().asTextArea();
  }

  private static boolean enabled(Class<? extends AbstractButton> type) {
    return specman.findComponent(type).isEnabled();
  }

  private static boolean stepButtonsEnabled() {
    boolean tone = enabled(ToneOpButton.class);
    assertEquals(tone, enabled(DeleteStepOpButton.class));
    assertEquals(tone, enabled(ToggleBorderTypeOpButton.class));
    return tone;
  }

  private static long enabledInsertButtons() {
    return insertButtons().stream().filter(AbstractButton::isEnabled).count();
  }

  private static List<AbstractButton> insertButtons() {
    return ((Specman) specman).shefEditorPane.getInsertGroupComponents().stream()
      .filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast).toList();
  }

  // Headless, no editor has ever had the focus, so SHEF itself may disable some of its actions.
  // With a focused text area the buttons must therefore simply follow their action.
  private static void assertInsertButtonsFollowTheirActions() {
    for (AbstractButton button : insertButtons()) {
      assertEquals(button.getAction().isEnabled(), button.isEnabled());
    }
  }

  @Test
  void nothingFocusedDisablesStepAndInsertButtons() {
    specman.setLastFocusedTextArea(null);
    assertFalse(stepButtonsEnabled());
    assertTrue(enabled(CreateCaseBranchADBLOpButton.class));
    assertTrue(enabled(CreateCatchSequenceADBLOpButton.class));
    // only the link button does not need a text area
    assertTrue(enabledInsertButtons() <= 1);
    assertTrue(enabled(CreateSimpleStepOpButton.class));
  }

  @Test
  void focusInStepEnablesStepAndInsertButtons() {
    specman.setLastFocusedTextArea(firstStepTextArea());
    assertTrue(stepButtonsEnabled());
    assertInsertButtonsFollowTheirActions();
  }

  @Test
  void focusInIntroDisablesStepButtonsButEnablesInsertButtons() {
    specman.setLastFocusedTextArea(specman.getIntro().getFirstEditArea().asTextArea());
    assertFalse(stepButtonsEnabled());
    assertInsertButtonsFollowTheirActions();
  }

  @Test
  void insertButtonsStayDisabledAfterShefUpdatesItsActions() {
    TextEditArea area = firstStepTextArea();
    // the caret can only move within a non-empty text
    area.setText("xy");
    area.setCaretPosition(area.getDocument().getLength());
    // a focus event gives SHEF its editor, so its actions are enabled from now on
    for (FocusListener listener : area.getFocusListeners()) {
      listener.focusGained(new FocusEvent(area, FocusEvent.FOCUS_GAINED));
    }
    specman.setLastFocusedTextArea(null);
    List<AbstractButton> needFocus = insertButtons().stream()
      .filter(button -> !button.getAction().getClass().getSimpleName().equals("HTMLLinkAction")).toList();
    assertEquals(5, needFocus.size());

    // SHEF only changes the button if its action actually flips, so start from a disabled action
    needFocus.forEach(button -> button.getAction().setEnabled(false));
    // the caret event makes SHEF re-evaluate and re-enable its own actions
    area.setCaretPosition(area.getDocument().getLength() - 1);

    for (AbstractButton button : needFocus) {
      assertTrue(button.getAction().isEnabled(), "SHEF did not re-enable " + button.getToolTipText());
      assertFalse(button.isEnabled(), button.getToolTipText() + " must stay disabled without focus");
    }
  }

  @Test
  void newDiagramDisablesEverythingThatNeedsFocus() {
    specman.setLastFocusedTextArea(firstStepTextArea());
    specman.getUndoManager().discardAllEdits();

    specman.diagrammNeu();

    assertFalse(stepButtonsEnabled());
    assertTrue(enabledInsertButtons() <= 1);
  }

}
