package specman.pdf;

import specman.pdf.navigation.PdfNavigationPlan;

import java.util.Objects;

/** Input prepared from one current diagram for a synchronous PDF export.
 * The plan describes semantic destinations; bound shapes supply identities for collecting
 * their positions during rendering. No live editor traversal is needed by the renderer. */
public record PreparedPdfExport(Shape diagramShape, PdfNavigationPlan navigation) {
  public PreparedPdfExport {
    Objects.requireNonNull(diagramShape, "diagramShape");
    Objects.requireNonNull(navigation, "navigation");
  }
}
