package classman.editor;

import classman.model.v001.AbstractMemberModel_V001;

import java.util.List;

/** Changes the order of the members of one compartment. Only the shown members count as places: a member which is hidden
 * keeps its place in front of the shown member which follows it. */
final class MemberOrder {
  private MemberOrder() {
  }

  /** @return true if the diagram draws a row for the member */
  static boolean isShown(AbstractMemberModel_V001 member) {
    return !member.hidden && !member.hiddenMarker;
  }

  /** @return the place of the member among the shown ones of the list, -1 if it isn't one of them */
  static int placeOf(List<? extends AbstractMemberModel_V001> members, AbstractMemberModel_V001 member) {
    return members.stream().filter(MemberOrder::isShown).toList().indexOf(member);
  }

  /** Puts the member at a place among the shown members, counted without the member itself: 0 is in front of the
   * first of the others, as many as there are others is behind the last.
   * @return true if the order has changed */
  static boolean moveToPlace(List<? extends AbstractMemberModel_V001> members, AbstractMemberModel_V001 member,
                             int place) {
    return moveCaptured(members, member, place);
  }

  /** Moves the member by places among the shown members, nothing happens beyond the first and the last.
   * @return true if the order has changed */
  static boolean moveBy(List<? extends AbstractMemberModel_V001> members, AbstractMemberModel_V001 member, int places) {
    int place = placeOf(members, member);
    int shown = (int) members.stream().filter(MemberOrder::isShown).count();
    boolean canMove = place >= 0 && place + places >= 0 && place + places < shown;
    return canMove && moveCaptured(members, member, place + places);
  }

  /** Gives the list the order which an earlier copy of it had, with the same members.
   * @return true if the order has changed */
  @SuppressWarnings("unchecked")
  static boolean restore(List<? extends AbstractMemberModel_V001> members, List<AbstractMemberModel_V001> earlier) {
    List<AbstractMemberModel_V001> editable = (List<AbstractMemberModel_V001>) members;
    boolean hasChanged = !earlier.equals(editable);
    editable.clear();
    editable.addAll(earlier);
    return hasChanged;
  }

  private static <M extends AbstractMemberModel_V001> boolean moveCaptured(List<M> members,
                                                                            AbstractMemberModel_V001 member,
                                                                            int place) {
    int from = members.indexOf(member);
    if (from < 0 || !isShown(member)) {
      return false;
    }
    M moved = members.get(from);
    List<M> others = members.stream().filter(other -> other != moved && isShown(other)).toList();
    if (others.isEmpty()) {
      return false;
    }
    int clamped = Math.max(0, Math.min(place, others.size()));
    List<M> before = List.copyOf(members);
    members.remove(from);
    int to = clamped < others.size()
      ? members.indexOf(others.get(clamped))
      : members.indexOf(others.get(others.size() - 1)) + 1;
    members.add(to, moved);
    return !before.equals(members);
  }
}
