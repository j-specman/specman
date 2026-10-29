package headless;

import net.atlanticbb.tantlinger.ui.text.actions.BasicEditAction;
import net.atlanticbb.tantlinger.ui.text.actions.StepnumberLinkAction;
import net.atlanticbb.tantlinger.ui.text.actions.StepnumberLinkPanel;
import specman.CatchLinkPanel;
import specman.ChangeSet;
import specman.DiagramToolBar;
import specman.EditException;
import specman.Specman;
import specman.editarea.EditArea;
import specman.editarea.TableEditArea;
import specman.editarea.TextEditArea;
import specman.metatag.stepnumberlabel.StepnumberContextMenu;
import specman.metatag.stepnumberlabel.StepnumberLabel;
import specman.model.v002.PdfExportOptionsModel_V002;
import specman.ops.ExportPDFSpecmanOp;
import specman.ops.buttons.CreateCatchSequenceADBLOpButton;
import specman.ops.buttons.CreateBreakStepOpButton;
import specman.ops.buttons.CreateSimpleStepOpButton;
import specman.ops.buttons.CreateSubsequenceStepOpButton;
import specman.view.AbstractSchrittView;
import specman.view.BreakSchrittView;
import specman.view.EinfacherSchrittView;
import specman.view.SubsequenzSchrittView;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
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
import static specman.DiagramToolBar.TRACK_CHANGES_BUTTON;
import static specman.i18n.Messages.i18n;
import static specman.menubar.SpecmanMenuBar.changeSetItemName;

/**
 * Specman <b>F</b>or <b>T</b>ests: the real Specman, instantiated without its {@link specman.SpecmanFrame}.
 * Everything which cannot work headless or must not touch the user's environment is disabled here:
 * <ul>
 *   <li>Dialogs and toasts are not shown. Their messages are collected in {@link #shownMessages()},
 *   so that errors do not vanish unnoticed. Confirmation dialogs are answered with YES/OK, unless
 *   {@link #answerConfirmDialogsWith(int)} says otherwise.</li>
 *   <li>Auto-save timer and working copy watcher are not started.</li>
 *   <li>Step copy and paste use an in-memory clipboard instead of the system clipboard. Table copy and
 *   paste, which still access the system clipboard directly, do nothing.</li>
 * </ul>
 * Code which opens a dialog or accesses the system clipboard directly is not covered. It fails with
 * a {@link java.awt.HeadlessException}, which is the right outcome for an unexpected dialog.
 * <p>
 * On top of that, it offers convenience methods to express user behavior compactly in tests:
 * {@link #focus(AbstractSchrittView)}, {@link #click(Class)} and {@link #doEdit(Class)} simulate the cursor
 * placement, a toolbar button click and a Shef edit action, {@link #findStepByStepID(String, Class)} and
 * {@link #findComponent(Class)} look up what the user would see.
 */
public class SpecmanFT extends Specman {

  private final List<String> shownMessages = new ArrayList<>();
  private final Clipboard clipboard = new Clipboard("specman-test");
  private int confirmDialogAnswer = JOptionPane.YES_OPTION;
  private EditArea focusedArea;
  private static final Dimension LAYOUT_SIZE = new Dimension(800, 600);
  private static final int MAX_LAYOUT_PASSES = 50;

  public SpecmanFT() throws Exception {
    super(null);
  }

  public List<String> shownMessages() {
    return Collections.unmodifiableList(shownMessages);
  }

