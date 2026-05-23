package experiments.itext;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.font.FontCharacteristics;
import com.itextpdf.layout.font.FontInfo;
import specman.graphics.Styles;
import specman.pdf.FormattedShapeText;
import specman.pdf.PDFRenderer;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Proof of concept: render a JEditorPane character by character into PDF,
 * using Swing's modelToView2D for exact positions and AttributeSet for styling.
 * This bypasses html2pdf entirely.
 */
public class CharLevelPdfTest extends JFrame {

  private static final float SCALE = 0.77f;
  private static final String PDF_OUT = "mini.pdf";
  private static final int EDITOR_COUNT = 2;
  private static final int EDITOR_WIDTH = 560;
  private static final int EDITOR_HEIGHT = 500;

  private final List<JEditorPane> editors = new ArrayList<>();

  public CharLevelPdfTest() {
    setTitle("Char-Level PDF Experiment");
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setSize(600, 600);

    JPanel stackPanel = new JPanel();
    stackPanel.setLayout(new BoxLayout(stackPanel, BoxLayout.Y_AXIS));

    for (int i = 0; i < EDITOR_COUNT; i++) {
      JEditorPane editor = createEditor();
      editors.add(editor);
      stackPanel.add(editor);
    }

    JButton exportBtn = new JButton("Als PDF exportieren");
    exportBtn.addActionListener(e -> exportToPDF());

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(new JScrollPane(stackPanel), BorderLayout.CENTER);
    getContentPane().add(exportBtn, BorderLayout.SOUTH);
  }

  private JEditorPane createEditor() {
    JEditorPane editor = new JEditorPane();
    editor.setEditorKit(new HTMLEditorKit());
    editor.setContentType("text/html");
    editor.setText(
      "<html><body style='font-family: SitkaDisplay; font-size: 12pt'>"
      + "<h1>Überschrift H1</h1>"
      + "Normaler Text, <b>fett</b>, <i>kursiv</i>, <b><i>beides</i></b>.<br>"
      + "<span style='color:red'>Roter</span> Text.<br>"
      + "<span style='font-family: Roboto'>Roboto-Text, <b>fett</b>, <i>kursiv</i>.</span><br>"
      + "<span style='font-family: CourierPrime'>Courier Prime, <b>fett</b>.</span><br>"
      + "<u>Unterstrichen</u>, normal, <s>durchgestrichen</s>, <b><u>fett unterstrichen</u></b>.<br>"
      + "Hintergrund: gelb markiert und durchgestrichen."
      + "</body></html>");
    editor.setEditable(false);
    editor.setMaximumSize(new Dimension(EDITOR_WIDTH, EDITOR_HEIGHT));
    editor.setPreferredSize(new Dimension(EDITOR_WIDTH, EDITOR_HEIGHT));
    editor.setSize(EDITOR_WIDTH, EDITOR_HEIGHT);
    editor.addNotify();
    editor.validate();

    StyledDocument doc = (StyledDocument) editor.getDocument();
    String fullText = getPlainText(doc);
    applyBackground(doc, fullText, "gelb markiert", Color.YELLOW);
    applyBackground(doc, fullText, "durchgestrichen", new Color(200, 200, 200));
    SimpleAttributeSet strikeAttr = new SimpleAttributeSet();
    StyleConstants.setStrikeThrough(strikeAttr, true);
    int strikeStart = fullText.indexOf("durchgestrichen");
    if (strikeStart >= 0)
      doc.setCharacterAttributes(strikeStart, "durchgestrichen".length(), strikeAttr, false);

    return editor;
  }

