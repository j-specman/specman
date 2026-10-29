package headless.links;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import specman.editarea.TextEditArea;
import specman.link.AliasPanel;
import specman.link.LinkAliases;
import specman.view.AbstractSchrittView;
import specman.view.EinfacherSchrittView;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static specman.i18n.Messages.i18n;
import static specman.link.SpecmanLink.SCHEME;
import static specman.link.SpecmanLink.SEPARATOR;
import static specman.menubar.SpecmanMenuBar.OPEN_LINK_ITEM;

/** Navigating to the file and step of the link in the clipboard via the file menu. */
class NavigateToLinkTest extends AbstractLinkTest {

  private static void openLinkFromClipboard() {
    specman.click(OPEN_LINK_ITEM);
  }

  private static AbstractSchrittView focusedStep() {
    return specman.findeSchritt((TextEditArea) specman.focusedArea());
  }

  private static String withoutStepNumber(String link) {
    return link.substring(0, link.lastIndexOf(SEPARATOR));
  }

  private static String fileOnly(String link) {
    return link.substring(0, link.indexOf(SEPARATOR));
  }

  /** Scenario: The diagram is saved, the user copied the link of step 3 and works in step 1. Opening the link
   * from the clipboard puts the cursor into step 3. */
  @Test
  void testNavigateToStepInOpenFile(@TempDir Path directory) throws Exception {
    EinfacherSchrittView step1 = specman.clickNewSimpleStep();
    specman.clickNewSimpleStep();
    EinfacherSchrittView step3 = specman.clickNewSimpleStep();
    saveDiagram(directory, "UC-720.nsd");
    copyLink(step3);
    specman.focus(step1);

    openLinkFromClipboard();

    assertSame(step3, focusedStep());
    assertEquals(List.of(), specman.shownMessages());
  }

  /** Scenario: The user copied the link of step 2, which uses the alias uc720 for the file, and works in step 1.
   * Opening the link finds the file by the alias and puts the cursor into step 2. */
  @Test
  void testNavigateToStepByAlias(@TempDir Path directory) {
    EinfacherSchrittView step1 = specman.clickNewSimpleStep();
    EinfacherSchrittView step2 = specman.clickNewSimpleStep();
    saveDiagram(directory, "UC-720.nsd");
    AliasPanel panel = clickCopyLink(step2);
    typeAlias(panel, "uc720");
    click(panel, AliasPanel.OK_BUTTON);
    specman.focus(step1);

    openLinkFromClipboard();

    assertSame(step2, focusedStep());
    assertEquals(List.of(), specman.shownMessages());
    assertEquals(List.of(), specman.aliasesAskedFor());
  }

  /** Scenario: The user copied the link of step 2 in UC-720 and now works in another file. Opening the link
   * loads UC-720 and puts the cursor into step 2. */
  @Test
  void testNavigateToStepInOtherFile(@TempDir Path directory) throws Exception {
    specman.clickNewSimpleStep();
    EinfacherSchrittView step2 = specman.clickNewSimpleStep();
    File linkedFile = saveDiagram(directory, "UC-720.nsd");
    String stepId = step2.getId();
    copyLink(step2);
    saveDiagram(directory, "UC-721.nsd");

    openLinkFromClipboard();

    assertEquals(linkedFile, specman.getDiagrammDatei());
    assertEquals(stepId, focusedStep().getId());
    assertEquals(List.of(), specman.shownMessages());
  }

  /** Scenario: The link only names the file, not a step. Opening it loads the file and shows no complaint. */
  @Test
  void testLinkWithoutStepOpensFileOnly(@TempDir Path directory) throws Exception {
    specman.clickNewSimpleStep();
    EinfacherSchrittView step2 = specman.clickNewSimpleStep();
    File linkedFile = saveDiagram(directory, "UC-720.nsd");
    putIntoClipboard(fileOnly(copyLink(step2)));
    saveDiagram(directory, "UC-721.nsd");

    openLinkFromClipboard();

    assertEquals(linkedFile, specman.getDiagrammDatei());
    assertEquals(List.of(), specman.shownMessages());
  }

  /** Scenario: Copying the link out of another tool added blanks, a line break or a slash at the end. Opening the
   * link works nevertheless. */
  @ParameterizedTest
  @ValueSource(strings = {"/", " ", "\n", "/\n", " /"})
  void testLinkSurvivesWhatOtherToolsAppend(String suffix, @TempDir Path directory) throws Exception {
    EinfacherSchrittView step1 = specman.clickNewSimpleStep();
    EinfacherSchrittView step2 = specman.clickNewSimpleStep();
    saveDiagram(directory, "UC-720.nsd");
    putIntoClipboard(copyLink(step2) + suffix);
    specman.focus(step1);

    openLinkFromClipboard();

    assertSame(step2, focusedStep());
    assertEquals(List.of(), specman.shownMessages());
  }

