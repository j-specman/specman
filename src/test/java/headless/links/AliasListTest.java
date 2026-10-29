package headless.links;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import specman.link.AliasListPanel;
import specman.link.AliasPanel;
import specman.link.LinkAliases.Entry;
import specman.view.EinfacherSchrittView;

import javax.swing.JButton;
import javax.swing.JTable;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static headless.HeadlessExtension.specman;
import static headless.SpecmanFT.findByName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static specman.i18n.Messages.i18n;
import static specman.link.LinkAliases.PROBLEM_TAKEN;

/** The list of all aliases the user reaches from the alias dialog. */
class AliasListTest extends AbstractLinkTest {

  /** Scenario: The user assigned aliases to three files, and decided against an alias for a fourth one. In a fifth
   * file the user opens the list from the alias dialog. It shows the three aliases with their files, sorted by
   * alias regardless of case. The file without alias isn't listed. The user closes the list. */
  @Test
  void testListShowsAllAliasesSorted(@TempDir Path directory) {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    File fileB = assignAlias(step, directory, "UC-720.nsd", "uc720");
    File fileA = assignAlias(step, directory, "UC-721.nsd", "Alpha");
    File fileC = assignAlias(step, directory, "UC-722.nsd", "zulu");
    assignAlias(step, directory, "UC-723.nsd", "");
    saveDiagram(directory, "UC-724.nsd");
    AliasPanel aliasPanel = clickCopyLink(step);

    AliasListPanel listPanel = clickAllAliases(aliasPanel);

    assertEquals(List.of(new Entry(fileA, "Alpha"), new Entry(fileB, "uc720"), new Entry(fileC, "zulu")),
      rowsOf(listPanel));
    AtomicBoolean closed = new AtomicBoolean();
    listPanel.onClose(() -> closed.set(true));
    click(findByName(listPanel, AliasListPanel.CLOSE_BUTTON, JButton.class));
    assertTrue(closed.get());
  }

  /** Scenario: The alias uc720 is assigned to a file. For another file, the user tries the same alias and is told it
   * is taken. The user opens the list to see which file has it, and finds it there. */
  @Test
  void testListHelpsToFindWhoHasTakenTheAlias(@TempDir Path directory) {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    File owner = assignAlias(step, directory, "UC-720.nsd", "uc720");
    saveDiagram(directory, "UC-721.nsd");
    AliasPanel aliasPanel = clickCopyLink(step);
    typeAlias(aliasPanel, "UC720");
    assertEquals(i18n(PROBLEM_TAKEN), problem(aliasPanel));

    AliasListPanel listPanel = clickAllAliases(aliasPanel);

    assertEquals(List.of(new Entry(owner, "uc720")), rowsOf(listPanel));
  }

  /** Scenario: The user didn't assign any alias yet and opens the list. It is empty. */
  @Test
  void testListIsEmptyWithoutAliases(@TempDir Path directory) {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    saveDiagram(directory, "UC-720.nsd");

    AliasListPanel listPanel = clickAllAliases(clickCopyLink(step));

    assertEquals(List.of(), rowsOf(listPanel));
  }

  /** Saves the diagram under the given file name, copies the link of the step and chooses the alias in the dialog. */
  private static File assignAlias(EinfacherSchrittView step, Path directory, String filename, String alias) {
    File file = saveDiagram(directory, filename);
    AliasPanel panel = clickCopyLink(step);
    typeAlias(panel, alias);
    click(panel, AliasPanel.OK_BUTTON);
    return file;
  }

  private static AliasListPanel clickAllAliases(AliasPanel aliasPanel) {
    aliasPanel.setDialogDisplayed(false);
    click(aliasPanel, AliasPanel.ALL_ALIASES_BUTTON);
    assertNotNull(aliasPanel.aliasListPanel());
    return aliasPanel.aliasListPanel();
  }

  /** The rows the user sees in the list, as the aliases with their files. */
  private static List<Entry> rowsOf(AliasListPanel listPanel) {
    JTable table = findByName(listPanel, AliasListPanel.TABLE, JTable.class);
    List<Entry> rows = new ArrayList<>();
    for (int row = 0; row < table.getRowCount(); row++) {
      String alias = (String) table.getValueAt(row, AliasListPanel.ALIAS_COLUMN);
      String path = (String) table.getValueAt(row, AliasListPanel.FILE_COLUMN);
      rows.add(new Entry(new File(path), alias));
    }
    return rows;
  }
}
