package specman.undo.props;

import specman.ChangeInfo;
import specman.editarea.EditArea;

public class UndoableSetChangeInfoEditArea extends UndoableSetProperty<ChangeInfo> {

  public UndoableSetChangeInfoEditArea(EditArea editArea, ChangeInfo undoChangeInfo) {
    super(undoChangeInfo, editArea::setChangeInfo, editArea::getChangeInfo);
  }
}
