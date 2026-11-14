package headless.table;

import headless.HeadlessExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import specman.ChangeSet;
import specman.editarea.EditArea;
import specman.editarea.EditContainer;
import specman.editarea.TableEditArea;
import specman.editarea.TextEditArea;
import specman.view.EinfacherSchrittView;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static specman.editarea.document.WrappedDocument.VISIBLE_TEXT_START;

/**
 * A table which ends a step leaves the user no place to continue typing. Clicking the margin below the table
 * provides one: either the text already following the table, or a new empty one.
 */
@ExtendWith(HeadlessExtension.class)
class ParagraphBelowTableTest {

  private static final ChangeSet CHANGE_SET = ChangeSet.ALL.get("blue");

  @Test
  void testClickBelowTableWithoutFollowingTextAddsEmptyTextBehindIt() {
    TableEditArea table = tableAtEndOfStep();
    EditContainer container = table.getParent();
    assertSame(table, container.getLastEditArea());

    specman.clickBelow(table);

    EditArea added = container.getEditAreaAfter(table);
    assertNotNull(added);
    assertTrue(added.isTextArea());
    assertEquals("", added.getPlainText());
    assertSame(added, container.getLastEditArea());

    specman.undo();
    assertSame(table, container.getLastEditArea());

    specman.redo();
    assertSame(added, container.getEditAreaAfter(table));
  }

  @Test
  void testClickBelowTableMovesCaretToStartOfFollowingText() {
    TextEditArea upper = newStepText("abcdef");
    specman.focus(upper);
    upper.setCaretPosition(VISIBLE_TEXT_START + 3);
    TableEditArea table = (TableEditArea) upper.addTable(2, 2);
    EditContainer container = table.getParent();
    EditArea following = container.getEditAreaAfter(table);
    assertEquals("def", following.getPlainText());

    specman.clickBelow(table);

    assertSame(following, container.getEditAreaAfter(table));
    assertSame(following, container.getLastEditArea());
    assertSame(following, specman.focusedArea());
    assertEquals(VISIBLE_TEXT_START, following.asTextArea().getCaretPosition());
  }

  @Test
  void testTextAddedBelowTableInChangeModeIsMarkedAsAdded() {
    TableEditArea table = tableAtEndOfStep();
    specman.toggleChangeMode();
    specman.selectChangeSet(CHANGE_SET);

    specman.clickBelow(table);

    EditArea added = table.getParent().getEditAreaAfter(table);
    assertTrue(added.getChangeInfo().isAdded(), "added text must be marked as added");
    assertEquals(CHANGE_SET, added.getChangeInfo().changeSet());
  }

  private static TableEditArea tableAtEndOfStep() {
    TextEditArea text = newStepText("abc");
    specman.focus(text);
    text.setCaretPosition(VISIBLE_TEXT_START + 3);
    return (TableEditArea) text.addTable(2, 2);
  }

  private static TextEditArea newStepText(String html) {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    TextEditArea text = step.getFirstEditArea().asTextArea();
    text.setText(html);
    return text;
  }
}
