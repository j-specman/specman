package specman.pdf;

import com.itextpdf.layout.font.FontProvider;
import com.itextpdf.io.font.FontProgramFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.font.FontCharacteristics;
import com.itextpdf.layout.font.FontInfo;
import com.itextpdf.layout.font.FontProvider;
import specman.editarea.TextEditArea;
import specman.editarea.TextStyles;

import javax.swing.text.*;
import java.awt.*;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FormattedShapeText extends AbstractShapeText {

  public static FontProvider fontProvider;

  private final TextEditArea content;

  public FormattedShapeText(TextEditArea content) {
    super(content.getInsets(), content.getForeground(), content.getFont());
    this.content = content;
  }

  public void writeToPDF(Point renderOffset, float swing2pdfScaleFactor, PdfCanvas pdfCanvas, Document document) {
    StyledDocument swingDoc = (StyledDocument) content.getDocument();
    int length = swingDoc.getLength();

    List<BackgroundSpan> backgrounds = collectBackgroundSpans(swingDoc, length, renderOffset, swing2pdfScaleFactor);
    drawBackgrounds(pdfCanvas, backgrounds);

    List<DecorationSpan> underlines = new ArrayList<>();
    List<DecorationSpan> strikethroughs = new ArrayList<>();
    Graphics2D g2 = (Graphics2D) content.getGraphics();

    pdfCanvas.beginText();

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
        bounds = content.modelToView2D(offset);
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
      Font resolvedFont = getResolvedFont(offset);
      float fontSize = resolvedFont.getSize2D();
      Color fg = StyleConstants.getForeground(attrs);

      PdfFont pdfFont = resolvePdfFont(resolvedFont);

      LineMetrics lm = resolvedFont.getLineMetrics("X", g2.getFontRenderContext());
      float scaledFontSize = fontSize * swing2pdfScaleFactor;
      float pdfX = (renderOffset.x + (float) bounds.getX()) * swing2pdfScaleFactor;
      float baselineOffset = (float) bounds.getHeight() - lm.getDescent();
      float pdfY = (renderOffset.y - (float) bounds.getY() - baselineOffset) * swing2pdfScaleFactor;

      float charWidth = pdfFont.getWidth(ch, scaledFontSize);

      pdfCanvas.setFillColor(new com.itextpdf.kernel.colors.DeviceRgb(fg.getRed(), fg.getGreen(), fg.getBlue()));
      pdfCanvas.setFontAndSize(pdfFont, scaledFontSize);
      pdfCanvas.setTextMatrix(pdfX, pdfY);
      pdfCanvas.showText(ch);

      if (StyleConstants.isUnderline(attrs)) {
        float decorY = pdfY - (lm.getUnderlineOffset() + lm.getDescent() * 0.5f) * swing2pdfScaleFactor;
        float thickness = Math.max(lm.getUnderlineThickness() * swing2pdfScaleFactor, 0.5f);
        collectDecoration(underlines, pdfX, charWidth, decorY, thickness, fg);
      }
      if (StyleConstants.isStrikeThrough(attrs)) {
        float decorY = pdfY - (lm.getStrikethroughOffset() + lm.getDescent() * 0.5f) * swing2pdfScaleFactor;
        float thickness = Math.max(lm.getStrikethroughThickness() * swing2pdfScaleFactor, 0.5f);
        collectDecoration(strikethroughs, pdfX, charWidth, decorY, thickness, fg);
      }

      offset++;
    }

    pdfCanvas.endText();
    drawDecorations(pdfCanvas, underlines);
    drawDecorations(pdfCanvas, strikethroughs);
  }

  private List<BackgroundSpan> collectBackgroundSpans(StyledDocument swingDoc, int length,
      Point renderOffset, float scale) {
    List<BackgroundSpan> spans = new ArrayList<>();
    Graphics2D g2 = (Graphics2D) content.getGraphics();
    for (int offset = 0; offset < length; offset++) {
      String ch;
      try { ch = swingDoc.getText(offset, 1); } catch (BadLocationException e) { continue; }
      if (ch.equals("\n") || ch.equals("\r") || ch.charAt(0) < 32) continue;

      AttributeSet attrs = swingDoc.getCharacterElement(offset).getAttributes();
      Color bg = (Color) attrs.getAttribute(StyleConstants.Background);
      if (bg == null) continue;

      Rectangle2D bounds;
      try { bounds = content.modelToView2D(offset); } catch (BadLocationException e) { continue; }
      if (bounds == null) continue;

      Font resolvedFont = getResolvedFont(offset);
      PdfFont pdfFont = resolvePdfFont(resolvedFont);
      float scaledFontSize = resolvedFont.getSize2D() * scale;
      float charWidth = pdfFont.getWidth(ch, scaledFontSize);
      LineMetrics lm = resolvedFont.getLineMetrics("X", g2.getFontRenderContext());
      float pdfX = (renderOffset.x + (float) bounds.getX()) * scale;
      float textHeight = (lm.getAscent() + lm.getDescent()) * scale;
      // rectY: bottom edge of the text box in PDF coordinates
      float rectY = (renderOffset.y - (float) bounds.getY() - (float) bounds.getHeight()) * scale;
      collectBackground(spans, pdfX, charWidth, rectY, textHeight, bg);
    }
    return spans;
  }

  private Font getResolvedFont(int offset) {
    View rootView = content.getUI().getRootView(content);
    Font resolved = findGlyphView(rootView, offset);
    // findGlyphView should always find a GlyphView for any visible character, but
    // content.getFont() is a safe fallback to prevent NPEs in the calling code.
    return resolved != null ? resolved : content.getFont();
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
    String familyName = toITextFamilyName(swingFont);
    FontInfo info = fontProvider
      .getFontSelector(Arrays.asList(familyName, "SitkaDisplay"), fc)
      .bestMatch();
    return fontProvider.getPdfFont(info);
  }

  private String toITextFamilyName(Font swingFont) {
    String family = swingFont.getFamily();
    // Map system monospace fonts to Specman's shipped CourierPrime
    if (family.equalsIgnoreCase("Monospaced") || family.equalsIgnoreCase("Courier New")
        || family.equalsIgnoreCase("Courier")) {
      return "CourierPrime";
    }
    // Strip spaces — iText registered names match the TTF PostScript family (no spaces)
    return family.replace(" ", "");
  }

  // ---- Span helpers (same pattern as CharLevelPdfTest) ----

  private static class BackgroundSpan {
    final float y, height;
    final Color color;
    float xStart, xEnd;

    BackgroundSpan(float xStart, float xEnd, float y, float height, Color color) {
      this.xStart = xStart; this.xEnd = xEnd; this.y = y; this.height = height; this.color = color;
    }

    boolean canExtend(float nextX, float nextY, float nextHeight, Color nextColor) {
      return Math.abs(nextY - y) < 0.1f && Math.abs(nextHeight - height) < 0.1f
        && nextColor.equals(color) && nextX <= xEnd + 1.0f;
    }
  }

  private void collectBackground(List<BackgroundSpan> spans,
      float pdfX, float charWidth, float pdfY, float lineHeight, Color color) {
    if (!spans.isEmpty()) {
      BackgroundSpan last = spans.get(spans.size() - 1);
      if (last.canExtend(pdfX, pdfY, lineHeight, color)) { last.xEnd = pdfX + charWidth; return; }
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

  private static class DecorationSpan {
    final float y, thickness;
    final Color color;
    float xStart, xEnd;

    DecorationSpan(float xStart, float xEnd, float y, float thickness, Color color) {
      this.xStart = xStart; this.xEnd = xEnd; this.y = y; this.thickness = thickness; this.color = color;
    }

    boolean canExtend(float nextX, float nextY, float nextThickness, Color nextColor) {
      return Math.abs(nextY - y) < 0.1f && Math.abs(nextThickness - thickness) < 0.01f
        && nextColor.equals(color) && nextX <= xEnd + 1.0f;
    }
  }

  private void collectDecoration(List<DecorationSpan> spans,
      float pdfX, float charWidth, float decorY, float thickness, Color color) {
    if (!spans.isEmpty()) {
      DecorationSpan last = spans.get(spans.size() - 1);
      if (last.canExtend(pdfX, decorY, thickness, color)) { last.xEnd = pdfX + charWidth; return; }
    }
    spans.add(new DecorationSpan(pdfX, pdfX + charWidth, decorY, thickness, color));
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

  @Override
  protected PdfFont getPDFFont() { return null; }

  public static void initFont(int uizoomfactor, float swing2pdfScaleFactor) {
    try {
      fontProvider = new FontProvider();
      for (String fontfile : TextStyles.FONTFILES) {
        fontProvider.addFont(FontProgramFactory.createFont(fontfile));
      }
      fontProvider.addFont(FontProgramFactory.createFont()); // Helvetica for step labels
    }
    catch (IOException iox) {
      iox.printStackTrace();
    }
  }

}
