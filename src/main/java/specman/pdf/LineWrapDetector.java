package specman.pdf;

import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.layout.LayoutContext;
import com.itextpdf.layout.layout.LayoutResult;
import com.itextpdf.layout.renderer.ParagraphRenderer;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** This class solves the problem that in very rare cases, the PDF rendering of a line of text
 * takes a tiny little bit more width than calculated from the space the text line takes in the
 * UI rendering. This may lead to an unexpected automatic line wrap in the PDF rendering, causing
 * the wrapped text portion being rendered into the area of the following line (the class {@link
 * FormattedShapeText} renders floating text line by line). The class here detects these cases
 * and informs the PDF renderer that it has to run another attempt. In this next attempt it suggests
 * slightly increased widths, for all those lines which were wrapped in the previous attempt. This
 * is repeated until all lines have enough space for rendering without wrapping anymore.
 * <br>
 * The initial width calculation is usually pretty accurate, so it is only a matter of a few pixel
 * and the rendering is not supposed to be repeated more than once or twice. It is actually hard to
 * create situations which force the re-rendering. You might artificially reduce the initial size
 * in {@link FormattedShapeText#writeToPDF(Point, float, PdfCanvas, Document)} to play around with
 * that feature.
 * <br>
 * The class itself extends {@link ParagraphRenderer} even though it does not actually render
 * anything. It only serves as a wrapper to detect line wrapping in the actual renderers from
 * html2pdf. */
public class LineWrapDetector extends ParagraphRenderer {
  public static final int MAX_RETRIES = 4;

  /** The list of detectors in preparation for the current rendering */
  private static List<LineWrapDetector> currentRendering;

  /** The list of detectors from the last rendering attempt. Used to suggest increased widths
   * for the next attempt wherever an unexpected line wrapping occurred. */
  private static List<LineWrapDetector> lastRendering;

  private static int retries;

  private float paragraphWidth;
  boolean wrapDetected;

  public LineWrapDetector(Paragraph paragraph, float paragraphWidth) {
    super(paragraph);
    currentRendering.add(this);
    paragraph.setNextRenderer(this);
    this.paragraphWidth = paragraphWidth;
  }

  @Override
  /** This method is called by html2pdf to actually render the text. Right afterward, we can
   * tell from the toString method if there occurred any line wrapping. The toString method
   * assembled the raw text by traversing all sub renderers which were created by html2pdf,
   * to layout the text. Occurrences of line break characters indicate line wraps. This
   * saves us from actually understanding the renderer structure. The raw text assembly
   * doesn't slow the process down. */
  public LayoutResult layout(LayoutContext layoutContext) {
    LayoutResult result = super.layout(layoutContext);
    String rawTextWithLineBreaks = toString();
    wrapDetected = rawTextWithLineBreaks.contains("\n");
    if (wrapDetected) {
      System.err.println("Line wrap detected in paragraph: '" + rawTextWithLineBreaks + "'");
    }
    return result;
  }

  private boolean wrapDetected() {
    return wrapDetected;
  }

  public static Float suggestedWidthFromLastRendering() {
    if (lastRendering == null) {
      return null;
    }
    // This is a bit tricky: we know that every rendering repetition requires the same number of paragraphs in the same
    // order. The paragraph being in preparation is the first one which no detector is yet present in the current detector
    // list. So the current list size can be used as an index to address the current paragraph's detector from tha last
    // rendering attempt.
    int lastDetectorIndex = currentRendering.size();
    LineWrapDetector lastDetector = lastRendering.get(lastDetectorIndex);
    float paragraphWidth = lastDetector.paragraphWidth;
    return lastDetector.wrapDetected() ? paragraphWidth + 2f : paragraphWidth;
  }

  public static void start() {
    lastRendering = null;
    currentRendering = new ArrayList<>();
    retries = 0;
  }

  /** Returns true if the last rendering caused any line wrapping and therefore needs to be
   * repeated. The method immeditely prepares the next rendering attempt by initializing the
   * detector lists accordingly. */
  public static boolean retryRequired() {
    if (retries < MAX_RETRIES && anyWrapDetected()) {
      lastRendering = currentRendering;
      currentRendering = new ArrayList<>();
      retries++;
      System.err.println("Retrying PDF rendering with increased widths for wrapped lines...");
      return true;
    }
    return false;
  }

  public static boolean anyWrapDetected() {
    return currentRendering.stream().anyMatch(LineWrapDetector::wrapDetected);
  }
}
