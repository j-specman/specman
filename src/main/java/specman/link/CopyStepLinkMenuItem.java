package specman.link;

import specman.i18n.IMenuItem;
import specman.view.AbstractSchrittView;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Frame;
import java.awt.datatransfer.StringSelection;
import java.io.File;

import static specman.Specman.editor;

/** Context menu item of a step number which copies a global link to the step to the clipboard. The {@link AliasPanel}
 * asks how the link names the file each time. */
public class CopyStepLinkMenuItem extends IMenuItem {

  private static final String MESSAGE_KEY = "menu.context.copy.link";

  private AbstractSchrittView currentStep;
  private boolean dialogDisplayed = true;
  private AliasPanel aliasPanel;

  public CopyStepLinkMenuItem() {
    super(MESSAGE_KEY);
    addActionListener(e -> copyLink());
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

  /** The dialog is always shown, because the user may want one form of the link this time and another one the next. The
   * alias which the file has is in the field already. */
  private void copyLink() {
    File file = editor().getDiagrammDatei();
    aliasPanel = new StepLinkAliasPanel(file, currentStep, location -> {
      LinkAliases.remember(file, location);
      copyLinkToClipboard(location);
    });
    aliasPanel.setDialogDisplayed(dialogDisplayed);
    if (dialogDisplayed) {
      new AliasDialog(owner(), aliasPanel);
    }
  }

  private void copyLinkToClipboard(LinkLocation location) {
    editor().clipboard().setContents(new StringSelection(SpecmanLink.of(location, currentStep)), null);
  }

  private Frame owner() {
    Component invoker = getParent() instanceof JPopupMenu popup ? popup.getInvoker() : null;
    return invoker == null ? null : (Frame) SwingUtilities.getAncestorOfClass(Frame.class, invoker);
  }
}
