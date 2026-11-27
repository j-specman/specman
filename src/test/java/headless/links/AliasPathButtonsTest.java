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
import static specman.link.SpecmanLink.FRAGMENT_SEPARATOR;
import static specman.link.SpecmanLink.SCHEME;
import static specman.link.SpecmanLink.SEPARATOR;

/** Instead of an alias, the user can choose how the link to a step names the file: by the absolute path, by the path
 * relative to the document, or not at all, which is a link into the document the link is in. */
class AliasPathButtonsTest extends AbstractLinkTest {

  private static String stepPart(EinfacherSchrittView step) {
    return FRAGMENT_SEPARATOR + step.getId() + SEPARATOR + step.getNumber();
  }

  /** Scenario: The user clicks the button for the local link. The link has no file, and the clipboard gets it. */
  @Test
  void testLocalLink(@TempDir Path directory) throws Exception {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    File file = saveDiagram(directory, "UC-720.nsd");
    AliasPanel panel = clickCopyLink(step);

    click(panel, AliasPanel.LOCAL_BUTTON);

    assertEquals(SCHEME + stepPart(step), preview(panel));
    click(panel, AliasPanel.OK_BUTTON);
    assertEquals(SCHEME + stepPart(step), clipboardText());
    assertTrue(LinkAliases.isAssigned(file), "the file counts as decided");
    assertEquals("", LinkAliases.aliasOf(file));
  }

  /** Scenario: The user clicks the button for the relative path. The document isn't known which the link is pasted
   * into, so it is the file in the same directory. */
  @Test
  void testRelativeLink(@TempDir Path directory) throws Exception {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    saveDiagram(directory, "UC 720.nsd");
    AliasPanel panel = clickCopyLink(step);

    click(panel, AliasPanel.RELATIVE_BUTTON);

    assertEquals(SCHEME + "./UC%20720.nsd" + stepPart(step), preview(panel));
    click(panel, AliasPanel.OK_BUTTON);
    assertEquals(SCHEME + "./UC%20720.nsd" + stepPart(step), clipboardText());
  }

  /** Scenario: The user types an alias and then clicks the button for the absolute path. The alias is gone. */
  @Test
  void testAbsoluteButtonTakesTheAliasAway(@TempDir Path directory) throws Exception {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    File file = saveDiagram(directory, "UC-720.nsd");
    AliasPanel panel = clickCopyLink(step);
    typeAlias(panel, "uc720");

    click(panel, AliasPanel.ABSOLUTE_BUTTON);

    assertEquals("", aliasField(panel).getText());
    assertEquals(SCHEME + file.toURI().getRawPath() + stepPart(step), preview(panel));
  }

  /** Scenario: The user has an alias for the file and chooses the local link once. The alias stays, and the next time the
   * dialog has it in the field again: a button is a choice for one link only. */
  @Test
  void testChoosingAPathKeepsTheAlias(@TempDir Path directory) throws Exception {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    File file = saveDiagram(directory, "UC-720.nsd");
    AliasPanel first = clickCopyLink(step);
    typeAlias(first, "uc720");
    click(first, AliasPanel.OK_BUTTON);

    AliasPanel second = clickCopyLink(step);
    assertEquals("uc720", aliasField(second).getText());
    click(second, AliasPanel.LOCAL_BUTTON);
    click(second, AliasPanel.OK_BUTTON);

    assertEquals(SCHEME + stepPart(step), clipboardText());
    assertEquals("uc720", LinkAliases.aliasOf(file));
    AliasPanel third = clickCopyLink(step);
    assertEquals("uc720", aliasField(third).getText());
    assertEquals(SCHEME + "uc720" + stepPart(step), preview(third));
  }

  /** Scenario: The user has an alias, and types another one. The file has the new alias from now on. */
  @Test
  void testTypedAliasReplacesTheOldOne(@TempDir Path directory) throws Exception {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    File file = saveDiagram(directory, "UC-720.nsd");
    AliasPanel first = clickCopyLink(step);
    typeAlias(first, "uc720");
    click(first, AliasPanel.OK_BUTTON);

    AliasPanel second = clickCopyLink(step);
    typeAlias(second, "kunden");
    click(second, AliasPanel.OK_BUTTON);

    assertEquals(SCHEME + "kunden" + stepPart(step), clipboardText());
    assertEquals("kunden", LinkAliases.aliasOf(file));
  }

  /** Scenario: The user has decided against an alias for the file by choosing a path. The dialog doesn't suggest one
   * again, although the aliases of other files would allow it. */
  @Test
  void testNoAliasIsSuggestedAfterDecidingAgainstOne(@TempDir Path directory) throws Exception {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    saveDiagram(directory, "UC-720.nsd");
    AliasPanel firstFile = clickCopyLink(step);
    typeAlias(firstFile, "uc720");
    click(firstFile, AliasPanel.OK_BUTTON);
    saveDiagram(directory, "UC-721.nsd");
    AliasPanel suggestion = clickCopyLink(step);
    assertEquals("uc721", aliasField(suggestion).getText(), "the other alias allows a suggestion");
    click(suggestion, AliasPanel.LOCAL_BUTTON);
    click(suggestion, AliasPanel.OK_BUTTON);

    AliasPanel again = clickCopyLink(step);

    assertEquals("", aliasField(again).getText());
  }
}
