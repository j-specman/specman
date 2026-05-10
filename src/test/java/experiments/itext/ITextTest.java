package experiments.itext;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.html2pdf.resolver.font.DefaultFontProvider;
import com.itextpdf.io.font.FontProgram;
import com.itextpdf.io.font.FontProgramFactory;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.IBlockElement;
import com.itextpdf.layout.element.IElement;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.font.FontProvider;
import com.itextpdf.layout.layout.LayoutContext;
import com.itextpdf.layout.layout.LayoutResult;
import com.itextpdf.layout.properties.Leading;
import com.itextpdf.layout.properties.LineHeight;
import com.itextpdf.layout.properties.Property;
import com.itextpdf.layout.renderer.IRenderer;
import com.itextpdf.layout.renderer.LineRenderer;
import com.itextpdf.layout.renderer.ParagraphRenderer;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.util.ArrayList;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import static com.itextpdf.kernel.pdf.PdfName.BaseFont;
import static com.itextpdf.layout.properties.Property.FONT_PROVIDER;

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

    PdfPage page =  pdf.addNewPage();
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
  void testHTML() throws Exception {
    java.util.List<IElement> elements = HtmlConverter.convertToElements("<H1>Thank</H1>God, its <i>Friday</i>!<H1>Hello</H1>zero<ul><li>one<br>oneone one<li>two</ul>three<li>four");
    System.out.println(elements);

//    https://kb.itextpdf.com/home/it7kb/ebooks/itext-7-converting-html-to-pdf-with-pdfhtml/chapter-5-custom-tag-workers-and-css-appliers
//    letter-spacing: 0.3px;
//    line-height: 18.8px;

    PdfDocument pdf = new PdfDocument(new PdfWriter("sample.pdf"));
    Document document = new Document(pdf);
    document.setFontSize(10.0f);
    for (IElement element : elements) {
      document.setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA));
      document.add(new Paragraph().add((IBlockElement)element));
    }
    document.close();
    Desktop desktop = Desktop.getDesktop();
    desktop.open(new java.io.File("sample.pdf"));
  }

  @Test
  void testHTMLParagraph() throws Exception {
    ConverterProperties properties = new ConverterProperties();
    FontProvider fontProvider = new DefaultFontProvider(true, false, false);

    fontProvider.addFont(FontProgramFactory.createFont("src/main/resources/fonts/Sitka.ttc", 4, false));
    fontProvider.addFont(FontProgramFactory.createFont("src/main/resources/fonts/SitkaB.ttc", 4, false));
    fontProvider.addFont(FontProgramFactory.createFont("src/main/resources/fonts/SitkaI.ttc", 4, false));
    fontProvider.addFont(FontProgramFactory.createFont("src/main/resources/fonts/SitkaZ.ttc", 4, false));

    properties.setFontProvider(fontProvider);

    java.util.List<IElement> elements = HtmlConverter.convertToElements(
      "<html><head></head>" +
       "<link rel=\"stylesheet\" type=\"text/css\" href=\"src/main/resources/stylesheets/specman-pdf.css\">" +
//        "<H1>WWWWWW</H1>WWWWWW<br>God, its <i>Friday</i>!<H1>iiiiiiiiiiiiiii</H1>zero<ul><li>one<br>oneone one<li>two<li>sublist<ul><li>sub 1<li>sub2</ul></ul>three four eins zwei drei vier fünf sechs sieben acht neuen zehn elf zwölf dreizehn vierzehn fünfzehn sechzehn siebzehn achtzehn<br>Neunzehn 19" +
        "abcdefg12" +
        "</html>",
      properties);
    System.out.println(elements);

    PdfDocument pdf = new PdfDocument(new PdfWriter("sample.pdf"));
    Document document = new Document(pdf);
    //document.setFontSize(10.0f);
    //document.setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA));
    Paragraph superp = new Paragraph();
    for (IElement element : elements) {
      Paragraph p = new Paragraph();
      p.setCharacterSpacing(0.2f); // Damit können wir die Schrift "zusammendrücken"
      p.setWordSpacing(-1.5f);
      p.setMargin(0);
      // Folgendes ist relevant für den Abstand zwischen den Paragrafen. Muss aber auch mir Margin 0 kombiniert werden
      p.setMultipliedLeading(1.37f);
      // Folgendes ist relevant für den Abstand der Zeilen innerhalb eines automatisch umgebrochenen Texten in einem Paragrafen
      p.setProperty(Property.LINE_HEIGHT, LineHeight.createMultipliedValue(1.37f)); // Damit geht's aber
      p.add((IBlockElement)element);
      superp.add(p);
      superp.add("\n");
    }
    document.add(superp);
    document.close();
    Desktop desktop = Desktop.getDesktop();
    desktop.open(new java.io.File("sample.pdf"));
  }

  @Test
  void testHTMLTextAlignment() throws Exception {
    java.util.List<IElement> elements = HtmlConverter.convertToElements(
      "<html><head></head>" +
        "<div style=\"color:red;text-align:right;width:100%\">Div1</div>" +
        "<div style=\"color:red;text-align:center;width:100%\">Div2</div>" +
        "<div align=\"center\">Div3</div>" +
        "</html>");

    PdfDocument pdf = new PdfDocument(new PdfWriter("sample.pdf"));
    Document document = new Document(pdf);
    Paragraph superp = new Paragraph();
    superp.setFixedPosition(0, 700, 300);
    for (IElement element : elements) {
      Paragraph p = new Paragraph();
      p.setBackgroundColor(ColorConstants.YELLOW);
      p.setWidth(300);
      p.add((IBlockElement)element);
      superp.add(p);
      superp.add("\n");
    }
    document.add(superp);
    document.close();
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

    //ImageData data = ImageDataFactory.create("testimage-small.jpg");

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

  @Test
  void testRenderingOfStylingFromJEditorPane() throws Exception {
    java.util.List<IElement> elements = HtmlConverter.convertToElements("<html>\n" +
      "  <body>\n" +
      "    <b><font style=\"font-style:italic\" size=\"6\" face=\"Helvetica\">&#220;<span style=\"background-color:#ffff00\"><strike>be</strike></span></font></b><span style=\"background-color:#ffff00\"><strike><font size=\"3\" face=\"Helvetica\">rschr</font><b><font size=\"6\" face=\"Helvetica\">ift</font></b></strike><br>drei<br><br>vier  " +
      "  </body>\n" +
      "</html>\n");

    PdfDocument pdf = new PdfDocument(new PdfWriter("sample.pdf"));
    Document document = new Document(pdf);
    for (IElement element : elements) {
      document.add(new Paragraph().add((IBlockElement)element));
    }
    document.close();
    Desktop desktop = Desktop.getDesktop();
    desktop.open(new java.io.File("sample.pdf"));
  }

  static class LineWrapDetectingRenderer extends ParagraphRenderer {

    private final float expectedHeight;
    private float renderedHeight = 0;

    LineWrapDetectingRenderer(Paragraph modelElement, float expectedHeight) {
      super(modelElement);
      this.expectedHeight = expectedHeight;
    }

    @Override
    public LayoutResult layout(LayoutContext layoutContext) {
      LayoutResult result = super.layout(layoutContext);
      if (getOccupiedArea() != null) {
        renderedHeight = getOccupiedArea().getBBox().getHeight();
      }
      return result;
    }

    boolean hasUnexpectedWrap() {
      return renderedHeight > expectedHeight * 1.5f;
    }
  }

  @Test
  void testUnexpectedLineWrap() throws Exception {
    java.util.List<IElement> elements = HtmlConverter.convertToElements("<html>\n" +
      "  <body>\n" +
      "    Hello World!" +
      "  </body>\n" +
      "</html>\n");

    PdfDocument pdf = new PdfDocument(new PdfWriter("sample.pdf"));
    Document document = new Document(pdf);

    java.util.List<LineWrapDetectingRenderer> renderers = new ArrayList<>();

    for (IElement elementToPrint : elements) {
      Paragraph p = new Paragraph()
        .setBackgroundColor(ColorConstants.YELLOW)
        .setMargin(0)
        .setMultipliedLeading(0.0f)
        .setCharacterSpacing(-0.1f)
        .setFixedPosition(200, 700, 60);
      p.add((IBlockElement) elementToPrint);

      LineWrapDetectingRenderer renderer = new LineWrapDetectingRenderer(p, 13.8f);
      p.setNextRenderer(renderer);
      renderers.add(renderer);

      document.add(p);
    }

    document.close();

    for (LineWrapDetectingRenderer renderer : renderers) {
      if (renderer.hasUnexpectedWrap()) {
        System.out.println("Unerwarteter Zeilenumbruch in: " + renderer.getOccupiedArea());
      }
    }

    Desktop desktop = Desktop.getDesktop();
    desktop.open(new java.io.File("sample.pdf"));
  }

}
