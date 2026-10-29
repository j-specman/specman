package headless.links;

import headless.HeadlessExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import specman.link.CopyStepLinkMenuItem;
import specman.view.EinfacherSchrittView;

import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.net.URI;
import java.nio.file.Path;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static specman.link.SpecmanLink.SCHEME;
import static specman.link.SpecmanLink.SEPARATOR;

/** Copying a global link to a step via the step number's context menu. */
@ExtendWith(HeadlessExtension.class)
class CopyLinkTest {

  /** Scenario: The diagram was not saved yet, so there is no file name a link could point to.
   * The user can see the menu item, but can't use it. */
  @Test
  void testCopyLinkDisabledForUnsavedDiagram() {
    EinfacherSchrittView step = specman.clickNewSimpleStep();

    assertFalse(specman.contextMenuItem(step, CopyStepLinkMenuItem.class).isEnabled());
  }

  /** Scenario: The diagram is saved in a file with blanks and a comma in its name. The user copies the link of
   * step 2. The clipboard then contains the link with the file, the step ID and the step number, separated by commas.
   * The comma in the file name is encoded, so it can't be mistaken for a separator. */
  @Test
  void testCopyLinkOfSavedDiagram(@TempDir Path directory) throws Exception {
    specman.clickNewSimpleStep();
    EinfacherSchrittView step2 = specman.clickNewSimpleStep();
    String stepNumber = step2.getNumber().toString();
    File file = directory.resolve("UC 720, Eins.nsd").toFile();
    specman.setDiagrammDatei(file);
    specman.diagrammSpeichern(false);

    specman.clickContextMenuItem(step2, CopyStepLinkMenuItem.class);

    String link = clipboardText();
    String[] parts = link.split(SEPARATOR, -1);
    assertEquals(3, parts.length, link);
    assertTrue(parts[0].startsWith(SCHEME), link);
    assertTrue(parts[0].endsWith("UC%20720%2C%20Eins.nsd"), link);
    assertEquals(file, new File(URI.create("file://" + parts[0].substring(SCHEME.length()))));
    assertEquals(step2.getId(), parts[1]);
    assertEquals(stepNumber, parts[2]);
    assertEquals(java.util.List.of(), specman.shownMessages());
  }

  private static String clipboardText() throws Exception {
    assertNotNull(specman.clipboard().getContents(null), "nothing in the clipboard");
    return (String) specman.clipboard().getData(DataFlavor.stringFlavor);
  }
}
