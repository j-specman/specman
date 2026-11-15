package headless.text;

import headless.HeadlessExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import specman.editarea.EditArea;
import specman.editarea.EditContainer;
import specman.editarea.TableEditArea;
import specman.editarea.TextEditArea;
import specman.view.EinfacherSchrittView;

import java.awt.event.KeyEvent;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static specman.editarea.document.WrappedDocument.VISIBLE_TEXT_START;

/**
 * An empty text which only exists as a place to type, e.g. behind a table, disappears on BACKSPACE.
 * The very first text of an edit container is mandatory and stays.
 */
@ExtendWith(HeadlessExtension.class)
class BackspaceInEmptyTextTest {

  @Test
  void testBackspaceInEmptyTextBelowTableRemovesItAndFocusesLastCell() {
    TableEditArea table = tableAtEndOfStep();
    EditContainer container = table.getParent();
    focusEmptyTextBelow(table);

    specman.press(KeyEvent.VK_BACK_SPACE);

    assertNull(container.getEditAreaAfter(table));
    assertSame(table, container.getLastEditArea());
    assertTrue(table.getLastCell().isAncestorOf(specman.focusedArea().asComponent()), "focus must move into the table's last cell");
  }

  @Test
  void testRemovalOfEmptyTextBelowTableCanBeUndoneAndRedone() {
    TableEditArea table = tableAtEndOfStep();
    EditContainer container = table.getParent();
    EditArea empty = focusEmptyTextBelow(table);

    specman.press(KeyEvent.VK_BACK_SPACE);
    specman.undo();

    assertSame(empty, container.getEditAreaAfter(table));
    assertSame(empty, container.getLastEditArea());

    specman.redo();

    assertNull(container.getEditAreaAfter(table));
  }

  @Test
  void testBackspaceAtStartOfTextBelowTableKeepsNonEmptyText() {
    TextEditArea upper = newStepText("abcdef");
    specman.focus(upper);
    upper.setCaretPosition(VISIBLE_TEXT_START + 3);
    TableEditArea table = (TableEditArea) upper.addTable(2, 2);
    EditContainer container = table.getParent();
    EditArea following = container.getEditAreaAfter(table);
    specman.focus(following);
    following.asTextArea().setCaretPosition(VISIBLE_TEXT_START);

    specman.press(KeyEvent.VK_BACK_SPACE);

    assertSame(following, container.getEditAreaAfter(table));
  }

  @Test
  void testBackspaceInEmptyFirstTextOfStepKeepsIt() {
    TextEditArea first = newStepText("");
    EditContainer container = first.getParent();
    specman.focus(first);
    first.setCaretPosition(VISIBLE_TEXT_START);

    specman.press(KeyEvent.VK_BACK_SPACE);

    assertSame(first, container.getFirstEditArea());
    assertNotNull(first.getParent());
  }

  private static EditArea focusEmptyTextBelow(TableEditArea table) {
    specman.clickBelow(table);
    EditArea empty = table.getParent().getEditAreaAfter(table);
    assertNotNull(empty);
    specman.focus(empty);
    empty.asTextArea().setCaretPosition(VISIBLE_TEXT_START);
    return empty;
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
