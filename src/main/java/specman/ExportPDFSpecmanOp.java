package specman;

import specman.pdf.PDFExportChooser;
import specman.pdf.PDFRenderer;
import specman.pdf.Shape;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

class ExportPDFSpecmanOp extends AbstractSpecmanOp {

  ExportPDFSpecmanOp(Specman specman) {
    super(specman);
  }

  void export() {
    if (specman.pdfExportChooser == null) {
      specman.pdfExportChooser = new PDFExportChooser();
    }
    specman.pdfExportChooser.initFromModel(specman.pdfExportOptions);
    int result = specman.pdfExportChooser.showSaveDialog(specman.scrollPane, specman.diagrammDatei);
    if (result != JFileChooser.APPROVE_OPTION) {
      return;
    }
    specman.pdfExportChooser.safeUserPreferences();
    specman.pdfExportOptions = specman.pdfExportChooser.getExportOptions();
    java.io.File selectedFile = specman.pdfExportChooser.getSelectedFile();
    if (selectedFile == null) {
      return;
    }
    Point scrollPosition = specman.scrollPane.getViewport().getViewPosition();
    Point workingAreaLocation = specman.arbeitsbereich.getLocation();
    workingAreaLocation.translate(scrollPosition.x, scrollPosition.y);
    Shape all = new Shape(workingAreaLocation)
        .add(specman.intro.getShape())
        .add(specman.hauptSequenz.getShapeSequence())
        .add(specman.breitenAnpasser.getShape())
        .add(specman.outro.getShape());
    try {
      new PDFRenderer(selectedFile.getAbsolutePath(),
          specman.pdfExportChooser.getSelectedPageSize(),
          specman.pdfExportChooser.isPortrait(),
          specman.pdfExportChooser.getPaging(),
          specman.zoomFaktor).render(all);
    }
    catch (IOException iox) {
      specman.displayException(iox);
    }
    if (specman.pdfExportChooser.displayResult()) {
      try {
        Desktop.getDesktop().open(selectedFile);
      }
      catch (IOException iox) {
        specman.displayException(iox);
      }
    }
  }

}
