package headless.navigation;

import headless.HeadlessExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import specman.editarea.EditArea;
import specman.editarea.EditContainer;
import specman.editarea.ImageEditArea;
import specman.editarea.TableEditArea;
import specman.editarea.TextEditArea;
import specman.ops.buttons.CreateIfElseStepOpButton;
import specman.ops.buttons.CreateSimpleStepOpButton;
import specman.ops.buttons.CreateWhileStepOpButton;
import specman.view.AbstractSchrittView;
import specman.view.EinfacherSchrittView;
import specman.view.IfElseSchrittView;
import specman.view.WhileSchrittView;
import specman.visitor.TextAreaCollector;

import javax.swing.SwingUtilities;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static specman.editarea.document.WrappedDocument.VISIBLE_TEXT_START;

/**
 * Arrow up / down moves the caret out of an edit area once it is in the area's last / first row. The next edit
 * area is the one found graphically above or below, regardless of which step or table cell it belongs to,
 * and the caret lands at the same x position as before, as far as the target text allows.
 */
@ExtendWith(HeadlessExtension.class)
class ArrowKeyNavigationTest {

  /** All texts consist of 'x', so equal character counts mean equal x positions. */
  private static final String TEN_X = "xxxxxxxxxx";
  private static final String SIXTEEN_X = "xxxxxxxxxxxxxxxx";
  /** Long enough to reach into the right half of a 700 pixel wide diagram, short enough not to wrap. */
  private static final String SIXTY_X = TEN_X.repeat(6);

  @Test
  void testDownMovesToNextStepKeepingX() {
    TextEditArea upper = newStepText(TEN_X);
    TextEditArea lower = newStepText(SIXTEEN_X);
    placeCaret(upper, 5);

    specman.press(KeyEvent.VK_DOWN);

    assertFocused(lower);
    assertEquals(5, caretIndex(lower));
  }

  @Test
  void testUpMovesToPreviousStepKeepingX() {
    TextEditArea upper = newStepText(SIXTEEN_X);
    TextEditArea lower = newStepText(TEN_X);
    placeCaret(lower, 5);

    specman.press(KeyEvent.VK_UP);

    assertFocused(upper);
    assertEquals(5, caretIndex(upper));
  }

  @Test
  void testDownLandsInFirstRowAndUpInLastRowOfTarget() {
    TextEditArea upper = newStepText("<p>" + TEN_X + "</p><p>" + TEN_X + "</p>");
    TextEditArea lower = newStepText("<p>" + TEN_X + "</p><p>" + TEN_X + "</p>");
    placeCaret(upper, TEN_X.length() + 3);

    specman.press(KeyEvent.VK_DOWN);

    assertFocused(lower);
    assertTrue(caretIndex(lower) <= TEN_X.length(), "caret must be in the first row of the lower text");

    specman.press(KeyEvent.VK_UP);

    assertFocused(upper);
    assertTrue(caretIndex(upper) > TEN_X.length(), "caret must be in the last row of the upper text");
  }

  @Test
  void testDownInsideMultiRowTextDoesNotLeaveTheText() {
    TextEditArea upper = newStepText("<p>" + TEN_X + "</p><p>" + TEN_X + "</p>");
    newStepText(TEN_X);
    placeCaret(upper, 3);

    specman.press(KeyEvent.VK_DOWN);

    assertFocused(upper);
  }

  @Test
  void testEmptyStepInBetweenKeepsTheColumn() {
    TextEditArea upper = newStepText(TEN_X);
    TextEditArea empty = newStepText("");
    TextEditArea lower = newStepText(SIXTEEN_X);
    placeCaret(upper, 8);

    specman.press(KeyEvent.VK_DOWN);
    assertFocused(empty);
    specman.press(KeyEvent.VK_DOWN);

    assertFocused(lower);
    assertEquals(8, caretIndex(lower));
  }