  private void exportToPDF() {
    try {
      FormattedShapeText.initFont(100, PDFRenderer.SWING2PDF_SCALEFACTOR_100PERCENT);

      float totalHeightPx = (float) EDITOR_COUNT * EDITOR_HEIGHT;
      PageSize pageSize = new PageSize(PageSize.A4.getWidth(), totalHeightPx * SCALE);

      PdfDocument pdfDoc = new PdfDocument(new PdfWriter(PDF_OUT));
      PdfCanvas canvas = new PdfCanvas(pdfDoc.addNewPage(pageSize));

      long start = System.currentTimeMillis();

      for (int i = 0; i < editors.size(); i++) {
        float yOffsetPx = (float) i * EDITOR_HEIGHT;
        renderEditor(editors.get(i), canvas, pageSize, yOffsetPx);
      }

      long elapsed = System.currentTimeMillis() - start;
      System.out.printf("PDF-Rendering: %d ms für %d Editoren%n", elapsed, editors.size());

      pdfDoc.close();
      System.out.println("PDF geschrieben: " + new File(PDF_OUT).getAbsolutePath());
      Desktop.getDesktop().open(new File(PDF_OUT));
    }
    catch (IOException ex) {
      ex.printStackTrace();
      JOptionPane.showMessageDialog(this, "Fehler: " + ex.getMessage());
    }
  }

  private void renderEditor(JEditorPane editor, PdfCanvas canvas, PageSize pageSize, float yOffsetPx) {
    StyledDocument swingDoc = (StyledDocument) editor.getDocument();
    int length = swingDoc.getLength();

    List<BackgroundSpan> backgrounds = collectBackgroundSpans(editor, swingDoc, length, pageSize, yOffsetPx);
    drawBackgrounds(canvas, backgrounds);

    List<DecorationSpan> underlines = new ArrayList<>();
    List<DecorationSpan> strikethroughs = new ArrayList<>();
    Graphics2D g2 = (Graphics2D) editor.getGraphics();

    canvas.beginText();

    int offset = 0;
    while (offset < length) {
      String ch;
      try {
        ch = swingDoc.getText(offset, 1);
      } catch (BadLocationException ex) {
        offset++;
        continue;
      }

      if (ch.equals("\n") || ch.equals("\r") || ch.charAt(0) < 32) {
        offset++;
        continue;
      }

      Rectangle2D bounds;
      try {
        bounds = editor.modelToView2D(offset);
      } catch (BadLocationException ex) {
        offset++;
        continue;
      }
      if (bounds == null) {
        offset++;
        continue;
      }

      Element charElement = swingDoc.getCharacterElement(offset);
      AttributeSet attrs = charElement.getAttributes();
      Font resolvedFont = getResolvedFont(editor, offset);
      float fontSize = resolvedFont.getSize2D();
      Color fg = StyleConstants.getForeground(attrs);

      PdfFont pdfFont = resolvePdfFont(resolvedFont);

      java.awt.font.LineMetrics lm = resolvedFont.getLineMetrics("X", g2.getFontRenderContext());
      float scaledFontSize = fontSize * SCALE;
      float pdfX = (float) bounds.getX() * SCALE;
      float baselineOffset = (float) bounds.getHeight() - lm.getDescent();
      float pdfY = pageSize.getHeight() - (yOffsetPx + (float)(bounds.getY() + baselineOffset)) * SCALE;

      float charWidth = pdfFont.getWidth(ch, scaledFontSize);

      canvas.setFillColor(new com.itextpdf.kernel.colors.DeviceRgb(
        fg.getRed(), fg.getGreen(), fg.getBlue()));
      canvas.setFontAndSize(pdfFont, scaledFontSize);
      canvas.setTextMatrix(pdfX, pdfY);
      canvas.showText(ch);

      if (StyleConstants.isUnderline(attrs)) {
        float decorY = pdfY - (lm.getUnderlineOffset() + lm.getDescent() * 0.5f) * SCALE;
        float thickness = Math.max(lm.getUnderlineThickness() * SCALE, 0.5f);
        collectDecoration(underlines, pdfX, charWidth, decorY, thickness, fg);
      }
      if (StyleConstants.isStrikeThrough(attrs)) {
        float decorY = pdfY - (lm.getStrikethroughOffset() + lm.getDescent() * 0.5f) * SCALE;
        float thickness = Math.max(lm.getStrikethroughThickness() * SCALE, 0.5f);
        collectDecoration(strikethroughs, pdfX, charWidth, decorY, thickness, fg);
      }

      offset++;
    }

    canvas.endText();
    drawDecorations(canvas, underlines);
    drawDecorations(canvas, strikethroughs);
  }

