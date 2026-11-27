package specman.editarea.image.classman;

/** What the user decides when asked before an operation changes the marks of a change set in linked diagrams. */
public enum LinkedChangesAnswer {
  /** The operation is not done at all. */
  CANCEL,
  /** The operation also changes the linked diagrams, and so their files. */
  CHANGE_LINKED,
  /** The operation leaves the linked diagrams as they are. */
  LEAVE_LINKED
}
