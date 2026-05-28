package specman.view;

/** A step may be removed from a sequence to either discard it or moving it to another location.
 * The appropriate removal methods might need to know that. E.g. a break step with a linked
 * catch sequence, will also discard this sequence in case it is discarded but keep it in case of
 * movement.
 * Another reason for step removal might be rejecting or accepting a change set. This is important
 * to know because in these cases we allow temporary inconsistencies during the change set clearance. */
public enum StepRemovalPurpose {
  Discard,
  Move,
  Reject,
  Accept;

  public boolean clearChangeSet() {
    return this == Reject || this == Accept;
  }
}
