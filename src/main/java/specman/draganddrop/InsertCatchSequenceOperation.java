package specman.draganddrop;

import specman.CatchLinkDialog;
import specman.EditException;
import specman.EditorI;
import specman.ops.SpecmanOpContext;

public class InsertCatchSequenceOperation implements DragOperation {
  private final SpecmanOpContext context;

  public InsertCatchSequenceOperation(SpecmanOpContext context) {
    this.context = context;
  }

  @Override
  public boolean canDrop(DropTarget target) {
    return target.sequence() != null;
  }

  @Override
  public void execute(DropTarget target, EditorI editor) throws EditException {
    context.dropWelcomeMessage();
    new CatchLinkDialog(null, target.sequence(), target.referenceCatch());
  }
}
