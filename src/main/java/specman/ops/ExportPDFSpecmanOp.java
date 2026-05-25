package specman.ops;

import specman.model.v001.PDFExportOptionsModel_V001;
import specman.pdf.PDFExportChooser;
import specman.pdf.PDFRenderer;
import specman.pdf.Shape;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class ExportPDFSpecmanOp extends AbstractSpecmanOp {

  private PDFExportChooser pdfExportChooser;
  private PDFExportOptionsModel_V001 pdfExportOptions;

  public void resetChooser() { pdfExportChooser = null; }
  public void setPdfExportOptions(PDFExportOptionsModel_V001 options) { pdfExportOptions = options; }
  public PDFExportOptionsModel_V001 getPdfExportOptions() { return pdfExportOptions; }

  public ExportPDFSpecmanOp(SpecmanOpContext context) {
    super(context);
  }

  private Shape getDiagramShape() {
    java.awt.Point scrollPosition = getScrollPane().getViewport().getViewPosition();
    java.awt.Point workingAreaLocation = getArbeitsbereich().getLocation();
    workingAreaLocation.translate(scrollPosition.x, scrollPosition.y);
    return new Shape(workingAreaLocation)
        .add(getIntro().getShape())
        .add(getHauptSequenz().getShapeSequence())
        .add(getBreitenAnpasser().getShape())
        .add(getOutro().getShape());
  }

  public void export() {
    if (pdfExportChooser == null) {
      pdfExportChooser = new PDFExportChooser();
    }
    pdfExportChooser.initFromModel(pdfExportOptions);
    int result = pdfExportChooser.showSaveDialog(getScrollPane(), getDiagrammDatei());
    if (result != JFileChooser.APPROVE_OPTION) {
      return;
    }
    pdfExportChooser.safeUserPreferences();
    pdfExportOptions = pdfExportChooser.getExportOptions();
    java.io.File selectedFile = pdfExportChooser.getSelectedFile();
    if (selectedFile == null) {
      return;
    }
    Shape all = getDiagramShape();
    try {
      new PDFRenderer(selectedFile.getAbsolutePath(),
          pdfExportChooser.getSelectedPageSize(),
          pdfExportChooser.isPortrait(),
          pdfExportChooser.getPaging(),
          getZoomFactor()).render(all);
    }
    catch (IOException iox) {
      displayException(iox);
    }
    if (pdfExportChooser.displayResult()) {
      try {
        Desktop.getDesktop().open(selectedFile);
      }
      catch (IOException iox) {
        displayException(iox);
      }
    }
  }

}
