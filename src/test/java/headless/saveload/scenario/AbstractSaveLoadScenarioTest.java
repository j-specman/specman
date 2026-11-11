package headless.saveload.scenario;

import headless.HeadlessExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static headless.HeadlessExtension.specman;

/**
 * Base class for scenario tests: small diagram constellations which are clicked together like a user would
 * do it, saved and loaded again. The reloaded diagram must show the same constellation.
 * <p>
 * The collection if these classes grows with the problem reports. Whenever a constellation turns out not to
 * survive saving and loading, it gets a scenario test of its own, which is kept afterward. The tests are
 * headless and therefore fast, so the collection can grow without slowing down the test run noticeably.
 * <p>
 * A scenario test builds its constellation with the toolbar buttons and the editing operations of
 * {@link headless.SpecmanFT}, calls {@link #saveAndReload()} and asserts on the reloaded diagram.
 */
@ExtendWith(HeadlessExtension.class)
abstract class AbstractSaveLoadScenarioTest {

  private static final String SCENARIO_FILENAME = "scenario.nsd";

  @TempDir
  protected Path directory;

  /** Saves the current diagram into the test's temporary directory and loads it again from there. */
  protected void saveAndReload() {
    File file = directory.resolve(SCENARIO_FILENAME).toFile();
    specman.setDiagrammDatei(file);
    specman.diagrammSpeichern(false);
    specman.diagrammLaden(file);
  }

  /** Like {@link #saveAndReload()}, additionally asserting that the diagram has exactly the given step numbers
   * before saving and again after loading. */
  protected void saveAndReload(String... expectedStepNumbers) {
    specman.assertStepNumbers(expectedStepNumbers);
    saveAndReload();
    specman.assertStepNumbers(expectedStepNumbers);
  }
}
