package specman.undo.props;

import specman.ChangeInfo;
import specman.view.CatchUeberschrift;

public class UndoableSetChangeInfoCatchUeberschrift extends UndoableSetProperty<ChangeInfo> {

  public UndoableSetChangeInfoCatchUeberschrift(CatchUeberschrift catchUeberschrift, ChangeInfo undoChangeInfo) {
    super(undoChangeInfo, catchUeberschrift::setChangeInfo, catchUeberschrift::getChangeInfo);
  }
}
