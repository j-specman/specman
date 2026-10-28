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

import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static specman.pdf.navigation.PdfNavigationPlan.*;
import specman.pdf.navigation.PdfNavigationPlan;

/** Real label drawing, nested offsets, page splitting and parsed internal PDF actions. */
class PdfBreakCatchExportTest {
  @TempDir Path directory;
  enum Mode { DIRECT, HIDDEN_CATCH, HIDDEN_BREAK, ROOT_HIDDEN_CATCH }
  private record Header(TargetId id, String text, int x, int y) {}
  private record Fixture(PreparedPdfExport export, Map<TargetId, Header> drawn, Map<String, TargetId> expected) {}
  private record Glyph(String text, float x, float right, float baseline) {}

  @Test void bothDirectionsAndMultipleHeadingsWorkOnOnePage() throws Exception { verify(600, 850, true, false, Mode.DIRECT); }
  @Test void pagingLinksEachBreakToItsOwnHeadingAndBack() throws Exception { verify(600, 2800, true, true, Mode.DIRECT); }
  @Test void landscapeWidthReductionKeepsTheLabelRectanglesAligned() throws Exception { verify(1400, 5000, false, true, Mode.DIRECT); }
  @Test void hiddenCatchUsesItsNumberedParent() throws Exception { verify(600, 2800, true, true, Mode.HIDDEN_CATCH); }
  @Test void hiddenBreakUsesItsNumberedParent() throws Exception { verify(600, 2800, true, true, Mode.HIDDEN_BREAK); }
  @Test void hiddenRootCatchWithoutAncestorStaysInactive() throws Exception { verify(600, 2800, true, true, Mode.ROOT_HIDDEN_CATCH); }

  private void verify(int width, int height, boolean portrait, boolean paging, Mode mode) throws Exception {
    Fixture fixture = fixture(width, height, mode);
    Path linked = directory.resolve("linked.pdf"), oldFile = directory.resolve("drawing.pdf");
    SwingUtilities.invokeAndWait(() -> {
      try {
        new PDFRenderer(linked.toString(), PageSize.A4, portrait, paging, 100).render(fixture.export());
        new PDFRenderer(oldFile.toString(), PageSize.A4, portrait, paging, 100).render(fixture.export().diagramShape());
      } catch (Exception e) { throw new AssertionError(e); }
    });
    try (PdfDocument pdf = new PdfDocument(new PdfReader(linked.toString()));
         PdfDocument old = new PdfDocument(new PdfReader(oldFile.toString()))) {
      assertEquals(old.getNumberOfPages(), pdf.getNumberOfPages());
      float pageHeight = pdf.getPage(1).getPageSize().getHeight();
      float scale = Math.min(0.77f, pdf.getPage(1).getPageSize().getWidth() / (width + 20));
      float originalHeight = Math.max(pageHeight, height * scale);
      float rootTop = height * scale > pageHeight ? height : (int)(pageHeight / scale);
      int count = 0;
      for (int pageNo = 1; pageNo <= pdf.getNumberOfPages(); pageNo++) {
        PdfPage page = pdf.getPage(pageNo);
        assertArrayEquals(old.getPage(pageNo).getContentBytes(), page.getContentBytes());
        List<Glyph> glyphs = glyphs(page);
        for (var annotation : page.getAnnotations()) {
          count++;
          Rectangle area = annotation.getRectangle().toRectangle();
          Glyph number = glyphs.stream().filter(g -> Math.abs(g.x - area.getLeft()) < 0.03f
            && Math.abs(g.right - area.getRight()) < 0.03f && g.baseline >= area.getBottom() - 0.03f
            && g.baseline <= area.getTop() + 0.03f).findFirst().orElseThrow();
          // Each source label has distinct text here; the two catch headings are separate targets.
          TargetId target = fixture.expected().get(number.text());
          assertNotNull(target, "An unlinked number must not become active");
          Header header = fixture.drawn().get(target);
          assertNotNull(header);
          float targetY = (rootTop - 13 - header.y()) * scale;
          int targetPage = paging ? Math.min(pdf.getNumberOfPages(), (int)((originalHeight - targetY) / pageHeight) + 1) : 1;
          float mappedY = paging && pdf.getNumberOfPages() > 1 ? targetY - originalHeight + targetPage * pageHeight : targetY;
          PdfDictionary action = annotation.getPdfObject().getAsDictionary(PdfName.A);
          assertEquals(PdfName.GoTo, action.getAsName(PdfName.S));
          PdfArray destination = action.getAsArray(PdfName.D);
          assertEquals(pdf.getPage(targetPage).getPdfObject().getIndirectReference(), destination.get(0).getIndirectReference());
          assertEquals(PdfName.XYZ, destination.getAsName(1));
          assertEquals((17 + header.x()) * scale, destination.getAsNumber(2).floatValue(), 0.03f);
          assertEquals(mappedY, destination.getAsNumber(3).floatValue(), 0.03f);
        }
      }
      assertEquals(fixture.expected().size(), count);
    }
  }

