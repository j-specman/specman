package headless.image.classman;

import headless.HeadlessExtension;
import headless.ModelCopyLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import specman.editarea.image.classman.ClassmanImageEditArea;
import specman.editarea.image.classman.LinkedClassmanImageEditArea;
import specman.link.LinkAliases;
import specman.link.classman.ClassmanLink;
import specman.model.v002.LinkedClassmanImageEditAreaModel_V002;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static builder.BuilderFactory.diagramm;
import static builder.BuilderFactory.sequence;
import static builder.BuilderFactory.simple;
import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A diagram which the model only links to is read from its file when the document is loaded, and shown like an
 * embedded one. The model keeps the link, whatever the file is. */
@ExtendWith(HeadlessExtension.class)
class LinkedClassmanLoadTest {
  private static final String SOURCE = "classdiagram {\n    class A (aaaaaaaa)\n}\n";

  private static File fileWithDiagram(Path directory) throws IOException {
    Path file = directory.resolve("hilfe.cld");
    Files.writeString(file, SOURCE, StandardCharsets.UTF_8);
    return file.toFile();
  }

  private static ClassmanImageEditArea load(Path directory, String link) throws IOException {
    File nsd = diagramm()
      .withMainSequence(sequence().withStep(simple("first").withLinkedClassmanDiagram(link)))
      .persist();
    new ModelCopyLoader(specman).loadModelCopy(nsd, directory);
    return (ClassmanImageEditArea) specman.findStepByStepID("1").getFirstEditArea().asTextArea().getParent()
      .getEditAreaAt(1);
  }

  /** Scenario: The link names the file by an alias. The diagram of the file is shown. */
  @Test
  void testDiagramIsReadFromFileOfAlias(@TempDir Path directory) throws IOException {
    LinkAliases.assign(fileWithDiagram(directory), "hilfe");

    ClassmanImageEditArea area = load(directory, "classman://hilfe");

    assertTrue(area instanceof LinkedClassmanImageEditArea);
    assertEquals(SOURCE, area.getSource());
  }

  /** Scenario: The link names the file by a path relative to the directory of the document, which is loaded from there. */
  @Test
  void testDiagramIsReadFromFileOfRelativePath(@TempDir Path directory) throws IOException {
    fileWithDiagram(directory);

    ClassmanImageEditArea area = load(directory, "classman://./hilfe.cld");

    assertEquals(SOURCE, area.getSource());
  }

  /** Scenario: The link names the file by its absolute path. */
  @Test
  void testDiagramIsReadFromFileOfPath(@TempDir Path directory) throws IOException {
    File file = fileWithDiagram(directory);

    ClassmanImageEditArea area = load(directory, ClassmanLink.of(file, "", null, null));

    assertEquals(SOURCE, area.getSource());
  }

  /** Scenario: Nobody knows the alias. The document loads anyway, and the link is kept for the next save. */
  @Test
  void testLinkWhichLeadsNowhereIsKept(@TempDir Path directory) throws IOException {
    ClassmanImageEditArea area = load(directory, "classman://nobody");

    LinkedClassmanImageEditAreaModel_V002 model = (LinkedClassmanImageEditAreaModel_V002) area.toModel(false);
    assertEquals("classman://nobody", model.link);
  }
}
