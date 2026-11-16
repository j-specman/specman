package headless.image.classman;

import headless.HeadlessExtension;
import net.atlanticbb.tantlinger.ui.text.actions.ImageAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import specman.editarea.EditContainer;
import specman.editarea.image.classman.ClassmanImageEditArea;
import specman.view.AbstractSchrittView;
import specman.view.EinfacherSchrittView;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static headless.HeadlessExtension.specman;
import static headless.pdf.PdfReader.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A Classman class diagram is added from the file with its source. The source is persisted as readable text, and the
 * diagram is laid out and rendered from it. */
@ExtendWith(HeadlessExtension.class)
class ClassmanImageTest {

  private static final String ORDERS = """
    classdiagram {
        class Order <<Entity>> {
            -number : String
        }
        class OrderLine
        composition Order 1 -> * OrderLine "consists of"
    }
    """;
  private static final String MESSAGE_KEY_REFUSED = "msg.classman.file.invalid";

  /** Scenario: The user chooses a class diagram file with the image button. The diagram is added after the text and
   * rendered, and a single undo takes it out again. */
  @Test
  void testChosenFileIsAddedAsDiagram(@TempDir Path directory) throws IOException {
    EditContainer container = containerOfNewStep();

    addImage(classdiagramFile(directory, ORDERS));

    assertTrue(container.getEditAreaAt(1) instanceof ClassmanImageEditArea, "A diagram must follow the text");
    ClassmanImageEditArea diagram = (ClassmanImageEditArea) container.getEditAreaAt(1);
    assertEquals(ORDERS, diagram.getSource());
    assertTrue(diagram.getFullSizeImage().getWidth() > 0, "The diagram must be rendered");
    assertEquals(List.of(), specman.shownMessages());

    specman.undo();

    assertFalse(hasSecondArea(container), "a single undo must take the diagram out");
  }

  /** Scenario: A source with a syntax error is refused with a message, nothing is added. */
  @Test
  void testSourceWithSyntaxErrorIsRefused(@TempDir Path directory) throws IOException {
    EditContainer container = containerOfNewStep();

    addImage(classdiagramFile(directory, "classdiagram { class }"));

    assertFalse(hasSecondArea(container), "no diagram must be added");
    assertEquals(List.of(MESSAGE_KEY_REFUSED), specman.shownMessages());
  }

  /** Scenario: A source which refers to a class it doesn't have can't be laid out and is refused too. */
  @Test
  void testSourceWithUnknownClassIsRefused(@TempDir Path directory) throws IOException {
    EditContainer container = containerOfNewStep();

    addImage(classdiagramFile(directory, "classdiagram { class A\nassociation A -> Missing }"));

    assertFalse(hasSecondArea(container), "no diagram must be added");
    assertEquals(List.of(MESSAGE_KEY_REFUSED), specman.shownMessages());
  }

  /** Scenario: A backtick would end the source in the model file. It is refused, even in a comment. */
  @Test
  void testSourceWithBacktickIsRefused(@TempDir Path directory) throws IOException {
    EditContainer container = containerOfNewStep();

    addImage(classdiagramFile(directory, "classdiagram {\n// don't use `this`\nclass A\n}"));

    assertFalse(hasSecondArea(container), "no diagram must be added");
    assertEquals(List.of(MESSAGE_KEY_REFUSED), specman.shownMessages());
  }

  /** Scenario: The source is saved as text between backticks, and the saved file holds no image data. */
  @Test
  void testSavedFileHoldsTheSourceAsText(@TempDir Path directory) throws IOException {
    containerOfNewStep();
    addImage(classdiagramFile(directory, ORDERS));

    String saved = Files.readString(save(directory).toPath(), StandardCharsets.UTF_8);

    assertTrue(saved.contains("type=classman"), "image type");
    assertTrue(saved.contains("`" + ORDERS + "`"), "source in backticks");
    assertFalse(saved.contains("%, type=png"), "no bitmap must be stored for a diagram");
  }