  private static String getPlainText(StyledDocument doc) {
    try {
      return doc.getText(0, doc.getLength());
    } catch (BadLocationException e) {
      return "";
    }
  }

  private static void applyBackground(StyledDocument doc, String fullText, String substring, Color color) {
    int start = fullText.indexOf(substring);
    if (start < 0) return;
    SimpleAttributeSet attr = new SimpleAttributeSet();
    StyleConstants.setBackground(attr, color);
    doc.setCharacterAttributes(start, substring.length(), attr, false);
  }

  /** Accumulates a background highlight rectangle across consecutive characters. */
  private static class BackgroundSpan {
    final float y;
    final float height;
    final Color color;
    float xStart;
    float xEnd;

    BackgroundSpan(float xStart, float xEnd, float y, float height, Color color) {
      this.xStart = xStart;
      this.xEnd = xEnd;
      this.y = y;
      this.height = height;
      this.color = color;
    }

    boolean canExtend(float nextX, float nextY, float nextHeight, Color nextColor) {
      return Math.abs(nextY - y) < 0.1f
        && Math.abs(nextHeight - height) < 0.1f
        && nextColor.equals(color)
        && nextX <= xEnd + 1.0f;
    }
  }

  private List<BackgroundSpan> collectBackgroundSpans(JEditorPane editor, StyledDocument swingDoc,
      int length, PageSize pageSize, float yOffsetPx) {
    List<BackgroundSpan> spans = new ArrayList<>();
    for (int offset = 0; offset < length; offset++) {
      String ch;
      try { ch = swingDoc.getText(offset, 1); } catch (BadLocationException e) { continue; }
      if (ch.equals("\n") || ch.equals("\r") || ch.charAt(0) < 32) continue;

      AttributeSet attrs = swingDoc.getCharacterElement(offset).getAttributes();
      Color bg = (Color) attrs.getAttribute(StyleConstants.Background);
      if (bg == null) continue;

      Rectangle2D bounds;
      try { bounds = editor.modelToView2D(offset); } catch (BadLocationException e) { continue; }
      if (bounds == null) continue;

      Font resolvedFont = getResolvedFont(editor, offset);
      PdfFont pdfFont = resolvePdfFont(resolvedFont);
      float scaledFontSize = resolvedFont.getSize2D() * SCALE;
      float charWidth = pdfFont.getWidth(ch, scaledFontSize);
      java.awt.font.LineMetrics lm = resolvedFont.getLineMetrics("X",
        ((Graphics2D) editor.getGraphics()).getFontRenderContext());
      float pdfX = (float) bounds.getX() * SCALE;
      float textHeight = (lm.getAscent() + lm.getDescent()) * SCALE;
      float rectY = pageSize.getHeight() - (yOffsetPx + (float)(bounds.getY() + bounds.getHeight())) * SCALE;
      collectBackground(spans, pdfX, charWidth, rectY, textHeight, bg);
    }
    return spans;
  }

  private void collectBackground(List<BackgroundSpan> spans,
      float pdfX, float charWidth, float pdfY, float lineHeight, Color color) {
    if (!spans.isEmpty()) {
      BackgroundSpan last = spans.get(spans.size() - 1);
      if (last.canExtend(pdfX, pdfY, lineHeight, color)) {
        last.xEnd = pdfX + charWidth;
        return;
      }
    }
    spans.add(new BackgroundSpan(pdfX, pdfX + charWidth, pdfY, lineHeight, color));
  }

  private void drawBackgrounds(PdfCanvas canvas, List<BackgroundSpan> spans) {
    for (BackgroundSpan span : spans) {
      canvas.setFillColor(new com.itextpdf.kernel.colors.DeviceRgb(
        span.color.getRed(), span.color.getGreen(), span.color.getBlue()));
      canvas.rectangle(span.xStart, span.y, span.xEnd - span.xStart, span.height);
      canvas.fill();
    }
  }

