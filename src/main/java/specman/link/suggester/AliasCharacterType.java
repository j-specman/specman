package specman.link.suggester;

/** The kinds of characters an alias is made of. Letters are ASCII letters only, as the aliases are. */
enum AliasCharacterType {
  LETTER, DIGIT, SEPARATOR;

  /** @return the type of the character, null if an alias must not contain it */
  static AliasCharacterType of(char c) {
    if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
      return LETTER;
    }
    if (c >= '0' && c <= '9') {
      return DIGIT;
    }
    if (c == '-' || c == '_') {
      return SEPARATOR;
    }
    return null;
  }

  /** @return whether the file name character is the alias character, letters ignoring case and any separator being any other */
  boolean accepts(char aliasCharacter, char fileNameCharacter) {
    return of(fileNameCharacter) == this
      && (this == SEPARATOR || Character.toLowerCase(aliasCharacter) == Character.toLowerCase(fileNameCharacter));
  }
}