  /** The intro and outro texts above and below the diagram are the first and last edit areas. */
  @Test
  void testDownFromLastStepGoesToOutroAndNoFurther() {
    newStepText(TEN_X);
    TextEditArea last = newStepText(TEN_X);
    placeCaret(last, 5);

    specman.press(KeyEvent.VK_DOWN);

    EditArea outro = specman.focusedArea();
    assertTrue(outro != last && outro.getPlainText().isEmpty(), "focus must have moved to the empty outro text");
    specman.press(KeyEvent.VK_DOWN);
    assertFocused(outro);
  }

  @Test
  void testShiftDownOnlyExtendsSelection() {
    TextEditArea upper = newStepText(TEN_X);
    newStepText(TEN_X);
    placeCaret(upper, 5);

    specman.press(KeyEvent.VK_DOWN, KeyEvent.SHIFT_DOWN_MASK);

    assertFocused(upper);
  }

  @Test
  void testNotEditableTextIsSkipped() {
    TextEditArea upper = newStepText(TEN_X);
    TextEditArea notEditable = newStepText(TEN_X);
    TextEditArea lower = newStepText(TEN_X);
    notEditable.setEditable(false);
    placeCaret(upper, 5);

    specman.press(KeyEvent.VK_DOWN);

    assertFocused(lower);
  }

  @Test
  void testWhileConditionLeadsToBodyAndBodyToFollower() {
    specman.click(CreateWhileStepOpButton.class);
    WhileSchrittView whileStep = specman.findStepByStepID("1", WhileSchrittView.class);
    TextEditArea condition = textOf(whileStep);
    TextEditArea body = textOf(specman.findSubstep(whileStep, "1.1", EinfacherSchrittView.class));
    specman.click(CreateSimpleStepOpButton.class);
    TextEditArea follower = textOf(specman.findStepByStepID("2", EinfacherSchrittView.class));
    condition.setText(TEN_X);
    body.setText(TEN_X);
    placeCaret(condition, 0);

    specman.press(KeyEvent.VK_DOWN);
    assertFocused(body);
    specman.press(KeyEvent.VK_DOWN);
    assertFocused(follower);
    specman.press(KeyEvent.VK_UP);
    assertFocused(body);
    specman.press(KeyEvent.VK_UP);
    assertFocused(condition);
  }

  /**
   * The if-else step's condition sits over the right branch, each branch has a header text (yes / no)
   * above its steps. The follower step spans both branches and picks the branch below the caret.
   */
  @Test
  void testIfElseBranchIsChosenByX() {
    specman.click(CreateIfElseStepOpButton.class);
    IfElseSchrittView ifElse = specman.findStepByStepID("1", IfElseSchrittView.class);
    TextEditArea condition = textOf(ifElse);
    TextEditArea left = textOf(specman.findSubstep(ifElse, "1.1", EinfacherSchrittView.class));
    TextEditArea right = textOf(specman.findSubstep(ifElse, "2.1", EinfacherSchrittView.class));
    specman.click(CreateSimpleStepOpButton.class);
    TextEditArea follower = textOf(specman.findStepByStepID("3", EinfacherSchrittView.class));
    follower.setText(SIXTY_X);

    placeCaret(condition, 0);
    specman.press(KeyEvent.VK_DOWN);
    assertTrue(specman.focusedArea() != left && specman.focusedArea() != right, "the branch header comes first");
    specman.press(KeyEvent.VK_DOWN);
    assertFocused(right);

    specman.press(KeyEvent.VK_DOWN);
    assertFocused(follower);

    placeCaret(follower, SIXTY_X.length());
    assertTrue(globalX(follower) > globalLeft(right), "test setup: caret must be below the right branch");
    specman.press(KeyEvent.VK_UP);
    assertFocused(right);

    placeCaret(follower, 0);
    specman.press(KeyEvent.VK_UP);
    assertFocused(left);

    specman.press(KeyEvent.VK_DOWN);
    assertFocused(follower);
  }

