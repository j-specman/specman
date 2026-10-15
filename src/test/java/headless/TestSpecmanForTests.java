package headless;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import specman.Specman;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@ExtendWith(HeadlessExtension.class)
class TestSpecmanForTests {

  @Test
  void testSpecmanCanBeInstantiated() throws Exception {
    SpecmanForTests specman = new SpecmanForTests();

    assertSame(specman, Specman.editor());
    assertNotNull(specman.getScrollPane());
    assertNotNull(specman.getArbeitsbereich());
    assertNotNull(specman.getHauptSequenz());
  }
}
