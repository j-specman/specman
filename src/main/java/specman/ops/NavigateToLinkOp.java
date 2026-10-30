package specman.ops;

import specman.editarea.TextEditArea;
import specman.link.LinkAliases;
import specman.link.SpecmanLink;
import specman.link.SpecmanLink.Reference;
import specman.link.suggestion.FileSuggester;
import specman.view.AbstractSchrittView;
import specman.visitor.StepByIdFinder;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;

import static specman.i18n.Messages.i18n;

/** Navigates to the file and step a link points to, which is the one in the clipboard or the one handed over from
 * outside. A file which isn't open yet is loaded,
 * which gets the user's chance to save unsaved changes first. Whatever prevents the navigation is reported
 * to the user, either by a toast if it's a matter of the link's step or by an error message if it's the file. */
public class NavigateToLinkOp extends AbstractSpecmanOp {

  public NavigateToLinkOp(SpecmanOpContext context) {
    super(context);
  }

  /** Navigates to the link in the clipboard. */
  public void run() {
    navigateTo(parseOrNull(clipboardText()), "msg.link.clipboard.invalid");
  }

  /** Navigates to the link the user opened from outside, e.g. as the startup argument. */
  public void run(String linkText) {
    navigateTo(parseOrNull(linkText), "msg.link.invalid");
  }

  private void navigateTo(Reference reference, String invalidLinkMessageKey) {
    if (reference == null) {
      context.showToast(i18n(invalidLinkMessageKey));
      return;
    }
    File file = fileOf(reference);
    if (file == null) {
      return;
    }
    if (!file.isFile()) {
      fehler("msg.link.file.missing", file.getPath());
      return;
    }
    if (!isOpen(file)) {
      context.diagrammLaden(file);
    }
    if (isOpen(file) && reference.hasStep()) {
      navigateToStep(reference);
    }
  }

  private String clipboardText() {
    Transferable clipboardContent = context.clipboard().getContents(null);
    if (clipboardContent == null || !clipboardContent.isDataFlavorSupported(DataFlavor.stringFlavor)) {
      return null;
    }
    try {
      return (String) clipboardContent.getTransferData(DataFlavor.stringFlavor);
    }
    catch (UnsupportedFlavorException | IOException e) {
      return null;
    }
  }

  private static Reference parseOrNull(String text) {
    if (text == null) {
      return null;
    }
    try {
      return SpecmanLink.parse(text);
    }
    catch (IllegalArgumentException e) {
      return null;
    }
  }

  /** The file the link points to. For an alias nobody knows, the user is asked which file it stands for, with the
   * file the existing aliases hint at preselected, and the answer is remembered. Null if the user doesn't tell. */
  private File fileOf(Reference reference) {
    if (!reference.hasAlias()) {
      return reference.file();
    }
    File knownFile = LinkAliases.fileOf(reference.alias());
    if (knownFile != null) {
      return knownFile;
    }
    File suggestedFile = new FileSuggester(LinkAliases.entries()).suggest(reference.alias());
    File chosen = context.chooseFileForAlias(reference.alias(), suggestedFile);
    if (chosen != null) {
      LinkAliases.assign(chosen, reference.alias());
    }
    return chosen;
  }

  private boolean isOpen(File file) {
    return getDiagrammDatei() != null
      && LinkAliases.absolutePath(getDiagrammDatei()).equals(LinkAliases.absolutePath(file));
  }

  private void navigateToStep(Reference reference) {
    StepByIdFinder finder = new StepByIdFinder(reference.stepId());
    context.traverseDiagram(finder);
    AbstractSchrittView step = finder.step();
    if (step == null) {
      context.showToast(i18n("msg.link.step.missing"));
      return;
    }
    // Lets the user come back by CTRL+ALT+Left, see TextEditArea#scrollToStepnumber
    TextEditArea origin = getLastFocusedTextArea();
    if (origin != null) {
      context.appendToEditHistory(origin.getParent());
    }
    step.scrollTo();
    String currentNumber = String.valueOf(step.getNumber());
    if (reference.stepNumber() != null && !reference.stepNumber().equals(currentNumber)) {
      context.showToast(i18n("msg.link.number.changed", reference.stepNumber(), currentNumber));
    }
  }
}
