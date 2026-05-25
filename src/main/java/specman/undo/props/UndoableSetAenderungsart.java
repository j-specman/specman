package specman.undo.props;

import specman.ChangeInfo;
import specman.view.AbstractSchrittView;

public class UndoableSetAenderungsart extends UndoableSetProperty<ChangeInfo> {

  public UndoableSetAenderungsart(AbstractSchrittView schrittView, ChangeInfo undoChangeInfo) {
    super(undoChangeInfo, schrittView::setChangeInfo, schrittView::getChangeInfo);
  }
}
