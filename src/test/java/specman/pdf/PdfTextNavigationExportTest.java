package specman.pdf;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.parser.EventType;
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor;
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData;
import com.itextpdf.kernel.pdf.canvas.parser.data.TextRenderInfo;
import com.itextpdf.kernel.pdf.canvas.parser.listener.IEventListener;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static specman.pdf.navigation.PdfNavigationPlan.*;

/** Real HTML layout -> shape drawing -> navigation writer -> parsed PDF, without a window. */
class PdfTextNavigationExportTest {
  @TempDir Path tempDir;

  @Test
  void exportsForwardBackwardSelfAndRepeatedReferencesAtTheirDrawnGlyphs() throws Exception {
    verifyOrdinaryLinks(600, 850, true);
  }

  @Test
  void widthReductionAndOverlengthUseTheSameCoordinatesForLinksAndDrawing() throws Exception {
    verifyOrdinaryLinks(1100, 2400, true);
  }

  @Test
  void landscapeUsesItsActualPageGeometry() throws Exception {
    verifyOrdinaryLinks(800, 850, false);
  }

  @Test
  void pagingLinksForwardToTheLastPageAndBackToTheFirstPage() throws Exception {
    verifyPagedLinks(600, 2400, true);
  }

  @Test
  void landscapePagingWithWidthReductionKeepsLinksAlignedWithTheDrawing() throws Exception {
    verifyPagedLinks(1400, 4000, false);
  }

  @Test
  void pagingEnabledOnAShortDiagramStillProducesLinksOnItsOnlyPage() throws Exception {
    Path file = render(fixture(600, 850, true), "short-paging.pdf", true, true);
    try (PdfDocument pdf = new PdfDocument(new PdfReader(file.toString()))) {
      assertEquals(1, pdf.getNumberOfPages());
      assertEquals(4, pdf.getPage(1).getAnnotations().size());
    }
  }

  @Test
  void multiDigitReferenceCreatesOneClickableAreaOnTheSourcePage() throws Exception {
    AtomicReference<PreparedPdfExport> prepared = new AtomicReference<>();
    SwingUtilities.invokeAndWait(() -> {
      TargetId target = TargetId.step("forty-two");
      Builder builder = PdfNavigationPlan.builder().target(new Target(target, "42", null, true));
      Shape root = rectangle(0, 0, 600, 2400)
        .add(textShape(pane("Vorwaerts zu " + mark("42") + ".", 450), "text", null, builder))
        .add(header(20, 2200, 540, "Ziel", target));
      prepared.set(new PreparedPdfExport(root, builder.build()));
    });
    Path file = render(prepared.get(), "two-digits.pdf", true, true);
    try (PdfDocument pdf = new PdfDocument(new PdfReader(file.toString()))) {
      var annotations = pdf.getPage(1).getAnnotations();
      assertEquals(1, annotations.size());
      var numbers = glyphs(pdf.getPage(1)).stream().filter(g -> g.text.matches("[42]")).toList();
      assertEquals(2, numbers.size());
      Rectangle area = annotations.getFirst().getRectangle().toRectangle();
      assertEquals(numbers.getFirst().x, area.getLeft(), 0.03f);
      assertEquals(numbers.getLast().endX, area.getRight(), 0.03f);
      PdfArray destination = annotations.getFirst().getPdfObject().getAsDictionary(PdfName.A).getAsArray(PdfName.D);
      assertEquals(pdf.getPage(pdf.getNumberOfPages()).getPdfObject().getIndirectReference(),
        destination.get(0).getIndirectReference());
    }
  }

  private void verifyPagedLinks(int width, int height, boolean portrait) throws Exception {
    PreparedPdfExport export = fixture(width, height, true);
    Path file = render(export, "paging.pdf", portrait, true);
    Path plain = tempDir.resolve("plain-paging.pdf");
    SwingUtilities.invokeAndWait(() -> {
      try { new PDFRenderer(plain.toString(), PageSize.A4, portrait, true, 100).render(export.diagramShape()); }
      catch (Exception e) { throw new AssertionError(e); }
    });
    try (PdfDocument pdf = new PdfDocument(new PdfReader(file.toString()));
         PdfDocument old = new PdfDocument(new PdfReader(plain.toString()))) {
      int last = pdf.getNumberOfPages();
      assertTrue(last >= 3);
      assertEquals(old.getNumberOfPages(), last);
      assertEquals(3, pdf.getPage(1).getAnnotations().size());
      assertEquals(1, pdf.getPage(last).getAnnotations().size());
      for (int pageNo = 1; pageNo <= last; pageNo++) {
        PdfPage page = pdf.getPage(pageNo);
        assertArrayEquals(old.getPage(pageNo).getContentBytes(), page.getContentBytes());
        if (pageNo > 1 && pageNo < last) assertTrue(page.getAnnotations().isEmpty());
        var annotations = page.getAnnotations();
        for (int i = 0; i < annotations.size(); i++) {
          Rectangle area = annotations.get(i).getRectangle().toRectangle();
          assertTrue(area.getBottom() >= 0 && area.getTop() <= page.getPageSize().getHeight());
          assertTrue(glyphs(page).stream().anyMatch(g -> g.text.matches("[12]")
            && Math.abs(g.x - area.getLeft()) < 0.03f && Math.abs(g.endX - area.getRight()) < 0.03f
            && g.baseline >= area.getBottom() && g.baseline <= area.getTop()),
            "Paged link must cover its actual rendered number");
          PdfArray destination = annotations.get(i).getPdfObject().getAsDictionary(PdfName.A).getAsArray(PdfName.D);
          int targetPage = pageNo == last || i == 1 ? 1 : last;
          assertEquals(pdf.getPage(targetPage).getPdfObject().getIndirectReference(),
            destination.get(0).getIndirectReference());
          float scale = Math.min(0.77f, page.getPageSize().getWidth() / (width + 20));
          int headerY = targetPage == 1 ? 40 : height - 180;
          float expectedY = (height - 30 - headerY) * scale - height * scale
            + page.getPageSize().getHeight() * targetPage;
          assertEquals(37 * scale, destination.getAsNumber(2).floatValue(), 0.03f);
          assertEquals(expectedY, destination.getAsNumber(3).floatValue(), 0.03f);
        }
      }
    }
  }

