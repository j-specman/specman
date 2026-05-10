package specman.pdf;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.html2pdf.resolver.font.DefaultFontProvider;
import com.itextpdf.io.font.FontProgramFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.IBlockElement;
import com.itextpdf.layout.element.IElement;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.font.FontProvider;
import org.apache.commons.io.IOUtils;
import specman.Specman;
import specman.editarea.HTMLTags;
import specman.editarea.TextEditArea;
import specman.editarea.TextStyles;
import specman.editarea.document.WrappedDocument;
import specman.editarea.document.WrappedPosition;
import specman.editarea.markups.MarkedCharSequence;

import javax.swing.text.BadLocationException;
import javax.swing.text.Utilities;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.itextpdf.layout.properties.Property.FONT_PROVIDER;
import static specman.pdf.PDFRenderer.SWING2PDF_SCALEFACTOR_100PERCENT;

public class FormattedShapeText extends AbstractShapeText {
  private static final String HTML2PDF_STYLESHEET = "stylesheets/specman-pdf.css";
  private static final Pattern FONTSIZE_PATTERN = Pattern.compile("(.*font-size:[\\s]*)([\\d\\.]+)(.+)");

  private static String htmlStyles;

  public static FontProvider fontProvider;

  /** Purpose of EMPTY_CONVERSION_PROPERTIES see detailed comment in {@link #writeToPDF(Point, float, PdfCanvas, Document)}. */
  private static final ConverterProperties EMPTY_CONVERSION_PROPERTIES = new ConverterProperties()
    .setFontProvider(new DefaultFontProvider(false, false, false));

  private TextEditArea content;

  public FormattedShapeText(TextEditArea content) {
    super(content.getInsets(), content.getForeground(), content.getFont());
    this.content = content;
  }

  public void writeToPDF(Point renderOffset, float swing2pdfScaleFactor, PdfCanvas pdfCanvas, Document document) {
    float scaledFontSize = getFontsize() * swing2pdfScaleFactor;
    pdfCanvas.setFillColor(getPDFColor());
    document.setFontSize(scaledFontSize);

    try {
      java.util.List<TextlineDimension> lines = scanLineDimensions();
      MarkedCharSequence changemarks = content.findMarkups();

      for (TextlineDimension line: lines) {
        System.out.println("Zeile bis " + line.getDocIndexTo() + ", Höhe " + line.getHeight() + ", y = " + line.getY());

        String lineHtml = line.extractLineHtml(content, changemarks);
        lineHtml = removeLinebreakingElementsFromHtmlLine(lineHtml);
        lineHtml = stylifyTextAlignment(lineHtml);
        lineHtml = removeMargin0Paragraphs(lineHtml);
        lineHtml = injectStylesheet(lineHtml);

        // Usage of EMPTY_CONVERSION_PROPERTIES see detailed comment below
        java.util.List<IElement> elements = HtmlConverter.convertToElements(lineHtml, EMPTY_CONVERSION_PROPERTIES);

        // This turned out to sometimes happen with line items. The reason is not yet really clear.
        // It is probably concerned with copy/paste from MS Word documents. Up to now, simply ignoring
        // these cases didn't lead to missing text in PDF exports ;-)
        if (!elements.isEmpty()) {
          IElement elementToPrint = elements.get(0);

          // This looks wrong but is actually correct: We could have called HtmlConverter.convertToElements
          // with passing ConverterProperties including the FontProvider. But then, each call would create
          // its own FontProvider copy, causing the used fonts to be embedded once per element. The resulting
          // PDF becomes horribly huge. To have all text elements share the same FontProvider (and thus the
          // same embedded font subset), we have to set the FontProvider manually here.
          // Passing an *empty* ConverterProperties to HtmlConverter.convertToElements is an important performance
          // issue. Otherwise, the method internally produces a full-fledged yet unused DefaultFontProvider
          // instance with all iText built-in and all system fonts which takes quite long. Passing an empty
          // Font Provider saves the time.
          // Special thanks go to ChatGPT for figuring this out ;-)
          if (elementToPrint instanceof com.itextpdf.layout.IPropertyContainer) {
            elementToPrint.setProperty(FONT_PROVIDER, fontProvider);
          }

          Float suggestedParagraphWidth = LineWrapDetector.suggestedWidthFromLastRendering();
          float paragraphWidth = suggestedParagraphWidth != null
            ? suggestedParagraphWidth
            : (content.getWidth() - getInsets().left - getInsets().right) * swing2pdfScaleFactor;

          Paragraph p = new Paragraph()
            .setMargin(0)
            .setMultipliedLeading(0.0f)
            .setCharacterSpacing(-0.1f)
            .setFontSize(scaledFontSize)
            .setFixedPosition(
              (renderOffset.x + line.getX()) * swing2pdfScaleFactor,
              (renderOffset.y - line.getY() - line.getHeight()) * swing2pdfScaleFactor,
              paragraphWidth);
          p.add((IBlockElement) elementToPrint);
          new LineWrapDetector(p, paragraphWidth);

          document.add(p);
        }
      }
    }
    catch(Exception x) {
      x.printStackTrace();
    }

  }

