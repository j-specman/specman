package specman.search;

/**
 * Finds the next occurrence of a text in another text, honoring the search options "match case",
 * "whole words only" and the search direction. Immutable.
 */
public class TextMatcher {
  public static final char[] WORD_SEPARATORS = new char[]{' ', '\t', '\n', '\r', '\f', '.', ',', ':', '-', '(', ')', '[', ']', '{', '}', '<', '>', '/', '|', '\\', '\'', '"'};

  private final boolean matchCase;
  private final boolean wholeWordsOnly;
  private final boolean searchUp;

  public TextMatcher(boolean matchCase, boolean wholeWordsOnly, boolean searchUp) {
    this.matchCase = matchCase;
    this.wholeWordsOnly = wholeWordsOnly;
    this.searchUp = searchUp;
  }

  public boolean isSearchUp() {
    return searchUp;
  }

  public TextMatcher searchingForwards() {
    return new TextMatcher(matchCase, wholeWordsOnly, false);
  }

  /** @return the first character which can't be part of a text to search for with the "whole words
   * only" option, null if there is none or the option is off. */
  public Character illegalCharacterIn(String key) {
    if (!wholeWordsOnly) {
      return null;
    }
    for (char separator : WORD_SEPARATORS) {
      if (key.indexOf(separator) >= 0) {
        return separator;
      }
    }
    return null;
  }

  /**
   * @param pos where to start searching: forward from it, or backward from the position before it
   * @return the index of the hit in data, -1 if there is none
   */
  public int indexOfHit(String data, String key, int pos) {
    String searchData = data;
    String searchKey = key;
    if (!matchCase) {
      searchData = searchData.toLowerCase();
      searchKey = searchKey.toLowerCase();
    }

    int searchPos = pos;
    while (true) {
      int index;
      if (!searchUp) {
        index = searchData.indexOf(searchKey, searchPos);
      } else {
        index = searchData.lastIndexOf(searchKey, searchPos - 1);
      }

      if (index < 0 || index >= searchData.length()) {
        return -1;
      }
      if (wholeWordsOnly) {
        boolean hasCharBefore = index > 0;
        boolean continuesBefore = hasCharBefore && !isSeparator(searchData.charAt(index - 1));
        boolean hasCharAfter = index + searchKey.length() < searchData.length();
        boolean continuesAfter = hasCharAfter && !isSeparator(searchData.charAt(index + searchKey.length()));
        if (continuesBefore || continuesAfter) {
          if (!searchUp && hasCharAfter) {
            searchPos = index + searchKey.length();
            continue;
          }
          if (searchUp && hasCharBefore) {
            searchPos = index;
            continue;
          }
          return -1;
        }
      }
      return index;
    }
  }

  private static boolean isSeparator(char ch) {
    for (char separator : WORD_SEPARATORS) {
      if (ch == separator) {
        return true;
      }
    }
    return false;
  }
}
