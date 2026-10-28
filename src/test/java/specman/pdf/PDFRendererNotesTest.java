package specman.pdf;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import specman.pdf.navigation.PdfNavigationPlan;
import specman.pdf.navigation.PdfNavigationRenderContext;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static specman.pdf.navigation.PdfNavigationPlan.*;

/** Comments and navigation must survive the same rendering and page-splitting passes. */
class PDFRendererNotesTest {
  @TempDir Path directory;

  @Test void singlePageKeepsNotesAndLinks() throws Exception { verify(400, 500, true, false); }
  @Test void overlengthPageKeepsNotesAndLinks() throws Exception { verify(400, 2800, true, false); }
  @Test void tiledPagesKeepNotesAndLinks() throws Exception { verify(400, 2800, true, true); }
  @Test void landscapeScalingKeepsNotesAndLinks() throws Exception { verify(1400, 5000, false, true); }

  @Test
  void reusingRendererDoesNotKeepNotesFromPreviousExport() throws Exception {
    Path file = directory.resolve("reused.pdf");
    PDFRenderer renderer = new PDFRenderer(file.toString(), PageSize.A4, true, false, 100);
    renderer.render(rectangle(400, 500).withNote(
      new ShapeNote("author", "old comment", new java.awt.Rectangle(10, 10, 40, 20))));
    renderer.render(rectangle(400, 500));
    try (PdfDocument pdf = new PdfDocument(new PdfReader(file.toString()))) {
      assertTrue(pdf.getPage(1).getAnnotations().isEmpty());
    }
  }

  private void verify(int width, int height, boolean portrait, boolean paging) throws Exception {
    TargetId top = TargetId.step("top"), bottom = TargetId.catchHeading("bottom");
    PdfNavigationPlan plan = PdfNavigationPlan.builder()
      .target(new Target(top, "1", null, true))
      .target(new Target(bottom, "2", null, true))
      .connection(LinkKind.BREAK_TO_CATCH, Source.label(top, true), bottom)
      .connection(LinkKind.CATCH_TO_BREAK, Source.label(bottom, true), top)
      .build();
    Shape diagram = rectangle(width, height);
    diagram.add(header(top, "top", 40));
    diagram.add(header(bottom, "bottom", height - 60));
    Path file = directory.resolve("comments-and-links.pdf");
    new PDFRenderer(file.toString(), PageSize.A4, portrait, paging, 100)
      .render(new PreparedPdfExport(diagram, plan));

    try (PdfDocument pdf = new PdfDocument(new PdfReader(file.toString()))) {
      float pageHeight = portrait ? PageSize.A4.getHeight() : PageSize.A4.getWidth();
      float pageWidth = portrait ? PageSize.A4.getWidth() : PageSize.A4.getHeight();
      float scale = Math.min(0.77f, pageWidth / (width + 20));
      float sourceHeight = Math.max(pageHeight, height * scale);
      float rootTop = height * scale > pageHeight ? height : (int) (pageHeight / scale);
      boolean tiled = paging && sourceHeight > pageHeight;
      assertEquals(tiled ? (int) Math.ceil(sourceHeight / pageHeight) : 1, pdf.getNumberOfPages());
      Set<String> comments = new HashSet<>();
      int links = 0;
      for (int pageNo = 1; pageNo <= pdf.getNumberOfPages(); pageNo++) {
        for (var annotation : pdf.getPage(pageNo).getAnnotations()) {
          PdfDictionary object = annotation.getPdfObject();
          if (PdfName.Highlight.equals(object.getAsName(PdfName.Subtype))) {
            String name = object.getAsString(PdfName.Contents).toUnicodeString();
            assertTrue(comments.add(name), "Each comment must appear exactly once");
            assertTrue(Set.of("top", "bottom").contains(name));
            int y = name.equals("top") ? 40 : height - 60;
            float originalY = (rootTop - y - 8 - 16) * scale;
            int expectedPage = tiled ? (int) ((sourceHeight - originalY - 8 * scale) / pageHeight) + 1 : 1;
            assertEquals(expectedPage, pageNo);
            float offset = tiled ? pageNo * pageHeight - sourceHeight : 0;
            var area = annotation.getRectangle().toRectangle();
            assertEquals(45 * scale, area.getX(), 0.03f);
            assertEquals(originalY + offset, area.getY(), 0.03f);
            assertEquals(45 * scale, area.getWidth(), 0.03f);
            assertEquals(16 * scale, area.getHeight(), 0.03f);
            assertEquals("author", object.getAsString(PdfName.T).toUnicodeString());
          } else {
            assertEquals(PdfName.Link, object.getAsName(PdfName.Subtype));
            links++;
            PdfDictionary action = object.getAsDictionary(PdfName.A);
            assertEquals(PdfName.GoTo, action.getAsName(PdfName.S));
            PdfArray destination = action.getAsArray(PdfName.D);
            float destinationY = destination.getAsNumber(3).floatValue();
            // The top source links to the bottom header, and vice versa.
            float sourceY = annotation.getRectangle().toRectangle().getY();
            if (tiled) sourceY -= pageNo * pageHeight - sourceHeight;
            boolean topSource = Math.abs(sourceY - (rootTop - 40 - 14) * scale) < 0.03f;
            float targetY = (rootTop - (topSource ? height - 60 : 40)) * scale;
            int targetPage = tiled ? (int) ((sourceHeight - targetY) / pageHeight) + 1 : 1;
            assertEquals(pdf.getPage(targetPage).getPdfObject().getIndirectReference(),
              destination.get(0).getIndirectReference());
            assertEquals(targetY + (tiled ? targetPage * pageHeight - sourceHeight : 0), destinationY, 0.03f);
          }
        }
      }
      assertEquals(Set.of("top", "bottom"), comments);
      assertEquals(2, links);
    }
  }

  private Shape header(TargetId id, String name, int y) {
    return new Shape(40, y).withNavigationTarget(id)
      .withNavigationTextKey(Source.label(id, true).key()).withText(new TestLabel())
      .withNote(new ShapeNote("author", name, new java.awt.Rectangle(5, 8, 45, 16)));
  }

  private static Shape rectangle(int width, int height) {
    return new Shape(0, 0).add(width, 0).add(width, height).add(0, height);
  }

  /** Supplies deterministic label geometry without depending on Swing component layout. */
  private static class TestLabel extends AbstractShapeText {
    TestLabel() { super(new Insets(0, 0, 0, 0), Color.BLACK, new Font(Font.SANS_SERIF, Font.PLAIN, 12)); }

    @Override protected PdfFont getPDFFont() { return FormattedShapeText.resolvePdfFont(font); }

    @Override
    public void writeToPDF(Point offset, float scale, PdfCanvas canvas, Document document) {
      canvas.beginText().setFontAndSize(getPDFFont(), 12 * scale)
        .moveText(offset.x * scale, (offset.y - 12) * scale).showText("Label").endText();
    }

    @Override
    public void writeToPDF(Point offset, float scale, PdfCanvas canvas, Document document,
                           PdfNavigationRenderContext navigation, String key) {
      writeToPDF(offset, scale, canvas, document);
      if (navigation != null) navigation.label(key, offset.x * scale, (offset.y - 14) * scale, 50 * scale, 14 * scale);
    }
  }
}
