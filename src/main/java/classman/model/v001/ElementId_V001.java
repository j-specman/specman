package classman.model.v001;

import java.util.Random;

/** The ID of a diagram element, which a diagram itself may have as well: 8 lower-case hex characters, like the IDs of
 * Specman steps. */
public final class ElementId_V001 {
  private static final int LENGTH = 8;

  private ElementId_V001() {}

  /** @return a random ID, which may be taken already: whoever has to avoid duplicates has to check */
  public static String random(Random random) {
    return String.format("%0" + LENGTH + "x", random.nextInt());
  }
}
