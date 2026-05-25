package specman.undo.props;

import specman.ChangeInfo;
import specman.view.AbstractSchrittView;

public class UndoableSetChangeInfoAbstractSchrittView extends UndoableSetProperty<ChangeInfo> {

  public UndoableSetChangeInfoAbstractSchrittView(AbstractSchrittView schrittView, ChangeInfo undoChangeInfo) {
    super(undoChangeInfo, schrittView::setChangeInfo, schrittView::getChangeInfo);
  }
}