  /** Accumulates a horizontal decoration line (underline or strikethrough) across characters. */
  private static class DecorationSpan {
    final float y;
    final float thickness;
    final Color color;
    float xStart;
    float xEnd;

    DecorationSpan(float xStart, float xEnd, float y, float thickness, Color color) {
      this.xStart = xStart;
      this.xEnd = xEnd;
      this.y = y;
      this.thickness = thickness;
      this.color = color;
    }

    boolean canExtend(float nextX, float nextY, float nextThickness, Color nextColor) {
      return Math.abs(nextY - y) < 0.1f
        && Math.abs(nextThickness - thickness) < 0.01f
        && nextColor.equals(color)
        && nextX <= xEnd + 1.0f;
    }
  }

  private void drawDecorations(PdfCanvas canvas, List<DecorationSpan> spans) {
    for (DecorationSpan span : spans) {
      canvas.setStrokeColor(new com.itextpdf.kernel.colors.DeviceRgb(
        span.color.getRed(), span.color.getGreen(), span.color.getBlue()));
      canvas.setLineWidth(span.thickness);
      canvas.moveTo(span.xStart, span.y);
      canvas.lineTo(span.xEnd, span.y);
      canvas.stroke();
    }
  }

  private void collectDecoration(List<DecorationSpan> spans,
      float pdfX, float charWidth, float decorY, float thickness, Color color) {
    if (!spans.isEmpty()) {
      DecorationSpan last = spans.get(spans.size() - 1);
      if (last.canExtend(pdfX, decorY, thickness, color)) {
        last.xEnd = pdfX + charWidth;
        return;
      }
    }
    spans.add(new DecorationSpan(pdfX, pdfX + charWidth, decorY, thickness, color));
  }

  private Font getResolvedFont(JEditorPane editor, int offset) {
    View rootView = editor.getUI().getRootView(editor);
    Font resolved = findGlyphView(rootView, offset);
    return resolved != null ? resolved : editor.getFont();
  }

  private Font findGlyphView(View view, int offset) {
    if (view.getStartOffset() > offset || view.getEndOffset() <= offset) {
      return null;
    }
    if (view instanceof GlyphView) {
      return ((GlyphView) view).getFont();
    }
    for (int i = 0; i < view.getViewCount(); i++) {
      Font f = findGlyphView(view.getView(i), offset);
      if (f != null) return f;
    }
    return null;
  }

  private PdfFont resolvePdfFont(Font swingFont) {
    FontCharacteristics fc = new FontCharacteristics();
    fc.setBoldFlag(swingFont.isBold());
    fc.setItalicFlag(swingFont.isItalic());
    String iTextFamilyName = toITextFamilyName(swingFont);
    FontInfo info = FormattedShapeText.fontProvider
      .getFontSelector(Arrays.asList(iTextFamilyName, "SitkaDisplay"), fc)
      .bestMatch();
    return FormattedShapeText.fontProvider.getPdfFont(info);
  }

  /**
   * Maps a Swing Font to the font family name as registered in iText's FontProvider.
   * iText uses the PostScript/internal name from the TTF file, which differs from
   * what Java's Font.getFamily() returns (e.g. "Sitka Display" vs "SitkaDisplay").
   */
  private String toITextFamilyName(Font swingFont) {
    String family = swingFont.getFamily();
    if (family.equalsIgnoreCase("Monospaced") || family.equalsIgnoreCase("Courier New")
        || family.equalsIgnoreCase("Courier")) {
      return "CourierPrime";
    }
    return family.replace(" ", "");
  }

  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
      Styles.FONTFILES.forEach(f -> {
        try (var s = CharLevelPdfTest.class.getClassLoader().getResourceAsStream(f)) {
          Font font = Font.createFont(Font.TRUETYPE_FONT, s);
          GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      });
      new CharLevelPdfTest().setVisible(true);
    });
  }
}
