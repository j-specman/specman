package headless.changemode.persistence;

import headless.changemode.AbstractChangemodeTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import specman.ChangeSet;
import specman.editarea.EditContainer;
import specman.editarea.TableEditArea;
import specman.editarea.TextEditArea;
import specman.model.v002.io.ModelParseException;
import specman.model.v002.io.ModelParser_V002;
import specman.model.v002.io.ModelSerializer_V002;
import specman.view.AbstractSchrittView;
import specman.view.EinfacherSchrittView;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static specman.graphics.Styles.BACKGROUND_COLOR_STANDARD;

/**
 * Rows and columns added to a table in change mode have the change set's panel color in the cell as well as in
 * its text. Saving and loading the table must not lose the cell coloring.
 */
class TablePersistenceTest extends AbstractChangemodeTest {

  private static final ChangeSet CHANGE_SET = ChangeSet.ALL.get("blue");

  @Test
  void testAddedRowKeepsCellColoringWhenSavedAndLoaded() {
    TableEditArea table = existingTable();
    enableChangeMode(CHANGE_SET);
    table.addRowUDBL(2);
    typeIntoAllCells(table);
    List<EditContainer> cells = cellsOf(table);
    assertCellsColoredBy(cells.subList(4, 6), CHANGE_SET.panelColor());

    List<EditContainer> reloaded = cellsOf(saveAndLoad(table));

    assertCellsColoredBy(reloaded.subList(4, 6), CHANGE_SET.panelColor());
    assertCellsColoredBy(reloaded.subList(0, 4), BACKGROUND_COLOR_STANDARD);
  }

  @Test
  void testAddedColumnKeepsCellColoringWhenSavedAndLoaded() {
    TableEditArea table = existingTable();
    enableChangeMode(CHANGE_SET);
    table.addColumnUDBL(2);
    typeIntoAllCells(table);
    List<EditContainer> cells = cellsOf(table);
    assertCellsColoredBy(List.of(cells.get(2), cells.get(5)), CHANGE_SET.panelColor());

    List<EditContainer> reloaded = cellsOf(saveAndLoad(table));

    assertCellsColoredBy(List.of(reloaded.get(2), reloaded.get(5)), CHANGE_SET.panelColor());
    assertCellsColoredBy(List.of(reloaded.get(0), reloaded.get(1), reloaded.get(3), reloaded.get(4)), BACKGROUND_COLOR_STANDARD);
  }

  @Test
  void testAddedRowKeepsCellColoringWhenWholeDiagramIsSavedAndLoaded(@TempDir Path directory) {
    TableEditArea table = existingTable();
    enableChangeMode(CHANGE_SET);
    table.addRowUDBL(2);
    typeIntoAllCells(table);
    specman.setDiagrammDatei(directory.resolve("table.nsd").toFile());
    specman.diagrammSpeichern(false);

    specman.diagrammLaden(directory.resolve("table.nsd").toFile());

    List<EditContainer> cells = cellsOf(tableOf(specman.findStepByStepID("1")));
    assertCellsColoredBy(cells.subList(4, 6), CHANGE_SET.panelColor());
    assertCellsColoredBy(cells.subList(0, 4), BACKGROUND_COLOR_STANDARD);
  }

  @Test
  void testAddedTableKeepsColoringWhenWholeDiagramIsSavedAndLoaded(@TempDir Path directory) {
    enableChangeMode(CHANGE_SET);
    TableEditArea table = existingTable();
    typeIntoAllCells(table);
    specman.setDiagrammDatei(directory.resolve("table.nsd").toFile());
    specman.diagrammSpeichern(false);

    specman.diagrammLaden(directory.resolve("table.nsd").toFile());

    TableEditArea reloaded = tableOf(specman.findStepByStepID("1"));
    assertEquals(CHANGE_SET.panelColor(), reloaded.getBackground(), "table background");
    assertCellsColoredBy(cellsOf(reloaded), CHANGE_SET.panelColor());
  }

  @Test
  void testAddedTableInUntrackedStepKeepsAreaColoringWhenWholeDiagramIsSavedAndLoaded(@TempDir Path directory) {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    enableChangeMode(CHANGE_SET);
    TableEditArea table = (TableEditArea) step.getFirstEditArea().asTextArea().addTable(2, 2);
    typeIntoAllCells(table);
    assertEquals(CHANGE_SET.panelColor(), table.getBackground(), "table background before saving");
    specman.setDiagrammDatei(directory.resolve("table.nsd").toFile());
    specman.diagrammSpeichern(false);

    specman.diagrammLaden(directory.resolve("table.nsd").toFile());

    TableEditArea reloaded = tableOf(specman.findStepByStepID("1"));
    assertEquals(CHANGE_SET.panelColor(), reloaded.getBackground(), "table background");
    assertCellsColoredBy(cellsOf(reloaded), CHANGE_SET.panelColor());
  }

  private static TableEditArea tableOf(AbstractSchrittView step) {
    return findTable(step.getDecoratedComponent());
  }

  private static TableEditArea findTable(Component component) {
    if (component instanceof TableEditArea table) {
      return table;
    }
    if (component instanceof Container container) {
      for (Component child : container.getComponents()) {
        TableEditArea table = findTable(child);
        if (table != null) {
          return table;
        }
      }
    }
    return null;
  }

  private static TableEditArea existingTable() {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    TextEditArea text = step.getFirstEditArea().asTextArea();
    return (TableEditArea) text.addTable(2, 2);
  }

  private static void typeIntoAllCells(TableEditArea table) {
    for (EditContainer cell : cellsOf(table)) {
      TextEditArea text = cell.getFirstEditArea().asTextArea();
      if (text.getChangeInfo().isChange()) {
        text.setText("text");
      }
    }
  }

  private static TableEditArea saveAndLoad(TableEditArea table) {
    try {
      String saved = new ModelSerializer_V002().serializeTable(table.toModel(true));
      return new TableEditArea(new ModelParser_V002().parseTable(saved));
    }
    catch (ModelParseException x) {
      throw new IllegalStateException(x);
    }
  }

  private static List<EditContainer> cellsOf(Container container) {
    List<EditContainer> result = new ArrayList<>();
    for (Component child : container.getComponents()) {
      if (child instanceof EditContainer cell) {
        result.add(cell);
      }
      else if (child instanceof Container childContainer) {
        result.addAll(cellsOf(childContainer));
      }
    }
    return result;
  }

  private static void assertCellsColoredBy(List<EditContainer> cells, Color expected) {
    for (EditContainer cell : cells) {
      assertEquals(expected, cell.getBackground(), "cell background");
      assertEquals(expected, cell.getFirstEditArea().asTextArea().getBackground(), "text background");
    }
  }
}
