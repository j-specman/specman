package headless.links;

import headless.HeadlessExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import specman.link.AliasPanel;
import specman.link.CopyStepLinkMenuItem;
import specman.view.EinfacherSchrittView;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.nio.file.Path;

import static headless.HeadlessExtension.specman;
import static headless.SpecmanFT.findByName;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Base class for tests about global links. It provides the headless Specman and the operation of the dialogs
 * around links like the user does. */
@ExtendWith(HeadlessExtension.class)
abstract class AbstractLinkTest {

  static File saveDiagram(Path directory, String filename) {
    File file = directory.resolve(filename).toFile();
    specman.setDiagrammDatei(file);
    specman.diagrammSpeichern(false);
    return file;
  }

  /** Clicks the menu item without displaying the dialog and returns the panel the dialog would show, null
   * if the click didn't ask for an alias. */
  static AliasPanel clickCopyLink(EinfacherSchrittView step) {
    CopyStepLinkMenuItem item = specman.contextMenuItem(step, CopyStepLinkMenuItem.class);
    item.setDialogDisplayed(false);
    specman.clickContextMenuItem(step, CopyStepLinkMenuItem.class);
    return item.aliasPanel();
  }

  /** Types the text like the user would after selecting the field's content, i.e. it replaces what is there. */
  static void typeAlias(AliasPanel panel, String alias) {
    JTextField field = aliasField(panel);
    field.selectAll();
    field.replaceSelection(alias);
  }

  static JTextField aliasField(AliasPanel panel) {
    return findByName(panel, AliasPanel.ALIAS_FIELD, JTextField.class);
  }

  static String preview(AliasPanel panel) {
    return findByName(panel, AliasPanel.PREVIEW_FIELD, JTextField.class).getText();
  }

  static String problem(AliasPanel panel) {
    return findByName(panel, AliasPanel.PROBLEM_LABEL, JLabel.class).getText();
  }

  static JButton okButton(AliasPanel panel) {
    return findByName(panel, AliasPanel.OK_BUTTON, JButton.class);
  }

  static void click(AliasPanel panel, String buttonName) {
    click(findByName(panel, buttonName, JButton.class), buttonName);
  }

  static void click(JButton button, String buttonName) {
    assertTrue(button.isEnabled(), buttonName + " must be enabled");
    button.doClick(0);
  }

  static String clipboardText() throws Exception {
    assertNotNull(specman.clipboard().getContents(null), "nothing in the clipboard");
    return (String) specman.clipboard().getData(DataFlavor.stringFlavor);
  }
}
