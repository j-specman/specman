package specman.search;

/** The areas the search dialog can search in. */
public enum SearchScope {
  MODEL("Model"),
  TAGS("Tags");

  private final String label;

  SearchScope(String label) {
    this.label = label;
  }

  @Override
  public String toString() {
    return label;
  }
}
