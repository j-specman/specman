package specman.link.resolver;

/** The outcome of checking a link, see {@link LinkResolver}. */
public enum ResolutionStatus {
  /** The link is valid as it is. */
  OK,
  /** The link is valid, but its step number was wrong. The resolution tells the corrected link. */
  SANITIZED,
  /** The link can't be followed. The resolution tells why. */
  ERROR
}
