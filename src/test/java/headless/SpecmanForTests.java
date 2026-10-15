package headless;

import specman.EditException;
import specman.Specman;
import specman.editarea.TableEditArea;
import specman.editarea.TextEditArea;
import specman.view.AbstractSchrittView;
import specman.editarea.InteractiveStepFragment;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Specman for tests: the real Specman, instantiated without its {@link specman.SpecmanFrame}.
 * Everything which cannot work headless or must not touch the user's environment is disabled here:
 * <ul>
 *   <li>Dialogs and toasts are not shown. Their messages are collected in {@link #shownMessages()},
 *   so that errors do not vanish unnoticed. Confirmation dialogs are answered with YES/OK.</li>
 *   <li>Auto-save timer and working copy watcher are not started.</li>
 *   <li>The Specman-level clipboard operations do nothing.</li>
 * </ul>
 * Code which opens a dialog or accesses the system clipboard directly is not covered. It fails with
 * a {@link java.awt.HeadlessException}, which is the right outcome for an unexpected dialog.
 */
public class SpecmanForTests extends Specman {

  private final List<String> shownMessages = new ArrayList<>();

  public SpecmanForTests() throws Exception {
    super(null);
  }

  public List<String> shownMessages() {
    return Collections.unmodifiableList(shownMessages);
  }

  /** Finds a step by its number and asserts that it exists and is of the expected view class. */
  public <T extends AbstractSchrittView> T findStepByStepID(String stepID, Class<T> expectedType) {
    AbstractSchrittView step = findStepByStepID(stepID);
    assertNotNull(step, "step " + stepID + " must exist");
    assertTrue(expectedType.isInstance(step),
      "step " + stepID + " must be a " + expectedType.getSimpleName() + " but is a " + step.getClass().getSimpleName());
    return expectedType.cast(step);
  }

  /** Finds the first component of the given type in Specman's component tree, e.g. a toolbar button. */
  public <T extends Component> T findComponent(Class<T> type) {
    T result = findComponent(this, type);
    if (result == null) {
      throw new IllegalStateException("No component of type " + type.getSimpleName() + " found");
    }
    return result;
  }

  private static <T extends Component> T findComponent(Container container, Class<T> type) {
    for (Component child : container.getComponents()) {
      if (type.isInstance(child)) {
        return type.cast(child);
      }
      if (child instanceof Container childContainer) {
        T result = findComponent(childContainer, type);
        if (result != null) {
          return result;
        }
      }
    }
    return null;
  }

  @Override
  public int showConfirmDialog(String message, String title, int optionType) {
    shownMessages.add(message);
    return JOptionPane.YES_OPTION;
  }

  @Override public void openSettings() {}
  @Override public void openMetaTagConfigs() {}
  @Override public void copyStepToClipboard(AbstractSchrittView step) {}
  @Override public void cutStepToClipboard(AbstractSchrittView step, InteractiveStepFragment initiatingFragment) {}
  @Override public void pasteStepsAfter(AbstractSchrittView referenceStep) {}
  @Override public void copyTableToClipboard(TableEditArea table) {}
  @Override public void cutTableToClipboard(TableEditArea table) {}
  @Override public void pasteTableInto(TextEditArea initiatingTextArea) {}
  @Override protected void initAutoSaveAndLoad() {}
  @Override public void notifyWorkingCopyInitialized(long timestamp) {}
  @Override public void fehler(String messageKey, Object... args) { shownMessages.add(messageKey); }
  @Override public void showMessage(String messageKey, Object... args) { shownMessages.add(messageKey); }
  @Override public void showToast(String message) { shownMessages.add(message); }
  @Override public void showToast(String briefMessage, String detailMessage) { shownMessages.add(briefMessage); }
  @Override public void showError(EditException ex) { shownMessages.add(ex.getMessage()); }
  @Override public void displayException(Exception x) { shownMessages.add(x.getMessage()); }

}
