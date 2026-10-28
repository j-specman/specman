package specman.pdf.navigation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static specman.pdf.navigation.PdfNavigationData.*;
import static specman.pdf.navigation.PdfNavigationPlan.TargetId;

/** Shared vertical page mapping for the drawing and its navigation. Pages run from top to bottom. */
public final class PdfPageTiling {
  private final float sourceHeight;
  private final float pageHeight;
  private final int pageCount;

  public PdfPageTiling(float sourceHeight, float pageHeight) {
    if (!Float.isFinite(sourceHeight) || !Float.isFinite(pageHeight) || sourceHeight <= 0 || pageHeight <= 0) {
      throw new IllegalArgumentException("Page heights must be finite and positive");
    }
    this.sourceHeight = sourceHeight;
    this.pageHeight = pageHeight;
    int count = 1;
    while (count * pageHeight < sourceHeight) count++;
    pageCount = count;
  }

  public int pageCount() { return pageCount; }

  /** The exact translation also used when placing the original page's form XObject. */
  public float offsetY(int page) {
    if (page < 1 || page > pageCount) throw new IllegalArgumentException("Unknown tile: " + page);
    return -sourceHeight + pageHeight * page;
  }

  public PdfNavigationData map(PdfNavigationData source) {
    Map<TargetId, Destination> destinations = new LinkedHashMap<>();
    source.destinations().forEach((id, position) -> {
      requireSourcePage(position.pageNumber());
      // A header exactly on a cut belongs to the following page, where it starts at the top.
      int page = 1;
      while (page < pageCount && position.y() <= -offsetY(page)) page++;
      destinations.put(id, new Destination(page, position.x(), onPage(position.y(), page)));
    });
    List<LinkArea> links = new ArrayList<>();
    for (LinkArea area : source.links()) {
      requireSourcePage(area.pageNumber());
      for (int page = 1; page <= pageCount; page++) {
        float lower = Math.max(0, -offsetY(page));
        float upper = Math.min(sourceHeight, pageHeight - offsetY(page));
        float bottom = Math.max(area.y(), lower);
        float top = Math.min(area.y() + area.height(), upper);
        if (top <= bottom) continue;
        float mappedBottom = onPage(bottom, page);
        float mappedTop = onPage(top, page);
        if (mappedTop > mappedBottom) {
          links.add(new LinkArea(page, area.x(), mappedBottom, area.width(),
            mappedTop - mappedBottom, area.target()));
        }
      }
    }
    return new PdfNavigationData(destinations, links);
  }

  private float onPage(float y, int page) {
    // Clip rounding at cut edges to the same visible page rectangle as the form XObject.
    return Math.max(0, Math.min(pageHeight, y + offsetY(page)));
  }

  private static void requireSourcePage(int page) {
    if (page != 1) throw new IllegalArgumentException("Tiling expects one original page");
  }
}
