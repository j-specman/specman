package builder;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import specman.model.v002.AbstractStepModel_V002;
import specman.model.v002.BreakStepModel_V002;
import specman.model.v002.CatchSequenceModel_V002;
import specman.model.v002.DiagramModel_V002;
import specman.model.v002.DoWhileStepModel_V002;
import specman.model.v002.EditorContentModel_V002;
import specman.model.v002.IfElseStepModel_V002;
import specman.model.v002.IfStepModel_V002;
import specman.model.v002.CaseStepModel_V002;
import specman.model.v002.SimpleStepModel_V002;
import specman.model.v002.SubsequenceStepModel_V002;
import specman.model.v002.TextEditAreaModel_V002;
import specman.model.v002.WhileStepModel_V002;
import specman.model.v002.io.ModelParseException;
import specman.model.v002.io.ModelParser_V002;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static builder.BuilderFactory.branch;
import static builder.BuilderFactory.breakStep;
import static builder.BuilderFactory.caseStep;
import static builder.BuilderFactory.catchOf;
import static builder.BuilderFactory.diagramm;
import static builder.BuilderFactory.doWhileStep;
import static builder.BuilderFactory.ifElseStep;
import static builder.BuilderFactory.ifStep;
import static builder.BuilderFactory.sequence;
import static builder.BuilderFactory.simple;
import static builder.BuilderFactory.subsequenceStep;
import static builder.BuilderFactory.whileStep;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Self-test of the NSD test data builders: What the builders persist must be readable
 * by the model parser and contain what the builders were told to put into it.
 */
public class BuilderTest {

  @BeforeAll
  static void deleteOldNsdFiles() throws IOException {
    DiagrammBuilder.deleteOldNsdFiles();
  }

  /** A persisted two-step diagram is parsed back with the given ids, texts and consecutive numbers. */
  @Test
  void testPersistedSimpleStepsAreParsedBack() throws IOException, ModelParseException {
    File nsdFile = diagramm()
      .withMainSequence(sequence()
        .withStep(simple("first").withId("12345678"))
        .withStep(simple("second").withId("abcdef01")))
      .persist();

    List<AbstractStepModel_V002> steps = parse(nsdFile).queryAllSteps();

    assertEquals(2, steps.size());
    assertEquals("12345678", steps.get(0).id);
    assertEquals("1", steps.get(0).stepNumber);
    assertEquals("abcdef01", steps.get(1).id);
    assertEquals("2", steps.get(1).stepNumber);
  }

  /** While and do-while steps keep their type, and their body steps are numbered one level deeper. */
  @Test
  void testLoopSteps() throws IOException, ModelParseException {
    File nsdFile = diagramm()
      .withMainSequence(sequence()
        .withStep(whileStep().withLoopSequence(sequence().withStep(simple())))
        .withStep(doWhileStep().withLoopSequence(sequence().withStep(simple())).withBarWidth(25)))
      .persist();

    List<AbstractStepModel_V002> steps = parse(nsdFile).queryAllSteps();

    assertEquals(List.of("1", "1.1", "2", "2.1"), numbersOf(steps));
    assertEquals(WhileStepModel_V002.class, steps.get(0).getClass());
    assertEquals(DoWhileStepModel_V002.class, steps.get(2).getClass());
    assertEquals(25, ((DoWhileStepModel_V002) steps.get(2)).barWidth);
  }

  /** Each branch of if, if/else and case steps occupies its own number slot. */
  @Test
  void testBranchSteps() throws IOException, ModelParseException {
    File nsdFile = diagramm()
      .withMainSequence(sequence()
        .withStep(ifStep().withIfBranch(branch("yes").withStep(simple())))
        .withStep(ifElseStep()
          .withIfBranch(branch().withStep(simple()))
          .withElseBranch(branch().withStep(simple())))
        .withStep(caseStep()
          .withDefaultBranch(branch().withStep(simple()))
          .withCase(branch("a").withStep(simple()))
          .withCase(branch("b").withStep(simple())))
        .withStep(simple()))
      .persist();

    List<AbstractStepModel_V002> steps = parse(nsdFile).queryAllSteps();

    assertEquals(List.of("1", "1.1", "2", "2.1", "3.1", "4", "4.1", "5.1", "6.1", "7"), numbersOf(steps));
    assertEquals(IfStepModel_V002.class, steps.get(0).getClass());
    assertEquals(IfElseStepModel_V002.class, steps.get(2).getClass());
    assertEquals(CaseStepModel_V002.class, steps.get(5).getClass());
    assertEquals(2, ((CaseStepModel_V002) steps.get(5)).caseSequences.size());
  }

