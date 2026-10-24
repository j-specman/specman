package specman.pdf.navigation;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static specman.pdf.navigation.PdfNavigationPlan.*;

/** Geometry collected during one drawing pass, already scaled into PDF coordinates.
 * Supports uniformly formatted, single-line text links, including visible ancestors
 * of folded targets. The final target must have an actual rendered position
 * on the original ordinary or overlength page. Optional tiling maps the result afterwards.
 * Break/catch labels use their actually painted text bounds and the same target lookup. */
public final class PdfNavigationRenderContext {
  public record GlyphStyle(String fontName, int fontStyle, float fontSize,
                           int foreground, Integer background, boolean underline, boolean strike) {}

  private final PdfNavigationPlan plan;
  private final Map<TargetId, PdfNavigationData.Destination> destinations = new LinkedHashMap<>();
  private final Map<String, List<TextArea>> textAreas = new HashMap<>();
  private final List<TextArea> references = new ArrayList<>();
  private final Map<String, List<Link>> labelLinks = new LinkedHashMap<>();
  private final Map<String, LabelArea> labelAreas = new HashMap<>();

  private record LabelArea(float x, float y, float width, float height) {}

  public PdfNavigationRenderContext(PdfNavigationPlan plan) {
    this.plan = Objects.requireNonNull(plan);
    for (Link link : plan.links()) {
      if (!link.source().visible() || link.resolution().logicalTarget() == null) continue;
      if (link.kind() != LinkKind.TEXT) {
        labelLinks.computeIfAbsent(link.source().key(), ignored -> new ArrayList<>()).add(link);
        continue;
      }
      TextArea area = new TextArea(link);
      references.add(area);
      textAreas.computeIfAbsent(link.source().key(), ignored -> new ArrayList<>()).add(area);
    }
  }

  /** Position of the actually drawn header, including the step number. */
  public void target(TargetId id, float x, float topY) {
    if (id != null && plan.isVisible(id)) {
      if (destinations.putIfAbsent(id, new PdfNavigationData.Destination(1, x, topY)) != null) {
        throw new IllegalStateException("Navigation header rendered twice: " + id);
      }
    }
  }

  /** Called only for a character successfully written to the PDF canvas. */
  public void glyph(String key, int offset, String character, float x, float baseline,
                    float width, float ascent, float descent, GlyphStyle style) {
    for (TextArea area : textAreas.getOrDefault(key, List.of())) {
      area.glyph(offset, character, x, baseline, width, ascent, descent, style);
    }
  }

  /** Called after writing a step-number label, using that PDF font's actual text metrics. */
  public void label(String key, float x, float y, float width, float height) {
    if (!labelLinks.containsKey(key)) return;
    if (labelAreas.putIfAbsent(key, new LabelArea(x, y, width, height)) != null) {
      throw new IllegalStateException("Navigation label rendered twice: " + key);
    }
  }

  public PdfNavigationData finish(float pageWidth, float pageHeight) {
    Map<TargetId, PdfNavigationData.Destination> visible = new LinkedHashMap<>();
    destinations.forEach((id, position) -> {
      if (inside(position.x(), position.y(), pageWidth, pageHeight)) visible.put(id, position);
    });
    List<PdfNavigationData.LinkArea> links = new ArrayList<>();
    for (TextArea area : references) {
      if (!area.complete()) continue;
      addLink(links, area.link, area.left, area.bottom, area.right - area.left, area.top - area.bottom,
        visible, pageWidth, pageHeight);
    }
    labelLinks.forEach((key, connections) -> {
      LabelArea area = labelAreas.get(key);
      if (area == null) return;
      for (Link connection : connections) {
        addLink(links, connection, area.x(), area.y(), area.width(), area.height(), visible, pageWidth, pageHeight);
      }
    });
    return new PdfNavigationData(visible, links);
  }

  private void addLink(List<PdfNavigationData.LinkArea> links, Link link, float x, float y,
                       float width, float height, Map<TargetId, PdfNavigationData.Destination> visible,
                       float pageWidth, float pageHeight) {
    TargetId target = renderedTarget(link.resolution().logicalTarget(), visible);
    if (target == null || width <= 0 || height <= 0
        || !inside(x, y, pageWidth, pageHeight) || !inside(x + width, y + height, pageWidth, pageHeight)) return;
    links.add(new PdfNavigationData.LinkArea(1, x, y, width, height, target));
  }

  /** Walk the semantic parent chain, never siblings or the break associated with a catch.
   * Registration already excludes semantically hidden headers; finish also excludes
   * positions outside the exported page. No editor state is changed for this lookup. */
  private TargetId renderedTarget(TargetId logicalTarget, Map<TargetId, PdfNavigationData.Destination> visible) {
    Target candidate = plan.targets().get(logicalTarget);
    while (candidate != null) {
      if (visible.containsKey(candidate.id())) return candidate.id();
      candidate = plan.targets().get(candidate.parent());
    }
    return null;
  }

  private static boolean inside(float x, float y, float width, float height) {
    return Float.isFinite(x) && Float.isFinite(y) && x >= 0 && y >= 0 && x <= width && y <= height;
  }

  private static final class TextArea {
    final Link link;
    final BitSet seen = new BitSet();
    GlyphStyle style;
    boolean rejected;
    float baseline, left, bottom, right, top;

    TextArea(Link link) { this.link = link; }

    void glyph(int offset, String character, float x, float y, float width,
               float ascent, float descent, GlyphStyle glyphStyle) {
      int index = offset - link.source().start();
      int length = link.source().end() - link.source().start();
      if (index < 0 || index >= length || rejected) return;
      if (seen.get(index) || index >= link.text().length()
          || !character.equals(link.text().substring(index, index + 1))
          || width <= 0 || ascent + descent <= 0) {
        rejected = true;
        return;
      }
      if (seen.isEmpty()) {
        style = glyphStyle;
        baseline = y;
        left = x;
        bottom = y - descent;
        right = x + width;
        top = y + ascent;
      }
      else {
        if (Math.abs(y - baseline) > 0.1f || !Objects.equals(style, glyphStyle)) {
          rejected = true;
          return;
        }
        left = Math.min(left, x);
        bottom = Math.min(bottom, y - descent);
        right = Math.max(right, x + width);
        top = Math.max(top, y + ascent);
      }
      seen.set(index);
    }

    boolean complete() {
      return !rejected && seen.cardinality() == link.source().end() - link.source().start();
    }
  }
}