  @Test
  void testImageIsPassedThroughKeepingX() {
    TextEditArea upper = newStepText(TEN_X + TEN_X);
    placeCaret(upper, 10);
    upper.addImage(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB));
    EditContainer container = upper.getParent();
    ImageEditArea image = (ImageEditArea) container.getEditAreaAt(1);
    TextEditArea lower = container.getEditAreaAt(2).asTextArea();
    placeCaret(upper, 6);

    specman.press(KeyEvent.VK_DOWN);
    assertFocused(image);
    specman.press(KeyEvent.VK_DOWN);
    assertFocused(lower);
    assertEquals(6, caretIndex(lower));

    specman.press(KeyEvent.VK_UP);
    assertFocused(image);
    specman.press(KeyEvent.VK_UP);
    assertFocused(upper);
    assertEquals(6, caretIndex(upper));
  }

  @Test
  void testTableCellsAreWalkedThroughLikeInWord() {
    TextEditArea upper = newStepText(TEN_X + TEN_X);
    placeCaret(upper, 10);
    TableEditArea table = (TableEditArea) upper.addTable(2, 2);
    TextEditArea lower = upper.getParent().getEditAreaAt(2).asTextArea();
    List<TextEditArea> cells = textAreasIn(table);
    TextEditArea topLeft = cells.get(0);
    TextEditArea topRight = cells.get(1);
    TextEditArea bottomLeft = cells.get(2);
    TextEditArea bottomRight = cells.get(3);

    placeCaret(upper, 0);
    specman.press(KeyEvent.VK_DOWN);
    assertFocused(topLeft);
    specman.press(KeyEvent.VK_DOWN);
    assertFocused(bottomLeft);
    specman.press(KeyEvent.VK_DOWN);
    assertFocused(lower);
    specman.press(KeyEvent.VK_UP);
    assertFocused(bottomLeft);

    placeCaret(topRight, 0);
    specman.press(KeyEvent.VK_DOWN);
    assertFocused(bottomRight);
    specman.press(KeyEvent.VK_UP);
    assertFocused(topRight);
  }

  /** Appends a simple step behind the last one, like the user clicking the toolbar button, and fills its text. */
  private static TextEditArea newStepText(String html) {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    TextEditArea text = textOf(step);
    text.setText(html);
    return text;
  }

  /** The text areas in the cells of the table, row by row. */
  private static List<TextEditArea> textAreasIn(TableEditArea table) {
    TextAreaCollector collector = new TextAreaCollector();
    specman.traverseDiagram(collector);
    return collector.textAreas().stream()
      .filter(textArea -> SwingUtilities.isDescendingFrom(textArea, table))
      .toList();
  }

  private static TextEditArea textOf(AbstractSchrittView step) {
    return step.getFirstEditArea().asTextArea();
  }

  /** Puts the caret in front of the given visible character, like the user clicking there. */
  private static void placeCaret(TextEditArea text, int visibleIndex) {
    specman.layoutComponents();
    specman.focus(text);
    text.setCaretPosition(VISIBLE_TEXT_START + visibleIndex);
  }

  private static int caretIndex(TextEditArea text) {
    return text.getCaretPosition() - VISIBLE_TEXT_START;
  }

  private static void assertFocused(EditArea expected) {
    assertSame(expected, specman.focusedArea());
  }

  private static int globalX(TextEditArea text) {
    try {
      Rectangle2D caretRect = text.modelToView2D(text.getCaretPosition());
      return SwingUtilities.convertPoint(text, new Point((int) caretRect.getX(), 0), specman.getArbeitsbereich()).x;
    }
    catch (javax.swing.text.BadLocationException x) {
      throw new IllegalStateException(x);
    }
  }

  private static int globalLeft(TextEditArea text) {
    return SwingUtilities.convertPoint(text, new Point(0, 0), specman.getArbeitsbereich()).x;
  }
}
