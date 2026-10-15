package specman.search;

import static specman.i18n.Messages.i18n;


/** The areas the search dialog can search in. */
public enum SearchScope {
  MODEL("search.scope.model"),
  TAGS("search.scope.tags");

  private final String messageKey;

  SearchScope(String messageKey) {
    this.messageKey = messageKey;
  }

  @Override
  public String toString() {
    return i18n(messageKey);
  }
}
