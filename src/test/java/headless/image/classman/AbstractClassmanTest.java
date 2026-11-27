package headless.image.classman;

import headless.HeadlessExtension;
import net.atlanticbb.tantlinger.ui.text.actions.ClassmanAction;
import org.junit.jupiter.api.extension.ExtendWith;
import specman.editarea.EditContainer;
import specman.editarea.TextEditArea;
import specman.editarea.image.SourceFileChoice;
import specman.editarea.image.classman.ClassmanImageEditArea;
import specman.view.EinfacherSchrittView;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(HeadlessExtension.class)
abstract class AbstractClassmanTest {

  static final String TWO_CLASSES = "classdiagram {\n    class Hello\n    class World\n}\n";
  static final String THREE_CLASSES = "classdiagram {\n    class Hello\n    class World\n    class Universe\n}\n";

  static EditContainer containerOfNewStep() {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    specman.focus(step);
    return step.getFirstEditArea().asTextArea().getParent();
  }

  /** A new step whose text is followed by a diagram with the source {@link #TWO_CLASSES}. */
  static ClassmanImageEditArea existingDiagram(EditContainer container) {
    return diagramWithSource(container, TWO_CLASSES);
  }

  /** A new step whose text is followed by a diagram with the given source. */
  static ClassmanImageEditArea diagramWithSource(EditContainer container, String source) {
    TextEditArea text = container.getEditAreaAt(0).asTextArea();
    text.addClassmanImage(source);
    specman.layoutComponents();
    return (ClassmanImageEditArea) container.getEditAreaAt(1);
  }

  /** Clicks the toolbar button for a Classman diagram, which by default offers an empty diagram. */
  static ClassmanImageEditArea insertDiagramByToolbarButton(EditContainer container) {
    specman.doEdit(ClassmanAction.class);
    specman.layoutComponents();
    assertTrue(hasDiagram(container), "A Classman diagram must follow the text");
    return (ClassmanImageEditArea) container.getEditAreaAt(1);
  }

  /** @return a file hilfe.cld with the content in the directory */
  static File diagramFile(Path directory, String content) throws IOException {
    Path file = directory.resolve("hilfe.cld");
    Files.writeString(file, content, StandardCharsets.UTF_8);
    return file.toFile();
  }

  /** Clicks the toolbar button for a Classman diagram, and answers the dialog by linking the file. If the file has no
   * alias yet, the user decides against an alias. */
  static void insertLinkedDiagram(File file) {
    insertLinkedDiagram(file, "");
  }

  /** Like {@link #insertLinkedDiagram(File)}, but the user answers the question for an alias with the given one, which
   * is null for cancelling. */
  static void insertLinkedDiagram(File file, String aliasAnswer) {
    specman.answerClassmanFileChoiceWith(SourceFileChoice.linkedFile(file));
    specman.answerClassmanAliasWith(aliasAnswer);
    specman.doEdit(ClassmanAction.class);
    specman.layoutComponents();
  }

  static boolean hasDiagram(EditContainer container) {
    try {
      return container.getEditAreaAt(1) instanceof ClassmanImageEditArea;
    }
    catch (IndexOutOfBoundsException noSecondArea) {
      return false;
    }
  }
}
