package specman.ops.buttons;

import specman.*;


public class RejectChangesADBLOpButton extends AbstractADBLSpecmanOpButton {

  private final LinkedMarksConfirmation linkedMarks = new LinkedMarksConfirmation(context);

  public RejectChangesADBLOpButton(Specman specman) {
    super(specman);
  }

  @Override
  boolean confirmed() {
    return linkedMarks.confirmed(ChangeSet.changeset());
  }

  @Override
  void finished() {
    linkedMarks.finished();
  }

  @Override
  void execute() throws EditException {
    int changesRejected = 0;
    changesRejected += getIntro().aenderungenVerwerfen();
    getIntro().aenderungsmarkierungenEntfernen(null);
    changesRejected += getHauptSequenz().aenderungenVerwerfen();
    changesRejected += getOutro().aenderungenVerwerfen();
    getOutro().aenderungsmarkierungenEntfernen(null);
    if (changesRejected > 0) {
      diagrammAktualisieren(null);
    } else {
      showMessage("msg.changes.none");
    }
  }

}