  /** For some unknown reason, JEditorPane (resp. Chef) sometimes adds paragraphs with a styling like
   * <p style="margin-top: 0">text goes here</p>. They don't have an effect within the editor pane but
   * they mess up the PDF rendering. So these paragraphs must be removed. As soon as Chef is removed
   * from the code, there shouldn't occur any paragraphs at all within the texts. */
  private String removeMargin0Paragraphs(String lineHtml) {
    String SEARCHSTRING = "<p style=\"margin-top: 0\">";
    int numParagraphs = 0;
    int searchIndex = 0;
    int finding;
    while(true) {
      finding = lineHtml.indexOf(SEARCHSTRING, searchIndex);
      if (finding == -1) {
        break;
      }
      numParagraphs++;
      searchIndex += finding + SEARCHSTRING.length();
    }
    lineHtml = lineHtml.replace(SEARCHSTRING, "");
    for (int i = 0; i < numParagraphs; i++) {
      lineHtml = lineHtml.replaceFirst("<p>", "");
    }
    return lineHtml;
  }

  private String removeLinebreakingElementsFromHtmlLine(String subHtml) {
    return subHtml
      // List items might be equipped with strange styling information when they were copied over
      // from MS Word documents, so we need some tolerance in replacement here. The styling has no
      // effect in a JEditorPane, so it is ok to completely erase it here for PDF export.
      // See also ListItemPromptFactory#createPrompt
      .replaceAll("<li[^>]*>", "")
      .replace("</li>", "")
      .replace("<ol>", "")
      .replace("</ol>", "")
      .replace("<ul>", "")
      .replace("</ul>", "")
      .replace("<br>", "");
  }

  private List<TextlineDimension> scanLineDimensions() throws BadLocationException {
    List<TextlineDimension> lines = new ArrayList<>();
    WrappedDocument document = content.getWrappedDocument();
    // Progress for rowStart is a little fuzzy due to the problem of trailing, unvisible newline in HTMLDocuments
    for (WrappedPosition rowStart = document.start(); !rowStart.isLast(); rowStart = rowStart.isLast() ? rowStart : rowStart.inc()) {
      Rectangle2D lineSpace = content.modelToView2D(rowStart.unwrap());
      int rowEndUI = Utilities.getRowEnd(content, rowStart.unwrap());
      WrappedPosition rowEnd = document.fromUI(rowEndUI);
      lines.add(new TextlineDimension(rowStart, rowEnd, lineSpace));
      rowStart = rowEnd;
    }

    return lines;
  }

  /** JEditorPane expresses text alignment by plain HTML style in div elements like that:
   * <div align="right">text goes here</div>
   * This is not suitable für pdf2html which only supports text alignment to be expressed by styling like that:
   * <div style="text-align: right">text goes here</div>
   * This must be morphed before rendering. Fortunately there occurs no other styling of divs in JEditorPane, so
   * we can simply work with an ordinary regexp replacement without merging. */
  private String stylifyTextAlignment(String rawHTML) {
    return rawHTML.replaceAll("align=\"([a-z]+)\"", "style=\"text-align:$1\"");
  }

  private String injectStylesheet(String rawHTML) {
    int headEnd = rawHTML.indexOf(HTMLTags.HEAD_OUTRO);
    String frontHTML = rawHTML.substring(0, headEnd-1);
    String tailHTML = rawHTML.substring(headEnd);
    return frontHTML + "<style>" + htmlStyles + "</style>" + tailHTML;
  }

  @Override
  protected PdfFont getPDFFont() { return null; }

  public static void initFont(int uizoomfactor, float swing2pdfScaleFactor) {
    try {
      // We explicetly do NOT want to use PDF standard fonts nor system fonts nor the built-in fonts of iText here,
      // because the rendering is based on the UI layout. Using exactly the same fonts for UI and PDF has the best
      // chances that the text lines are placed accurately in the boxes for the diagramm steps.
      fontProvider = new DefaultFontProvider(false, false, false);

      for (String fontfile: TextStyles.FONTFILES) {
        fontProvider.addFont(FontProgramFactory.createFont(fontfile));
      }
      fontProvider.addFont(FontProgramFactory.createFont()); // Helvetica for step labels

      initHTMLStyles(uizoomfactor, swing2pdfScaleFactor);
    }
    catch(IOException iox) {
      iox.printStackTrace();
    }
  }

  /** The method reads Specman's style sheet for PDF rendering and resizes any
   * fonts according to the UI zoom factor and the scaling factor required to
   * fit the diagramm into the maximum PDF page width. If the factors have the
   * default of 100% UI and {@link PDFRenderer#SWING2PDF_SCALEFACTOR_100PERCENT},
   * the method simply returns the stylesheet content as is. */
  private static void initHTMLStyles(int uiZoomfactor, float swing2pdfScaleFactor) throws IOException {
    float fontShrinkingFactor = swing2pdfScaleFactor / SWING2PDF_SCALEFACTOR_100PERCENT;
    float zoomAndScalefactor = fontShrinkingFactor * uiZoomfactor / 100.0f;
    //float zoomAndScalefactor = uiZoomfactor / 100.0f;
    StringBuilder sb = new StringBuilder();
    InputStream stylesheetStream = Specman.class.getClassLoader().getResourceAsStream(HTML2PDF_STYLESHEET);
    java.util.List<String> rawStylesheet = IOUtils.readLines(stylesheetStream, StandardCharsets.US_ASCII);
    for (String line: rawStylesheet) {
      if (uiZoomfactor != 100 || swing2pdfScaleFactor != SWING2PDF_SCALEFACTOR_100PERCENT) {
        Matcher matcher = FONTSIZE_PATTERN.matcher(line);
        if (matcher.matches()) {
          float rawFontsize = Float.parseFloat(matcher.group(2));
          float zoomedFontsize = rawFontsize * zoomAndScalefactor;
          sb.append(matcher.group(1));
          sb.append(zoomedFontsize);
          sb.append(matcher.group(3));
          sb.append("\n");
          continue;
        }
      }
      sb.append(line);
      sb.append("\n");
    }
    stylesheetStream.close();
    htmlStyles = sb.toString();
  }

}
