package headless.image.classman;

import headless.ModelCopyLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import specman.ChangeSet;
import specman.editarea.EditContainer;
import specman.editarea.image.classman.ClassmanImageEditArea;
import specman.editarea.image.classman.LinkedChangesAnswer;
import specman.link.classman.ClassmanLink;
import specman.ops.buttons.MergeChangeSetsADBLOpButton;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static builder.BuilderFactory.diagramm;
import static builder.BuilderFactory.sequence;
import static builder.BuilderFactory.simple;
import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static specman.DiagramToolBar.ACCEPT_CHANGES_BUTTON;

/** Accepting, rejecting and merging change a linked diagram, and so its file, which other documents may link as well.
 * The user is asked before, if a linked diagram has marks of the change set. */
class LinkedClassmanChangeModeTest extends AbstractClassmanTest {

  private static final ChangeSet BLUE = ChangeSet.ALL.get("blue");
  private static final ChangeSet PINK = ChangeSet.ALL.get("pink");

  private static final String MARKED = "classdiagram {\n    class A\n    class B change(added, blue)\n}\n";

  private static ClassmanImageEditArea linkedDiagramWithSource(Path directory, String source) throws IOException {
    EditContainer container = containerOfNewStep();
    insertLinkedDiagram(diagramFile(directory, source));
    return (ClassmanImageEditArea) container.getEditAreaAt(1);
  }

  private static void startChangeMode(ChangeSet changeSet) {
    specman.toggleChangeMode();
    specman.selectChangeSet(changeSet);
  }

  /** Scenario: The user accepts the changes of a change set which a linked diagram has marks of, and continues after
   * being told. The marks are resolved. */
  @Test
  void testAcceptingAfterWarning(@TempDir Path directory) throws IOException {
    ClassmanImageEditArea diagram = linkedDiagramWithSource(directory, MARKED);
    startChangeMode(BLUE);

    specman.click(ACCEPT_CHANGES_BUTTON);

    assertEquals(List.of(1), specman.linkedChangesAskedFor(), "the user is asked once");
    assertFalse(diagram.getSource().contains("blue"), diagram.getSource());
  }

  /** Scenario: The user continues without changing the linked diagrams: The change set is accepted in the document, but
   * the linked diagram keeps its marks. The next accepting, with the linked diagrams, changes it, so the diagram was only
   * left alone for one operation. */
  @Test
  void testLinkedDiagramCanBeLeftAlone(@TempDir Path directory) throws IOException {
    ClassmanImageEditArea diagram = linkedDiagramWithSource(directory, MARKED);
    startChangeMode(BLUE);
    specman.answerLinkedChangesWith(LinkedChangesAnswer.LEAVE_LINKED);

    specman.click(ACCEPT_CHANGES_BUTTON);

    assertEquals(MARKED, diagram.getSource());
    specman.answerLinkedChangesWith(LinkedChangesAnswer.CHANGE_LINKED);
    specman.click(ACCEPT_CHANGES_BUTTON);
    assertFalse(diagram.getSource().contains("blue"), diagram.getSource());
  }

  /** Scenario: The user leaves the linked diagrams alone when merging. The marks in the linked diagram keep their name. */
  @Test
  void testLinkedDiagramCanBeLeftAloneWhenMerging(@TempDir Path directory) throws IOException {
    ClassmanImageEditArea diagram = linkedDiagramWithSource(directory, MARKED);
    startChangeMode(PINK);
    specman.answerLinkedChangesWith(LinkedChangesAnswer.LEAVE_LINKED);

    specman.findComponent(MergeChangeSetsADBLOpButton.class).mergeChangeSets(BLUE);

    assertEquals(MARKED, diagram.getSource());
  }

  /** Scenario: The user cancels. Nothing is changed, not even an undo step is left. */
  @Test
  void testCancelChangesNothing(@TempDir Path directory) throws IOException {
    ClassmanImageEditArea diagram = linkedDiagramWithSource(directory, MARKED);
    startChangeMode(BLUE);
    specman.answerLinkedChangesWith(LinkedChangesAnswer.CANCEL);

    specman.click(ACCEPT_CHANGES_BUTTON);

    assertEquals(MARKED, diagram.getSource());
    specman.undo();
    assertEquals(MARKED, diagram.getSource(), "the undo must not have undone the diagram itself");
  }

  /** Scenario: The linked diagram has marks of another change set only. Accepting leaves it alone, so the user isn't
   * asked. */
  @Test
  void testNoQuestionWithoutMarksOfTheChangeSet(@TempDir Path directory) throws IOException {
    linkedDiagramWithSource(directory, MARKED);
    startChangeMode(PINK);

    specman.click(ACCEPT_CHANGES_BUTTON);

    assertTrue(specman.linkedChangesAskedFor().isEmpty());
  }

  /** Scenario: An embedded diagram with marks is the document's own, so accepting needs no question. */
  @Test
  void testNoQuestionForEmbeddedDiagram() {
    ClassmanImageEditArea diagram = diagramWithSource(containerOfNewStep(), MARKED);
    startChangeMode(BLUE);

    specman.click(ACCEPT_CHANGES_BUTTON);

    assertTrue(specman.linkedChangesAskedFor().isEmpty());
    assertFalse(diagram.getSource().contains("blue"), diagram.getSource());
  }

  /** Scenario: Merging gives the marks in the linked file the name of the current change set, so the user is asked
   * as well. After cancelling the marks keep their name. */
  @Test
  void testCancelBeforeMerging(@TempDir Path directory) throws IOException {
    ClassmanImageEditArea diagram = linkedDiagramWithSource(directory, MARKED);
    startChangeMode(PINK);
    specman.answerLinkedChangesWith(LinkedChangesAnswer.CANCEL);

    specman.findComponent(MergeChangeSetsADBLOpButton.class).mergeChangeSets(BLUE);

    assertEquals(List.of(1), specman.linkedChangesAskedFor(), "the user is asked once");
    assertEquals(MARKED, diagram.getSource());
  }

  /** Scenario: Two diagrams link the same file. The second is a duplicate, which accepting leaves alone, so only the
   * first is counted. */
  @Test
  void testDuplicateIsNotCounted(@TempDir Path directory) throws IOException {
    File file = diagramFile(directory, MARKED);
    String link = ClassmanLink.of(file, "", null, null);
    File nsd = diagramm().withMainSequence(sequence()
      .withStep(simple("first").withLinkedClassmanDiagram(link))
      .withStep(simple("second").withLinkedClassmanDiagram(link)))
      .persist();
    new ModelCopyLoader(specman).loadModelCopy(nsd, directory);
    startChangeMode(BLUE);

    specman.click(ACCEPT_CHANGES_BUTTON);

    assertEquals(List.of(1), specman.linkedChangesAskedFor());
  }
}
