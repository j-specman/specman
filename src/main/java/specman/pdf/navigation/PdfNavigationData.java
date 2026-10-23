package specman.pdf.navigation;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import specman.pdf.navigation.PdfNavigationPlan.TargetId;

/** Positions on the final PDF pages, after scaling and any page tiling.
 * Page numbers are one-based. Coordinates are unrotated PDF user-space coordinates
 * (origin at the page's bottom left for ordinary Specman PDFs), not Swing coordinates.
 * Rectangles must already be split/clipped to their individual visible pages. */
public record PdfNavigationData(Map<TargetId, Destination> destinations, List<LinkArea> links) {
  public PdfNavigationData {
    destinations = Map.copyOf(destinations);
    links = List.copyOf(links);
  }

  public static PdfNavigationData empty() { return new PdfNavigationData(Map.of(), List.of()); }

  public record Destination(int pageNumber, float x, float y) {
    public Destination {
      requirePage(pageNumber);
      requireFinite(x, y);
    }
  }

  /** target refers to the already selected visible destination, not to a hidden logical target. */
  public record LinkArea(int pageNumber, float x, float y, float width, float height, TargetId target) {
    public LinkArea {
      requirePage(pageNumber);
      requireFinite(x, y, width, height);
      if (width <= 0 || height <= 0) throw new IllegalArgumentException("Link area must have positive size");
      Objects.requireNonNull(target, "target");
    }
  }

  private static void requirePage(int pageNumber) {
    if (pageNumber < 1) throw new IllegalArgumentException("PDF page numbers start at 1");
  }

  private static void requireFinite(float... coordinates) {
    for (float coordinate : coordinates) {
      if (!Float.isFinite(coordinate)) throw new IllegalArgumentException("Non-finite PDF coordinate");
    }
  }
}
