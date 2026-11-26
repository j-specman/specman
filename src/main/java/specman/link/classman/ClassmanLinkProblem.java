package specman.link.classman;

/** What keeps a Classman link from leading to a class diagram. */
public enum ClassmanLinkProblem {
  /** The detail is the text which isn't a link. */
  NOT_A_LINK,
  /** The detail is the alias. */
  ALIAS_UNKNOWN,
  /** The detail is the path of the file. */
  WRONG_EXTENSION,
  /** The detail is the path of the file. */
  FILE_MISSING,
  /** The detail is what the system says about it. */
  UNREADABLE,
  /** The file was read, but its content is no class diagram. The detail is what the parser says. */
  NOT_A_DIAGRAM
}