  /** Scenario: The saved diagram is loaded again with the same source and rendered. */
  @Test
  void testSourceSurvivesSavingAndLoading(@TempDir Path directory) throws IOException {
    containerOfNewStep();
    addImage(classdiagramFile(directory, ORDERS));

    specman.diagrammLaden(save(directory));

    assertEquals(ORDERS, reloadedDiagram().getSource());
    assertTrue(reloadedDiagram().getFullSizeImage().getWidth() > 0, "The reloaded diagram must be rendered");
  }

  /** Scenario: An agent edits the source in the file. The edited source is shown after loading. */
  @Test
  void testSourceEditedInTheFileIsShownAfterLoading(@TempDir Path directory) throws IOException {
    containerOfNewStep();
    addImage(classdiagramFile(directory, ORDERS));
    File file = save(directory);
    String edited = Files.readString(file.toPath(), StandardCharsets.UTF_8).replace("OrderLine", "Position");
    Files.writeString(file.toPath(), edited, StandardCharsets.UTF_8);

    specman.diagrammLaden(file);

    assertEquals(ORDERS.replace("OrderLine", "Position"), reloadedDiagram().getSource());
  }

  /** Scenario: An agent leaves a syntax error in the source. The file can still be loaded, the diagram shows the
   * problem, and the source is kept so that it can be repaired. */
  @Test
  void testBrokenSourceInTheFileStillLoads(@TempDir Path directory) throws IOException {
    containerOfNewStep();
    addImage(classdiagramFile(directory, ORDERS));
    File file = save(directory);
    String broken = Files.readString(file.toPath(), StandardCharsets.UTF_8).replace("class OrderLine", "clas OrderLine");
    Files.writeString(file.toPath(), broken, StandardCharsets.UTF_8);

    specman.diagrammLaden(file);

    assertTrue(reloadedDiagram().getSource().contains("clas OrderLine"), "the source must be kept as it is");
    assertTrue(reloadedDiagram().getFullSizeImage().getWidth() > 0, "a picture of the problem must be shown");
  }

  /** Scenario: The texts of the diagram are real PDF text, because the diagram is a vector graphic in the PDF. */
  @Test
  void testDiagramTextsAreRealPdfText(@TempDir Path directory) throws IOException {
    containerOfNewStep();
    addImage(classdiagramFile(directory, ORDERS));
    specman.layoutComponents();
    File pdf = directory.resolve("classman.pdf").toFile();

    specman.exportPdf(pdf, "A4", true, false);

    assertEquals(List.of(), specman.shownMessages());
    String pdfText = text(pdf);
    assertTrue(pdfText.contains("consists of"), pdfText);
    assertTrue(pdfText.contains("OrderLine"), pdfText);
  }

  private static EditContainer containerOfNewStep() {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    specman.focus(step);
    return step.getFirstEditArea().asTextArea().getParent();
  }

  private static boolean hasSecondArea(EditContainer container) {
    try {
      container.getEditAreaAt(1);
      return true;
    }
    catch (IndexOutOfBoundsException noSecondArea) {
      return false;
    }
  }

  private static File classdiagramFile(Path directory, String content) throws IOException {
    Path file = directory.resolve("orders.cld");
    Files.writeString(file, content, StandardCharsets.UTF_8);
    return file.toFile();
  }

  private static void addImage(File file) {
    specman.answerImageFileChoiceWith(file);
    specman.doEdit(ImageAction.class);
    specman.layoutComponents();
  }

  private static File save(Path directory) {
    File file = directory.resolve("classman.nsd").toFile();
    specman.setDiagrammDatei(file);
    specman.diagrammSpeichern(false);
    return file;
  }

  private static ClassmanImageEditArea reloadedDiagram() {
    AbstractSchrittView step = specman.findStepByStepID("1");
    EditContainer container = step.getFirstEditArea().asTextArea().getParent();
    return (ClassmanImageEditArea) container.getEditAreaAt(1);
  }
}
