package specman;

import headless.HeadlessExtension;
import net.atlanticbb.tantlinger.ui.text.actions.HTMLAlignAction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import specman.editarea.TextEditArea;

import javax.swing.AbstractButton;
import java.io.File;
import java.util.List;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The four alignment buttons of the text group: exactly the button of the paragraph's alignment is selected,
 * and a click aligns the text, whether it is wrapped in an aligned div already or is a plain text.
 */
@ExtendWith(HeadlessExtension.class)
class AlignmentButtonsTest {

  private static final File DIAGRAM = new File("doc/anagramms.nsd");
  /** The step with a text in a centered div. */
  private static final String CENTERED_STEP_ID = "06dd40d1";

  private static final int LEFT = HTMLAlignAction.LEFT;
  private static final int CENTER = HTMLAlignAction.CENTER;
  private static final int RIGHT = HTMLAlignAction.RIGHT;
  private static final int JUSTIFY = HTMLAlignAction.JUSTIFY;

  @BeforeEach
  void loadDiagram() {
    specman.diagrammLaden(DIAGRAM);
  }

  @AfterEach
  void deleteWorkingCopy() {
    specman.getUndoManager().discardAllEdits();
    new File("doc/anagramms.nsd.wrk").delete();
  }

  private static TextEditArea plainTextArea() {
    return focused(specman.listAllSteps().get(0).getFirstEditArea().asTextArea());
  }

  private static TextEditArea centeredTextArea() {
    return focused(specman.listAllSteps().stream().filter(step -> step.getId().equals(CENTERED_STEP_ID))
      .findFirst().orElseThrow().getFirstEditArea().asTextArea());
  }

  private static TextEditArea focused(TextEditArea area) {
    return AlignmentButtonsSupport.focus(area);
  }

  private static List<AbstractButton> alignButtons() {
    return AlignmentButtonsSupport.alignButtons();
  }

  private static void click(int alignment) {
    AlignmentButtonsSupport.click(alignment);
  }

  private static void assertOnlySelected(int alignment) {
    List<AbstractButton> buttons = alignButtons();
    for (int i = 0; i < buttons.size(); i++) {
      assertEquals(i == alignment, buttons.get(i).isSelected(),
        "button " + i + " of " + HTMLAlignAction.ALIGNMENTS[i] + ", expected only " + HTMLAlignAction.ALIGNMENTS[alignment]);
    }
  }

  private static String html(TextEditArea area) {
    return area.getText().replaceAll("\\s+", " ");
  }

  @Test
  void clickAlignsPlainText() {
    TextEditArea area = plainTextArea();
    click(CENTER);
    assertTrue(html(area).contains("<div align=\"center\">"), html(area));
  }

  @Test
  void leftAlignedPlainTextStaysUnwrapped() {
    TextEditArea area = plainTextArea();
    String before = html(area);
    click(LEFT);
    assertEquals(before, html(area));
    assertOnlySelected(LEFT);
  }

  @Test
  void leftAfterAnotherAlignmentOfPlainTextAlignsLeft() {
    TextEditArea area = plainTextArea();
    click(CENTER);
    click(LEFT);
    assertTrue(html(area).contains("<div align=\"left\">"), html(area));
    assertEquals(1, html(area).split("<div").length - 1, html(area));
    assertOnlySelected(LEFT);
  }

  @Test
  void leftOnCenteredTextRealignsItsDiv() {
    TextEditArea area = centeredTextArea();
    click(LEFT);
    assertTrue(html(area).contains("<div align=\"left\">"), html(area));
    assertEquals(1, html(area).split("<div").length - 1, html(area));
    assertOnlySelected(LEFT);
  }

  @Test
  void clickRealignsTextInDiv() {
    TextEditArea area = centeredTextArea();
    click(RIGHT);
    assertTrue(html(area).contains("<div align=\"right\">"), html(area));
  }

  @Test
  void alignedPlainTextKeepsItsContentAndGetsNoSecondDiv() {
    TextEditArea area = plainTextArea();
    int length = area.getDocument().getLength();
    click(CENTER);
    click(RIGHT);
    assertEquals(1, html(area).split("<div").length - 1, html(area));
    assertTrue(html(area).contains("<div align=\"right\">"), html(area));
    assertEquals(length, area.getDocument().getLength());
  }

  @Test
  void onlyTheClickedButtonIsSelected() {
    plainTextArea();
    click(CENTER);
    assertOnlySelected(CENTER);
    click(RIGHT);
    assertOnlySelected(RIGHT);
    click(JUSTIFY);
    assertOnlySelected(JUSTIFY);
  }

  @Test
  void clickingTheSelectedButtonKeepsItSelected() {
    centeredTextArea();
    assertOnlySelected(CENTER);
    click(CENTER);
    assertOnlySelected(CENTER);
  }

  @Test
  void buttonOfTheParagraphAlignmentIsSelectedOnFocus() {
    centeredTextArea();
    assertOnlySelected(CENTER);
  }

  @Test
  void plainTextCountsAsLeftAligned() {
    plainTextArea();
    assertOnlySelected(LEFT);
  }

  @Test
  void selectionFollowsTheFocusedText() {
    centeredTextArea();
    plainTextArea();
    assertOnlySelected(LEFT);
    centeredTextArea();
    assertOnlySelected(CENTER);
  }

}
