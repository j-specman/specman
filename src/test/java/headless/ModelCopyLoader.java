package headless;

import specman.SpecmanVersion;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads copies of model files into a {@link SpecmanFT}. Loading may create or delete a working copy beside the
 * loaded file, which must not happen to the original, so tests always load a copy in a directory of their own
 * as long as they don't intend to address exactly the creation and the work with working copys.
 */
public class ModelCopyLoader {

  private static final File REFERENCE_MODEL = new File(
    "specman-reference-v" + SpecmanVersion.getCompatibilityVersionPrefix() + "-model-v2.nsd");

  private final SpecmanFT specman;

  public ModelCopyLoader(SpecmanFT specman) {
    this.specman = specman;
  }

  /** Loads a copy of the given model file, located in the given directory. */
  public void loadModelCopy(File modelFile, Path copyDirectory) throws IOException {
    File copy = copyDirectory.resolve(modelFile.getName()).toFile();
    Files.copy(modelFile.toPath(), copy.toPath());
    specman.diagrammLaden(copy);
  }

  /**
   * Loads a copy of the reference model, located in the given directory. The reference model contains all step
   * types, edit areas and constellations Specman supports, including catch areas, change marks, tables,
   * list items and images, so it is the natural subject for tests which need a diagram containing everything.
   */
  public void loadReferenceModelCopy(Path copyDirectory) throws IOException {
    loadModelCopy(REFERENCE_MODEL, copyDirectory);
  }
}