  @Test
  void unmarkedTextDoesNotBecomeClickable() throws Exception {
    PreparedPdfExport export = fixture(600, 850, false);
    assertTrue(export.navigation().links().isEmpty());
    Path file = render(export, "plain.pdf", true, false);
    try (PdfDocument pdf = new PdfDocument(new PdfReader(file.toString()))) {
      assertTrue(pdf.getPage(1).getAnnotations().isEmpty());
    }
  }

  @Test
  void incompleteAndMixedFontReferencesStayInactive() throws Exception {
    AtomicReference<PreparedPdfExport> prepared = new AtomicReference<>();
    SwingUtilities.invokeAndWait(() -> {
      Builder builder = PdfNavigationPlan.builder();
      TargetId target = TargetId.step("target");
      builder.target(new Target(target, "12.3", null, true));
      Shape root = rectangle(0, 0, 600, 850).withNavigationTarget(target);
      JEditorPane mixed = pane("<span style='background-color:#dcdcdc'><b>12</b><i>.3</i></span>", 450);
      root.add(new Shape(10, 30).add(textShape(mixed, "mixed", null, builder)));
      // Capture a valid marked source, then omit its actual text shape from the drawing.
      textShape(pane(mark("12.3"), 450), "not-drawn", null, builder);
      prepared.set(new PreparedPdfExport(root, builder.build()));
    });
    Path file = render(prepared.get(), "unsupported.pdf", true, false);
    try (PdfDocument pdf = new PdfDocument(new PdfReader(file.toString()))) {
      assertTrue(pdf.getPage(1).getAnnotations().isEmpty());
    }
  }

  private void verifyOrdinaryLinks(int width, int height, boolean portrait) throws Exception {
    PreparedPdfExport export = fixture(width, height, true);
    Path linked = render(export, "linked.pdf", portrait, false);
    Path plain = tempDir.resolve("drawing.pdf");
    SwingUtilities.invokeAndWait(() -> {
      try { new PDFRenderer(plain.toString(), PageSize.A4, portrait, false, 100).render(export.diagramShape()); }
      catch (Exception e) { throw new AssertionError(e); }
    });
    try (PdfDocument pdf = new PdfDocument(new PdfReader(linked.toString()));
         PdfDocument old = new PdfDocument(new PdfReader(plain.toString()))) {
      assertEquals(1, pdf.getNumberOfPages());
      var page = pdf.getPage(1);
      assertArrayEquals(old.getPage(1).getContentBytes(), page.getContentBytes(), "Navigation must preserve drawing bytes");
      var annotations = page.getAnnotations();
      assertEquals(4, annotations.size(), "Forward, self, repeated forward, backward; no broken/plain number");
      List<Glyph> glyphs = glyphs(page);
      // The only digits in this fixture are the text references and the deliberately unmarked/broken numbers.
      List<Glyph> digits = glyphs.stream().filter(g -> g.text.matches("[129]")).toList();
      assertEquals(7, digits.size());
      int[] sourceGlyph = {0, 1, 2, 6};
      for (int i = 0; i < annotations.size(); i++) {
        Rectangle area = annotations.get(i).getRectangle().toRectangle();
        Glyph glyph = digits.get(sourceGlyph[i]);
        assertEquals(glyph.x, area.getLeft(), 0.03f);
        assertEquals(glyph.endX, area.getRight(), 0.03f);
        assertTrue(glyph.baseline >= area.getBottom() && glyph.baseline <= area.getTop());
        assertTrue(area.getHeight() < 30, "Click area must describe the number, not the whole text pane");
        PdfDictionary action = annotations.get(i).getPdfObject().getAsDictionary(PdfName.A);
        assertEquals(PdfName.GoTo, action.getAsName(PdfName.S));
        PdfArray destination = action.getAsArray(PdfName.D);
        assertEquals(page.getPdfObject().getIndirectReference(), destination.get(0).getIndirectReference());
        assertEquals(PdfName.XYZ, destination.getAsName(1));
        float scale = Math.min(0.77f, page.getPageSize().getWidth() / (width + 20));
        // Nested shapes: root -> sequence(17,30) -> header(20,40 or height-180).
        float expectedX = 37 * scale;
        float rootTop = height * scale > (portrait ? PageSize.A4.getHeight() : PageSize.A4.getWidth())
          ? height : (int)((portrait ? PageSize.A4.getHeight() : PageSize.A4.getWidth()) / scale);
        int headerY = i == 0 || i == 2 ? height - 180 : 40;
        assertEquals(expectedX, destination.getAsNumber(2).floatValue(), 0.03f);
        assertEquals((rootTop - 30 - headerY) * scale, destination.getAsNumber(3).floatValue(), 0.03f);
      }
      for (int i : new int[] {3, 4, 5}) {
        Glyph excluded = digits.get(i);
        assertTrue(annotations.stream().noneMatch(a -> {
          Rectangle area = a.getRectangle().toRectangle();
          return excluded.x >= area.getLeft() && excluded.x <= area.getRight()
            && excluded.baseline >= area.getBottom() && excluded.baseline <= area.getTop();
        }), "Plain/broken numbers must not be clickable");
      }
    }
  }

