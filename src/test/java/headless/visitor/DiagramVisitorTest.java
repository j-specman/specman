package headless.visitor;

import headless.HeadlessExtension;
import headless.ModelCopyLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import specman.editarea.AbstractListItemEditArea;
import specman.editarea.EditContainer;
import specman.editarea.ImageEditArea;
import specman.editarea.TableEditArea;
import specman.editarea.TextEditArea;
import specman.model.v002.DiagramModel_V002;
import specman.model.v002.io.ModelParser_V002;
import specman.model.v002.io.ModelRenumberer_V002;
import specman.view.AbstractSchrittView;
import specman.view.CaseSchrittView;
import specman.view.CatchBereich;
import specman.view.CatchSchrittSequenzView;
import specman.view.SchrittSequenzView;
import specman.visitor.DiagramVisitor;
import specman.visitor.IgnoreChildrenException;
import specman.visitor.StepNumberIndexCollector;
import specman.visitor.StopTraversalException;

import java.awt.Component;
import java.awt.Container;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A {@link DiagramVisitor} gets to see every step and edit area of the diagram, no matter how deeply nested in
 * if-else or case branches, catch areas, tables or list items. The tests traverse the reference model, which
 * contains all step types and constellations. What the visitor sees is compared with independent references:
 * the Swing component tree for the edit areas, the saved model for the steps.
 */
@ExtendWith(HeadlessExtension.class)
class DiagramVisitorTest {

  @BeforeEach
  void loadReferenceModel(@TempDir Path modelCopyDirectory) throws IOException {
    new ModelCopyLoader(specman).loadReferenceModelCopy(modelCopyDirectory);
  }

  @Test
  void testVisitorSeesAllTextAreas() {
    assertVisitorSeesAllInComponentTreeInAnyOrder(TextEditArea.class, collector -> collector.textAreas);
  }

  /** The model is an independent reference: It is traversed by its own code, not by the views' one. */
  @Test
  void testVisitorSeesTheSameStepsAsTheModel() throws Exception {
    Collector collector = new Collector();
    specman.traverseDiagram(collector);

    assertEquals(new ArrayList<>(stepNumbersSavedInModel().keySet()), idsOf(collector.steps));
  }

  @Test
  void testListAllStepsReturnsTheSameStepsAsTheModel() throws Exception {
    assertEquals(new ArrayList<>(stepNumbersSavedInModel().keySet()), idsOf(specman.listAllSteps()));
  }

  @Test
  void testStepNumberIndexMatchesTheNumbersSavedInTheModel() throws Exception {
    StepNumberIndexCollector indexCollector = new StepNumberIndexCollector();
    specman.traverseDiagram(indexCollector);

    assertEquals(stepNumbersSavedInModel(), indexCollector.index());
  }

  /** Step numbers by step id, as saved in the reference model file. */
  private static Map<String, String> stepNumbersSavedInModel() throws Exception {
    String modelText = Files.readString(ModelCopyLoader.referenceModelFile().toPath(), StandardCharsets.UTF_8);
    DiagramModel_V002 model = new ModelParser_V002().parse(modelText);
    return ModelRenumberer_V002.collectNumbers(model.mainSequence);
  }

  private static List<String> idsOf(List<AbstractSchrittView> steps) {
    return steps.stream().map(AbstractSchrittView::getId).toList();
  }

  @Test
  void testVisitorSeesAllImageAreas() {
    assertVisitorSeesAllInComponentTree(ImageEditArea.class, collector -> collector.imageAreas);
  }

  @Test
  void testVisitorSeesAllTables() {
    assertVisitorSeesAllInComponentTree(TableEditArea.class, collector -> collector.tables);
  }

  @Test
  void testVisitorSeesAllListItems() {
    assertVisitorSeesAllInComponentTree(AbstractListItemEditArea.class, collector -> collector.listItems);
  }

  @Test
  void testVisitorSeesAllEditContainers() {
    assertVisitorSeesAllInComponentTreeInAnyOrder(EditContainer.class, collector -> collector.editContainers);
  }

  /** The step sequences have no component of their own, so what is checked is that the sequences are
   * the ones the steps live in and that they are reported before their steps. A sequence without steps,
   * like the hidden else branch of an if step, is reported, too. */
  @Test
  void testVisitorSeesTheSequencesOfAllStepsBeforeTheirSteps() {
    Collector collector = traverseDiagram();

    assertSame(specman.getHauptSequenz(), collector.sequences.get(0));
    for (AbstractSchrittView step : collector.steps) {
      assertReportedBefore(collector, step.getParent(), step);
    }
    assertEquals(collector.sequences.size(), new HashSet<>(collector.sequences).size(), "no sequence is reported twice");
  }

