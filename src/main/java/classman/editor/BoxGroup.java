package classman.editor;

import classman.model.v001.AbstractBoxModel_V001;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/** The boxes which are selected together. Only boxes can be: classes, notes, association classes. */
class BoxGroup {
  private final Set<AbstractBoxModel_V001> members = new LinkedHashSet<>();

  boolean isEmpty() { return members.isEmpty(); }

  boolean contains(AbstractBoxModel_V001 box) { return members.contains(box); }

  List<AbstractBoxModel_V001> members() { return List.copyOf(members); }

  void replace(Collection<AbstractBoxModel_V001> boxes) {
    members.clear();
    members.addAll(boxes);
  }

  void clear() { members.clear(); }

  void retain(Predicate<AbstractBoxModel_V001> keep) { members.removeIf(box -> !keep.test(box)); }
}
