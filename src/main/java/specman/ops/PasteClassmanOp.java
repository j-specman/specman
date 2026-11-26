package specman.ops;

import classman.clipboard.ClassDiagramClipboardContent;
import classman.clipboard.ClassDiagramTransferable;
import specman.EditException;
import specman.editarea.TextEditArea;

import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import static specman.Specman.editor;

/** Pastes a class diagram which was copied in Specman as a diagram, not as a picture. */
public class PasteClassmanOp extends AbstractADBLSpecmanOp {

  private final TextEditArea initiatingTextArea;

  public PasteClassmanOp(SpecmanOpContext context, TextEditArea initiatingTextArea) {
    super(context);
    this.initiatingTextArea = initiatingTextArea;
  }

  @Override
  void execute() throws EditException {
    try {
      ClassDiagramClipboardContent content = (ClassDiagramClipboardContent)
        editor().clipboard().getData(ClassDiagramTransferable.CLASS_DIAGRAM_FLAVOR);
      initiatingTextArea.addClassmanImage(content.source);
    }
    catch (UnsupportedFlavorException | IOException ex) {
      throw new EditException("msg.paste.classman.failed", ex.getMessage());
    }
  }
}
