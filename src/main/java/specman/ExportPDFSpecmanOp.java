package specman;

import specman.model.v001.PDFExportOptionsModel_V001;
import specman.pdf.PDFExportChooser;
import specman.pdf.PDFRenderer;
import specman.pdf.Shape;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

class ExportPDFSpecmanOp extends AbstractSpecmanOp {

  private PDFExportChooser pdfExportChooser;
  private PDFExportOptionsModel_V001 pdfExportOptions;

  void resetChooser() { pdfExportChooser = null; }
  void setPdfExportOptions(PDFExportOptionsModel_V001 options) { pdfExportOptions = options; }
  PDFExportOptionsModel_V001 getPdfExportOptions() { return pdfExportOptions; }

  ExportPDFSpecmanOp(Specman specman) {
    super(specman);
  }

  void export() {
    if (pdfExportChooser == null) {
      pdfExportChooser = new PDFExportChooser();
    }
    pdfExportChooser.initFromModel(pdfExportOptions);
    int result = pdfExportChooser.showSaveDialog(specman.scrollPane, specman.diagrammDatei);
    if (result != JFileChooser.APPROVE_OPTION) {
      return;
    }
    pdfExportChooser.safeUserPreferences();
    pdfExportOptions = pdfExportChooser.getExportOptions();
    java.io.File selectedFile = pdfExportChooser.getSelectedFile();
    if (selectedFile == null) {
      return;
    }
    Point scrollPosition = specman.scrollPane.getViewport().getViewPosition();
    Point workingAreaLocation = specman.arbeitsbereich.getLocation();
    workingAreaLocation.translate(scrollPosition.x, scrollPosition.y);
    Shape all = new Shape(workingAreaLocation)
        .add(specman.intro.getShape())
        .add(specman.getHauptSequenz().getShapeSequence())
        .add(specman.breitenAnpasser.getShape())
        .add(specman.outro.getShape());
    try {
      new PDFRenderer(selectedFile.getAbsolutePath(),
          pdfExportChooser.getSelectedPageSize(),
          pdfExportChooser.isPortrait(),
          pdfExportChooser.getPaging(),
          specman.zoomFaktor).render(all);
    }
    catch (IOException iox) {
      specman.displayException(iox);
    }
    if (pdfExportChooser.displayResult()) {
      try {
        Desktop.getDesktop().open(selectedFile);
      }
      catch (IOException iox) {
        specman.displayException(iox);
      }
    }
  }

}
