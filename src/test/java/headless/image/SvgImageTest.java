package headless.image;

import headless.HeadlessExtension;
import net.atlanticbb.tantlinger.ui.text.actions.ImageAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import specman.editarea.EditContainer;
import specman.editarea.image.AbstractSvgImageEditArea;
import specman.editarea.image.SvgImageEditArea;
import specman.editarea.image.SvgNotDisplayableException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** An SVG file is added like a bitmap, but is kept as vector graphic, and its source is persisted as readable text. */
@ExtendWith(HeadlessExtension.class)
class SvgImageTest {

  private static final String GREETING =
    "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"200\" height=\"100\" viewBox=\"0 0 200 100\">\n"
      + "<rect width=\"200\" height=\"100\" fill=\"#eef\"/>\n"
      + "<text x=\"10\" y=\"50\" font-family=\"Roboto\" font-size=\"20\">Hello SVG</text>\n"
      + "</svg>\n";
  private static final String MESSAGE_KEY_REFUSED = "msg.svg.file.invalid";

  /** Scenario: The user chooses an SVG file with the image button. It is added after the text as vector graphic,
   * and a single undo takes it out again. */
  @Test
  void testChosenSvgFileIsAddedAsVectorGraphic(@TempDir Path directory) throws IOException {
    EditContainer container = containerOfNewStep();

    addImage(svgFile(directory, GREETING));

    assertTrue(container.getEditAreaAt(1) instanceof SvgImageEditArea, "An SVG graphic must follow the text");
    SvgImageEditArea svg = (SvgImageEditArea) container.getEditAreaAt(1);
    assertTrue(svg.getFullSizeImage().getWidth() > 0, "The graphic must be rendered");
    assertEquals(List.of(), specman.shownMessages());

    specman.undo();

    assertFalse(hasSecondArea(container), "a single undo must take the graphic out");
  }

  /** Scenario: A graphic refers to an image file by a relative path, which the SVG loader can't resolve. The loader's
   * reason is passed on, so the user can see what it is about. */
  @Test
  void testReasonOfTheLoaderIsReported() {
    String withImageFile = GREETING.replace("<rect", "<image xlink:href=\"icon.png\" width=\"16\" height=\"16\"/>\n<rect")
      .replace("<svg ", "<svg xmlns:xlink=\"http://www.w3.org/1999/xlink\" ");

    SvgNotDisplayableException refusal = assertThrows(SvgNotDisplayableException.class,
      () -> AbstractSvgImageEditArea.toIcon(withImageFile.getBytes(StandardCharsets.UTF_8)));

    assertFalse(refusal.getMessage() == null || refusal.getMessage().isBlank(), "the loader's reason must be given");
  }

  /** Scenario: A file which isn't an SVG graphic is refused with a message, nothing is added. */
  @Test
  void testFileWhichIsNoSvgIsRefused(@TempDir Path directory) throws IOException {
    EditContainer container = containerOfNewStep();

    addImage(svgFile(directory, "This is not a graphic"));

    assertFalse(hasSecondArea(container), "no graphic must be added");
    assertEquals(List.of(MESSAGE_KEY_REFUSED), specman.shownMessages());
  }

  /** Scenario: The source is saved as text between backticks. A backtick in the source, e.g. in a comment, would end
   * it, so it is saved as the character reference every XML reader takes for a backtick. */
  @Test
  void testSourceSurvivesSavingAndLoading(@TempDir Path directory) throws IOException {
    String withBacktick = GREETING.replace("<rect", "<!-- `quoted` -->\n<rect");
    containerOfNewStep();
    addImage(svgFile(directory, withBacktick));
    File file = directory.resolve("svg.nsd").toFile();
    specman.setDiagrammDatei(file);
    specman.diagrammSpeichern(false);

    String saved = Files.readString(file.toPath(), StandardCharsets.UTF_8);
    assertTrue(saved.contains("type=svg"), "image type");
    assertTrue(saved.contains("Hello SVG"), "source as text");
    specman.diagrammLaden(file);

    assertEquals(withBacktick.replace("`", "&#96;"), reloadedSvg().toModel(false).source);
    assertTrue(reloadedSvg().getFullSizeImage().getWidth() > 0, "The reloaded graphic must be rendered");
  }

  /** Scenario: The graphic is a vector graphic in the PDF, so its texts are real PDF text. */
  @Test
  void testSvgTextsAreRealPdfText(@TempDir Path directory) throws IOException {
    containerOfNewStep();
    addImage(svgFile(directory, GREETING));
    specman.layoutComponents();
    File pdf = directory.resolve("svg.pdf").toFile();

    specman.exportPdf(pdf, "A4", true, false);

    assertEquals(List.of(), specman.shownMessages());
    String pdfText = text(pdf);
    assertTrue(pdfText.contains("Hello SVG"), pdfText);
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

  private static File svgFile(Path directory, String content) throws IOException {
    Path file = directory.resolve("graphic.svg");
    Files.writeString(file, content, StandardCharsets.UTF_8);
    return file.toFile();
  }

  private static void addImage(File file) {
    specman.answerImageFileChoiceWith(file);
    specman.doEdit(ImageAction.class);
    specman.layoutComponents();
  }

  private static SvgImageEditArea reloadedSvg() {
    AbstractSchrittView step = specman.findStepByStepID("1");
    EditContainer container = step.getFirstEditArea().asTextArea().getParent();
    return (SvgImageEditArea) container.getEditAreaAt(1);
  }
}
