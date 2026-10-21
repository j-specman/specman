package headless;

import specman.CatchLinkPanel;
import specman.EditException;
import specman.Specman;
import specman.editarea.TableEditArea;
import specman.editarea.TextEditArea;
import specman.metatag.stepnumberlabel.StepnumberContextMenu;
import specman.metatag.stepnumberlabel.StepnumberLabel;
import specman.model.v002.PdfExportOptionsModel_V002;
import specman.ops.ExportPDFSpecmanOp;
import specman.ops.buttons.CreateCatchSequenceADBLOpButton;
import specman.view.AbstractSchrittView;
import specman.view.BreakSchrittView;
import specman.editarea.InteractiveStepFragment;

import javax.swing.AbstractButton;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static specman.i18n.Messages.i18n;

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
 * <p>
 * On top of that, it offers convenience methods to express user behavior compactly in tests:
 * {@link #focus(AbstractSchrittView)} and {@link #click(Class)} simulate the cursor placement and a
 * toolbar button click, {@link #findStepByStepID(String, Class)} and {@link #findComponent(Class)}
 * look up what the user would see.
 */
public class SpecmanForTests extends Specman {

  private final List<String> shownMessages = new ArrayList<>();
  private static final Dimension LAYOUT_SIZE = new Dimension(800, 600);

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

  /** Checks that the step with the given number exists, has the expected view class and lives in one of the given step's sequences. */
  public <T extends AbstractSchrittView> T findSubstep(AbstractSchrittView step, String stepID, Class<T> expectedType) {
    T substep = findStepByStepID(stepID, expectedType);
    assertSame(step, substep.getParent().getParent(),
      "step " + stepID + " must be a substep of step " + step.getNumber());
    return substep;
  }

  /** Simulates the user placing the cursor into the first text of the given step. */
  public void focus(AbstractSchrittView step) {
    setLastFocusedTextArea(step.getFirstEditArea().asTextArea());
  }

  /** Clicks the first button of the given type, without the default press delay of {@link AbstractButton#doClick()}. */
  public void click(Class<? extends AbstractButton> buttonType) {
    findComponent(buttonType).doClick(0);
    waitForEventsProcessed();
  }

  /**
   * Operates the step number's context menu like the user: Right-clicks the given step's number and
   * clicks the menu item with the given message key. The menu is not shown, but the item is only
   * clicked if the user could do so, i.e. if it is visible and enabled for that step.
   * Check box items toggle their state when clicked.
   */
  public void clickContextMenuItem(AbstractSchrittView step, String itemMessageKey) {
    rightClick(findStepnumberLabel(step));
    String itemText = i18n(itemMessageKey);
    for (Component component : StepnumberContextMenu.popupMenu().getComponents()) {
      if (component instanceof JMenuItem item && itemText.equals(item.getText())) {
        assertTrue(item.isVisible() && item.isEnabled(),
          "context menu item '" + itemText + "' must be available for step " + step.getNumber());
        item.doClick(0);
        waitForEventsProcessed();
        return;
      }
    }
    throw new IllegalStateException("No context menu item '" + itemText + "'");
  }

  private StepnumberLabel findStepnumberLabel(AbstractSchrittView step) {
    List<StepnumberLabel> labels = new ArrayList<>();
    collectComponents(this, StepnumberLabel.class, labels);
    for (StepnumberLabel label : labels) {
      if (findStep(label) == step) {
        return label;
      }
    }
    throw new IllegalStateException("No step number label found for step " + step.getNumber());
  }

  private static void rightClick(Component component) {
    component.dispatchEvent(new MouseEvent(component, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
      MouseEvent.BUTTON3_DOWN_MASK, 1, 1, 1, false, MouseEvent.BUTTON3));
  }

  private static <T extends Component> void collectComponents(Container container, Class<T> type, List<T> result) {
    for (Component child : container.getComponents()) {
      if (type.isInstance(child)) {
        result.add(type.cast(child));
      }
      if (child instanceof Container childContainer) {
        collectComponents(childContainer, type, result);
      }
    }
  }

  /**
   * Clicks the button for creating a catch sequence like the user would. The dialog for choosing the
   * break step is not displayed, but the panel it would show is returned for checking the offered
   * break steps and choosing one via {@link #choose(CatchLinkPanel, BreakSchrittView)}.
   */
  public CatchLinkPanel clickCreateCatchButton() {
    CreateCatchSequenceADBLOpButton button = findComponent(CreateCatchSequenceADBLOpButton.class);
    button.setDialogDisplayed(false);
    click(CreateCatchSequenceADBLOpButton.class);
    return button.catchLinkPanel();
  }

  /** Simulates the user clicking the given break step in the catch link dialog's list. */
  public void choose(CatchLinkPanel catchLinkPanel, BreakSchrittView breakStep) {
    catchLinkPanel.choose(breakStep);
    waitForEventsProcessed();
  }

  /** The numbers of all steps in diagram order, substeps directly following their parent step. */
  public List<String> allStepNumbers() {
    List<String> numbers = new ArrayList<>();
    for (AbstractSchrittView step : listAllSteps()) {
      numbers.add(step.getNumber().toString());
    }
    return numbers;
  }

  public void assertStepNumbers(String... expectedNumbers) {
    assertEquals(List.of(expectedNumbers), allStepNumbers());
  }

  /** Exports the diagram to the given PDF file like the user would, but without file chooser dialog. */
  public void exportPdf(File pdfFile, String pageSize, boolean portrait, boolean paging) {
    setPdfExportOptions(new PdfExportOptionsModel_V002(pdfFile.getAbsolutePath(), null, pageSize, portrait, paging));
    exportAsPDF();
  }

  /** Without a displayed frame nobody lays out the components, so their bounds stay empty. */
  void layoutComponents() {
    setSize(LAYOUT_SIZE);
    layoutTree(this);
  }

  private static void layoutTree(Component component) {
    if (component instanceof Container container) {
      container.doLayout();
      for (Component child : container.getComponents()) {
        layoutTree(child);
      }
    }
  }

  public void undo() {
    getUndoManager().undo();
    waitForEventsProcessed();
  }

  public void redo() {
    getUndoManager().redo();
    waitForEventsProcessed();
  }

  /** Specman defers parts of the relayout to the event queue. This method ensures not to carry
   * on with the test before all events pending at the time of the call are processed. Events
   * scheduled by those events themselves are not awaited. Sometimes this is required for
   * a deterministic application state before checking it. Must not be called on the EDT. */
  private static void waitForEventsProcessed() {
    try {
      SwingUtilities.invokeAndWait(() -> { });
    }
    catch (InterruptedException x) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(x);
    }
    catch (InvocationTargetException x) {
      throw new IllegalStateException(x.getCause());
    }
  }

  /** Finds the first component of the given type in Specman's component tree, e.g. a toolbar button. */
  public <T extends Component> T findComponent(Class<T> type) {
    List<T> components = new ArrayList<>();
    collectComponents(this, type, components);
    if (components.isEmpty()) {
      throw new IllegalStateException("No component of type " + type.getSimpleName() + " found");
    }
    return components.get(0);
  }

  @Override
  protected ExportPDFSpecmanOp createExportPDFOp() {
    return new ExportPDFSpecmanOpForTest(this);
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
