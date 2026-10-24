package specman.view;

import specman.editarea.EditContainer;
import specman.editarea.TextEditArea;
import specman.pdf.navigation.PdfNavigationPlan;
import specman.pdf.navigation.PdfTextReferences;
import specman.pdf.Shape;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static specman.pdf.navigation.PdfNavigationPlan.*;

/** Reads one current diagram on the EDT, without scrolling, unfolding, registering links or saving.
 * Called by ExportPDFSpecmanOp before building the diagram's shape tree. */
public final class PdfNavigationCollector {
  /** View bindings are separate from the immutable semantic plan and live for one export only. */
  public record Capture(PdfNavigationPlan plan, Map<AbstractSchrittView, TargetId> steps,
                        Map<CatchUeberschrift, TargetId> headings, Map<TextEditArea, String> textAreas) {
    public Capture {
      steps = identityCopy(steps);
      headings = identityCopy(headings);
      textAreas = identityCopy(textAreas);
    }
    private static <K, V> Map<K, V> identityCopy(Map<K, V> source) {
      return Collections.unmodifiableMap(new IdentityHashMap<>(source));
    }

    /** Bind identities once, before passing the shape tree to the view-independent renderer. */
    public void bindNavigation(Shape diagram) {
      Map<Object, TargetId> targets = new IdentityHashMap<>();
      Map<Object, TargetId> labels = new IdentityHashMap<>();
      // The header container starts before its text and includes the exported step number.
      steps.forEach((step, id) -> bindHeader(step.getTextShef(), id, targets, labels));
      headings.forEach((heading, id) -> bindHeader(heading.ueberschrift, id, targets, labels));
      bindShapes(diagram, targets, textAreas, labels);
    }

    private static void bindHeader(EditContainer header, TargetId id,
                                   Map<Object, TargetId> targets, Map<Object, TargetId> labels) {
      targets.put(header, id);
      if (header.getExportedStepNumber() != null) labels.put(header.getExportedStepNumber(), id);
    }
  }

  static void bindShapes(Shape shape, Map<?, TargetId> targets, Map<?, String> textAreas) {
    bindShapes(shape, targets, textAreas, Collections.emptyMap());
  }

  static void bindShapes(Shape shape, Map<?, TargetId> targets, Map<?, String> textAreas,
                         Map<?, TargetId> labels) {
    shape.withNavigationTarget(targets.get(shape.getSource()));
    shape.withNavigationTextKey(textAreas.get(shape.getSource()));
    TargetId label = shape.getSource() == null ? null : labels.get(shape.getSource());
    if (label != null) shape.withNavigationTextKey(Source.label(label, true).key());
    for (Shape child : shape.getSubshapes()) bindShapes(child, targets, textAreas, labels);
  }

  private final Builder builder = PdfNavigationPlan.builder();
  private final Map<AbstractSchrittView, TargetId> steps = new IdentityHashMap<>();
  private final Map<CatchUeberschrift, TargetId> headings = new IdentityHashMap<>();
  private final Map<TextEditArea, String> textAreas = new IdentityHashMap<>();
  private final List<CatchUeberschrift> catchConnections = new ArrayList<>();

  private PdfNavigationCollector() {}

  public static Capture collect(SchrittSequenzView mainSequence, EditContainer intro, EditContainer outro) {
    if (!SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Capture live diagram navigation on the Swing EDT");
    }
    PdfNavigationCollector collector = new PdfNavigationCollector();
    collector.content(Objects.requireNonNull(intro), null, true);
    collector.sequence(Objects.requireNonNull(mainSequence), null, true);
    collector.content(Objects.requireNonNull(outro), null, true);
    collector.connectCatches();
    return new Capture(collector.builder.build(), collector.steps, collector.headings, collector.textAreas);
  }

  private void sequence(SchrittSequenzView sequence, TargetId parent, boolean ancestorsVisible) {
    boolean visible = ancestorsVisible && sequence.sequenzBereich.isVisible();
    for (AbstractSchrittView step : sequence.getSchritte()) {
      TargetId id = TargetId.step(step.getId());
      steps.put(step, id);
      builder.target(new Target(id, step.getNumber().toString(), parent, visible));
      content(step.getTextShef(), id, visible);
      for (SchrittSequenzView child : step.unterSequenzen()) {
        if (child instanceof ZweigSchrittSequenzView branch) {
          // Branch headings are drawn in the condition area even when their bodies are folded.
          content(branch.getUeberschrift(), id, visible);
        }
        sequence(child, id, visible);
      }
    }
    CatchBereich area = sequence.getCatchBereich();
    if (area != null) catches(area, parent, visible);
  }

  private void catches(CatchBereich area, TargetId parent, boolean ancestorsVisible) {
    boolean areaVisible = ancestorsVisible && area.bereichPanel.isVisible();
    boolean contentVisible = areaVisible && area.sequencesPanel.isVisible();
    for (CatchSchrittSequenzView catchSequence : area.catchSequences) {
      boolean headingVisible = contentVisible && catchSequence.sequenzBereich.isVisible();
      heading(catchSequence.primaryCatchHeading, parent, headingVisible);
      for (CatchUeberschrift heading : catchSequence.coCatchHeadings) heading(heading, parent, headingVisible);
      sequence(catchSequence, parent, contentVisible);
    }
  }

  private void heading(CatchUeberschrift heading, TargetId parent, boolean visible) {
    TargetId id = TargetId.catchHeading(Integer.toString(headings.size()));
    headings.put(heading, id);
    builder.target(new Target(id, heading.linkedBreakStepId().toString(), parent, visible));
    content(heading.ueberschrift, id, visible);
    catchConnections.add(heading);
  }

  private void content(EditContainer container, TargetId owner, boolean visible) {
    for (TextEditArea area : container.getTextAreas()) {
      if (textAreas.containsKey(area)) throw new IllegalArgumentException("Text area visited twice");
      String key = "text:" + textAreas.size();
      textAreas.put(area, key);
      for (PdfTextReferences.Reference reference : PdfTextReferences.read(area.getWrappedDocument())) {
        builder.textReference(new Source(key, owner, reference.start(), reference.end(), visible), reference.text());
      }
    }
  }

  private void connectCatches() {
    for (CatchUeberschrift heading : catchConnections) {
      TargetId catchId = headings.get(heading);
      TargetId breakId = steps.get(heading.linkedBreakStep);
      if (breakId == null || heading.linkedBreakStep.catchHeading != heading) {
        throw new IllegalArgumentException("Inconsistent break/catch relation: " + heading.linkedBreakStepUUID());
      }
      // Source visibility includes the actual label flag; target visibility belongs to the plan.
      builder.connection(LinkKind.BREAK_TO_CATCH,
        Source.label(breakId, visibleWithin(heading.linkedBreakStep.getPanel())
          && heading.linkedBreakStep.getTextShef().hasExportedStepNumber()), catchId);
      builder.connection(LinkKind.CATCH_TO_BREAK,
        Source.label(catchId, visibleWithin(heading) && heading.ueberschrift.hasExportedStepNumber()), breakId);
    }
  }

  /** Only checks content flags, not isShowing(), focus, viewport clipping or window visibility. */
  static boolean visibleWithin(java.awt.Component component) {
    for (java.awt.Component current = component; current != null && !(current instanceof java.awt.Window);
         current = current.getParent()) {
      if (!current.isVisible()) return false;
    }
    return true;
  }
}
