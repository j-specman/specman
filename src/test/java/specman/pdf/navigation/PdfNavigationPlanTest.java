package specman.pdf.navigation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static specman.pdf.navigation.PdfNavigationPlan.*;

class PdfNavigationPlanTest {
  private final TargetId first = TargetId.step("first");
  private final TargetId second = TargetId.step("second");

  @Test
  void forwardBackwardRepeatedAndSelfReferencesKeepTheirIdentity() {
    PdfNavigationPlan plan = PdfNavigationPlan.builder()
      .target(new Target(first, "1", null, true))
      .target(new Target(second, "2", null, true))
      .textReference(text("intro", null, 0, "2", true), "2")
      .textReference(text("step-1", first, 5, "2", true), "2")
      .textReference(text("step-2", second, 0, "1", true), "1")
      .textReference(text("step-2", second, 4, "2", true), "2")
      .build();

    assertEquals(List.of(second, second, first, second), plan.links().stream()
      .map(link -> link.resolution().logicalTarget()).toList());
    assertTrue(plan.links().stream().allMatch(link -> link.resolution().status() == Status.DIRECT));
    assertTrue(plan.links().stream().allMatch(Link::hasVisibleSourceAndTarget));
    assertEquals(5, plan.links().get(1).source().start());
  }

  @Test
  void aFreshSnapshotAfterRenumberingResolvesTheSameStepId() {
    Builder builder = PdfNavigationPlan.builder()
      .target(new Target(first, "1", null, true));
    PdfNavigationPlan before = builder.build();
    PdfNavigationPlan after = PdfNavigationPlan.builder()
      .target(new Target(first, "7.2", null, true)).build();

    assertEquals(first, before.resolveStepNumber("1").logicalTarget());
    assertEquals(first, after.resolveStepNumber("7.2").logicalTarget());
    assertEquals(Status.BROKEN_REFERENCE, after.resolveStepNumber("1").status());
    assertEquals("1", before.targets().get(first).number());
    builder.target(new Target(second, "2", null, true));
    assertEquals(1, before.targets().size());
    assertThrows(UnsupportedOperationException.class, () -> before.targets().clear());
    assertThrows(UnsupportedOperationException.class, () -> before.links().clear());
  }

  @Test
  void missingAndExplicitlyBrokenReferencesHaveNoDestination() {
    PdfNavigationPlan plan = PdfNavigationPlan.builder()
      .target(new Target(first, "1", null, true)).build();
    for (String number : new String[] {"1?", "999", "", null}) {
      Resolution result = plan.resolveStepNumber(number);
      assertEquals(Status.BROKEN_REFERENCE, result.status());
      assertNull(result.logicalTarget());
      assertNull(result.visibleTarget());
    }
    assertEquals(Status.DIRECT, plan.resolveStepNumber("1").status());
  }

  @Test
  void nearestVisibleAncestorIsUsedAcrossMultipleCollapsedLevels() {
    TargetId middle = TargetId.step("middle");
    PdfNavigationPlan plan = PdfNavigationPlan.builder()
      .target(new Target(first, "1", null, true))
      .target(new Target(middle, "1.1", first, false))
      // A child's own visible flag cannot override a hidden ancestor.
      .target(new Target(second, "1.1.1", middle, true))
      .textReference(text("hidden-text", second, 0, "1", true), "1")
      .build();
    Resolution result = plan.resolveStepNumber("1.1.1");
    assertEquals(Status.VISIBLE_ANCESTOR, result.status());
    assertEquals(second, result.logicalTarget());
    assertEquals(first, result.visibleTarget());
    assertFalse(plan.isVisible(second));
    assertFalse(plan.links().getFirst().hasVisibleSourceAndTarget());
    assertEquals(Status.DIRECT, plan.links().getFirst().resolution().status());
  }

