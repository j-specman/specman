package specman.ops.buttons;

import specman.*;


public class AcceptChangesADBLOpButton extends AbstractADBLSpecmanOpButton {

  private final LinkedMarksConfirmation linkedMarks = new LinkedMarksConfirmation(context);

  public AcceptChangesADBLOpButton(Specman specman) {
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
    int changesMade = 0;
    changesMade += getIntro().aenderungenUebernehmen();
    getIntro().aenderungsmarkierungenEntfernen(null);
    changesMade += getHauptSequenz().aenderungenUebernehmen();
    changesMade += getOutro().aenderungenUebernehmen();
    getOutro().aenderungsmarkierungenEntfernen(null);
    if (changesMade > 0) {
      diagrammAktualisieren(null);
    } else {
      showMessage("msg.changes.none");
    }
  }

}
