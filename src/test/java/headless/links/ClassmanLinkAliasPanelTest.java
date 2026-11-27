package headless.links;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import specman.link.AliasPanel;
import specman.link.LinkLocation;
import specman.link.classman.ClassmanLinkAliasPanel;

import javax.swing.JButton;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static headless.SpecmanFT.findByName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The dialog which asks how the link of a linked Classman diagram names the file shows the link the model gets. */
class ClassmanLinkAliasPanelTest extends AbstractLinkTest {
  private final List<LinkLocation> accepted = new ArrayList<>();

  private AliasPanel panel(File file, String diagramId, String diagramName) {
    return new ClassmanLinkAliasPanel(file, diagramId, diagramName, LinkLocation.relative(file, file.getParentFile()),
      accepted::add);
  }

  private static JButton button(AliasPanel panel, String name) {
    return findByName(panel, name, JButton.class);
  }

  /** Scenario: The user types an alias. The preview shows the link, with the ID and the name of the diagram. */
  @Test
  void testPreviewShowsTheClassmanLink(@TempDir Path directory) {
    AliasPanel panel = panel(directory.resolve("hilfe.cld").toFile(), "0a1b2c3d", "Hilfe Versionierung");

    typeAlias(panel, "hilfe");

    assertEquals("classman://hilfe#0a1b2c3d,Hilfe Versionierung", preview(panel));
  }

  /** Scenario: The user types no alias. The link names the absolute path of the file. */
  @Test
  void testPreviewWithoutAliasShowsThePath(@TempDir Path directory) {
    File file = directory.resolve("hilfe.cld").toFile();
    AliasPanel panel = panel(file, null, null);

    typeAlias(panel, "");

    assertEquals("classman://" + file.toURI().getRawPath(), preview(panel));
  }

  /** Scenario: The user confirms. The alias is handed over. */
  @Test
  void testOkHandsOverTheAlias(@TempDir Path directory) {
    AliasPanel panel = panel(directory.resolve("hilfe.cld").toFile(), null, null);

    typeAlias(panel, "hilfe");
    click(panel, AliasPanel.OK_BUTTON);

    assertEquals(List.of(LinkLocation.alias("hilfe")), accepted);
  }

  /** Scenario: The user cancels. Nothing is handed over. */
  @Test
  void testCancelHandsOverNothing(@TempDir Path directory) {
    AliasPanel panel = panel(directory.resolve("hilfe.cld").toFile(), null, null);

    typeAlias(panel, "hilfe");
    click(panel, AliasPanel.CANCEL_BUTTON);

    assertEquals(List.of(), accepted);
  }

  /** Scenario: The user types an alias and then clicks the button for the relative path. What was typed is gone, and
   * the link has the path relative to the document. Confirming hands the path over. */
  @Test
  void testRelativeButtonReplacesTheAlias(@TempDir Path directory) {
    AliasPanel panel = panel(directory.resolve("hilfe.cld").toFile(), "0a1b2c3d", null);
    typeAlias(panel, "hilfe");

    click(panel, AliasPanel.RELATIVE_BUTTON);

    assertEquals("", aliasField(panel).getText());
    assertEquals("classman://./hilfe.cld#0a1b2c3d", preview(panel));
    click(panel, AliasPanel.OK_BUTTON);
    assertEquals("./hilfe.cld", accepted.get(0).text());
  }

  /** Scenario: The user clicks the button for the absolute path. The link has the path, and an alias typed before is
   * gone. */
  @Test
  void testAbsoluteButtonReplacesTheAlias(@TempDir Path directory) {
    File file = directory.resolve("hilfe.cld").toFile();
    AliasPanel panel = panel(file, null, null);
    typeAlias(panel, "hilfe");

    click(panel, AliasPanel.ABSOLUTE_BUTTON);

    assertEquals("", aliasField(panel).getText());
    assertEquals("classman://" + file.toURI().getRawPath(), preview(panel));
  }

  /** Scenario: After a button, the user types an alias, and then takes it away again. Typing makes the alias count
   * again, and an empty alias is the absolute path, as it was before the buttons. */
  @Test
  void testTypingAfterAButtonGoesBackToTheAlias(@TempDir Path directory) {
    File file = directory.resolve("hilfe.cld").toFile();
    AliasPanel panel = panel(file, null, null);
    click(panel, AliasPanel.RELATIVE_BUTTON);

    typeAlias(panel, "hilfe");
    assertEquals("classman://hilfe", preview(panel));

    typeAlias(panel, "");
    assertEquals("classman://" + file.toURI().getRawPath(), preview(panel));
  }

  /** Scenario: A button changes the link once. The user can click any button again later. */
  @Test
  void testButtonsCanBeClickedAnyTime(@TempDir Path directory) {
    File file = directory.resolve("hilfe.cld").toFile();
    AliasPanel panel = panel(file, null, null);

    click(panel, AliasPanel.RELATIVE_BUTTON);
    click(panel, AliasPanel.ABSOLUTE_BUTTON);
    click(panel, AliasPanel.RELATIVE_BUTTON);

    assertEquals("classman://./hilfe.cld", preview(panel));
    assertTrue(button(panel, AliasPanel.ABSOLUTE_BUTTON).isEnabled());
  }

  /** Scenario: The document has never been saved, so there is no path relative to it. The button is disabled. */
  @Test
  void testRelativeButtonIsDisabledWithoutRelativePath(@TempDir Path directory) {
    AliasPanel panel = new ClassmanLinkAliasPanel(directory.resolve("hilfe.cld").toFile(), null, null, null, accepted::add);

    assertFalse(button(panel, AliasPanel.RELATIVE_BUTTON).isEnabled());
  }

  /** Scenario: A diagram is in another file than the document, so there is no link without a file. The button is not
   * there. */
  @Test
  void testThereIsNoLocalButton(@TempDir Path directory) {
    AliasPanel panel = panel(directory.resolve("hilfe.cld").toFile(), null, null);

    assertNull(button(panel, AliasPanel.LOCAL_BUTTON));
  }
}
