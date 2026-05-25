package specman.undo.props;

import specman.ChangeInfo;
import specman.view.SchrittSequenzView;

public class UndoableSetChangeInfoSchrittSequenzView extends UndoableSetProperty<ChangeInfo> {

  public UndoableSetChangeInfoSchrittSequenzView(SchrittSequenzView schrittSequenzView, ChangeInfo undoChangeInfo) {
    super(undoChangeInfo, schrittSequenzView::setChangeInfo, schrittSequenzView::getChangeInfo);
  }
}
