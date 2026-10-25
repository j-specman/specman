package specman.ops;

import specman.clipboard.SpecmanClipboardContent;
import specman.clipboard.SpecmanStepsTransferable;
import specman.model.v002.AbstractStepModel_V002;
import specman.model.v002.io.ModelSerializer_V002;
import specman.undo.manager.UndoRecording;
import specman.view.AbstractSchrittView;

import static specman.Specman.editor;

public class CopyStepOp {

  private final AbstractSchrittView step;

  public CopyStepOp(AbstractSchrittView step) {
    this.step = step;
  }

  public void run() {
    // generiereModel(true) cleans up text formatting before serializing, which would
    // otherwise pollute the undo history with invisible side-effect edits from the copy action
    try (UndoRecording ur = editor().pauseUndo()) {
      AbstractStepModel_V002 model = step.generiereModel(true);
      String serialized = new ModelSerializer_V002().serializeStep(model);
      SpecmanClipboardContent content = new SpecmanClipboardContent(editor().instanceId(), serialized);
      editor().clipboard().setContents(new SpecmanStepsTransferable(content), null);
    }
  }
}
