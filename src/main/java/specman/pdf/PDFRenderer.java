package specman.pdf;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.PdfCanvasConstants.LineCapStyle;
import com.itextpdf.kernel.pdf.canvas.PdfCanvasConstants.LineJoinStyle;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Document;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import static specman.pdf.Shape.DEFAULT_LINE_COLOR;
import static specman.pdf.Shape.toPDFColor;

public class PDFRenderer {
  // This factor turned out to be the one, which causes the PDF-rendered
  // diagramm to get nearly the same size in Acrobat Reader as it appears
  // in the UI on the same monitor. For size comparison, make sure to
  // set the Acrobat Reader's zoom factor in a way, that the pages are actually
  // displayed with the size of an A4 sheet. Hold a sheet on the monitor for
  // zoom factor adaption. On my monitor the Acrobat Reader requires a zoom
  // factor of 85% rather than 100% to display the pages correctly.
  public static float SWING2PDF_SCALEFACTOR_100PERCENT = 0.77f;

  public static final int LEFT_RIGHT_PAGE_MARGIN = 10;
  String pdfFilename;
  PdfWriter writer;
  PdfDocument pdfDoc;
  Document document;
  PdfCanvas pdfCanvas;
  ByteArrayOutputStream pdfOutputStream;
  PageSize pageSize;
  boolean withPageTiling;
  int uizoomfactor;
  float swing2pdfScaleFactor;

  public PDFRenderer(String pdfFilename, PageSize pageSize, boolean portrait, boolean withPageTiling, int uizoomfactor) {
    try {
      this.pdfFilename = pdfFilename;
      this.pageSize = portrait ? pageSize : pageSize.rotate();
      this.withPageTiling = withPageTiling;
      this.uizoomfactor = uizoomfactor;
    }
    catch(Exception x) {
      x.printStackTrace();
    }
  }

  private void initWriterAndDocument() {
    pdfOutputStream = new ByteArrayOutputStream();
    writer = new PdfWriter(pdfOutputStream);
    pdfDoc = new PdfDocument(writer);
    document = new Document(pdfDoc);
  }

  public void render(Shape rootShape) throws IOException {
    LineWrapDetector.start();
    do {
      initWriterAndDocument();
      PageSize overlengthPagesize = initPdfCanvasAndScaleFactor(rootShape);
      int yTop = hasOverlength(overlengthPagesize)
        ? rootShape.getHeight()
        : (int)(pageSize.getHeight() / swing2pdfScaleFactor);
      Point topLeftCorner = new Point(0, yTop);
      render(rootShape, topLeftCorner, overlengthPagesize);
    }
    while(LineWrapDetector.retryRequired());
  }

  private PageSize initPdfCanvasAndScaleFactor(Shape rootShape) {
    swing2pdfScaleFactor = SWING2PDF_SCALEFACTOR_100PERCENT;
    int shapeWidth = rootShape.getWidth();
    float availableWidth100Percent = pageSize.getWidth() / SWING2PDF_SCALEFACTOR_100PERCENT;
    float requiredWidth = shapeWidth + 2 * LEFT_RIGHT_PAGE_MARGIN;
    if (requiredWidth > availableWidth100Percent) {
      swing2pdfScaleFactor *= availableWidth100Percent / requiredWidth;
    }

    // We create a canvas which is high enough to contain the whole shape but therefore potentially
    // exceeds the maximum height of the selected page size. This happens on purpose. Either the user
    // has decided to allow over-length pages or we split the over-length page into multiple tiles
    // later on. There is currently no intelligent way to find reasonable splitting points in a
    // multipage rendering
    PageSize overlengthPagesize = new PageSize(pageSize.getWidth(),
      Math.max(pageSize.getHeight(), rootShape.getHeight() * swing2pdfScaleFactor));
    pdfCanvas = new PdfCanvas(pdfDoc.addNewPage(overlengthPagesize));

    pdfCanvas.setFillColor(Shape.DEFAULT_FILL_COLOR);
    pdfCanvas.setStrokeColor(Shape.DEFAULT_LINE_COLOR);
    pdfCanvas.setLineJoinStyle(LineJoinStyle.ROUND);
    pdfCanvas.setLineCapStyle(LineCapStyle.PROJECTING_SQUARE);

    FormattedShapeText.initFont(uizoomfactor, swing2pdfScaleFactor);

    return overlengthPagesize;
  }

