package headless.links;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import specman.link.AliasPanel;
import specman.link.classman.ClassmanLinkAliasPanel;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The dialog which asks for the alias of the file of a linked Classman diagram shows the link the model gets. */
class ClassmanLinkAliasPanelTest extends AbstractLinkTest {
  private final List<String> accepted = new ArrayList<>();

  private AliasPanel panel(File file, String diagramId, String diagramName) {
    return new ClassmanLinkAliasPanel(file, diagramId, diagramName, accepted::add);
  }

  /** Scenario: The user types an alias. The preview shows the link, with the ID and the name of the diagram. */
  @Test
  void testPreviewShowsTheClassmanLink(@TempDir Path directory) {
    AliasPanel panel = panel(directory.resolve("hilfe.cld").toFile(), "0a1b2c3d", "Hilfe Versionierung");

    typeAlias(panel, "hilfe");

    assertEquals("classman://hilfe,0a1b2c3d,Hilfe Versionierung", preview(panel));
  }

  /** Scenario: The user types no alias. The link names the path of the file. */
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

    assertEquals(List.of("hilfe"), accepted);
  }

  /** Scenario: The user cancels. Nothing is handed over. */
  @Test
  void testCancelHandsOverNothing(@TempDir Path directory) {
    AliasPanel panel = panel(directory.resolve("hilfe.cld").toFile(), null, null);

    typeAlias(panel, "hilfe");
    click(panel, AliasPanel.CANCEL_BUTTON);

    assertEquals(List.of(), accepted);
  }
}
