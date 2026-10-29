package specman.link;

import specman.graphics.SvgIcon;
import specman.i18n.IMenuItem;
import specman.view.AbstractSchrittView;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Frame;
import java.awt.datatransfer.StringSelection;
import java.io.File;

import static specman.Specman.editor;

/** Context menu item of a step number which copies a global link to the step to the clipboard. If the user
 * hasn't decided yet whether links to the file use an alias, the {@link AliasPanel} asks first. */
public class CopyStepLinkMenuItem extends IMenuItem {

  private static final String MESSAGE_KEY = "menu.context.copy.link";
  private static final int ICON_SIZE = 16;

  private AbstractSchrittView currentStep;
  private boolean dialogDisplayed = true;
  private AliasPanel aliasPanel;

  public CopyStepLinkMenuItem() {
    super(MESSAGE_KEY, new SvgIcon("copy-link", ICON_SIZE));
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

  /** The panel of the dialog opened by the last click, null if the click didn't need a dialog. */
  public AliasPanel aliasPanel() {
    return aliasPanel;
  }

  private void copyLink() {
    File file = editor().getDiagrammDatei();
    aliasPanel = null;
    if (LinkAliases.isAssigned(file)) {
      copyLinkToClipboard(file);
    }
    else {
      askForAlias(file);
    }
  }

  private void askForAlias(File file) {
    aliasPanel = new AliasPanel(file, currentStep, alias -> {
      LinkAliases.assign(file, alias);
      copyLinkToClipboard(file);
    });
    aliasPanel.setDialogDisplayed(dialogDisplayed);
    if (dialogDisplayed) {
      new AliasDialog(owner(), aliasPanel);
    }
  }

  private void copyLinkToClipboard(File file) {
    String link = SpecmanLink.of(file, LinkAliases.aliasOf(file), currentStep);
    editor().clipboard().setContents(new StringSelection(link), null);
  }

  private Frame owner() {
    Component invoker = getParent() instanceof JPopupMenu popup ? popup.getInvoker() : null;
    return invoker == null ? null : (Frame) SwingUtilities.getAncestorOfClass(Frame.class, invoker);
  }
}