  /** Defines how the user answers the following confirmation dialogs, e.g. {@link JOptionPane#CANCEL_OPTION}. */
  public void answerConfirmDialogsWith(int option) {
    confirmDialogAnswer = option;
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

  /**
   * Simulates the user placing the cursor into the first text of the given step. The text area's focus
   * listeners are notified like on a real focus change. This is what makes Shef's edit actions work
   * on that text, see {@link #doEdit(Class)}.
   */
  public void focus(AbstractSchrittView step) {
    focus(step.getFirstEditArea());
  }

  /**
   * Simulates the keyboard focus moving to the given edit area: The previously focused area is notified
   * about losing and the given area about gaining the focus, like on a real focus change.
   */
  @Override
  public void focus(EditArea editArea) {
    if (focusedArea != null) {
      notifyFocus(focusedArea, new FocusEvent(focusedArea.asComponent(), FocusEvent.FOCUS_LOST));
    }
    focusedArea = editArea;
    notifyFocus(editArea, new FocusEvent(editArea.asComponent(), FocusEvent.FOCUS_GAINED));
  }

  private static void notifyFocus(EditArea editArea, FocusEvent event) {
    for (FocusListener listener : editArea.asComponent().getFocusListeners()) {
      if (event.getID() == FocusEvent.FOCUS_GAINED) {
        listener.focusGained(event);
      }
      else {
        listener.focusLost(event);
      }
    }
  }

  /** The edit area which got the focus last via {@link #focus(EditArea)}, e.g. by the user's arrow keys. */
  public EditArea focusedArea() {
    return focusedArea;
  }

  /** Simulates the user pressing a key in the edit area which currently has the focus. */
  public void press(int keyCode) {
    press(focusedArea.asComponent(), keyCode, 0);
  }

  /** Simulates the user pressing a key together with the given modifiers, e.g. {@link KeyEvent#SHIFT_DOWN_MASK}, in the focused edit area. */
  public void press(int keyCode, int modifiers) {
    press(focusedArea.asComponent(), keyCode, modifiers);
  }

  /**
   * Simulates the user pressing a key (e.g. {@link KeyEvent#VK_BACK_SPACE}) in the given step's first text. The text
   * area processes the key like Swing does: First its key listeners get the chance to handle and consume the
   * event, otherwise the editor kit's action bound to the key is performed. Without a focus owner, Swing would
   * not deliver key events to a text area at all, so {@code dispatchEvent} does not work here. The key handlers
   * ask the text area for its rows, which requires the layout to be up to date.
   */
  public void press(AbstractSchrittView step, int keyCode) {
    press(step.getFirstEditArea().asComponent(), keyCode, 0);
  }

  private void press(Component component, int keyCode, int modifiers) {
    layoutComponents();
    JComponent target = (JComponent) component;
    KeyEvent keyEvent = new KeyEvent(target, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), modifiers, keyCode, KeyEvent.CHAR_UNDEFINED);
    for (KeyListener listener : target.getKeyListeners()) {
      listener.keyPressed(keyEvent);
    }
    if (!keyEvent.isConsumed()) {
      Object actionKey = target.getInputMap().get(KeyStroke.getKeyStrokeForEvent(keyEvent));
      Action action = actionKey == null ? null : target.getActionMap().get(actionKey);
      if (action != null) {
        action.actionPerformed(new ActionEvent(target, ActionEvent.ACTION_PERFORMED, null));
      }
    }
    waitForEventsProcessed();
  }

  /** Clicks the first button of the given type, without the default press delay of {@link AbstractButton#doClick()}. */
  public void click(Class<? extends AbstractButton> buttonType) {
    findComponent(buttonType).doClick(0);
    waitForEventsProcessed();
  }

  /** Clicks the toolbar button or menu item with the given name. Use the name constants of {@link DiagramToolBar}
   * and the menu classes, e.g. {@link DiagramToolBar#TRACK_CHANGES_BUTTON}. The click works like the user's,
   * except for menus, which are not opened first. */
  public void click(String name) {
    AbstractButton button = findByName(this, name, AbstractButton.class);
    if (button == null) {
      throw new IllegalStateException("No button or menu item named '" + name + "'");
    }
    button.doClick(0);
    waitForEventsProcessed();
  }

  /** Clicks the toolbar's toggle button for tracking changes, which switches the change mode on or off. */
  public void toggleChangeMode() {
    click(TRACK_CHANGES_BUTTON);
  }

  /** Chooses the given change set from the change set menu like the user would. */
  public void selectChangeSet(ChangeSet changeSet) {
    click(changeSetItemName(changeSet));
  }

