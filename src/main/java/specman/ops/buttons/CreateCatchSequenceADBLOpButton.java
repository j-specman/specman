package specman.ops.buttons;

import specman.*;

import specman.draganddrop.DragSource;
import specman.editarea.TextEditArea;
import specman.view.AbstractSchrittView;
import specman.view.CatchUeberschrift;
import specman.view.SchrittSequenzView;

public class CreateCatchSequenceADBLOpButton extends AbstractADBLSpecmanOpButton implements DragSourceProvider {

  private boolean dialogDisplayed = true;
  private CatchLinkPanel catchLinkPanel;

  public CreateCatchSequenceADBLOpButton(Specman specman) { super(specman); }

  public DragSource dragSource() { return new DragSource.CatchSequenceCreation(); }

  /** Allows tests to operate the {@link CatchLinkPanel} without displaying
   * the dialog around it */
  public void setDialogDisplayed(boolean dialogDisplayed) { this.dialogDisplayed = dialogDisplayed; }

  /** The panel of the dialog opened by the last click. */
  public CatchLinkPanel catchLinkPanel() { return catchLinkPanel; }

  @Override
  void execute() throws EditException {
    TextEditArea lastFocused = getLastFocusedTextArea();
    // A keyboard shortcut can trigger this without the button being enabled.
    if (lastFocused == null) {
      return;
    }
    AbstractSchrittView referenceStep = getHauptSequenz().findeSchritt(lastFocused);
    CatchUeberschrift referenceCatchHeading = lastFocused.containingCatchHeading();
    if (referenceStep != null) {
      dropWelcomeMessage();
      openCatchLinkDialog(referenceStep.getParent(), referenceCatchHeading);
    }
  }

  private void openCatchLinkDialog(SchrittSequenzView sequence, CatchUeberschrift referenceCatchHeading) {
    catchLinkPanel = new CatchLinkPanel(sequence, referenceCatchHeading);
    if (dialogDisplayed) {
      new CatchLinkDialog(null, catchLinkPanel);
    }
  }

}
