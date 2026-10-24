package specman.pdf;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import specman.pdf.navigation.PdfNavigationPlan;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static specman.pdf.navigation.PdfNavigationPlan.*;

/** Exercises the new renderer boundary against the existing shape-only renderer, without a GUI. */
class PreparedPdfExportTest {
  @TempDir
  Path tempDir;

  @Test
  void ordinaryPortraitPageKeepsItsExistingDrawingAndText() throws Exception {
    compareRenderings(400, true, false);
  }

  @Test
  void overlengthPageKeepsItsExistingSizeAndContents() throws Exception {
    compareRenderings(2400, true, false);
  }

  @Test
  void pagingKeepsAllFinalPagesAndTheirContents() throws Exception {
    compareRenderings(2400, true, true);
  }

  @Test
  void landscapePagingKeepsItsPageGeometryAndContents() throws Exception {
    compareRenderings(2400, false, true);
  }

  private void compareRenderings(int height, boolean portrait, boolean paging) throws Exception {
    Shape diagram = new Shape(0, 0).add(500, 0).add(500, height).add(0, height)
      .withBackgroundColor(Color.WHITE).withOutline(true)
      .withText(new LabelShapeText("Vorbereitung 1", new Insets(10, 10, 0, 0), Color.BLACK,
        new Font(Font.SANS_SERIF, Font.PLAIN, 12)))
      .add(new LineShape(20, 60, 250, 60));
    TargetId target = TargetId.step("step-id");
    PdfNavigationPlan navigation = PdfNavigationPlan.builder()
      .target(new Target(target, "1", null, true))
      .textReference(new Source("intro", null, 0, 1, true), "1")
      .textReference(new Source("intro", null, 3, 5, true), "2?")
      .build();
    PreparedPdfExport prepared = new PreparedPdfExport(diagram, navigation);
    Path oldFile = tempDir.resolve("shape-only.pdf");
    Path newFile = tempDir.resolve("prepared.pdf");

    new PDFRenderer(oldFile.toString(), PageSize.A4, portrait, paging, 100).render(diagram);
    new PDFRenderer(newFile.toString(), PageSize.A4, portrait, paging, 100).render(prepared);

    try (PdfDocument previous = new PdfDocument(new PdfReader(oldFile.toString()));
         PdfDocument current = new PdfDocument(new PdfReader(newFile.toString()))) {
      assertEquals(previous.getNumberOfPages(), current.getNumberOfPages());
      if (paging) assertTrue(current.getNumberOfPages() > 1);
      else assertEquals(1, current.getNumberOfPages());
      assertTrue(PdfTextExtractor.getTextFromPage(current.getPage(1)).contains("Vorbereitung 1"));
      for (int pageNo = 1; pageNo <= previous.getNumberOfPages(); pageNo++) {
        var before = previous.getPage(pageNo);
        var after = current.getPage(pageNo);
        assertEquals(before.getPageSize().getWidth(), after.getPageSize().getWidth(), 0.01F);
        assertEquals(before.getPageSize().getHeight(), after.getPageSize().getHeight(), 0.01F);
        assertArrayEquals(before.getContentBytes(), after.getContentBytes());
        assertEquals(PdfTextExtractor.getTextFromPage(before), PdfTextExtractor.getTextFromPage(after));
        assertTrue(after.getAnnotations().isEmpty(), "Unbound shapes must not produce invented links");
      }
    }
    assertSame(navigation, prepared.navigation());
    assertEquals(Status.DIRECT, navigation.links().get(0).resolution().status());
    assertEquals(Status.BROKEN_REFERENCE, navigation.links().get(1).resolution().status());
  }
}