  private Fixture fixture(int width, int height, Mode mode) throws Exception {
    AtomicReference<Fixture> result = new AtomicReference<>();
    SwingUtilities.invokeAndWait(() -> {
      TargetId parent = TargetId.step("parent"), b1 = TargetId.step("b1"), b2 = TargetId.step("b2"),
        h1 = TargetId.catchHeading("primary"), h2 = TargetId.catchHeading("co"), unlinked = TargetId.step("unlinked");
      boolean breaks = mode != Mode.HIDDEN_BREAK;
      boolean catches = mode == Mode.DIRECT || mode == Mode.HIDDEN_BREAK;
      Builder builder = PdfNavigationPlan.builder().target(new Target(parent, "1", null, true))
        .target(new Target(b1, "1.1", parent, breaks)).target(new Target(b2, "1.2", parent, breaks))
        .target(new Target(h1, "1.1", mode == Mode.ROOT_HIDDEN_CATCH ? null : parent, catches))
        .target(new Target(h2, "1.2", mode == Mode.ROOT_HIDDEN_CATCH ? null : parent, catches))
        .target(new Target(unlinked, "3", null, true));
      builder.connection(LinkKind.BREAK_TO_CATCH, Source.label(b1, true), h1)
        .connection(LinkKind.CATCH_TO_BREAK, Source.label(h1, true), b1)
        .connection(LinkKind.BREAK_TO_CATCH, Source.label(b2, true), h2)
        .connection(LinkKind.CATCH_TO_BREAK, Source.label(h2, true), b2);
      Shape root = rectangle(0, 0, width, height), nested = new Shape(17, 13);
      root.add(nested);
      Map<TargetId, Header> drawn = new LinkedHashMap<>();
      Map<String, TargetId> expected = new LinkedHashMap<>();
      List<Header> headers = new ArrayList<>();
      headers.add(new Header(parent, "RAHMEN", 20, height / 2));
      headers.add(new Header(unlinked, "3", 20, 210));
      if (breaks) {
        headers.add(new Header(b1, "1.1", 25, 30));
        headers.add(new Header(b2, "1.2", 25, 110));
        if (catches) { expected.put("1.1", h1); expected.put("1.2", h2); }
        else if (mode != Mode.ROOT_HIDDEN_CATCH) { expected.put("1.1", parent); expected.put("1.2", parent); }
      }
      if (catches) {
        headers.add(new Header(h1, "Catch 1.1", 45, height - 400));
        headers.add(new Header(h2, "Catch 1.2", 60, height - 150));
        expected.put("Catch 1.1", breaks ? b1 : parent);
        expected.put("Catch 1.2", breaks ? b2 : parent);
      }
      for (Header header : headers) {
        drawn.put(header.id(), header);
        Shape shape = rectangle(header.x(), header.y(), width - 100, 60).withNavigationTarget(header.id());
        JLabel label = new JLabel(header.text());
        label.setBounds(3, 2, 120, 24);
        label.setBackground(Color.GRAY);
        shape.add(new Shape(7, 0).add(new HangingTabShape(label, 8)
          .withText(new LabelShapeText(header.text(), new Insets(0, 5, 2, 5), Color.WHITE,
            new Font(Font.SANS_SERIF, Font.PLAIN, 12)))
          .withNavigationTextKey(Source.label(header.id(), true).key())));
        nested.add(shape);
      }
      result.set(new Fixture(new PreparedPdfExport(root, builder.build()), drawn, expected));
    });
    return result.get();
  }

  private static Shape rectangle(int x, int y, int width, int height) {
    return new Shape(x, y).add(x + width, y).add(x + width, y + height).add(x, y + height).withBackgroundColor(Color.WHITE);
  }

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
