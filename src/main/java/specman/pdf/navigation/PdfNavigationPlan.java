package specman.pdf.navigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Immutable, export-local semantic navigation data. No PDF coordinates or live UI objects. */
public final class PdfNavigationPlan {
  public enum TargetKind { STEP, CATCH_HEADING }
  public enum LinkKind { TEXT, BREAK_TO_CATCH, CATCH_TO_BREAK }
  public enum Status {
    DIRECT, VISIBLE_ANCESTOR, BROKEN_REFERENCE,
    /** Valid hidden target without a numbered visible ancestor: intentionally no active link. */
    NO_VISIBLE_ANCESTOR
  }
  public enum IssueKind { CATCH_EXPORT_VISIBILITY_MISMATCH }

  public record TargetId(TargetKind kind, String value) {
    public TargetId {
      Objects.requireNonNull(kind);
      if (value == null || value.isBlank()) throw new IllegalArgumentException("Empty target ID");
    }
    public static TargetId step(String id) { return new TargetId(TargetKind.STEP, id); }
    public static TargetId catchHeading(String id) { return new TargetId(TargetKind.CATCH_HEADING, id); }
  }

  /** parent is the nearest enclosing numbered step, never the linked break or an arbitrary sibling.
   * visible describes the current expanded content, independent of screen/window visibility.
   * The renderer must later confirm the corresponding actually rendered destination. */
  public record Target(TargetId id, String number, TargetId parent, boolean visible) {
    public Target { Objects.requireNonNull(id); }
  }

  /** Logical text positions are start-inclusive/end-exclusive; labels use -1/-1.
   * owner is null for intro/outro. key binds this source to a view only in the collector. */
  public record Source(String key, TargetId owner, int start, int end, boolean visible) {
    public Source {
      if (key == null || key.isBlank()) throw new IllegalArgumentException("Empty source key");
      if (!((start == -1 && end == -1) || (start >= 0 && end > start))) {
        throw new IllegalArgumentException("Invalid source range: " + start + ".." + end);
      }
    }
    public static Source label(TargetId target, boolean visible) {
      return new Source("label:" + target.kind() + ":" + target.value(), target, -1, -1, visible);
    }
  }

  public record Resolution(Status status, TargetId logicalTarget, TargetId visibleTarget) {
    public boolean resolved() { return visibleTarget != null; }
  }

  public record Link(LinkKind kind, Source source, String text, Resolution resolution) {
    public boolean hasVisibleSourceAndTarget() { return source.visible() && resolution.resolved(); }
  }

  /** Recorded for later export integration; semantic preparation never repairs the existing exporter. */
  public record Issue(IssueKind kind, String context) {}

  private final Map<TargetId, Target> targets;
  private final Map<String, TargetId> stepNumbers;
  private final List<Link> links;
  private final List<Issue> issues;

  private PdfNavigationPlan(Builder builder) {
    targets = Collections.unmodifiableMap(new LinkedHashMap<>(builder.targets));
    Map<String, TargetId> numbers = new LinkedHashMap<>();
    for (Target target : targets.values()) {
      if (target.id().kind() == TargetKind.STEP) {
        if (target.number() == null || target.number().isBlank()) {
          throw new IllegalArgumentException("Missing step number: " + target.id());
        }
        if (numbers.putIfAbsent(target.number(), target.id()) != null) {
          throw new IllegalArgumentException("Duplicate step number: " + target.number());
        }
      }
      validateParents(target);
    }
    stepNumbers = Map.copyOf(numbers);
    List<Link> resolvedLinks = new ArrayList<>();
    for (PendingLink link : builder.links) {
      if (link.source().owner() != null && !targets.containsKey(link.source().owner())) {
        throw new IllegalArgumentException("Unknown source owner: " + link.source().owner());
      }
      if (link.kind() != LinkKind.TEXT && !targets.containsKey(link.target())) {
        throw new IllegalArgumentException("Unknown break/catch target: " + link.target());
      }
      Resolution resolution = link.kind() == LinkKind.TEXT
        ? resolveStepNumber(link.text()) : resolve(link.target());
      Source source = link.source();
      if (source.owner() != null && !isVisible(source.owner())) {
        source = new Source(source.key(), source.owner(), source.start(), source.end(), false);
      }
      resolvedLinks.add(new Link(link.kind(), source, link.text(), resolution));
    }
    links = List.copyOf(resolvedLinks);
    issues = List.copyOf(builder.issues);
  }

  private void validateParents(Target target) {
    Set<TargetId> visited = new HashSet<>();
    Target current = target;
    while (current != null) {
      if (!visited.add(current.id())) throw new IllegalArgumentException("Cyclic hierarchy: " + target.id());
      TargetId parent = current.parent();
      if (parent != null && (parent.kind() != TargetKind.STEP || !targets.containsKey(parent))) {
        throw new IllegalArgumentException("Unknown/non-step parent: " + parent);
      }
      current = targets.get(parent);
    }
  }

  public Map<TargetId, Target> targets() { return targets; }
  public List<Link> links() { return links; }
  public List<Issue> issues() { return issues; }

  /** Exact current-number lookup, matching the UI; ordinary unmarked numbers are never submitted. */
  public Resolution resolveStepNumber(String number) {
    return resolve(number == null ? null : stepNumbers.get(number));
  }

  /** Also accounts for a locally visible child inside an invisible outer step. */
  public boolean isVisible(TargetId id) {
    Target current = targets.get(id);
    if (current == null) return false;
    while (current != null) {
      if (!current.visible()) return false;
      current = targets.get(current.parent());
    }
    return true;
  }

  public Resolution resolve(TargetId id) {
    Target target = targets.get(id);
    if (target == null) return new Resolution(Status.BROKEN_REFERENCE, null, null);
    Target candidate = target;
    while (candidate != null) {
      if (isVisible(candidate.id())) {
        return new Resolution(candidate == target ? Status.DIRECT : Status.VISIBLE_ANCESTOR,
          target.id(), candidate.id());
      }
      candidate = targets.get(candidate.parent());
    }
    return new Resolution(Status.NO_VISIBLE_ANCESTOR, target.id(), null);
  }

  public static Builder builder() { return new Builder(); }

  private record PendingLink(LinkKind kind, Source source, String text, TargetId target) {}

  public static final class Builder {
    private final Map<TargetId, Target> targets = new LinkedHashMap<>();
    private final List<PendingLink> links = new ArrayList<>();
    private final List<Issue> issues = new ArrayList<>();

    public Builder target(Target target) {
      Objects.requireNonNull(target);
      if (targets.putIfAbsent(target.id(), target) != null) {
        throw new IllegalArgumentException("Duplicate target ID: " + target.id());
      }
      return this;
    }

    public Builder textReference(Source source, String text) {
      links.add(new PendingLink(LinkKind.TEXT, Objects.requireNonNull(source), Objects.requireNonNull(text), null));
      return this;
    }

    public Builder connection(LinkKind kind, Source source, TargetId target) {
      if (kind == null || kind == LinkKind.TEXT) throw new IllegalArgumentException("Expected break/catch connection");
      links.add(new PendingLink(kind, Objects.requireNonNull(source), null, Objects.requireNonNull(target)));
      return this;
    }

    public Builder issue(Issue issue) { issues.add(Objects.requireNonNull(issue)); return this; }
    public PdfNavigationPlan build() { return new PdfNavigationPlan(this); }
  }
}
