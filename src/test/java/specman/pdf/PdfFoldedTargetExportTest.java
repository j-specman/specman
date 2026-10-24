package specman.pdf;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import specman.editarea.document.WrappedDocument;
import specman.pdf.navigation.PdfNavigationPlan;
import specman.pdf.navigation.PdfTextReferences;

import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.text.StyledDocument;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static specman.pdf.navigation.PdfNavigationPlan.*;

/** Folded target resolution through drawing, optional paging and final PDF annotations. */
class PdfFoldedTargetExportTest {
  @TempDir Path directory;

  @Test void singlePageUsesTheNearestVisibleParent() throws Exception { verify(false, true); }
  @Test void pagingUsesTheParentOnItsActualFinalPage() throws Exception { verify(true, true); }
  @Test void missingParentGeometryFallsBackToTheNextExportedAncestor() throws Exception { verify(false, false); }
  @Test void pagingAlsoHandlesMultipleMissingOrFoldedLevels() throws Exception { verify(true, false); }

  private void verify(boolean paging, boolean drawMiddle) throws Exception {
    AtomicReference<PreparedPdfExport> prepared = new AtomicReference<>();
    TargetId outer = TargetId.step("outer"), folded = TargetId.step("folded"),
      deep = TargetId.step("deep"), middle = TargetId.step("middle"), child = TargetId.step("child"),
      catchChild = TargetId.step("catch-child"), source = TargetId.step("source");
    SwingUtilities.invokeAndWait(() -> {
      Builder builder = PdfNavigationPlan.builder()
        .target(new Target(source, "1", null, true))
        .target(new Target(outer, "2", null, true))
        .target(new Target(folded, "2.1", outer, false))
        .target(new Target(deep, "2.1.1", folded, true))
        .target(new Target(middle, "2.2", outer, true))
        .target(new Target(child, "2.2.1", middle, false))
        .target(new Target(catchChild, "3.1", null, false));
      Shape root = rectangle(0, 0, 600, 2600);
      root.add(header(20, 40, "QUELLE", source)
        .add(text("Siehe " + mark("2.1.1") + " und " + mark("2.2.1")
          + "; ohne Vorfahr " + mark("3.1") + "; defekt " + mark("99?") + ".", "text", source, builder)));
      root.add(header(20, 1800, "AEUSSERES ZIEL", outer));
      if (drawMiddle) root.add(header(50, 2000, "NAECHSTER VORFAHR", middle));
      // Defensively simulate stale hidden catch geometry: neither source nor target may activate.
      root.add(header(50, 2200, "VERBORGEN", deep)
        .add(text(mark("1"), "hidden-source", deep, builder)));
      prepared.set(new PreparedPdfExport(root, builder.build()));
    });
    PreparedPdfExport export = prepared.get();
    var originalTargets = export.navigation().targets();
    var originalLinks = export.navigation().links();
    Path withLinks = directory.resolve("linked.pdf"), withoutLinks = directory.resolve("drawing.pdf");
    SwingUtilities.invokeAndWait(() -> {
      try {
        new PDFRenderer(withLinks.toString(), PageSize.A4, true, paging, 100).render(export);
        new PDFRenderer(withoutLinks.toString(), PageSize.A4, true, paging, 100).render(export.diagramShape());
      } catch (Exception e) { throw new AssertionError(e); }
    });
    assertEquals(originalTargets, export.navigation().targets());
    assertEquals(originalLinks, export.navigation().links());
    assertEquals(Status.NO_VISIBLE_ANCESTOR, originalLinks.get(2).resolution().status());
    try (PdfDocument pdf = new PdfDocument(new PdfReader(withLinks.toString()));
         PdfDocument old = new PdfDocument(new PdfReader(withoutLinks.toString()))) {
      assertEquals(paging ? 3 : 1, pdf.getNumberOfPages());
      var annotations = pdf.getPage(1).getAnnotations();
      assertEquals(2, annotations.size(), "Only visible, valid sources with an exported ancestor get a link");
      for (int i = 0; i < annotations.size(); i++) {
        int headerY = i == 1 && drawMiddle ? 2000 : 1800;
        float originalY = (2600 - headerY) * 0.77f;
        int pageNo = paging ? (int)((2600 * 0.77f - originalY) / PageSize.A4.getHeight()) + 1 : 1;
        float expectedY = paging ? originalY - 2600 * 0.77f + pageNo * PageSize.A4.getHeight() : originalY;
        PdfArray destination = annotations.get(i).getPdfObject().getAsDictionary(PdfName.A).getAsArray(PdfName.D);
        assertEquals(pdf.getPage(pageNo).getPdfObject().getIndirectReference(), destination.get(0).getIndirectReference());
        assertEquals((i == 1 && drawMiddle ? 50 : 20) * 0.77f, destination.getAsNumber(2).floatValue(), 0.03f);
        assertEquals(expectedY, destination.getAsNumber(3).floatValue(), 0.03f);
        // The extractor may infer spaces between individually painted characters.
        String extracted = PdfTextExtractor.getTextFromPage(pdf.getPage(1)).replaceAll("\\s+", "");
        assertTrue(extracted.contains("2.1.1"), "Keep the original reference text: " + extracted);
      }
      for (int pageNo = 1; pageNo <= pdf.getNumberOfPages(); pageNo++) {
        assertArrayEquals(old.getPage(pageNo).getContentBytes(), pdf.getPage(pageNo).getContentBytes());
        if (pageNo != 1) assertTrue(pdf.getPage(pageNo).getAnnotations().isEmpty(), "No links from hidden sources");
      }
    }
  }

  private static Shape header(int x, int y, String label, TargetId id) {
    return rectangle(x, y, 530, 120).withNavigationTarget(id).withText(
      new LabelShapeText(label, new Insets(2, 2, 0, 0), Color.BLACK, new Font(Font.SANS_SERIF, Font.PLAIN, 12)));
  }

  private static Shape rectangle(int x, int y, int width, int height) {
    return new Shape(x, y).add(x + width, y).add(x + width, y + height).add(x, y + height)
      .withBackgroundColor(Color.WHITE);
  }

  private static String mark(String text) { return "<span style='background-color:#dcdcdc'>" + text + "</span>"; }

  private static Shape text(String html, String key, TargetId owner, Builder builder) {
    JEditorPane pane = new JEditorPane("text/html", "<html><body>" + html + "</body></html>");
    pane.setBounds(20, 25, 500, 90);
    pane.doLayout();
    for (var ref : PdfTextReferences.read(new WrappedDocument((StyledDocument) pane.getDocument()))) {
      builder.textReference(new Source(key, owner, ref.start(), ref.end(), true), ref.text());
    }
    return new Shape(pane).withText(new FormattedShapeText(pane)).withNavigationTextKey(key);
  }
}
