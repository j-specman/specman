package headless.links;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import specman.link.AliasListPanel;
import specman.link.AliasPanel;
import specman.link.LinkAliases;
import specman.link.LinkAliases.Entry;
import specman.view.EinfacherSchrittView;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static headless.HeadlessExtension.specman;
import static headless.SpecmanFT.findByName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static specman.i18n.Messages.i18n;
import static specman.link.LinkAliases.PROBLEM_FILE_TWICE;
import static specman.link.LinkAliases.PROBLEM_TAKEN;
import static specman.link.SpecmanLink.SCHEME;
import static specman.link.SpecmanLink.SEPARATOR;
import static specman.menubar.SpecmanMenuBar.ALIASES_ITEM;

/** The list of all aliases, which the user maintains via the file menu or reaches from the alias dialog. */
class AliasListTest extends AbstractLinkTest {

  /** Scenario: The user assigned aliases to three files, and decided against an alias for a fourth one. In a fifth
   * file the user opens the list from the alias dialog. It shows the three aliases with their files, sorted by
   * alias regardless of case. The file without alias isn't listed. The user cancels the list. */
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
    click(listPanel, AliasListPanel.CANCEL_BUTTON);
    assertTrue(closed.get());
  }

  /** Scenario: The user chooses "Aliases..." from the file menu and sees the aliases assigned so far. */
  @Test
  void testListOpensFromFileMenu(@TempDir Path directory) {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    File file = assignAlias(step, directory, "UC-720.nsd", "uc720");

    assertEquals(List.of(new Entry(file, "uc720")), rowsOf(openList()));
  }

  /** Scenario: The user changes the alias uc720 to uc720b and saves. The list closes. The next link to the file
   * uses the new alias, and the list shows it. */
  @Test
  void testChangedAliasIsSaved(@TempDir Path directory) throws Exception {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    File file = assignAlias(step, directory, "UC-720.nsd", "uc720");
    AliasListPanel listPanel = openList();
    AtomicBoolean closed = new AtomicBoolean();
    listPanel.onClose(() -> closed.set(true));

    typeAliasInRow(listPanel, 0, "uc720b");
    click(listPanel, AliasListPanel.SAVE_BUTTON);

    assertTrue(closed.get());
    assertEquals(List.of(new Entry(file, "uc720b")), rowsOf(openList()));
    assertEquals(SCHEME + "uc720b" + SEPARATOR + step.getId() + SEPARATOR + step.getNumber(), copyLink(step));
  }

  /** Scenario: The user types characters into an alias in the list which an alias must not contain. They don't
   * make it into the cell. */
  @Test
  void testForbiddenCharactersAreNotTypedInList(@TempDir Path directory) {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    assignAlias(step, directory, "UC-720.nsd", "uc720");
    AliasListPanel listPanel = openList();

    typeAliasInRow(listPanel, 0, "u c.7");

    assertEquals("uc7", tableOf(listPanel).getValueAt(0, AliasListPanel.ALIAS_COLUMN));
  }

  /** Scenario: The user changes and deletes aliases, but cancels. Nothing is stored. */
  @Test
  void testCancelDiscardsChanges(@TempDir Path directory) {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    File fileA = assignAlias(step, directory, "UC-720.nsd", "uc720");
    File fileB = assignAlias(step, directory, "UC-721.nsd", "uc721");
    AliasListPanel listPanel = openList();
    typeAliasInRow(listPanel, 0, "changed");
    selectRow(listPanel, 1);
    click(listPanel, AliasListPanel.DELETE_BUTTON);
    assertEquals(1, tableOf(listPanel).getRowCount());

    click(listPanel, AliasListPanel.CANCEL_BUTTON);

    assertEquals(List.of(new Entry(fileA, "uc720"), new Entry(fileB, "uc721")), rowsOf(openList()));
  }

  /** Scenario: The user deletes the row of a file and saves. The alias is gone, and as the user didn't decide against
   * an alias, copying a link in that file asks for an alias again. */
  @Test
  void testDeletedRowIsForgotten(@TempDir Path directory) {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    assignAlias(step, directory, "UC-720.nsd", "uc720");
    AliasListPanel listPanel = openList();
    selectRow(listPanel, 0);

    click(listPanel, AliasListPanel.DELETE_BUTTON);
    click(listPanel, AliasListPanel.SAVE_BUTTON);

    assertEquals(List.of(), rowsOf(openList()));
    assertNotNull(clickCopyLink(step), "without an alias, the user must be asked again");
  }

  /** Scenario: The user empties the alias of a file and saves. Links to the file use the path then, and the user
   * isn't asked again. The file doesn't show up in the list anymore. */
  @Test
  void testEmptiedAliasMeansDecidedAgainstAlias(@TempDir Path directory) throws Exception {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    assignAlias(step, directory, "UC-720.nsd", "uc720");
    AliasListPanel listPanel = openList();

    typeAliasInRow(listPanel, 0, "");
    click(listPanel, AliasListPanel.SAVE_BUTTON);

    assertEquals(List.of(), rowsOf(openList()));
    assertNull(clickCopyLink(step), "the user decided against an alias, so there is nothing to ask");
    assertTrue(clipboardText().startsWith(SCHEME + "/"));
  }

  /** Scenario: The user gives a file an alias which another file has already, in different case. The user can't save
   * and is told why. Emptying the alias helps, and several files may have an empty alias at the same time. */
  @Test
  void testSavingNeedsUniqueAliases(@TempDir Path directory) {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    assignAlias(step, directory, "UC-720.nsd", "uc720");
    assignAlias(step, directory, "UC-721.nsd", "uc721");
    AliasListPanel listPanel = openList();

    typeAliasInRow(listPanel, 1, "UC720");

    assertFalse(button(listPanel, AliasListPanel.SAVE_BUTTON).isEnabled());
    assertEquals(i18n(PROBLEM_TAKEN), problem(listPanel));

    typeAliasInRow(listPanel, 1, "");
    typeAliasInRow(listPanel, 0, "");

    assertTrue(button(listPanel, AliasListPanel.SAVE_BUTTON).isEnabled());
    assertEquals(" ", problem(listPanel));
  }

  /** Scenario: A file an alias was assigned to was moved. The list shows its path as missing, the file of another
   * row is not marked. The user chooses the file at its new place and saves. The alias now belongs to the new file. */
  @Test
  void testChoosingAnotherFileForMissingFile(@TempDir Path directory) {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    File oldFile = assignAlias(step, directory, "UC-720.nsd", "uc720");
    assignAlias(step, directory, "UC-721.nsd", "uc721");
    File newFile = saveDiagram(directory, "UC-722.nsd");
    assertTrue(oldFile.delete());
    AliasListPanel listPanel = openList();
    String missing = i18n("dialog.link.aliases.file.missing");
    assertTrue(renderedFile(listPanel, 0).contains(missing));
    assertFalse(renderedFile(listPanel, 1).contains(missing));
    List<File> offered = new ArrayList<>();
    listPanel.setFileChooser(current -> {
      offered.add(current);
      return newFile;
    });
    selectRow(listPanel, 0);

    click(listPanel, AliasListPanel.CHOOSE_FILE_BUTTON);

    assertEquals(List.of(oldFile), offered);
    assertEquals(newFile.getPath(), tableOf(listPanel).getValueAt(0, AliasListPanel.FILE_COLUMN));
    assertFalse(renderedFile(listPanel, 0).contains(missing));
    click(listPanel, AliasListPanel.SAVE_BUTTON);
    assertEquals(newFile, LinkAliases.fileOf("uc720"));
  }

  /** Scenario: The user doesn't choose a file when asked. The row keeps its file. */
  @Test
  void testCancelledFileChoiceKeepsFile(@TempDir Path directory) {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    File file = assignAlias(step, directory, "UC-720.nsd", "uc720");
    AliasListPanel listPanel = openList();
    listPanel.setFileChooser(current -> null);
    selectRow(listPanel, 0);

    click(listPanel, AliasListPanel.CHOOSE_FILE_BUTTON);

    assertEquals(List.of(new Entry(file, "uc720")), rowsOf(listPanel));
  }

  /** Scenario: Of three files, the middle one is missing. Only its row is marked in red, however the rows get rendered. */
  @Test
  void testOnlyMissingFileIsMarkedRed(@TempDir Path directory) {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    assignAlias(step, directory, "UC-720.nsd", "uc720");
    File missingFile = assignAlias(step, directory, "UC-721.nsd", "uc721");
    assignAlias(step, directory, "UC-722.nsd", "uc722");
    assertTrue(missingFile.delete());
    AliasListPanel listPanel = openList();

    Color first = colorOfFile(listPanel, 0);
    Color second = colorOfFile(listPanel, 1);
    Color third = colorOfFile(listPanel, 2);
    Color firstAgain = colorOfFile(listPanel, 0);

    assertNotEquals(second, first);
    assertNotEquals(second, third);
    assertEquals(first, third);
    assertEquals(first, firstAgain);
  }

  /** Scenario: The user double-clicks the file of a row and is asked for another file, which the row gets. A double
   * click on the alias doesn't ask for a file. */
  @Test
  void testDoubleClickOnFileChoosesFile(@TempDir Path directory) {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    File oldFile = assignAlias(step, directory, "UC-720.nsd", "uc720");
    File newFile = saveDiagram(directory, "UC-721.nsd");
    AliasListPanel listPanel = openList();
    List<File> offered = new ArrayList<>();
    listPanel.setFileChooser(current -> {
      offered.add(current);
      return newFile;
    });

    doubleClick(listPanel, 0, AliasListPanel.ALIAS_COLUMN);

    assertEquals(List.of(), offered);

    doubleClick(listPanel, 0, AliasListPanel.FILE_COLUMN);

    assertEquals(List.of(oldFile), offered);
    assertEquals(newFile.getPath(), tableOf(listPanel).getValueAt(0, AliasListPanel.FILE_COLUMN));
  }

  /** Scenario: The user chooses for a row the file another row already has. A file must not appear twice,
   * so the user is told and can't save. */
  @Test
  void testSameFileInTwoRowsCantBeSaved(@TempDir Path directory) {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    File fileA = assignAlias(step, directory, "UC-720.nsd", "uc720");
    assignAlias(step, directory, "UC-721.nsd", "uc721");
    AliasListPanel listPanel = openList();
    listPanel.setFileChooser(current -> fileA);
    selectRow(listPanel, 1);

    click(listPanel, AliasListPanel.CHOOSE_FILE_BUTTON);

    assertFalse(button(listPanel, AliasListPanel.SAVE_BUTTON).isEnabled());
    assertEquals(i18n(PROBLEM_FILE_TWICE), problem(listPanel));
  }

  /** Scenario: Without a selected row, there is nothing to delete or to choose a file for. With several rows
   * selected, a file can't be chosen, but the rows can be deleted. */
  @Test
  void testButtonsFollowTheSelection(@TempDir Path directory) {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    assignAlias(step, directory, "UC-720.nsd", "uc720");
    assignAlias(step, directory, "UC-721.nsd", "uc721");
    AliasListPanel listPanel = openList();
    assertFalse(button(listPanel, AliasListPanel.DELETE_BUTTON).isEnabled());
    assertFalse(button(listPanel, AliasListPanel.CHOOSE_FILE_BUTTON).isEnabled());

    selectRow(listPanel, 0);
    assertTrue(button(listPanel, AliasListPanel.DELETE_BUTTON).isEnabled());
    assertTrue(button(listPanel, AliasListPanel.CHOOSE_FILE_BUTTON).isEnabled());

    tableOf(listPanel).selectAll();
    assertTrue(button(listPanel, AliasListPanel.DELETE_BUTTON).isEnabled());
    assertFalse(button(listPanel, AliasListPanel.CHOOSE_FILE_BUTTON).isEnabled());
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

  private static AliasListPanel openList() {
    specman.click(ALIASES_ITEM);
    assertNotNull(specman.openedAliasListPanel());
    return specman.openedAliasListPanel();
  }

  private static JTable tableOf(AliasListPanel listPanel) {
    return findByName(listPanel, AliasListPanel.TABLE, JTable.class);
  }

  private static JButton button(AliasListPanel listPanel, String name) {
    return findByName(listPanel, name, JButton.class);
  }

  private static void click(AliasListPanel listPanel, String buttonName) {
    click(button(listPanel, buttonName));
  }

  private static String problem(AliasListPanel listPanel) {
    return findByName(listPanel, AliasListPanel.PROBLEM_LABEL, JLabel.class).getText();
  }

  private static void selectRow(AliasListPanel listPanel, int row) {
    tableOf(listPanel).setRowSelectionInterval(row, row);
  }

  /** Types into the alias cell like the user does: Edit the cell, replace its content, confirm. */
  private static void typeAliasInRow(AliasListPanel listPanel, int row, String alias) {
    JTable table = tableOf(listPanel);
    table.editCellAt(row, AliasListPanel.ALIAS_COLUMN);
    JTextField field = (JTextField) table.getEditorComponent();
    field.selectAll();
    field.replaceSelection(alias);
    table.getCellEditor().stopCellEditing();
  }

  private static void doubleClick(AliasListPanel listPanel, int row, int column) {
    JTable table = tableOf(listPanel);
    Rectangle cell = table.getCellRect(row, column, false);
    table.dispatchEvent(new MouseEvent(table, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
      MouseEvent.BUTTON1_DOWN_MASK, cell.x + cell.width / 2, cell.y + cell.height / 2, 2, false, MouseEvent.BUTTON1));
  }

  /** The text the user sees in the file column of the row. */
  private static String renderedFile(AliasListPanel listPanel, int row) {
    return ((JLabel) renderFile(listPanel, row)).getText();
  }

  private static Color colorOfFile(AliasListPanel listPanel, int row) {
    return renderFile(listPanel, row).getForeground();
  }

  private static Component renderFile(AliasListPanel listPanel, int row) {
    JTable table = tableOf(listPanel);
    return table.getCellRenderer(row, AliasListPanel.FILE_COLUMN).getTableCellRendererComponent(
      table, table.getValueAt(row, AliasListPanel.FILE_COLUMN), false, false, row, AliasListPanel.FILE_COLUMN);
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
