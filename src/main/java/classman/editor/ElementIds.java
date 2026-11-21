package classman.editor;

import java.util.Random;
import java.util.Set;

/** Makes up the IDs of new diagram elements: 8 lower-case hex characters, like Specman step IDs. */
class ElementIds {
  private static final int LENGTH = 8;

  private final Random random;

  ElementIds(Random random) {
    this.random = random;
  }

  String newIdNotIn(Set<String> taken) {
    String id;
    do {
      id = String.format("%0" + LENGTH + "x", random.nextInt());
    }
    while (taken.contains(id));
    return id;
  }
}