  /** Each sequence has a catch area, except for the catch sequences themselves. */
  @Test
  void testVisitorSeesTheCatchAreaOfEachSequenceExceptCatchSequences() {
    Collector collector = traverseDiagram();

    List<SchrittSequenzView> sequencesWithCatchArea = collector.sequences.stream()
      .filter(sequence -> !(sequence instanceof CatchSchrittSequenzView))
      .toList();
    assertEquals(sequencesWithCatchArea.size(), collector.catchAreas.size());
    assertEquals(
      new HashSet<>(sequencesWithCatchArea),
      new HashSet<>(collector.catchAreas.stream().map(CatchBereich::getParent).toList()));
    collector.catchAreas.forEach(catchArea -> assertReportedBefore(collector, catchArea.getParent(), catchArea));
  }

  @Test
  void testVisitorCanKeepOutOfCatchAreas() {
    List<AbstractSchrittView> allSteps = specman.listAllSteps();
    List<AbstractSchrittView> stepsOutsideCatchAreas = allSteps.stream()
      .filter(step -> !isInCatchSequence(step))
      .toList();
    assertFalse(stepsOutsideCatchAreas.size() == allSteps.size(),
      "test setup: reference model must contain steps in catch sequences");

    Collector collector = new Collector() {
      @Override public void visitCatchArea(CatchBereich catchArea) throws IgnoreChildrenException {
        super.visitCatchArea(catchArea);
        throw new IgnoreChildrenException();
      }
    };
    specman.traverseDiagram(collector);

    assertEquals(stepsOutsideCatchAreas, collector.steps);
    assertFalse(collector.catchAreas.isEmpty(), "catch areas are still reported");
    assertTrue(collector.sequences.stream().noneMatch(CatchSchrittSequenzView.class::isInstance));
  }

  /** Ignoring the children of a step skips everything inside the step, its own edit areas included,
   * but not what follows the step. */
  @Test
  void testIgnoringTheChildrenOfAStepSkipsEverythingInsideTheStepOnly() {
    List<AbstractSchrittView> allSteps = specman.listAllSteps();
    CaseSchrittView caseStep = allSteps.stream()
      .filter(CaseSchrittView.class::isInstance)
      .map(CaseSchrittView.class::cast)
      .findFirst()
      .orElseThrow();
    AbstractSchrittView lastStep = specman.getHauptSequenz().getLastStep();
    assertFalse(isNestedIn(lastStep, caseStep), "test setup: a step must follow the case step");

    Collector collector = new Collector() {
      @Override public void visitStep(AbstractSchrittView step) throws IgnoreChildrenException, StopTraversalException {
        super.visitStep(step);
        if (step == caseStep) {
          throw new IgnoreChildrenException();
        }
      }
    };
    specman.traverseDiagram(collector);

    assertEquals(allSteps.stream().filter(step -> !isNestedIn(step, caseStep)).toList(), collector.steps);
    assertTrue(collector.steps.contains(caseStep));
    assertFalse(collector.textAreas.contains(caseStep.getFirstEditArea().asTextArea()));
    assertTrue(collector.textAreas.contains(lastStep.getFirstEditArea().asTextArea()));
  }

  @Test
  void testStoppingAtAStepEndsTheTraversalRightThere() {
    List<AbstractSchrittView> allSteps = specman.listAllSteps();
    AbstractSchrittView stopStep = allSteps.get(2);

    Collector collector = new Collector() {
      @Override public void visitStep(AbstractSchrittView step) throws IgnoreChildrenException, StopTraversalException {
        super.visitStep(step);
        if (step == stopStep) {
          throw new StopTraversalException();
        }
      }
    };
    specman.traverseDiagram(collector);

    assertEquals(allSteps.subList(0, 3), collector.steps);
    assertSame(stopStep, collector.visited.get(collector.visited.size() - 1));
  }

  /** The traversal is stopped from deep inside a table cell or a list item. */
  @Test
  void testStoppingAtAnEditAreaEndsTheTraversalRightThere() {
    List<TextEditArea> allTextAreas = traverseDiagram().textAreas;
    TextEditArea stopArea = allTextAreas.get(allTextAreas.size() / 2);

    Collector collector = new Collector() {
      @Override public void visitTextArea(TextEditArea textArea) throws StopTraversalException {
        super.visitTextArea(textArea);
        if (textArea == stopArea) {
          throw new StopTraversalException();
        }
      }
    };
    specman.traverseDiagram(collector);

    assertEquals(allTextAreas.subList(0, allTextAreas.size() / 2 + 1), collector.textAreas);
    assertSame(stopArea, collector.visited.get(collector.visited.size() - 1));
  }

