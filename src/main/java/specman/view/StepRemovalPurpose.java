package specman.view;

/** A step may be removed from a sequence to either discard it or moving it to another location.
 * The appropriate removal methods might need to know that. E.g. a break step with a linked
 * catch sequence, will also discard this sequence in case it is discarded but keep it in case of
 * movement.
 * Another reason for step removal might be the reversal or the confirmation of a change set.
 * in these cases the */
public enum StepRemovalPurpose {
  Discard,
  Move,
  Revert,
  Confirm;

  public boolean clearChangeSet() {
    return this == Revert || this == Confirm;
  }
}