  /** Finds the component with the given name below the passed one, e.g. in a dialog panel. Menu items live in a
   * menu's popup, which is not part of the menu's component children. */
  public static <T extends Component> T findByName(Component component, String name, Class<T> type) {
    if (type.isInstance(component) && name.equals(component.getName())) {
      return type.cast(component);
    }
    Component[] children = component instanceof JMenu menu ? menu.getMenuComponents()
      : component instanceof Container container ? container.getComponents()
      : new Component[0];
    for (Component child : children) {
      T found = findByName(child, name, type);
      if (found != null) {
        return found;
      }
    }
    return null;
  }

  /** Creates a simple step like the user would by clicking the toolbar button, i.e. behind the focused step. Returns the new step. */
  public EinfacherSchrittView clickNewSimpleStep() {
    return clickNewStep(CreateSimpleStepOpButton.class, EinfacherSchrittView.class);
  }

  /** Creates a subsequence step behind the focused step. Returns it, not its first substep, which comes along. */
  public SubsequenzSchrittView clickNewSubsequenceStep() {
    return clickNewStep(CreateSubsequenceStepOpButton.class, SubsequenzSchrittView.class);
  }

  /** Creates a break step behind the focused step. Returns it. */
  public BreakSchrittView clickNewBreakStep() {
    return clickNewStep(CreateBreakStepOpButton.class, BreakSchrittView.class);
  }

  /** Clicks the button and returns the one new step of the given type. Further new steps of other types, e.g. substeps, are fine. */
  private <T extends AbstractSchrittView> T clickNewStep(Class<? extends AbstractButton> buttonType, Class<T> stepType) {
    List<AbstractSchrittView> stepsBefore = new ArrayList<>(listAllSteps());
    click(buttonType);
    List<AbstractSchrittView> newSteps = new ArrayList<>(listAllSteps());
    newSteps.removeAll(stepsBefore);
    List<T> newStepsOfType = newSteps.stream().filter(stepType::isInstance).map(stepType::cast).toList();
    assertEquals(1, newStepsOfType.size(),
      "Clicking " + buttonType.getSimpleName() + " must create exactly one " + stepType.getSimpleName());
    return newStepsOfType.get(0);
  }

  /**
   * Triggers the given Shef edit action on the text which got the cursor via {@link #focus(AbstractSchrittView)},
   * like the user clicking the action's format toolbar button. Fails if the user couldn't do so, i.e. if the
   * action is disabled because there is no focused text.
   */
  public void doEdit(Class<? extends BasicEditAction> actionType) {
    BasicEditAction action = findEditAction(actionType);
    assertTrue(action.isEnabled(), actionType.getSimpleName() + " must be enabled, focus a step first");
    action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
    waitForEventsProcessed();
  }

  /** Finds the Shef edit action of the given type behind a button of the format toolbar. */
  public <T extends BasicEditAction> T findEditAction(Class<T> actionType) {
    List<AbstractButton> buttons = new ArrayList<>();
    collectComponents(this, AbstractButton.class, buttons);
    for (AbstractButton button : buttons) {
      if (actionType.isInstance(button.getAction())) {
        return actionType.cast(button.getAction());
      }
    }
    throw new IllegalStateException("No toolbar button for action " + actionType.getSimpleName());
  }

  /**
   * Operates the step number's context menu like the user: Right-clicks the given step's number and
   * clicks the menu item with the given message key. The menu is not shown, but the item is only
   * clicked if the user could do so, i.e. if it is visible and enabled for that step.
   * Check box items toggle their state when clicked. The layout is brought up to date first, like it always
   * is when a user can click: Serializing a step reads the column widths of if/else steps from the layout.
   */
  public void clickContextMenuItem(AbstractSchrittView step, String itemMessageKey) {
    click(step, contextMenuItem(step, itemMessageKey));
  }

  /** Like {@link #clickContextMenuItem(AbstractSchrittView, String)}, but finds the item by its type. */
  public void clickContextMenuItem(AbstractSchrittView step, Class<? extends JMenuItem> itemType) {
    click(step, contextMenuItem(step, itemType));
  }

  private void click(AbstractSchrittView step, JMenuItem item) {
    assertTrue(item.isVisible() && item.isEnabled(),
      "context menu item '" + item.getText() + "' must be available for step " + step.getNumber());
    item.doClick(0);
    waitForEventsProcessed();
  }

