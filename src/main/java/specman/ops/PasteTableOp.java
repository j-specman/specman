package specman.ops;

import specman.EditException;
import specman.clipboard.ExternalPasteChangemarksAdjuster;
import specman.clipboard.InternalPasteChangemarksAdjuster;
import specman.clipboard.PasteChangemarksAdjusterI;
import specman.clipboard.SpecmanTableClipboardContent;
import specman.clipboard.SpecmanTableTransferable;
import specman.editarea.TextEditArea;
import specman.model.v002.TableEditAreaModel_V002;
import specman.model.v002.io.ModelParseException;
import specman.model.v002.io.ModelParser_V002;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;

import static specman.Specman.editor;

public class PasteTableOp extends AbstractADBLSpecmanOp {

  private final TextEditArea initiatingTextArea;

  public PasteTableOp(SpecmanOpContext context, TextEditArea initiatingTextArea) {
    super(context);
    this.initiatingTextArea = initiatingTextArea;
  }

  @Override
  void execute() throws EditException {
    try {
      SpecmanTableClipboardContent content = readClipboard();
      if (content != null) {
        TableEditAreaModel_V002 tableModel = new ModelParser_V002().parseTable(content.serializedTable);
        boolean sameInstance = editor().instanceId().equals(content.instanceId);
        PasteChangemarksAdjusterI adjuster = sameInstance
            ? new InternalPasteChangemarksAdjuster()
            : new ExternalPasteChangemarksAdjuster();
        tableModel = adjuster.adjustTable(tableModel, aenderungenVerfolgen());
        initiatingTextArea.addTable(tableModel);
      }
    }
    catch (ModelParseException ex) {
      throw new EditException("Clipboard content cannot be pasted as a Specman table:\n" + ex.getMessage());
    }
  }

  private SpecmanTableClipboardContent readClipboard() {
    try {
      Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      if (clipboard.isDataFlavorAvailable(SpecmanTableTransferable.SPECMAN_TABLE_FLAVOR)) {
        return (SpecmanTableClipboardContent) clipboard.getData(SpecmanTableTransferable.SPECMAN_TABLE_FLAVOR);
      }
      String text = (String) clipboard.getData(DataFlavor.stringFlavor);
      return (text != null && !text.isBlank()) ? new SpecmanTableClipboardContent(null, text) : null;
    }
    catch (Exception ex) {
      return null;
    }
  }
}
