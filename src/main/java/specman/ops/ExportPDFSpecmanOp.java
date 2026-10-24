package specman.ops;

import specman.model.v002.PdfExportOptionsModel_V002;
import specman.pdf.PDFExportChooser;
import specman.pdf.PDFRenderer;
import specman.pdf.Shape;
import specman.pdf.PreparedPdfExport;
import specman.view.PdfNavigationCollector;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;

public class ExportPDFSpecmanOp extends AbstractSpecmanOp {

  private PDFExportChooser pdfExportChooser;
  private PdfExportOptionsModel_V002 pdfExportOptions;

  public void resetChooser() {
    pdfExportChooser = null;
  }

  public void setPdfExportOptions(PdfExportOptionsModel_V002 options) {
    pdfExportOptions = options;
  }

  public PdfExportOptionsModel_V002 getPdfExportOptions() {
    return pdfExportOptions;
  }

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

  private PreparedPdfExport prepareExport() {
    if (!SwingUtilities.isEventDispatchThread()) {
      AtomicReference<PreparedPdfExport> result = new AtomicReference<>();
      try {
        SwingUtilities.invokeAndWait(() -> result.set(prepareExport()));
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("PDF export preparation interrupted", exception);
      } catch (InvocationTargetException exception) {
        throw new IllegalStateException("Could not prepare PDF export", exception.getCause());
      }
      return result.get();
    }
    PdfNavigationCollector.Capture navigation = PdfNavigationCollector.collect(
      getHauptSequenz(), getIntro(), getOutro());
    Shape diagram = getDiagramShape();
    navigation.bindNavigation(diagram);
    return new PreparedPdfExport(diagram, navigation.plan());
  }

  public void export() {
    File selectedFile = chooseFileAndOptions();
    if (selectedFile != null) {
      renderPDF(selectedFile);
      displayResult(selectedFile);
    }
  }

  protected void renderPDF(File selectedFile) {
    try {
      PreparedPdfExport preparedExport = prepareExport();
      new PDFRenderer(selectedFile.getAbsolutePath(),
        PDFExportChooser.pageSizeByName(pdfExportOptions.pageSize),
        pdfExportOptions.portrait,
        pdfExportOptions.paging,
        getZoomFactor()).render(preparedExport);
    } catch (IOException | RuntimeException exception) {
      displayException(exception);
    }
  }

  protected void displayResult(File selectedFile) {
    if (pdfExportChooser.displayResult()) {
      try {
        Desktop.getDesktop().open(selectedFile);
      } catch (IOException iox) {
        displayException(iox);
      }
    }
  }

  protected File chooseFileAndOptions() {
    if (pdfExportChooser == null) {
      pdfExportChooser = new PDFExportChooser();
    }
    pdfExportChooser.initFromModel(pdfExportOptions, getDiagrammDatei());
    int result = pdfExportChooser.showSaveDialog(getScrollPane());
    if (result != JFileChooser.APPROVE_OPTION) {
      return null;
    }
    pdfExportChooser.safeUserPreferences();
    pdfExportOptions = pdfExportChooser.getExportOptions();
    File selectedFile = pdfExportChooser.getSelectedFile();
    return selectedFile;
  }
}
