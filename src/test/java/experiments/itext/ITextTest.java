package experiments.itext;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 *  Simple examples taken from https://www.tutorialspoint.com/itext/itext_drawing_line.htm
 */
public class ITextTest {

  @Test
  void testCreateEmptyDocument() throws Exception {
    String dest = "sample.pdf";
    PdfWriter writer = new PdfWriter(dest);
    PdfDocument pdfDoc = new PdfDocument(writer);
    pdfDoc.addNewPage();
    Document document = new Document(pdfDoc);
    document.close();
    System.out.println("PDF Created");
    Desktop desktop = Desktop.getDesktop();
    desktop.open(new java.io.File("sample.pdf"));
  }

  @Test
  void testDrawLine() throws Exception {
    String dest = "sample.pdf";
    PdfWriter writer = new PdfWriter(dest);
    PdfDocument pdfDoc = new PdfDocument(writer);
    PdfPage page = pdfDoc.addNewPage();
    Document document = new Document(pdfDoc);

    PdfCanvas canvas = new PdfCanvas(page);
    canvas.moveTo(100, 300);
    canvas.lineTo(500, 300);
    canvas.closePathStroke();

    document.close();
    Desktop desktop = Desktop.getDesktop();
    desktop.open(new java.io.File("sample.pdf"));
  }

  @Test
  void testDrawRoundedRectangle() throws Exception {
    String dest = "sample.pdf";
    PdfWriter writer = new PdfWriter(dest);
    PdfDocument pdfDoc = new PdfDocument(writer);
    PdfPage page = pdfDoc.addNewPage();
    Document document = new Document(pdfDoc);

    PdfCanvas canvas = new PdfCanvas(page);
    canvas.roundRectangle(50, 700, 300, 50, 10);
    canvas.closePathStroke();

    document.close();
    Desktop desktop = Desktop.getDesktop();
    desktop.open(new java.io.File("sample.pdf"));
  }

  @Test
  void testParagraphs() throws Exception {
    String dest = "sample.pdf";
    PdfWriter writer = new PdfWriter(dest);

    PdfDocument pdf = new PdfDocument(writer);

    Document document = new Document(pdf);
    String para1 = "Tutorials Point originated from the idea that there exists " +
      "a class of readers who respond better to online content and prefer to learn " +
      "new skills at their own pace from the comforts of their drawing rooms.";

    String para2 = "The journey commenced with a single tutorial on HTML in 2006 " +
      "and elated by the response it generated, we worked our way to adding fresh " +
      "tutorials to our repository which now proudly flaunts a wealth of tutorials " +
      "and allied articles on topics ranging from programming languages to web designing " +
      "to academics and much more.";

    Paragraph paragraph1 = new Paragraph(para1);
    Paragraph paragraph2 = new Paragraph(para2);

    document.add(paragraph1);
    document.add(paragraph2);

    document.close();
    System.out.println("Paragraph added");
    Desktop desktop = Desktop.getDesktop();
    desktop.open(new java.io.File("sample.pdf"));
  }

  @Test
  void testShowText() throws Exception {
    String dest = "sample.pdf";
    PdfWriter writer = new PdfWriter(dest);
    PdfDocument pdf = new PdfDocument(writer);
    Document document = new Document(pdf);

    PdfFont labelFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

    PdfPage page = pdf.addNewPage();
    PdfCanvas pdfCanvas = new PdfCanvas(page);
    pdfCanvas.setFillColor(DeviceRgb.BLUE);

    pdfCanvas.beginText().setFontAndSize(labelFont, 12)
      .moveText(20, 800)
      .showText("Hell")
      .setFontAndSize(labelFont, 20)
      .showText("o")
      .setFontAndSize(labelFont, 12)
      .showText(" World!")
      .endText();

    document.close();
    pdf.close();

    Desktop desktop = Desktop.getDesktop();
    desktop.open(new java.io.File("sample.pdf"));
  }

  @Test
  void testScaledImage() throws Exception {
    PdfDocument pdf = new PdfDocument(new PdfWriter("sample.pdf"));
    Document document = new Document(pdf);
    Paragraph p = new Paragraph()
      .setFixedPosition(20, 500, 100)
      .setMargin(0);

    BufferedImage fullSizeImage = ImageIO.read(new File("testimage-small.jpg"));
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    ImageIO.write(fullSizeImage, "jpg", bytes);
    ImageData data = ImageDataFactory.create(bytes.toByteArray());

    com.itextpdf.layout.element.Image img = new com.itextpdf.layout.element.Image(data);
    img.setAutoScale(true);

    p.add(img);

    document.add(p);
    document.close();
    Desktop desktop = Desktop.getDesktop();
    desktop.open(new java.io.File("sample.pdf"));
  }

  @Test
  void testStylifyTextAlignment() {
    String rawHTML = "<div align=\"right\">      Neuer Schritt 1    </div>";
    System.out.println(rawHTML.replaceAll("align=\"([a-z]+)\"", "style=\"text-align:$1\""));
  }

}