  static PreparedPdfExport fixture(int width, int height, boolean marked) throws Exception {
    AtomicReference<PreparedPdfExport> prepared = new AtomicReference<>();
    SwingUtilities.invokeAndWait(() -> {
      Builder builder = PdfNavigationPlan.builder();
      TargetId one = TargetId.step("one"), two = TargetId.step("two");
      builder.target(new Target(one, "1", null, true)).target(new Target(two, "2", null, true));
      String first = "Vorwaerts " + mark("2") + ", selbst " + mark("1") + ", erneut " + mark("2")
        + "; unmarkiert 2 und 1; defekt " + mark("9?") + ".";
      String second = "Zurueck " + mark("1") + ".";
      if (!marked) {
        first = first.replace("background-color:#dcdcdc", "color:#000000");
        second = second.replace("background-color:#dcdcdc", "color:#000000");
      }
      Shape root = rectangle(0, 0, width, height);
      Shape sequence = new Shape(17, 30);
      root.add(sequence);
      sequence.add(header(20, 40, width - 60, "Erster Schritt", one)
        .add(textShape(pane(first, width - 120), "first", one, builder)));
      sequence.add(header(20, height - 180, width - 60, "Zweiter Schritt", two)
        .add(textShape(pane(second, width - 120), "second", two, builder)));
      prepared.set(new PreparedPdfExport(root, builder.build()));
    });
    return prepared.get();
  }

  private static Shape header(int x, int y, int width, String label, TargetId id) {
    return rectangle(x, y, width, 130).withNavigationTarget(id)
      .withText(new LabelShapeText(label, new Insets(2, 2, 0, 0), Color.BLACK,
        new Font(Font.SANS_SERIF, Font.PLAIN, 12)));
  }

  private static Shape rectangle(int x, int y, int width, int height) {
    return new Shape(x, y).add(x + width, y).add(x + width, y + height).add(x, y + height)
      .withBackgroundColor(Color.WHITE).withOutline(true);
  }

  private static String mark(String text) { return "<span style='background-color:#dcdcdc'>" + text + "</span>"; }

  private static JEditorPane pane(String html, int width) {
    JEditorPane pane = new JEditorPane("text/html", "<html><body style='font-family:Serif;font-size:14pt'>"
      + html + "</body></html>");
    pane.setBounds(28, 24, width, 110);
    pane.doLayout();
    return pane;
  }

  private static Shape textShape(JEditorPane pane, String key, TargetId owner, Builder builder) {
    WrappedDocument document = new WrappedDocument((StyledDocument) pane.getDocument());
    for (var ref : PdfTextReferences.read(document)) {
      builder.textReference(new Source(key, owner, ref.start(), ref.end(), true), ref.text());
    }
    return new Shape(pane).withText(new FormattedShapeText(pane)).withNavigationTextKey(key);
  }

  private Path render(PreparedPdfExport export, String filename, boolean portrait, boolean paging) throws Exception {
    Path file = tempDir.resolve(filename);
    SwingUtilities.invokeAndWait(() -> {
      try { new PDFRenderer(file.toString(), PageSize.A4, portrait, paging, 100).render(export); }
      catch (Exception e) { throw new AssertionError(e); }
    });
    return file;
  }

  private record Glyph(String text, float x, float endX, float baseline) {}

  private static List<Glyph> glyphs(PdfPage page) {
    List<Glyph> result = new ArrayList<>();
    new PdfCanvasProcessor(new IEventListener() {
      public Set<EventType> getSupportedEvents() { return Set.of(EventType.RENDER_TEXT); }
      public void eventOccurred(IEventData data, EventType type) {
        TextRenderInfo text = (TextRenderInfo) data;
        result.add(new Glyph(text.getText(), text.getBaseline().getStartPoint().get(0),
          text.getBaseline().getEndPoint().get(0), text.getBaseline().getStartPoint().get(1)));
      }
    }).processPageContent(page);
    return result;
  }
}
