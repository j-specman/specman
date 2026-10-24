package specman.pdf.navigation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static specman.pdf.navigation.PdfNavigationPlan.*;

class PdfNavigationRenderContextTest {
  private final TargetId target = TargetId.step("target");
  private final PdfNavigationRenderContext.GlyphStyle style =
    new PdfNavigationRenderContext.GlyphStyle("Serif", 0, 14, 0, null, false, false);

  @Test
  void multilineAndIncompleteReferencesNeverProducePartialLinks() {
    var builder = PdfNavigationPlan.builder().target(new Target(target, "12.3", null, true));
    builder.textReference(new Source("multiline", null, 0, 4, true), "12.3");
    builder.textReference(new Source("partial", null, 0, 4, true), "12.3");
    var context = new PdfNavigationRenderContext(builder.build());
    context.target(target, 10, 700);
    for (int i = 0; i < 4; i++) glyph(context, "multiline", i, "12.3".substring(i, i + 1), i < 2 ? 600 : 580);
    glyph(context, "partial", 0, "1", 500);
    assertTrue(context.finish(600, 800).links().isEmpty());
  }

  @Test
  void targetsMustActuallyBeRenderedAndInsideThePage() {
    var plan = PdfNavigationPlan.builder().target(new Target(target, "1", null, true))
      .textReference(new Source("text", null, 0, 1, true), "1").build();
    var context = new PdfNavigationRenderContext(plan);
    glyph(context, "text", 0, "1", 500);
    assertTrue(context.finish(600, 800).links().isEmpty(), "No invented position for an undrawn target");
    context.target(target, 10, 900);
    assertTrue(context.finish(600, 800).links().isEmpty(), "No active target outside the page");
  }

  @Test
  void hiddenSourcesAndUndrawnLabelsRemainInactive() {
    TargetId hidden = TargetId.step("hidden");
    TargetId heading = TargetId.catchHeading("heading");
    var plan = PdfNavigationPlan.builder().target(new Target(target, "1", null, true))
      .target(new Target(hidden, "1.1", target, false))
      .target(new Target(heading, "1", null, true))
      .textReference(new Source("hidden-source", hidden, 0, 1, true), "1")
      .textReference(new Source("ancestor", null, 0, 3, true), "1.1")
      .connection(LinkKind.BREAK_TO_CATCH, Source.label(target, true), heading).build();
    var context = new PdfNavigationRenderContext(plan);
    context.target(target, 10, 700);
    context.target(hidden, 10, 600); // Even an accidentally drawn hidden catch child stays inactive.
    glyph(context, "hidden-source", 0, "1", 500);
    for (int i = 0; i < 3; i++) glyph(context, "ancestor", i, "1.1".substring(i, i + 1), 400);
    var data = context.finish(600, 800);
    assertEquals(1, data.destinations().size());
    assertEquals(1, data.links().size());
    assertEquals(target, data.links().getFirst().target());
  }

  @Test
  void breakAndCatchLabelsUseTheirOwnGeometryAndTheOppositeHeader() {
    TargetId heading = TargetId.catchHeading("heading");
    var plan = PdfNavigationPlan.builder().target(new Target(target, "1", null, true))
      .target(new Target(heading, "1", null, true))
      .connection(LinkKind.BREAK_TO_CATCH, Source.label(target, true), heading)
      .connection(LinkKind.CATCH_TO_BREAK, Source.label(heading, true), target).build();
    var context = new PdfNavigationRenderContext(plan);
    context.target(target, 10, 700);
    context.target(heading, 20, 200);
    context.label(Source.label(target, true).key(), 15, 680, 8, 12);
    context.label(Source.label(heading, true).key(), 25, 180, 8, 12);
    var data = context.finish(600, 800);
    assertEquals(2, data.destinations().size());
    assertEquals(java.util.List.of(new PdfNavigationData.LinkArea(1, 15, 680, 8, 12, heading),
      new PdfNavigationData.LinkArea(1, 25, 180, 8, 12, target)), data.links());
  }

