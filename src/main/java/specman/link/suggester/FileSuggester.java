package specman.link.suggester;

import specman.link.LinkAliases;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.io.FilenameUtils.getExtension;

/** The reverse of {@link AliasSuggester}: Suggests the file an alias nobody knows yet stands for, derived from the existing
 * assignments of aliases to files.
 * <ul>
 * <li>An assignment is an example if the alias has the same length as the example's alias and the same type of character
 * (letter, digit, separator) at every position</li>
 * <li>The example's alias characters are located in its file's base name like {@link AliasSuggester} does. A diagram in the
 * example's directory follows the pattern if its base name has the alias' characters at these positions. Letters are compared
 * ignoring case, and "-" and "_" are the same separator</li>
 * <li>There is only a suggestion if all examples together have exactly one file following their patterns. If there are
 * several, guessing would send the user to the wrong one too often, so the user has to choose</li>
 * </ul>
 * Example: The alias "uc730" for "C:/project/BALI UC-730 Kundendaten pflegen.nsd" makes "C:/project/BALI UC-135 Was Anderes.nsd"
 * the suggestion for the alias "uc135", provided no other diagram in the directory has "UC-135" at the same place. */
public class FileSuggester {
  private static final String DIAGRAM_EXTENSION = "nsd";

  private final List<LinkAliases.Entry> entries;

  public FileSuggester(List<LinkAliases.Entry> entries) {
    this.entries = entries;
  }

  /** @return the file the alias probably stands for, null if there is none or if it's ambiguous */
  public File suggest(String alias) {
    List<FileMatch> matches = collectMatches(alias);
    return allMatchTheSameFile(matches) ? matches.get(0).file() : null;
  }

  private List<FileMatch> collectMatches(String alias) {
    List<FileMatch> matches = new ArrayList<>();
    for (LinkAliases.Entry example : entries) {
      matches.addAll(matchesOf(example, alias));
    }
    return matches;
  }

  /** @return false if there are no matches or if there are several different files among them */
  private boolean allMatchTheSameFile(List<FileMatch> matches) {
    if (matches.isEmpty()) {
      return false;
    }
    for (FileMatch match : matches) {
      if (!match.isSameFileAs(matches.get(0))) {
        return false;
      }
    }
    return true;
  }

  /** @return a match for each diagram in the example's directory which has the alias' characters at the positions of the
   * example's alias, none if the example isn't made like the alias */
  private List<FileMatch> matchesOf(LinkAliases.Entry example, String alias) {
    List<AliasCharacter> pattern = locateInExample(example);
    if (pattern == null || pattern.size() != alias.length()) {
      return List.of();
    }
    List<FileMatch> matches = new ArrayList<>();
    for (File diagram : diagramsInDirectoryOf(example)) {
      if (followsPattern(pattern, alias, getBaseName(diagram.getName()))) {
        matches.add(new FileMatch(example, diagram));
      }
    }
    return matches;
  }

  /** @return the characters of the example's alias with their indices in the example's base name, null if they can't all be found */
  private List<AliasCharacter> locateInExample(LinkAliases.Entry example) {
    return AliasCharacter.locateAll(example.alias(), getBaseName(example.file().getName()));
  }

  /** @return the diagrams next to the example's file, none if the directory can't be read, e.g. because it is gone */
  private List<File> diagramsInDirectoryOf(LinkAliases.Entry example) {
    File directory = example.file().getParentFile();
    File[] files = directory != null ? directory.listFiles() : null;
    if (files == null) {
      return List.of();
    }
    List<File> diagrams = new ArrayList<>();
    for (File file : files) {
      if (isDiagram(file)) {
        diagrams.add(file);
      }
    }
    return diagrams;
  }

  private boolean isDiagram(File file) {
    return file.isFile() && getExtension(file.getName()).equalsIgnoreCase(DIAGRAM_EXTENSION);
  }

  /** @return whether the file name has the alias' character at the position of each pattern character */
  private boolean followsPattern(List<AliasCharacter> pattern, String alias, String fileBaseName) {
    for (int i = 0; i < pattern.size(); i++) {
      if (!pattern.get(i).isMatchedBy(alias.charAt(i), fileBaseName)) {
        return false;
      }
    }
    return true;
  }
}
