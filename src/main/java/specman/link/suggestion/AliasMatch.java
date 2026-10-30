package specman.link.suggestion;

import specman.link.LinkAliases;

import java.util.List;

/** An alias assignment an alias for a new file can be derived from. It keeps the raw material of the derivation, the alias'
 * characters with their positions in the example's file name, together with the new file's base name, whose characters at
 * these positions are all of the right type. The matches of all assignments which allow a suggestion are scored to find the best one. */
class AliasMatch {
  private final LinkAliases.Entry source;
  private final List<AliasCharacter> aliasCharacters;
  private final String newFileBaseName;

  AliasMatch(LinkAliases.Entry source, List<AliasCharacter> aliasCharacters, String newFileBaseName) {
    this.source = source;
    this.aliasCharacters = List.copyOf(aliasCharacters);
    this.newFileBaseName = newFileBaseName;
  }

  LinkAliases.Entry source() {
    return source;
  }

  /** @return the new file's characters at the alias characters' positions in the alias' letter case and separator form */
  String suggestion() {
    StringBuilder suggestion = new StringBuilder();
    for (AliasCharacter aliasCharacter : aliasCharacters) {
      suggestion.append(aliasCharacter.adaptedFrom(newFileBaseName));
    }
    return suggestion.toString();
  }

  int score() {
    return source.alias().length();
  }
}
