package experiments.itext;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import experiments.NoSuiteTest;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@NoSuiteTest
public class ITextPageTest {
  @Test
  void testCustomSizePages() throws Exception {
    String dest = "sample-testCustomSizePages.pdf";
    PdfWriter writer = new PdfWriter(dest);

    PdfDocument pdf = new PdfDocument(writer);
    Document document = new Document(pdf);
    pdf.addNewPage(new PageSize(PageSize.A4.getWidth(), 2000.0F));

    // Das sind mehr Texte, als auf die große 1. Seite passen.
    // Für die überzähligen macht iText eine neue Seite, die
    // dann wieder DIN A4 hat
    for (int i = 0; i < 80; i++) {
      document.add(new Paragraph("Hello World " + i));
    }

    document.close();
    Desktop desktop = Desktop.getDesktop();
    desktop.open(new java.io.File(dest));
  }

  @Test
  void testTileOversizedPage() throws Exception {
    String oversized = "sample-testTileOversizedPage.pdf";
    PdfWriter writer = new PdfWriter(oversized);
    PdfDocument pdf = new PdfDocument(writer);
    Document document = new Document(pdf);
    pdf.addNewPage(new PageSize(PageSize.A4.getWidth(), 2000.0F));
    for (int i = 0; i < 50; i++) {
      document.add(new Paragraph("Hello World " + i));
    }
    document.close();

    PdfReader reader = new PdfReader(oversized);
    pdf = new PdfDocument(reader);
    PdfPage page = pdf.getPage(1);

    String a4sized = "sample-a4-testTileOversizedPage.pdf";
    PdfWriter a4writer = new PdfWriter(a4sized);
    PdfDocument a4pdf = new PdfDocument(a4writer);

    com.itextpdf.kernel.geom.Rectangle oversizedRect = page.getPageSizeWithRotation();
    // Getting the size of the page
    PdfFormXObject pageCopy = page.copyAsFormXObject(a4pdf);

    // Tile size
    com.itextpdf.kernel.geom.Rectangle tileSize = PageSize.A4;

    // The first tile
    PdfPage a4page1 = a4pdf.addNewPage(PageSize.A4);
    PdfCanvas canvas1 = new PdfCanvas(a4page1);
    canvas1.addXObjectAt(pageCopy, 0, -oversizedRect.getHeight() + tileSize.getHeight());

    // The second tile
    PdfPage a4page2 = a4pdf.addNewPage(PageSize.A4);
    PdfCanvas canvas2 = new PdfCanvas(a4page2);
    canvas2.addXObjectAt(pageCopy, 0, -oversizedRect.getHeight() + 2 * tileSize.getHeight());

    a4pdf.close();

    Desktop desktop = Desktop.getDesktop();
    desktop.open(new java.io.File(a4sized));
  }

  @Test
  /** Wie {@link #testTileOversizedPage()}, aber ohne dass das PDF mit der übergroßen Seite
   * vorher auf Datei geschrieben und wieder eingelesen wird. */
  void testTileOversizedUnfiledPage() throws Exception {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    PdfDocument pdf = new PdfDocument(new PdfWriter(os));
    Document document = new Document(pdf);
    pdf.addNewPage(new PageSize(PageSize.A4.getWidth(), 2000.0F));
    for (int i = 0; i < 50; i++) {
      document.add(new Paragraph("Hello World " + i));
    }
    document.close();

    byte[] rawDocument = os.toByteArray();
    System.out.println(rawDocument.length);
    ByteArrayInputStream is = new ByteArrayInputStream(rawDocument);
    PdfReader reader = new PdfReader(is);
    pdf = new PdfDocument(reader);
    PdfPage page = pdf.getPage(1);
    com.itextpdf.kernel.geom.Rectangle oversizedRect = page.getPageSizeWithRotation();

    String a4sized = "sample-a4-testTileOversizedUnfiledPage.pdf";
    PdfWriter a4writer = new PdfWriter(a4sized);
    PdfDocument a4pdf = new PdfDocument(a4writer);

    // Getting the size of the page
    PdfFormXObject pageCopy = page.copyAsFormXObject(a4pdf);

    PageSize tileSize = PageSize.A4;
    float tileHeight = tileSize.getHeight();

    for (int tileNo = 1; tileNo * tileHeight < oversizedRect.getHeight(); tileNo++) {
      PdfPage a4page = a4pdf.addNewPage(tileSize);
      PdfCanvas canvas = new PdfCanvas(a4page);
      canvas.addXObjectAt(pageCopy, 0, -oversizedRect.getHeight() + tileHeight * tileNo);
    }

    a4pdf.close();

    Desktop desktop = Desktop.getDesktop();
    desktop.open(new java.io.File(a4sized));
  }

  @Test
  void testMultiplePages() throws Exception {
    String dest = "sample-testMultiplePages.pdf";
    PdfWriter writer = new PdfWriter(dest);

    PdfDocument pdf = new PdfDocument(writer);
    Document document = new Document(pdf);
    PdfPage page1 = pdf.addNewPage();
    PdfCanvas canvas1 = new PdfCanvas(page1);
    canvas1.moveTo(100, 300).lineTo(500, 300).closePathStroke();

    PdfPage page2 = pdf.addNewPage();
    PdfCanvas canvas2 = new PdfCanvas(page2);
    canvas2.moveTo(100, 300).lineTo(500, 300).closePathStroke();

    document.close();
    Desktop desktop = Desktop.getDesktop();
    desktop.open(new java.io.File(dest));
  }
}
