package specman.pdf.navigation;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static specman.pdf.navigation.PdfNavigationData.*;
import static specman.pdf.navigation.PdfNavigationPlan.TargetId;

class PdfPageTilingTest {
  private final TargetId target = TargetId.step("target");

  @Test
  void mapsHeadersFromTopToBottomIncludingExactCutsAndTheBottomOfTheLastPage() {
    PdfPageTiling tiling = new PdfPageTiling(2500, 1000);
    assertEquals(3, tiling.pageCount());
    float[] original = {2500, 2400, 1500, 500, 0};
    int[] pages = {1, 1, 2, 3, 3};
    float[] expectedY = {1000, 900, 1000, 1000, 500};
    for (int i = 0; i < original.length; i++) {
      var mapped = tiling.map(new PdfNavigationData(Map.of(target, new Destination(1, 20, original[i])), List.of()));
      assertEquals(new Destination(pages[i], 20, expectedY[i]), mapped.destinations().get(target));
    }
  }

  @Test
  void splitsAClickRectangleAtTheSameCutAsTheDrawing() {
    var mapped = new PdfPageTiling(2500, 1000).map(new PdfNavigationData(
      Map.of(target, new Destination(1, 20, 2400)), List.of(new LinkArea(1, 30, 1490, 15, 20, target))));
    assertEquals(List.of(new LinkArea(1, 30, 0, 15, 10, target),
      new LinkArea(2, 30, 990, 15, 10, target)), mapped.links());
    assertEquals(new Destination(1, 20, 900), mapped.destinations().get(target));
  }

  @Test
  void rectanglesEndingOrStartingOnACutDoNotCreateEmptyDuplicates() {
    var mapped = new PdfPageTiling(2500, 1000).map(new PdfNavigationData(Map.of(),
      List.of(new LinkArea(1, 30, 1490, 15, 10, target), new LinkArea(1, 50, 1500, 15, 10, target))));
    assertEquals(List.of(new LinkArea(2, 30, 990, 15, 10, target),
      new LinkArea(1, 50, 0, 15, 10, target)), mapped.links());
  }

  @Test
  void exactMultiplesDoNotCreateAnExtraPageAndPartialLastPagesKeepTheirTopAlignment() {
    assertEquals(2, new PdfPageTiling(2000, 1000).pageCount());
    var mapped = new PdfPageTiling(2500, 1000).map(new PdfNavigationData(Map.of(),
      List.of(new LinkArea(1, 30, 0, 15, 20, target))));
    assertEquals(List.of(new LinkArea(3, 30, 500, 15, 20, target)), mapped.links());
  }
}