  /** Right-clicks the given step's number and returns the context menu item with the given message key
   * as initialized for that step, without clicking it. Allows tests to check visibility and enablement. */
  public JMenuItem contextMenuItem(AbstractSchrittView step, String itemMessageKey) {
    String itemText = i18n(itemMessageKey);
    for (Component component : openContextMenu(step)) {
      if (component instanceof JMenuItem item && itemText.equals(item.getText())) {
        return item;
      }
    }
    throw new IllegalStateException("No context menu item '" + itemText + "'");
  }

  /** Like {@link #contextMenuItem(AbstractSchrittView, String)}, but finds the item by its type, which is
   * the way to get hold of items with a behavior of their own. */
  public <T extends JMenuItem> T contextMenuItem(AbstractSchrittView step, Class<T> itemType) {
    for (Component component : openContextMenu(step)) {
      if (itemType.isInstance(component)) {
        return itemType.cast(component);
      }
    }
    throw new IllegalStateException("No context menu item of type " + itemType.getSimpleName());
  }

  private Component[] openContextMenu(AbstractSchrittView step) {
    layoutComponents();
    rightClick(findStepnumberLabel(step));
    return StepnumberContextMenu.popupMenu().getComponents();
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

  /**
   * Triggers the step number link action on the focused text like the user would. The dialog for choosing
   * the step to link to is not displayed, but the panel it would show is returned for checking the offered
   * steps and choosing one via {@link #choose(StepnumberLinkPanel, AbstractSchrittView)}.
   */
  public StepnumberLinkPanel doEditStepnumberLink() {
    StepnumberLinkAction action = findEditAction(StepnumberLinkAction.class);
    action.setDialogDisplayed(false);
    doEdit(StepnumberLinkAction.class);
    return action.stepnumberLinkPanel();
  }

  /** Simulates the user clicking the given step in the step number link dialog's list. */
  public void choose(StepnumberLinkPanel stepnumberLinkPanel, AbstractSchrittView step) {
    stepnumberLinkPanel.choose(step);
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

  public void assertStepNumbers(List<String> expectedNumbers) {
    assertEquals(expectedNumbers, allStepNumbers());
  }

  /** Exports the diagram to the given PDF file like the user would, but without file chooser dialog. */
  public void exportPdf(File pdfFile, String pageSize, boolean portrait, boolean paging) {
    setPdfExportOptions(new PdfExportOptionsModel_V002(pdfFile.getAbsolutePath(), null, pageSize, portrait, paging));
    exportAsPDF();
  }

  /**
   * Without a displayed frame nobody lays out the components, so their bounds stay empty. A single
   * pass is not enough: The height of a text depends on the width it has been given, which in turn
   * changes the height of its parents. Each pass settles roughly one nesting level, so we lay out
   * until no bounds change anymore. The layout managers must be invalidated for every pass, otherwise
   * they keep working with their cached preferred sizes.
   */
  public void layoutComponents() {
    setSize(LAYOUT_SIZE);
    int previousBoundsHash;
    int boundsHash = 0;
    int passes = 0;
    do {
      if (++passes > MAX_LAYOUT_PASSES) {
        throw new IllegalStateException("Layout did not settle within " + MAX_LAYOUT_PASSES + " passes");
      }
      previousBoundsHash = boundsHash;
      layoutTree(this);
      waitForEventsProcessed();
      boundsHash = boundsHash(this);
    }
    while (boundsHash != previousBoundsHash);
  }

  private static int boundsHash(Component component) {
    int hash = component.getBounds().hashCode();
    if (component instanceof Container container) {
      for (Component child : container.getComponents()) {
        hash = 31 * hash + boundsHash(child);
      }
    }
    return hash;
  }

  private static void layoutTree(Component component) {
    if (component instanceof Container container) {
      container.invalidate();
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
    return new ExportPDFSpecmanOpFT(this);
  }

  @Override
  public Clipboard clipboard() {
    return clipboard;
  }

  @Override
  public int showConfirmDialog(String message, String title, int optionType) {
    shownMessages.add(message);
    return confirmDialogAnswer;
  }

  @Override public void openSettings() {}
  @Override public void addRecentFile(File file) {}
  @Override public void openMetaTagConfigs() {}
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
