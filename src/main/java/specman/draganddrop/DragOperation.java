package specman.draganddrop;

import specman.EditException;
import specman.EditorI;

public interface DragOperation {
    boolean canDrop(DropTarget target);
    void execute(DropTarget target, EditorI editor) throws EditException;
}
