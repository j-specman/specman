package specman.ops;

import specman.clipboard.SpecmanTableClipboardContent;
import specman.clipboard.SpecmanTableTransferable;
import specman.editarea.TableEditArea;
import specman.model.v002.TableEditAreaModel_V002;
import specman.model.v002.io.ModelSerializer_V002;
import specman.undo.manager.UndoRecording;

import java.awt.Toolkit;

import static specman.Specman.editor;

public class CopyTableOp {

  private final TableEditArea table;

  public CopyTableOp(TableEditArea table) {
    this.table = table;
  }

  public void run() {
    // toModel(true) cleans up text formatting before serializing, which would
    // otherwise pollute the undo history with invisible side-effect edits from the copy action
    try (UndoRecording ur = editor().pauseUndo()) {
      TableEditAreaModel_V002 model = table.toModel(true);
      String serialized = new ModelSerializer_V002().serializeTable(model);
      SpecmanTableClipboardContent content = new SpecmanTableClipboardContent(editor().instanceId(), serialized);
      Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new SpecmanTableTransferable(content), null);
    }
  }
}
