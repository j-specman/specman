package specman.pdf.navigation;

import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfArray;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.action.PdfAction;
import com.itextpdf.kernel.pdf.annot.PdfLinkAnnotation;
import com.itextpdf.kernel.pdf.navigation.PdfExplicitDestination;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import specman.pdf.navigation.PdfNavigationData.Destination;
import specman.pdf.navigation.PdfNavigationData.LinkArea;
import specman.pdf.navigation.PdfNavigationPlan.TargetId;

/** Adds document-internal links to a finished PDF, without consulting or changing the editor.
 * The production renderer supplies positions collected during its drawing pass.
 * Each invocation creates a separate document; no destinations survive between exports. */
public final class PdfNavigationWriter {
  /** Missing destination IDs are skipped. Invalid PDF/layout data and iText failures propagate.
   * If no links resolve, a validated, unchanged copy of the original PDF bytes is returned. */
  public byte[] addNavigation(byte[] pdfBytes, PdfNavigationData navigation) throws IOException {
    Objects.requireNonNull(pdfBytes, "pdfBytes");
    Objects.requireNonNull(navigation, "navigation");
    boolean hasResolvedLinks = navigation.links().stream()
      .anyMatch(link -> navigation.destinations().containsKey(link.target()));
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (PdfDocument pdf = hasResolvedLinks
      ? new PdfDocument(new PdfReader(new ByteArrayInputStream(pdfBytes)), new PdfWriter(output))
      : new PdfDocument(new PdfReader(new ByteArrayInputStream(pdfBytes)))) {
      validatePositions(pdf, navigation);
      if (!hasResolvedLinks) return pdfBytes.clone();

      // Resolve every destination against the final document before adding any annotation.
      Map<TargetId, PdfExplicitDestination> destinations = new LinkedHashMap<>();
      navigation.destinations().forEach((id, position) -> destinations.put(id,
        PdfExplicitDestination.createXYZ(pdf.getPage(position.pageNumber()), position.x(), position.y(), 0)));

      for (LinkArea link : navigation.links()) {
        PdfExplicitDestination destination = destinations.get(link.target());
        if (destination == null) continue;
        PdfLinkAnnotation annotation = new PdfLinkAnnotation(
          new Rectangle(link.x(), link.y(), link.width(), link.height()));
        annotation.setAction(PdfAction.createGoTo(destination));
        annotation.setBorder(new PdfArray(new float[] {0, 0, 0}));
        pdf.getPage(link.pageNumber()).addAnnotation(annotation);
      }
    }
    return output.toByteArray();
  }

  private void validatePositions(PdfDocument pdf, PdfNavigationData navigation) {
    for (Destination destination : navigation.destinations().values()) {
      Rectangle bounds = pageBounds(pdf, destination.pageNumber());
      requireInside(bounds, destination.x(), destination.y(), destination.x(), destination.y(), "Destination");
    }
    for (LinkArea link : navigation.links()) {
      Rectangle bounds = pageBounds(pdf, link.pageNumber());
      requireInside(bounds, link.x(), link.y(), (double) link.x() + link.width(),
        (double) link.y() + link.height(), "Link area");
    }
  }

  private Rectangle pageBounds(PdfDocument pdf, int pageNumber) {
    if (pageNumber > pdf.getNumberOfPages()) {
      throw new IllegalArgumentException("Navigation refers to missing PDF page " + pageNumber);
    }
    return pdf.getPage(pageNumber).getCropBox();
  }

  private void requireInside(Rectangle bounds, double left, double bottom, double right, double top,
                             String description) {
    if (left < bounds.getLeft() || bottom < bounds.getBottom()
      || right > bounds.getRight() || top > bounds.getTop()) {
      throw new IllegalArgumentException(description + " lies outside the PDF page's crop box");
    }
  }
}
