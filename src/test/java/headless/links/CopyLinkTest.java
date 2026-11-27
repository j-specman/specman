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
import static specman.link.SpecmanLink.FRAGMENT_SEPARATOR;
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
    assertEquals(SCHEME + file.toURI().getRawPath() + FRAGMENT_SEPARATOR + step.getId() + SEPARATOR + step.getNumber(), preview(panel));

    click(panel, AliasPanel.CANCEL_BUTTON);

    assertTrue(closed.get());
    assertNull(specman.clipboard().getContents(null));
    assertNotNull(clickCopyLink(step), "the user didn't decide on an alias, so the dialog must show up again");
    assertEquals(List.of(), specman.shownMessages());
  }

  /** Scenario: The diagram is saved in a file with blanks and a comma in its name. The user copies the link of
   * step 2 and decides against an alias. The clipboard then contains the link with the file, a hash sign, the step ID
   * and the step number, separated by a comma. The comma in the file name is encoded, so a link which names the file only
   * can't be mistaken for one of the old form, in which commas separated the step.
   * Copying the link of step 1 afterwards shows the dialog again, without an alias, as the user decided against one. */
  @Test
  void testCopyLinkWithoutAlias(@TempDir Path directory) throws Exception {
    EinfacherSchrittView step1 = specman.clickNewSimpleStep();
    EinfacherSchrittView step2 = specman.clickNewSimpleStep();
    File file = saveDiagram(directory, "UC 720, Eins.nsd");

    click(clickCopyLink(step2), AliasPanel.OK_BUTTON);

    String[] locationAndFragment = clipboardText().split(FRAGMENT_SEPARATOR, -1);
    assertEquals(2, locationAndFragment.length, String.join("|", locationAndFragment));
    String location = locationAndFragment[0];
    assertTrue(location.startsWith(SCHEME));
    assertTrue(location.endsWith("UC%20720%2C%20Eins.nsd"), location);
    assertEquals(file, new File(URI.create("file://" + location.substring(SCHEME.length()))));
    String[] step = locationAndFragment[1].split(SEPARATOR, -1);
    assertEquals(2, step.length, locationAndFragment[1]);
    assertEquals(step2.getId(), step[0]);
    assertEquals(step2.getNumber().toString(), step[1]);

    specman.clipboard().setContents(new StringSelection("something else"), null);
    AliasPanel again = clickCopyLink(step1);
    assertEquals("", aliasField(again).getText(), "the user decided against an alias, so none is offered");
    click(again, AliasPanel.OK_BUTTON);
    assertTrue(clipboardText().contains(FRAGMENT_SEPARATOR + step1.getId() + SEPARATOR), clipboardText());
    assertEquals(List.of(), specman.shownMessages());
  }

  /** Scenario: The user types an alias while copying a link. The link shown in the dialog uses the alias instead of
   * the file. After confirming, the link is in the clipboard. Copying the link to another step of the file shows the
   * dialog again, with the alias in the field and the link using it. */
  @Test
  void testCopyLinkWithAlias(@TempDir Path directory) throws Exception {
    EinfacherSchrittView step1 = specman.clickNewSimpleStep();
    EinfacherSchrittView step2 = specman.clickNewSimpleStep();
    saveDiagram(directory, "UC-720.nsd");
    AliasPanel panel = clickCopyLink(step1);

    typeAlias(panel, "uc720");

    String expectedLink = SCHEME + "uc720" + FRAGMENT_SEPARATOR + step1.getId() + SEPARATOR + step1.getNumber();
    assertEquals(expectedLink, preview(panel));
    click(panel, AliasPanel.OK_BUTTON);
    assertEquals(expectedLink, clipboardText());
    AliasPanel again = clickCopyLink(step2);
    assertEquals("uc720", aliasField(again).getText());
    assertNull(aliasField(again).getSelectedText(), "an existing alias must not be selected, a key press would replace it");
    assertEquals(SCHEME + "uc720" + FRAGMENT_SEPARATOR + step2.getId() + SEPARATOR + step2.getNumber(), preview(again));
    click(again, AliasPanel.OK_BUTTON);
    assertEquals(SCHEME + "uc720" + FRAGMENT_SEPARATOR + step2.getId() + SEPARATOR + step2.getNumber(), clipboardText());
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
