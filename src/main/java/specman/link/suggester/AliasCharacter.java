package specman.link.suggester;

import java.util.ArrayList;
import java.util.List;

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

  /** @return the characters of the alias with their indices in the file name, null if they can't all be found in order or the alias is empty */
  static List<AliasCharacter> locateAll(String alias, String fileName) {
    List<AliasCharacter> located = new ArrayList<>();
    int searchFrom = 0;
    for (char c : alias.toCharArray()) {
      AliasCharacter aliasCharacter = locate(c, fileName, searchFrom);
      if (aliasCharacter == null) {
        return null;
      }
      located.add(aliasCharacter);
      searchFrom = aliasCharacter.index() + 1;
    }
    return located.isEmpty() ? null : located;
  }

  int index() {
    return index;
  }

  /** @return whether the file name has the character of another alias at this alias character's position, which means
   * the same type, the same letter ignoring case or any separator for a separator */
  boolean isMatchedBy(char otherAliasCharacter, String fileName) {
    return index < fileName.length()
      && AliasCharacterType.of(otherAliasCharacter) == type
      && type.accepts(otherAliasCharacter, fileName.charAt(index));
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