  /** Subsequence steps keep the flat numbering flag and number their steps one level deeper otherwise. */
  @Test
  void testSubsequenceSteps() throws IOException, ModelParseException {
    File nsdFile = diagramm()
      .withMainSequence(sequence()
        .withStep(BuilderFactory.subsequenceStep().withSubsequence(sequence().withStep(simple())))
        .withStep(BuilderFactory.subsequenceStep().withFlatNumbering(true).withSubsequence(sequence().withStep(simple()))))
      .persist();

    List<AbstractStepModel_V002> steps = parse(nsdFile).queryAllSteps();

    assertEquals("1.1", steps.get(1).stepNumber);
    assertEquals(SubsequenceStepModel_V002.class, steps.get(2).getClass());
    assertTrue(((SubsequenceStepModel_V002) steps.get(2)).flatNumbering);
  }

  /** A catch sequence in the main sequence is linked to its break step inside a loop via the break's id. */
  @Test
  void testBreakAndCatch() throws IOException, ModelParseException {
    BreakStepBuilder breakInLoop = breakStep("Not found").withId("b1a2c3d4");
    File nsdFile = diagramm()
      .withMainSequence(sequence()
        .withStep(whileStep().withLoopSequence(sequence().withStep(breakInLoop)))
        .withCatch(catchOf(breakInLoop).withStep(simple())))
      .persist();

    DiagramModel_V002 diagram = parse(nsdFile);

    List<AbstractStepModel_V002> steps = diagram.queryAllSteps();
    assertEquals(BreakStepModel_V002.class, steps.get(1).getClass());
    List<CatchSequenceModel_V002> catches = diagram.mainSequence.catchArea.catchSequences;
    assertEquals(1, catches.size());
    assertEquals("b1a2c3d4", catches.get(0).id);
    assertEquals(1, catches.get(0).steps.size());
    assertEquals(SimpleStepModel_V002.class, catches.get(0).steps.get(0).getClass());
  }

  /** A catch sequence takes its heading from the break step it catches, and so do its co-catches. */
  @Test
  void testCatchHeadingsAreTakenFromBreakSteps() throws IOException, ModelParseException {
    BreakStepBuilder notFound = breakStep("Not found");
    BreakStepBuilder noAccess = breakStep("No access");
    File nsdFile = diagramm()
      .withMainSequence(sequence()
        .withStep(whileStep().withLoopSequence(sequence().withStep(notFound).withStep(noAccess)))
        .withCatch(catchOf(notFound).withCoCatch(noAccess).withStep(simple())))
      .persist();

    CatchSequenceModel_V002 catchSequence = parse(nsdFile).mainSequence.catchArea.catchSequences.get(0);

    assertEquals("Not found", headingText(catchSequence.heading));
    assertEquals("No access", headingText(catchSequence.coCatches.get(0).heading));
  }

  private static String headingText(EditorContentModel_V002 heading) {
    return ((TextEditAreaModel_V002) heading.areas.get(0)).text;
  }

  private static List<String> numbersOf(List<AbstractStepModel_V002> steps) {
    return steps.stream().map(step -> step.stepNumber).toList();
  }

  private static DiagramModel_V002 parse(File nsdFile) throws IOException, ModelParseException {
    String nsdText = Files.readString(nsdFile.toPath(), StandardCharsets.UTF_8);
    return new ModelParser_V002().parse(nsdText);
  }
}
