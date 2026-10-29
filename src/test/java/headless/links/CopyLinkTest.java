package headless.links;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import specman.link.AliasPanel;
import specman.link.CopyStepLinkMenuItem;
import specman.view.EinfacherSchrittView;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static specman.i18n.Messages.i18n;
import static specman.link.LinkAliases.PROBLEM_TAKEN;
import static specman.link.SpecmanLink.SCHEME;
import static specman.link.SpecmanLink.SEPARATOR;

/** Copying a global link to a step via the step number's context menu. */
class CopyLinkTest extends AbstractLinkTest {

  /** Scenario: The diagram was not saved yet, so there is no file name a link could point to.
   * The user can see the menu item, but can't use it. */
  @Test
  void testCopyLinkDisabledForUnsavedDiagram() {
    EinfacherSchrittView step = specman.clickNewSimpleStep();

    assertFalse(specman.contextMenuItem(step, CopyStepLinkMenuItem.class).isEnabled());
  }

  /** Scenario: The diagram is saved. The user chooses to copy the link of step 1. A dialog asks for an alias, which
   * shows the link as it would look without an alias. The user cancels, so the dialog closes and the clipboard stays
   * empty. As the user didn't decide anything, the next try asks again. */
  @Test
  void testCancelAliasPanel(@TempDir Path directory) {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    File file = saveDiagram(directory, "UC-720.nsd");
    AliasPanel panel = clickCopyLink(step);
    AtomicBoolean closed = new AtomicBoolean();
    panel.onClose(() -> closed.set(true));
    assertEquals(SCHEME + file.toURI().getRawPath() + SEPARATOR + step.getId() + SEPARATOR + step.getNumber(), preview(panel));

    click(panel, AliasPanel.CANCEL_BUTTON);

    assertTrue(closed.get());
    assertNull(specman.clipboard().getContents(null));
    assertNotNull(clickCopyLink(step), "the user didn't decide on an alias, so the dialog must show up again");
    assertEquals(List.of(), specman.shownMessages());
  }

  /** Scenario: The diagram is saved in a file with blanks and a comma in its name. The user copies the link of
   * step 2 and decides against an alias. The clipboard then contains the link with the file, the step ID and the
   * step number, separated by commas. The comma in the file name is encoded, so it can't be mistaken for a separator.
   * Copying the link of step 1 afterwards doesn't ask again. */
  @Test
  void testCopyLinkWithoutAlias(@TempDir Path directory) throws Exception {
    EinfacherSchrittView step1 = specman.clickNewSimpleStep();
    EinfacherSchrittView step2 = specman.clickNewSimpleStep();
    File file = saveDiagram(directory, "UC 720, Eins.nsd");

    click(clickCopyLink(step2), AliasPanel.OK_BUTTON);

    String[] parts = clipboardText().split(SEPARATOR, -1);
    assertEquals(3, parts.length, String.join("|", parts));
    assertTrue(parts[0].startsWith(SCHEME));
    assertTrue(parts[0].endsWith("UC%20720%2C%20Eins.nsd"), parts[0]);
    assertEquals(file, new File(URI.create("file://" + parts[0].substring(SCHEME.length()))));
    assertEquals(step2.getId(), parts[1]);
    assertEquals(step2.getNumber().toString(), parts[2]);

    specman.clipboard().setContents(new StringSelection("something else"), null);
    assertNull(clickCopyLink(step1), "the user already decided, so the dialog must not show up again");
    assertEquals(step1.getId(), clipboardText().split(SEPARATOR, -1)[1]);
    assertEquals(List.of(), specman.shownMessages());
  }

  /** Scenario: The user types an alias while copying a link. The link shown in the dialog uses the alias instead of
   * the file. After confirming, the link is in the clipboard. Links to other steps of the file use the alias right
   * away, without asking. */
  @Test
  void testCopyLinkWithAlias(@TempDir Path directory) throws Exception {
    EinfacherSchrittView step1 = specman.clickNewSimpleStep();
    EinfacherSchrittView step2 = specman.clickNewSimpleStep();
    saveDiagram(directory, "UC-720.nsd");
    AliasPanel panel = clickCopyLink(step1);

    typeAlias(panel, "uc720");

    String expectedLink = SCHEME + "uc720" + SEPARATOR + step1.getId() + SEPARATOR + step1.getNumber();
    assertEquals(expectedLink, preview(panel));
    click(panel, AliasPanel.OK_BUTTON);
    assertEquals(expectedLink, clipboardText());
    assertNull(clickCopyLink(step2));
    assertEquals(SCHEME + "uc720" + SEPARATOR + step2.getId() + SEPARATOR + step2.getNumber(), clipboardText());
  }

  /** Scenario: The user types or pastes an alias with forbidden characters or too long. The characters an alias
   * must not contain never make it into the field, and neither does anything beyond 20 characters. */
  @ParameterizedTest
  @CsvSource({"'uc 720',uc720", "uc.720,uc720", "über,ber", "Uc-720_1,Uc-720_1", "012345678901234567890,01234567890123456789"})
  void testForbiddenCharactersAndExcessLengthAreNotTyped(String typed, String expected, @TempDir Path directory) {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    saveDiagram(directory, "UC-720.nsd");
    AliasPanel panel = clickCopyLink(step);

    typeAlias(panel, typed);

    assertEquals(expected, aliasField(panel).getText());
    assertTrue(okButton(panel).isEnabled());
    assertEquals(" ", problem(panel));
  }

  /** Scenario: The alias uc720 is typed. The user selects it and types a character which is forbidden. Nothing
   * happens, the selected alias isn't replaced by nothing. */
  @Test
  void testTypingForbiddenCharacterOverSelectionChangesNothing(@TempDir Path directory) {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    saveDiagram(directory, "UC-720.nsd");
    AliasPanel panel = clickCopyLink(step);
    typeAlias(panel, "uc720");

    typeAlias(panel, ".");

    assertEquals("uc720", aliasField(panel).getText());
  }

  /** Scenario: A pasted alias is too long for the room left in the field. It is shortened to fit. */
  @Test
  void testPastedAliasIsShortenedToFit(@TempDir Path directory) {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    saveDiagram(directory, "UC-720.nsd");
    AliasPanel panel = clickCopyLink(step);
    typeAlias(panel, "0123456789012345");

    aliasField(panel).setCaretPosition(aliasField(panel).getText().length());
    aliasField(panel).replaceSelection("abcdefgh");

    assertEquals("0123456789012345abcd", aliasField(panel).getText());
  }

  /** Scenario: One file has the alias uc720. The user tries the same alias for another file, in different case.
   * Aliases must be unique regardless of case, so the dialog doesn't let the user confirm. The other file may
   * be used without an alias, though. */
  @Test
  void testAliasUsedForOtherFileCantBeConfirmed(@TempDir Path directory) {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    saveDiagram(directory, "UC-720.nsd");
    AliasPanel firstFilePanel = clickCopyLink(step);
    typeAlias(firstFilePanel, "uc720");
    click(firstFilePanel, AliasPanel.OK_BUTTON);
    saveDiagram(directory, "UC-721.nsd");
    AliasPanel secondFilePanel = clickCopyLink(step);

    typeAlias(secondFilePanel, "UC720");

    assertFalse(okButton(secondFilePanel).isEnabled());
    assertEquals(i18n(PROBLEM_TAKEN), problem(secondFilePanel));

    typeAlias(secondFilePanel, "");

    assertTrue(okButton(secondFilePanel).isEnabled());
  }
}
