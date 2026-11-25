package headless.image.classman;

import org.junit.jupiter.api.Test;
import specman.ChangeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Classman gets the selection color of the change sets as text; only blue has one of its own. */
class ClassmanChangeColorsTest {

  /** Blue is gray, because the usual selection color is blue. */
  @Test
  void blueSelectsInGray() {
    assertEquals(java.awt.Color.GRAY, ChangeSet.ALL.get("blue").selectColor());
  }

  /** All the other change sets leave the selection color to the editor. */
  @Test
  void othersUseTheUsualOne() {
    for (ChangeSet changeSet : ChangeSet.ALL.values()) {
      if (!"blue".equals(changeSet.name)) {
        assertNull(changeSet.selectColor(), changeSet.name);
      }
    }
  }
}