  private void render(Shape rootShape, Point renderOffset, PageSize overlengthPagesize) throws IOException {
    drawShape(rootShape, renderOffset);
    document.close();
    pdfOutputStream.close();

    tileOverlengthPage(overlengthPagesize);

    FileOutputStream fos = new FileOutputStream(pdfFilename);
    fos.write(pdfOutputStream.toByteArray());
    fos.close();
  }

  private boolean hasOverlength(PageSize overlengthPagesize) {
    return overlengthPagesize.getHeight() > pageSize.getHeight();
  }

  private void tileOverlengthPage(PageSize overlengthPagesize) throws IOException {
    if (!hasOverlength(overlengthPagesize) || !withPageTiling) {
      return;
    }
    ByteArrayInputStream is = new ByteArrayInputStream(pdfOutputStream.toByteArray());
    PdfReader overlengthReader = new PdfReader(is);
    PdfDocument overlengthPdf = new PdfDocument(overlengthReader);
    PdfPage overlengthPage = overlengthPdf.getPage(1); // Overlength PDF has only one page
    com.itextpdf.kernel.geom.Rectangle overlengthRect = overlengthPage.getPageSizeWithRotation();

    pdfOutputStream = new ByteArrayOutputStream();
    PdfDocument tilePdf = new PdfDocument(new PdfWriter(pdfOutputStream));
    PdfFormXObject overlengthForm = overlengthPage.copyAsFormXObject(tilePdf);

    float tileHeight = pageSize.getHeight();
//    PdfFont pageNumberingFont = AbstractShapeText.createFont("Helvetica");

    for (int tileNo = 1; (tileNo-1) * tileHeight < overlengthRect.getHeight(); tileNo++) {
      PdfPage tilePage = tilePdf.addNewPage(pageSize);
      PdfCanvas canvas = new PdfCanvas(tilePage);
      canvas.addXObjectAt(overlengthForm, 0, -overlengthRect.getHeight() + tileHeight * tileNo);
//      canvas
//        .beginText()
//        .setFontAndSize(pageNumberingFont, 10)
//        .setStrokeColor(ColorConstants.RED)
//        .moveText(3, 3)
//        .showText(Integer.toString(tileNo))
//        .endText();
    }
    tilePdf.close();
    pdfOutputStream.close();
  }

  private void drawShape(Shape shape, Point renderOffset) {
    if (shape.hasForm()) {
        if (!shape.isLine()) {
          pdfCanvas.setFillColor(shape.getPDFBackgroundColor());
          shape.runPath(renderOffset, swing2pdfScaleFactor, pdfCanvas);
          // This looks wrong and is probably caused by a lack of knowledge of the iText API:
          // If we run the shape's path *twice*, the gap between the shapes shows up with a
          // reasonable width. If we run the path only *once*, the gaps appear thinner than
          // expected when displaying the PDF in Acrobat Reader in 100% resolution. The
          // solution with the double-run was an accidential finding.
          shape.runPath(renderOffset, swing2pdfScaleFactor, pdfCanvas);
          pdfCanvas.fill();
        }
        if (shape.withOutline()) {
          pdfCanvas.setLineWidth(shape.lineWidth() * swing2pdfScaleFactor);
          pdfCanvas.setStrokeColor(toPDFColor(shape.backgroundColor, DEFAULT_LINE_COLOR));
          shape.runPath(renderOffset, swing2pdfScaleFactor, pdfCanvas);
          pdfCanvas.stroke();
        }
    }
    renderOffset = shape.translate(renderOffset);
    writeShapeText(shape, renderOffset);
    drawShapeImage(shape, renderOffset);
    for (Shape subshape: shape.getSubshapes()) {
      drawShape(subshape, renderOffset);
    }
    shape.applyDecoration(renderOffset, swing2pdfScaleFactor, pdfCanvas);
  }

  private void drawShapeImage(Shape shape, Point renderOffset) {
    ShapeImage image = shape.getImage();
    if (image != null) {
      image.drawToPDF(renderOffset, swing2pdfScaleFactor, pdfCanvas, document);
    }
  }

  private void writeShapeText(Shape shape, Point renderOffset) {
    AbstractShapeText text = shape.getText();
    if (text != null) {
      text.writeToPDF(renderOffset, swing2pdfScaleFactor, pdfCanvas, document);
    }
  }

}
