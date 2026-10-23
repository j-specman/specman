package specman.pdf.navigation;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static specman.pdf.navigation.PdfNavigationData.*;
import static specman.pdf.navigation.PdfNavigationPlan.TargetId;

/** Standalone reader-check fixture. Creates a PDF but never opens a desktop application. */
public final class PdfNavigationExample {
  private PdfNavigationExample() {}

  public static void main(String[] args) throws Exception {
    if (args.length != 1) throw new IllegalArgumentException("Usage: PdfNavigationExample <new-output.pdf>");
    byte[] result = create();
    Path destination = Path.of(args[0]);
    Files.write(destination, result, StandardOpenOption.CREATE_NEW);
    try (PdfDocument pdf = new PdfDocument(new PdfReader(new ByteArrayInputStream(result)))) {
      int annotations = 0;
      for (int i = 1; i <= pdf.getNumberOfPages(); i++) annotations += pdf.getPage(i).getAnnotations().size();
      System.out.println(destination.toAbsolutePath());
      System.out.println(pdf.getNumberOfPages() + " pages, " + annotations + " internal links");
    }
  }

  public static byte[] create() throws Exception {
    TargetId first = TargetId.step("example-page-1");
    TargetId second = TargetId.step("example-page-2");
    List<LinkArea> links = new ArrayList<>();
    ByteArrayOutputStream base = new ByteArrayOutputStream();
    try (PdfDocument pdf = new PdfDocument(new PdfWriter(base))) {
      PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
      PdfPage page1 = pdf.addNewPage(PageSize.A4);
      PdfPage page2 = pdf.addNewPage(PageSize.A4);
      text(page1, font, 780, "PDF-Navigation: Seite 1 / Start");
      text(page1, font, 750, "Bitte die blauen Zeilen mit einem normalen Klick aktivieren.");
      link(page1, font, links, 1, 700, "Zu Seite 2", second);
      link(page1, font, links, 1, 650, "Noch einmal zu Seite 2", second);
      text(page1, font, 590, "Erwartung: Der Anfang der zweiten Seite wird angezeigt.");
      text(page2, font, 780, "PDF-Navigation: Seite 2 / Ziel");
      text(page2, font, 750, "Vorwaerts- und Rueckwaertssprung funktionieren im selben PDF.");
      link(page2, font, links, 2, 700, "Zurueck zu Seite 1", first);
      link(page2, font, links, 2, 650, "Auf Seite 2 nach oben (Selbstverweis)", second);
      text(page2, font, 590, "Diese Datei kann umbenannt und ohne Specman weitergegeben werden.");
    }
    return new PdfNavigationWriter().addNavigation(base.toByteArray(), new PdfNavigationData(
      Map.of(first, new Destination(1, 40, 800), second, new Destination(2, 40, 800)), links));
  }

  private static void text(PdfPage page, PdfFont font, float y, String content) {
    new PdfCanvas(page).setFillColor(ColorConstants.BLACK).beginText().setFontAndSize(font, 14)
      .moveText(40, y).showText(content).endText();
  }

  private static void link(PdfPage page, PdfFont font, List<LinkArea> links, int pageNumber,
                           float y, String content, TargetId target) {
    float width = font.getWidth(content, 16);
    PdfCanvas canvas = new PdfCanvas(page);
    canvas.setFillColor(ColorConstants.BLUE).beginText().setFontAndSize(font, 16)
      .moveText(40, y).showText(content).endText();
    canvas.setStrokeColor(ColorConstants.BLUE).moveTo(40, y - 2).lineTo(40 + width, y - 2).stroke();
    links.add(new LinkArea(pageNumber, 40, y - 3, width, 21, target));
  }
}