  @Test
  void hiddenTopLevelCatchIsPresentButHasNoInventedAncestor() {
    TargetId catchId = TargetId.catchHeading("primary");
    TargetId catchChild = TargetId.step("catch-child");
    PdfNavigationPlan plan = PdfNavigationPlan.builder()
      .target(new Target(first, "1", null, true))
      .target(new Target(catchId, "1", null, false))
      .target(new Target(catchChild, "1.1", null, false))
      .connection(LinkKind.BREAK_TO_CATCH, Source.label(first, true), catchId)
      .textReference(text("intro", null, 0, "1.1", true), "1.1")
      .build();
    Resolution result = plan.links().getFirst().resolution();
    assertEquals(Status.NO_VISIBLE_ANCESTOR, result.status());
    assertEquals(catchId, result.logicalTarget());
    assertNull(result.visibleTarget());
    assertFalse(plan.links().getFirst().hasVisibleSourceAndTarget());
    assertEquals(Status.NO_VISIBLE_ANCESTOR, plan.links().get(1).resolution().status());
    assertEquals(catchChild, plan.links().get(1).resolution().logicalTarget());
    assertFalse(plan.links().get(1).hasVisibleSourceAndTarget());
  }

  @Test
  void primaryAndCoCatchHeadingsAreDistinctAndBidirectional() {
    TargetId primary = TargetId.catchHeading("first"); // Different namespace from the step ID.
    TargetId coCatch = TargetId.catchHeading("co");
    Builder builder = PdfNavigationPlan.builder()
      .target(new Target(first, "1", null, true))
      .target(new Target(second, "2", null, true))
      .target(new Target(primary, "1", null, true))
      .target(new Target(coCatch, "2", null, true));
    builder.connection(LinkKind.BREAK_TO_CATCH, Source.label(first, true), primary)
      .connection(LinkKind.CATCH_TO_BREAK, Source.label(primary, true), first)
      .connection(LinkKind.BREAK_TO_CATCH, Source.label(second, true), coCatch)
      .connection(LinkKind.CATCH_TO_BREAK, Source.label(coCatch, true), second);
    PdfNavigationPlan plan = builder.build();
    assertEquals(4, plan.targets().size());
    assertEquals(List.of(primary, first, coCatch, second), plan.links().stream()
      .map(link -> link.resolution().logicalTarget()).toList());
    assertEquals(first, plan.resolveStepNumber("1").logicalTarget());
  }

  @Test
  void hiddenCatchUsesEnclosingStepAndNeverTheLinkedBreak() {
    TargetId catchId = TargetId.catchHeading("catch");
    PdfNavigationPlan plan = PdfNavigationPlan.builder()
      .target(new Target(first, "1", null, true))
      .target(new Target(second, "1.1", first, true))
      .target(new Target(catchId, "1.1", first, false)).build();
    assertEquals(first, plan.resolve(catchId).visibleTarget());
    assertEquals(catchId, plan.resolve(catchId).logicalTarget());
  }

  @Test
  void duplicateIdsNumbersMissingParentsAndCyclesAreRejected() {
    assertThrows(IllegalArgumentException.class, () -> PdfNavigationPlan.builder()
      .target(new Target(first, "1", null, true)).target(new Target(first, "2", null, true)));
    assertThrows(IllegalArgumentException.class, () -> PdfNavigationPlan.builder()
      .target(new Target(first, "1", null, true)).target(new Target(second, "1", null, true)).build());
    assertThrows(IllegalArgumentException.class, () -> PdfNavigationPlan.builder()
      .target(new Target(first, "1", second, true)).build());
    assertThrows(IllegalArgumentException.class, () -> PdfNavigationPlan.builder()
      .target(new Target(first, "1", second, true)).target(new Target(second, "1.1", first, true)).build());
    assertThrows(IllegalArgumentException.class, () -> PdfNavigationPlan.builder()
      .connection(LinkKind.BREAK_TO_CATCH, Source.label(first, true), second).build());
  }

  @Test
  void diagramsWithoutReferencesAndRepeatedExportsHaveNoStaleLinks() {
    assertTrue(PdfNavigationPlan.builder().build().links().isEmpty());
    PdfNavigationPlan plan = PdfNavigationPlan.builder()
      .target(new Target(first, "1", null, true)).build();
    assertTrue(plan.links().isEmpty());
    assertFalse(PdfNavigationPlan.builder().build().targets().containsKey(first));
  }

  private Source text(String key, TargetId owner, int start, String text, boolean visible) {
    return new Source(key, owner, start, start + text.length(), visible);
  }
}
