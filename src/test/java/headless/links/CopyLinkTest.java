package headless.links;

import headless.HeadlessExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import specman.link.AliasPanel;
import specman.link.CopyStepLinkMenuItem;
import specman.link.SpecmanLink;
import specman.view.EinfacherSchrittView;

import javax.swing.JButton;
import javax.swing.JTextField;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static headless.HeadlessExtension.specman;
import static headless.SpecmanFT.findByName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

  /** Scenario: The diagram is saved. The user chooses to copy the link of step 1. A dialog asks for an alias, which
   * shows the link as it would look without an alias. The user cancels, so the dialog closes and the clipboard stays empty. */
  @Test
  void testCancelAliasPanel(@TempDir Path directory) {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    File file = saveDiagram(directory, "UC-720.nsd");
    AliasPanel panel = clickCopyLink(step);
    AtomicBoolean closed = new AtomicBoolean();
    panel.onClose(() -> closed.set(true));
    assertEquals(SpecmanLink.of(file, step), findByName(panel, AliasPanel.PREVIEW_FIELD, JTextField.class).getText());

    findByName(panel, AliasPanel.CANCEL_BUTTON, JButton.class).doClick(0);

    assertTrue(closed.get());
    assertNull(specman.clipboard().getContents(null));
    assertEquals(List.of(), specman.shownMessages());
  }

  /** Scenario: The diagram is saved in a file with blanks and a comma in its name. The user copies the link of
   * step 2. The clipboard then contains the link with the file, the step ID and the step number, separated by commas.
   * The comma in the file name is encoded, so it can't be mistaken for a separator. */
  @Disabled("The alias panel can't be confirmed yet, so nothing is copied")
  @Test
  void testCopyLinkOfSavedDiagram(@TempDir Path directory) throws Exception {
    specman.clickNewSimpleStep();
    EinfacherSchrittView step2 = specman.clickNewSimpleStep();
    String stepNumber = step2.getNumber().toString();
    File file = saveDiagram(directory, "UC 720, Eins.nsd");

    clickCopyLink(step2);

    String link = clipboardText();
    String[] parts = link.split(SEPARATOR, -1);
    assertEquals(3, parts.length, link);
    assertTrue(parts[0].startsWith(SCHEME), link);
    assertTrue(parts[0].endsWith("UC%20720%2C%20Eins.nsd"), link);
    assertEquals(file, new File(URI.create("file://" + parts[0].substring(SCHEME.length()))));
    assertEquals(step2.getId(), parts[1]);
    assertEquals(stepNumber, parts[2]);
    assertEquals(List.of(), specman.shownMessages());
  }

  private static File saveDiagram(Path directory, String filename) {
    File file = directory.resolve(filename).toFile();
    specman.setDiagrammDatei(file);
    specman.diagrammSpeichern(false);
    return file;
  }

  /** Clicks the menu item without displaying the dialog and returns the panel the dialog would show. */
  private static AliasPanel clickCopyLink(EinfacherSchrittView step) {
    CopyStepLinkMenuItem item = specman.contextMenuItem(step, CopyStepLinkMenuItem.class);
    item.setDialogDisplayed(false);
    specman.clickContextMenuItem(step, CopyStepLinkMenuItem.class);
    return item.aliasPanel();
  }

  private static String clipboardText() throws Exception {
    assertNotNull(specman.clipboard().getContents(null), "nothing in the clipboard");
    return (String) specman.clipboard().getData(DataFlavor.stringFlavor);
  }
}