  /** Scenario: The link has an alias the user never assigned to a file. The user is asked which file it stands for
   * and chooses UC-720. Specman remembers that, so the next link with this alias doesn't ask again. */
  @Test
  void testUnknownAliasAsksForFileAndRemembersAnswer(@TempDir Path directory) {
    EinfacherSchrittView step1 = specman.clickNewSimpleStep();
    EinfacherSchrittView step2 = specman.clickNewSimpleStep();
    File file = saveDiagram(directory, "UC-720.nsd");
    putIntoClipboard(SCHEME + "uc720" + SEPARATOR + step2.getId() + SEPARATOR + step2.getNumber());
    specman.answerFileChoicesWith(file);
    specman.focus(step1);

    openLinkFromClipboard();

    assertEquals(List.of("uc720"), specman.aliasesAskedFor());
    assertEquals(file, LinkAliases.fileOf("uc720"));
    assertSame(step2, focusedStep());
    specman.focus(step1);

    openLinkFromClipboard();

    assertEquals(List.of("uc720"), specman.aliasesAskedFor());
    assertSame(step2, focusedStep());
  }

  /** Scenario: The link has an alias the user never assigned to a file, and the user doesn't choose a file when
   * asked. Nothing happens and nothing is remembered. */
  @Test
  void testCancelledFileChoiceChangesNothing(@TempDir Path directory) {
    EinfacherSchrittView step1 = specman.clickNewSimpleStep();
    EinfacherSchrittView step2 = specman.clickNewSimpleStep();
    saveDiagram(directory, "UC-720.nsd");
    putIntoClipboard(SCHEME + "uc720" + SEPARATOR + step2.getId() + SEPARATOR + step2.getNumber());
    specman.answerFileChoicesWith(null);
    specman.focus(step1);

    openLinkFromClipboard();

    assertEquals(List.of("uc720"), specman.aliasesAskedFor());
    assertNull(LinkAliases.fileOf("uc720"));
    assertSame(step1, focusedStep());
    assertEquals(List.of(), specman.shownMessages());
  }

  /** Scenario: The link points to a file which doesn't exist (anymore). The user is told so. */
  @Test
  void testMissingFileIsReported(@TempDir Path directory) throws Exception {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    File file = saveDiagram(directory, "UC-720.nsd");
    String link = copyLink(step);
    saveDiagram(directory, "UC-721.nsd");
    org.junit.jupiter.api.Assertions.assertTrue(file.delete());
    putIntoClipboard(link);

    openLinkFromClipboard();

    assertEquals(List.of("msg.link.file.missing"), specman.shownMessages());
  }

  /** Scenario: The step the link points to doesn't exist in the file (anymore). The user is told so by a toast,
   * and the cursor stays where it is. */
  @Test
  void testMissingStepIsReported(@TempDir Path directory) throws Exception {
    EinfacherSchrittView step1 = specman.clickNewSimpleStep();
    EinfacherSchrittView step2 = specman.clickNewSimpleStep();
    saveDiagram(directory, "UC-720.nsd");
    putIntoClipboard(copyLink(step2).replace(step2.getId(), "nosuchstep"));
    specman.focus(step1);

    openLinkFromClipboard();

    assertSame(step1, focusedStep());
    assertEquals(List.of(i18n("msg.link.step.missing")), specman.shownMessages());
  }

  /** Scenario: The step was moved since the link was copied, so its number differs from the one in the link.
   * The step is addressed by its ID, so the cursor goes there, and the user is told that the number changed. */
  @Test
  void testChangedStepNumberIsReported(@TempDir Path directory) throws Exception {
    EinfacherSchrittView step1 = specman.clickNewSimpleStep();
    EinfacherSchrittView step2 = specman.clickNewSimpleStep();
    saveDiagram(directory, "UC-720.nsd");
    putIntoClipboard(withoutStepNumber(copyLink(step2)) + SEPARATOR + "9.9");
    specman.focus(step1);

    openLinkFromClipboard();

    assertSame(step2, focusedStep());
    assertEquals(List.of(i18n("msg.link.number.changed", "9.9", step2.getNumber())), specman.shownMessages());
  }

  /** Scenario: The link doesn't carry the step number. That's fine, no toast. */
  @Test
  void testLinkWithoutStepNumberIsFine(@TempDir Path directory) throws Exception {
    EinfacherSchrittView step1 = specman.clickNewSimpleStep();
    EinfacherSchrittView step2 = specman.clickNewSimpleStep();
    saveDiagram(directory, "UC-720.nsd");
    putIntoClipboard(withoutStepNumber(copyLink(step2)));
    specman.focus(step1);

    openLinkFromClipboard();

    assertSame(step2, focusedStep());
    assertEquals(List.of(), specman.shownMessages());
  }

  /** Scenario: The clipboard holds something which is not a Specman link. The user is told so. */
  @ParameterizedTest
  @ValueSource(strings = {"hello", "https://specman.org/uc-720", "specman://", "specman:///", "specman://,1,2", "specman://a,b,c,d"})
  void testNoLinkInClipboardIsReported(String text) {
    specman.clickNewSimpleStep();
    putIntoClipboard(text);

    openLinkFromClipboard();

    assertEquals(List.of(i18n("msg.link.clipboard.invalid")), specman.shownMessages());
  }

  /** Scenario: The clipboard is empty. The user is told that it holds no link. */
  @Test
  void testEmptyClipboardIsReported() {
    specman.clickNewSimpleStep();

    openLinkFromClipboard();

    assertEquals(List.of(i18n("msg.link.clipboard.invalid")), specman.shownMessages());
  }
}
