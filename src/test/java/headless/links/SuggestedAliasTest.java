package headless.links;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import specman.link.AliasPanel;
import specman.link.LinkAliases;
import specman.view.EinfacherSchrittView;

import java.io.File;
import java.nio.file.Path;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static specman.link.SpecmanLink.SCHEME;
import static specman.link.SpecmanLink.SEPARATOR;

/** The alias dialog suggests an alias made like the ones the user has already chosen for other files. */
class SuggestedAliasTest extends AbstractLinkTest {

  /** Scenario: The file UC-720.nsd has the alias uc720. The user copies a link in UC-721.nsd. The dialog offers the
   * alias uc721 with the link already using it. The user confirms, so the alias is assigned and the link copied. */
  @Test
  void testSuggestedAliasCanBeConfirmed(@TempDir Path directory) throws Exception {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    saveDiagram(directory, "UC-720.nsd");
    AliasPanel firstFilePanel = clickCopyLink(step);
    typeAlias(firstFilePanel, "uc720");
    click(firstFilePanel, AliasPanel.OK_BUTTON);
    File file = saveDiagram(directory, "UC-721.nsd");

    AliasPanel panel = clickCopyLink(step);

    String expectedLink = SCHEME + "uc721" + SEPARATOR + step.getId() + SEPARATOR + step.getNumber();
    assertEquals("uc721", aliasField(panel).getText());
    assertEquals("uc721", aliasField(panel).getSelectedText());
    assertEquals(expectedLink, preview(panel));
    click(panel, AliasPanel.OK_BUTTON);
    assertEquals("uc721", LinkAliases.aliasOf(file));
    assertEquals(expectedLink, clipboardText());
  }

  /** Scenario: The file UC-720.nsd has the alias uc720. The user copies a link in a file which would get the same
   * alias, e.g. a copy of it. Aliases must be unique, so nothing is suggested. */
  @Test
  void testTakenAliasIsNotSuggested(@TempDir Path directory) {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    saveDiagram(directory, "UC-720.nsd");
    AliasPanel firstFilePanel = clickCopyLink(step);
    typeAlias(firstFilePanel, "uc720");
    click(firstFilePanel, AliasPanel.OK_BUTTON);
    saveDiagram(directory, "UC-720 Kopie.nsd");

    AliasPanel panel = clickCopyLink(step);

    assertEquals("", aliasField(panel).getText());
    assertTrue(okButton(panel).isEnabled());
  }

  /** Scenario: The file UC-720.nsd has the alias uc720. The user copies a link in a file whose name is built
   * differently, starting with digits instead of letters. Nothing is suggested. */
  @Test
  void testNothingIsSuggestedForFileNamedDifferently(@TempDir Path directory) {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    saveDiagram(directory, "UC-720.nsd");
    AliasPanel firstFilePanel = clickCopyLink(step);
    typeAlias(firstFilePanel, "uc720");
    click(firstFilePanel, AliasPanel.OK_BUTTON);
    saveDiagram(directory, "2026-10-29 Protokoll.nsd");

    AliasPanel panel = clickCopyLink(step);

    assertEquals("", aliasField(panel).getText());
  }
}
