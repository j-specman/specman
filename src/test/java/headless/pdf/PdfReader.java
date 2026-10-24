package headless.pdf;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.canvas.parser.EventType;
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData;
import com.itextpdf.kernel.pdf.canvas.parser.data.TextRenderInfo;
import com.itextpdf.kernel.pdf.canvas.parser.listener.IEventListener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/** Reads back what a test has exported to PDF. */
public class PdfReader {

  private PdfReader() {}

  /** The text of all pages in reading order. Lines are separated by line breaks. */
  public static String text(File pdf) throws IOException {
    try (PdfDocument document = new PdfDocument(new com.itextpdf.kernel.pdf.PdfReader(pdf))) {
      StringBuilder text = new StringBuilder();
      for (int pageNo = 1; pageNo <= document.getNumberOfPages(); pageNo++) {
        text.append(PdfTextExtractor.getTextFromPage(document.getPage(pageNo))).append('\n');
      }
      return text.toString();
    }
  }

  /** The height of the first page. */
  public static float firstPageHeight(File pdf) throws IOException {
    try (PdfDocument document = new PdfDocument(new com.itextpdf.kernel.pdf.PdfReader(pdf))) {
      return document.getFirstPage().getPageSize().getHeight();
    }
  }

  /** The vertical baseline positions on the first page of every text drawn with the given content,
   * measured from the bottom of the page like PDF does. */
  public static List<Float> baselineYsOfText(File pdf, String content) throws IOException {
    List<Float> ys = new ArrayList<>();
    try (PdfDocument document = new PdfDocument(new com.itextpdf.kernel.pdf.PdfReader(pdf))) {
      new PdfCanvasProcessor(new IEventListener() {
        @Override
        public void eventOccurred(IEventData data, EventType type) {
          TextRenderInfo info = (TextRenderInfo) data;
          if (content.equals(info.getText())) {
            ys.add(info.getBaseline().getStartPoint().get(1));
          }
        }

        @Override
        public Set<EventType> getSupportedEvents() {
          return Collections.singleton(EventType.RENDER_TEXT);
        }
      }).processPageContent(document.getFirstPage());
    }
    return ys;
  }
}
