package specman.ops;

import classman.clipboard.ClassDiagramClipboardContent;
import classman.clipboard.ClassDiagramTransferable;
import specman.EditException;
import specman.editarea.TextEditArea;
import specman.editarea.image.classman.ForeignStereotypes;

import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

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
      Set<String> dropped = new LinkedHashSet<>();
      String source = ForeignStereotypes.withoutUnknownConfigs(content.source, dropped);
      initiatingTextArea.addClassmanImage(source);
      toastSkippedMetaTags(dropped);
    }
    catch (UnsupportedFlavorException | IOException ex) {
      throw new EditException("msg.paste.classman.failed", ex.getMessage());
    }
  }
}
