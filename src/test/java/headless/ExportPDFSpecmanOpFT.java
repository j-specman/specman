package headless;

import specman.ops.ExportPDFSpecmanOp;
import specman.ops.SpecmanOpContext;

import java.io.File;

/**
 * The PDF export without dialog: The export options which tests prepare via
 * {@link SpecmanFT#exportPdf} play the role of the user's choices in the file chooser.
 * The diagram is laid out before rendering, because without a frame nothing else does it.
 */
public class ExportPDFSpecmanOpFT extends ExportPDFSpecmanOp {

  public ExportPDFSpecmanOpFT(SpecmanOpContext context) {
    super(context);
  }

  @Override
  protected File chooseFileAndOptions() {
    return new File(getPdfExportOptions().filename);
  }

  @Override
  protected void renderPDF(File selectedFile) {
    ((SpecmanFT) context).layoutComponents();
    super.renderPDF(selectedFile);
  }

  @Override
  protected void displayResult(File selectedFile) {}

}
