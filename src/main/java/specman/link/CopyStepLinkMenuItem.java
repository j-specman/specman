package specman.link;

import specman.graphics.SvgIcon;
import specman.i18n.IMenuItem;
import specman.view.AbstractSchrittView;

import java.awt.datatransfer.StringSelection;

import static specman.Specman.editor;

/** Context menu item of a step number which copies a global link to the step to the clipboard. */
public class CopyStepLinkMenuItem extends IMenuItem {

  private static final String MESSAGE_KEY = "menu.context.copy.link";
  private static final int ICON_SIZE = 16;

  private AbstractSchrittView currentStep;

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

  private void copyLink() {
    String link = SpecmanLink.of(editor().getDiagrammDatei(), currentStep);
    editor().clipboard().setContents(new StringSelection(link), null);
  }
}
