package classman.editor;

import classman.model.v001.ElementId_V001;

import java.util.Random;
import java.util.Set;

/** Makes up the IDs of new diagram elements, see {@link ElementId_V001}. */
class ElementIds {
  private final Random random;

  ElementIds(Random random) {
    this.random = random;
  }

  String newIdNotIn(Set<String> taken) {
    String id;
    do {
      id = ElementId_V001.random(random);
    }
    while (taken.contains(id));
    return id;
  }
}
