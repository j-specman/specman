package builder;

import specman.model.v002.DiagramModel_V002;
import specman.model.v002.io.ModelRenumberer_V002;
import specman.model.v002.io.ModelSerializer_V002;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class DiagrammBuilder {
  private static final Path NSD_DIRECTORY = Path.of("target", "nsd-tests");
  private static final String DEFAULT_NAME = "frame0";
  private static final int DEFAULT_WIDTH = 700;
  private static final int DEFAULT_ZOOM = 100;
  private static final String DEFAULT_CHANGESET = "yellow";

  private static int nsdCounter = 0;

  private DiagramModel_V002 diagramm;
  private StepSequenceBuilder mainSequence;

  public DiagrammBuilder() {
    this.diagramm = new DiagramModel_V002();
    withDefaults();
  }

  public DiagrammBuilder withDefaults() {
    return withName(DEFAULT_NAME)
      .withWidth(DEFAULT_WIDTH)
      .withZoom(DEFAULT_ZOOM)
      .withChangeSetName(DEFAULT_CHANGESET);
  }

  public DiagrammBuilder withName(String name) {
    diagramm.name = name;
    return this;
  }

  public DiagrammBuilder withWidth(int width) {
    diagramm.width = width;
    return this;
  }

  public DiagrammBuilder withZoom(int zoomFactor) {
    diagramm.zoomFactor = zoomFactor;
    return this;
  }

  public DiagrammBuilder withChangeSetName(String changeSetName) {
    diagramm.changeSetName = changeSetName;
    return this;
  }

  public DiagrammBuilder withMainSequence(StepSequenceBuilder mainSequence) {
    this.mainSequence = mainSequence;
    return this;
  }

  public DiagramModel_V002 build() {
    diagramm.mainSequence = mainSequence.build();
    ModelRenumberer_V002.renumber(diagramm.mainSequence);
    return diagramm;
  }

  /** Writes the diagram to a file with a running number in {@link #NSD_DIRECTORY}, analogous
   *  to the PDF export tests. The files are kept for a manual look. */
  public File persist() {
    String nsdText = new ModelSerializer_V002().serialize(build());
    File nsdFile = nextNsdFile();
    try {
      Files.createDirectories(NSD_DIRECTORY);
      Files.write(nsdFile.toPath(), nsdText.getBytes(StandardCharsets.UTF_8));
    }
    catch (IOException iox) {
      throw new UncheckedIOException(iox);
    }
    return nsdFile;
  }

  private static File nextNsdFile() {
    nsdCounter++;
    return NSD_DIRECTORY.resolve("test" + nsdCounter + ".nsd").toAbsolutePath().toFile();
  }
}
