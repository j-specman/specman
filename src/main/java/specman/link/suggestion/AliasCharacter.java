package specman.link.suggestion;

/** A character of an alias together with its type and the position where it was found in the file name of the alias' example. */
class AliasCharacter {
  private final char c;
  private final AliasCharacterType type;
  private final int index;

  private AliasCharacter(char c, AliasCharacterType type, int index) {
    this.c = c;
    this.type = type;
    this.index = index;
  }

  /** @return the alias character found first in the file name at or behind the index, null if it isn't found or not allowed in an alias */
  static AliasCharacter locate(char c, String fileName, int from) {
    AliasCharacterType type = AliasCharacterType.of(c);
    if (type == null) {
      return null;
    }
    for (int i = from; i < fileName.length(); i++) {
      if (type.accepts(c, fileName.charAt(i))) {
        return new AliasCharacter(c, type, i);
      }
    }
    return null;
  }

  int index() {
    return index;
  }

  /** @return whether a file name has a character of this alias character's type at its position */
  boolean isTypeMatchedIn(String fileName) {
    return index < fileName.length() && AliasCharacterType.of(fileName.charAt(index)) == type;
  }

  /** @return the file name's character at this alias character's position in the alias' letter case and separator form.
   * Only to be called if {@link #isTypeMatchedIn(String)}. */
  char adaptedFrom(String fileName) {
    char fileNameCharacter = fileName.charAt(index);
    switch (type) {
      case LETTER:
        return Character.isUpperCase(c) ? Character.toUpperCase(fileNameCharacter) : Character.toLowerCase(fileNameCharacter);
      case SEPARATOR:
        return c;
      default:
        return fileNameCharacter;
    }
  }
}