  /**
   * Records every node the traversal reports to it, in the order the traversal reports them.
   * <p>
   * Some tests derive a variant from it which steers the traversal by throwing an {@link IgnoreChildrenException}
   * or a {@link StopTraversalException}. Java doesn't allow an overriding method to throw more checked exceptions
   * than the overridden one declares. That's why the recording methods of the nodes involved declare these
   * exceptions, although the collector itself never throws them.
   */
  private static class Collector implements DiagramVisitor {
    final List<Object> visited = new ArrayList<>();
    final List<SchrittSequenzView> sequences = new ArrayList<>();
    final List<AbstractSchrittView> steps = new ArrayList<>();
    final List<CatchBereich> catchAreas = new ArrayList<>();
    final List<EditContainer> editContainers = new ArrayList<>();
    final List<TextEditArea> textAreas = new ArrayList<>();
    final List<ImageEditArea> imageAreas = new ArrayList<>();
    final List<TableEditArea> tables = new ArrayList<>();
    final List<AbstractListItemEditArea> listItems = new ArrayList<>();

    @Override public void visitSequence(SchrittSequenzView sequence) { visited.add(sequence); sequences.add(sequence); }
    @Override public void visitStep(AbstractSchrittView step) throws IgnoreChildrenException, StopTraversalException { visited.add(step); steps.add(step); }
    @Override public void visitCatchArea(CatchBereich catchArea) throws IgnoreChildrenException { visited.add(catchArea); catchAreas.add(catchArea); }
    @Override public void visitEditContainer(EditContainer editContainer) { visited.add(editContainer); editContainers.add(editContainer); }
    @Override public void visitTextArea(TextEditArea textArea) throws StopTraversalException { visited.add(textArea); textAreas.add(textArea); }
    @Override public void visitImageArea(ImageEditArea imageArea) { visited.add(imageArea); imageAreas.add(imageArea); }
    @Override public void visitTable(TableEditArea table) { visited.add(table); tables.add(table); }
    @Override public void visitListItem(AbstractListItemEditArea listItem) { visited.add(listItem); listItems.add(listItem); }
  }

  private static void assertReportedBefore(Collector collector, Object container, Object content) {
    int containerIndex = collector.visited.indexOf(container);
    assertTrue(containerIndex >= 0, container + " was not reported");
    assertTrue(containerIndex < collector.visited.indexOf(content), container + " must be reported before " + content);
  }

  /** True for steps in a catch sequence, as well as for steps nested deeper in a step of a catch sequence. */
  private static boolean isInCatchSequence(AbstractSchrittView step) {
    SchrittSequenzView sequence = step.getParent();
    while (sequence != null) {
      if (sequence instanceof CatchSchrittSequenzView) {
        return true;
      }
      AbstractSchrittView owner = sequence.getParent();
      sequence = (owner == null) ? null : owner.getParent();
    }
    return false;
  }

  /** True for steps somewhere inside the ancestor step, i.e. in one of its sequences. */
  private static boolean isNestedIn(AbstractSchrittView step, AbstractSchrittView ancestor) {
    SchrittSequenzView sequence = step.getParent();
    while (sequence != null) {
      AbstractSchrittView owner = sequence.getParent();
      if (owner == ancestor) {
        return true;
      }
      sequence = (owner == null) ? null : owner.getParent();
    }
    return false;
  }

  /**
   * The component tree is independent of the diagram structure and provides a reference for the edit areas
   * of a type, in layout order.
   */
  private static <T extends Component> void assertVisitorSeesAllInComponentTree(
      Class<T> type, Function<Collector, List<T>> visitedByCollector) {
    assertEquals(componentsOfType(type), visitedByCollector.apply(traverseDiagram()));
  }

  /**
   * Like {@link #assertVisitorSeesAllInComponentTree}, but ignoring the order. The component tree lists what
   * is placed side by side on screen, e.g. the headings of all branches of an if-else step, in front of
   * their branches' steps. The visitor reports each heading right in front of the steps of its branch.
   */
  private static <T extends Component> void assertVisitorSeesAllInComponentTreeInAnyOrder(
      Class<T> type, Function<Collector, List<T>> visitedByCollector) {
    List<T> inComponentTree = componentsOfType(type);
    List<T> visited = visitedByCollector.apply(traverseDiagram());
    assertEquals(inComponentTree.size(), visited.size());
    assertEquals(new HashSet<>(inComponentTree), new HashSet<>(visited));
  }

  private static Collector traverseDiagram() {
    Collector collector = new Collector();
    specman.traverseDiagram(collector);
    return collector;
  }

  private static <T extends Component> List<T> componentsOfType(Class<T> type) {
    List<T> result = new ArrayList<>();
    collectComponents(specman.getArbeitsbereich(), type, result);
    assertFalse(result.isEmpty(), "test setup: reference model must contain " + type.getSimpleName() + "s");
    return result;
  }

  private static <T extends Component> void collectComponents(Container container, Class<T> type, List<T> result) {
    for (Component child : container.getComponents()) {
      if (type.isInstance(child)) {
        result.add(type.cast(child));
      }
      if (child instanceof Container childContainer) {
        collectComponents(childContainer, type, result);
      }
    }
  }
}