  @Test
  void bothDirectionsUseTheNearestExportedNumberedAncestorOfAHiddenTarget() {
    TargetId parent = TargetId.step("parent"), heading = TargetId.catchHeading("heading");
    for (boolean hideCatch : new boolean[] {true, false}) {
      var plan = PdfNavigationPlan.builder().target(new Target(parent, "1", null, true))
        .target(new Target(target, "1.1", parent, hideCatch))
        .target(new Target(heading, "1.1", parent, !hideCatch))
        .connection(LinkKind.BREAK_TO_CATCH, Source.label(target, true), heading)
        .connection(LinkKind.CATCH_TO_BREAK, Source.label(heading, true), target).build();
      var context = new PdfNavigationRenderContext(plan);
      context.target(parent, 10, 700);
      context.target(target, 20, 600);
      context.target(heading, 30, 200);
      context.label(Source.label(target, true).key(), 25, 580, 8, 12);
      context.label(Source.label(heading, true).key(), 35, 180, 8, 12);
      var links = context.finish(600, 800).links();
      assertEquals(1, links.size(), "The hidden source must stay inactive");
      assertEquals(parent, links.getFirst().target());
      assertEquals(hideCatch ? 25 : 35, links.getFirst().x());
    }
  }

  @Test
  void anUnlinkedLabelOrARootCatchWithoutAncestorDoesNotCreateAnActiveLink() {
    TargetId heading = TargetId.catchHeading("heading");
    var plan = PdfNavigationPlan.builder().target(new Target(target, "1", null, true))
      .target(new Target(heading, "1", null, false))
      .connection(LinkKind.BREAK_TO_CATCH, Source.label(target, true), heading).build();
    var context = new PdfNavigationRenderContext(plan);
    context.target(target, 10, 700);
    context.label(Source.label(target, true).key(), 15, 680, 8, 12);
    context.label("unlinked", 15, 500, 8, 12);
    assertTrue(context.finish(600, 800).links().isEmpty());
    assertEquals(Status.NO_VISIBLE_ANCESTOR, plan.links().getFirst().resolution().status());
  }

  @Test
  void nearestAncestorMustHaveAnActualInPagePosition() {
    TargetId middle = TargetId.step("middle"), child = TargetId.step("child");
    var plan = PdfNavigationPlan.builder().target(new Target(target, "1", null, true))
      .target(new Target(middle, "1.1", target, true))
      .target(new Target(child, "1.1.1", middle, false))
      .textReference(new Source("text", null, 0, 5, true), "1.1.1").build();
    var originalLinks = plan.links();
    for (int middleY : new int[] {600, 900, -1}) {
      var context = new PdfNavigationRenderContext(plan);
      context.target(target, 10, 700);
      if (middleY >= 0) context.target(middle, 20, middleY);
      text(context, "text", "1.1.1");
      assertEquals(middleY == 600 ? middle : target, context.finish(600, 800).links().getFirst().target());
    }
    assertEquals(originalLinks, plan.links());
    assertEquals(child, plan.links().getFirst().resolution().logicalTarget());
    assertEquals("1.1.1", plan.links().getFirst().text());
  }

  @Test
  void locallyVisibleChildInsideHiddenParentCannotOverrideTheExportedAncestor() {
    TargetId middle = TargetId.step("middle"), child = TargetId.step("child");
    var plan = PdfNavigationPlan.builder().target(new Target(target, "1", null, true))
      .target(new Target(middle, "1.1", target, false))
      .target(new Target(child, "1.1.1", middle, true))
      .textReference(new Source("text", null, 0, 5, true), "1.1.1").build();
    var context = new PdfNavigationRenderContext(plan);
    context.target(target, 10, 700);
    context.target(middle, 20, 600);
    context.target(child, 30, 500);
    text(context, "text", "1.1.1");
    assertEquals(target, context.finish(600, 800).links().getFirst().target());
  }

  @Test
  void hiddenRootCatchWithoutAncestorRemainsValidButInactiveEvenIfOtherStepsAreDrawn() {
    TargetId catchChild = TargetId.step("catch-child");
    var plan = PdfNavigationPlan.builder().target(new Target(target, "1", null, true))
      .target(new Target(catchChild, "1.1", null, false))
      .textReference(new Source("text", null, 0, 3, true), "1.1").build();
    var context = new PdfNavigationRenderContext(plan);
    context.target(target, 10, 700);
    context.target(catchChild, 10, 600);
    text(context, "text", "1.1");
    assertTrue(context.finish(600, 800).links().isEmpty());
    assertEquals(Status.NO_VISIBLE_ANCESTOR, plan.links().getFirst().resolution().status());
    assertEquals(catchChild, plan.links().getFirst().resolution().logicalTarget());
  }

  private void text(PdfNavigationRenderContext context, String key, String text) {
    for (int i = 0; i < text.length(); i++) glyph(context, key, i, text.substring(i, i + 1), 400);
  }

  private void glyph(PdfNavigationRenderContext context, String key, int offset, String ch, float baseline) {
    context.glyph(key, offset, ch, 20 + offset * 8, baseline, 8, 12, 3, style);
  }
}
