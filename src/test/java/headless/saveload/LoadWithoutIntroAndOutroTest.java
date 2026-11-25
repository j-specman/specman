package headless.saveload;

import headless.HeadlessExtension;
import headless.ModelCopyLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static builder.BuilderFactory.diagramm;
import static builder.BuilderFactory.sequence;
import static builder.BuilderFactory.simple;
import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** The grammar makes intro and outro optional, so a file which has neither, e.g. one which an agent wrote, can be
 * loaded. */
@ExtendWith(HeadlessExtension.class)
class LoadWithoutIntroAndOutroTest {

  /** Scenario: The file has a main sequence only. It loads, and intro and outro are empty. */
  @Test
  void testFileWithMainSequenceOnlyLoads(@TempDir Path directory) throws IOException {
    File file = diagramm().withMainSequence(sequence().withStep(simple("first"))).persist();

    new ModelCopyLoader(specman).loadModelCopy(file, directory);

    specman.assertStepNumbers(List.of("1"));
    assertEquals(List.of(), specman.shownMessages());
  }
}
