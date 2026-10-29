package specman.link;

import specman.graphics.SvgIcon;
import specman.i18n.IMenuItem;
import specman.view.AbstractSchrittView;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Frame;

import static specman.Specman.editor;

/** Context menu item of a step number which copies a global link to the step to the clipboard. */
public class CopyStepLinkMenuItem extends IMenuItem {

  private static final String MESSAGE_KEY = "menu.context.copy.link";
  private static final int ICON_SIZE = 16;

  private AbstractSchrittView currentStep;
  private boolean dialogDisplayed = true;
  private AliasPanel aliasPanel;

  public CopyStepLinkMenuItem() {
    super(MESSAGE_KEY, new SvgIcon("copy-link", ICON_SIZE));
    addActionListener(e -> openAliasDialog());
  }

  /** Prepares the item for the step whose number the user right-clicked. */
  public void init(AbstractSchrittView step) {
    currentStep = step;
    // A link needs a file name, which an unsaved diagram doesn't have yet
    setEnabled(editor().getDiagrammDatei() != null);
  }

  /** Allows tests to operate the {@link AliasPanel} without displaying the dialog around it */
  public void setDialogDisplayed(boolean dialogDisplayed) {
    this.dialogDisplayed = dialogDisplayed;
  }

  /** The panel of the dialog opened by the last click. */
  public AliasPanel aliasPanel() {
    return aliasPanel;
  }

  private void openAliasDialog() {
    aliasPanel = new AliasPanel(editor().getDiagrammDatei(), currentStep);
    if (dialogDisplayed) {
      new AliasDialog(owner(), aliasPanel);
    }
  }

  private Frame owner() {
    Component invoker = getParent() instanceof JPopupMenu popup ? popup.getInvoker() : null;
    return invoker == null ? null : (Frame) SwingUtilities.getAncestorOfClass(Frame.class, invoker);
  }
}
