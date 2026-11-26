package specman.link.classman;

import static specman.i18n.Messages.i18n;

/** What keeps a Classman link from leading to a class diagram. */
public enum ClassmanLinkProblem {
  /** The detail is the text which isn't a link. */
  NOT_A_LINK("msg.classman.link.problem.not_a_link"),
  /** The detail is the alias. */
  ALIAS_UNKNOWN("msg.classman.link.problem.alias_unknown"),
  /** The detail is the path of the file. */
  WRONG_EXTENSION("msg.classman.link.problem.wrong_extension"),
  /** The detail is the path of the file. */
  FILE_MISSING("msg.classman.link.problem.file_missing"),
  /** The detail is what the system says about it. */
  UNREADABLE("msg.classman.link.problem.unreadable"),
  /** The file was read, but its content is no class diagram. The detail is what the parser says. */
  NOT_A_DIAGRAM(null);

  private final String messageKey;

  ClassmanLinkProblem(String messageKey) {
    this.messageKey = messageKey;
  }

  /** @return the problem in the language of the user */
  public String describe(String detail) {
    return messageKey == null ? detail : i18n(messageKey, detail);
  }
}
