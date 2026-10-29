package specman.link;

import specman.Specman;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * The user's aliases for files links point to, kept in the user preferences. An alias replaces the
 * file's path in a link, which makes links independent of where a team member keeps the file.
 * A file may be assigned an empty alias, which means the user decided not to use one. The distinction
 * between "no alias" and "not decided yet" is what stops the alias dialog from popping up each time.
 */
public final class LinkAliases {

  public static final int MAX_ALIAS_LENGTH = 20;
  public static final String PROBLEM_INVALID = "dialog.link.alias.problem.invalid";
  public static final String PROBLEM_TAKEN = "dialog.link.alias.problem.taken";
  public static final String PROBLEM_FILE_TWICE = "dialog.link.aliases.problem.file.twice";

  /** The alias the user chose for a file. */
  public record Entry(File file, String alias) {}

  private static final String ALLOWED_CHARACTERS = "A-Za-z0-9_-";
  private static final Pattern VALID_ALIAS = Pattern.compile("[" + ALLOWED_CHARACTERS + "]{0," + MAX_ALIAS_LENGTH + "}");
  private static final String ALIASES_NODE = "linkAliases";
  private static final String FILE_KEY = "file";
  private static final String ALIAS_KEY = "alias";

  private static Preferences node;

  private LinkAliases() {}

  /** Redirects the aliases to another preferences node, so tests don't touch the user's real aliases.
   * Passing null returns to the default node. */
  public static void useNode(Preferences replacement) {
    node = replacement;
  }

  /** Whether the user already decided about an alias for the file, even for "no alias". */
  public static boolean isAssigned(File file) {
    try {
      return aliases().nodeExists(entryName(file));
    }
    catch (BackingStoreException e) {
      throw new IllegalStateException("Link aliases can't be read", e);
    }
  }

  /** The file's alias, empty if there is none. */
  public static String aliasOf(File file) {
    return isAssigned(file) ? aliases().node(entryName(file)).get(ALIAS_KEY, "") : "";
  }

  /** Assigns the alias to the file. The alias must be valid for the file, see {@link #problemKeyFor(File, String)}. */
  public static void assign(File file, String alias) {
    String problemKey = problemKeyFor(file, alias);
    if (problemKey != null) {
      throw new IllegalArgumentException("Alias '" + alias + "' not allowed: " + problemKey);
    }
    Preferences entry = aliases().node(entryName(file));
    entry.put(FILE_KEY, absolutePath(file));
    entry.put(ALIAS_KEY, alias);
  }

  /** The file the alias is assigned to regardless of the alias' case, null if there is none. */
  public static File fileOf(String alias) {
    return entries().stream()
      .filter(entry -> entry.alias().equalsIgnoreCase(alias))
      .map(Entry::file)
      .findFirst()
      .orElse(null);
  }

  /** The text without the characters an alias must not contain. */
  public static String keepAllowedCharacters(String text) {
    return text.replaceAll("[^" + ALLOWED_CHARACTERS + "]", "");
  }

  /** The message key of the reason why the alias can't be assigned to the file, null if it can. */
  public static String problemKeyFor(File file, String alias) {
    String ownPath = absolutePath(file);
    List<String> aliasesOfOtherFiles = entries().stream()
      .filter(entry -> !ownPath.equals(absolutePath(entry.file())))
      .map(Entry::alias)
      .toList();
    return problemKeyOfAlias(alias, aliasesOfOtherFiles);
  }

  /** The message key of the first reason why the passed entries can't be stored together, null if they can. */
  public static String problemKeyFor(List<Entry> edited) {
    Set<String> paths = new HashSet<>();
    for (int i = 0; i < edited.size(); i++) {
      List<String> otherAliases = new ArrayList<>();
      for (int j = 0; j < edited.size(); j++) {
        if (j != i) {
          otherAliases.add(edited.get(j).alias());
        }
      }
      String problemKey = problemKeyOfAlias(edited.get(i).alias(), otherAliases);
      if (problemKey != null) {
        return problemKey;
      }
      if (!paths.add(absolutePath(edited.get(i).file()))) {
        return PROBLEM_FILE_TWICE;
      }
    }
    return null;
  }

  private static String problemKeyOfAlias(String alias, List<String> aliasesOfOtherFiles) {
    if (!VALID_ALIAS.matcher(alias).matches()) {
      return PROBLEM_INVALID;
    }
    if (!alias.isEmpty() && aliasesOfOtherFiles.stream().anyMatch(alias::equalsIgnoreCase)) {
      return PROBLEM_TAKEN;
    }
    return null;
  }

  /** Replaces all aliases shown by {@link #entries()} with the passed ones: A file missing in the passed entries
   * loses its alias and will be asked about again, a file with an empty alias counts as decided against an alias.
   * Files the user decided against an alias for and which {@link #entries()} doesn't list are left alone.
   * @throws IllegalArgumentException if the entries can't be stored together, see {@link #problemKeyFor(List)} */
  public static void replaceAll(List<Entry> edited) {
    String problemKey = problemKeyFor(edited);
    if (problemKey != null) {
      throw new IllegalArgumentException("Aliases not allowed: " + problemKey);
    }
    try {
      for (Entry entry : entries()) {
        aliases().node(entryName(entry.file())).removeNode();
      }
    }
    catch (BackingStoreException e) {
      throw new IllegalStateException("Link aliases can't be changed", e);
    }
    edited.forEach(entry -> assign(entry.file(), entry.alias()));
  }

  /** All aliases in use, sorted by alias regardless of case. Files the user decided against an alias for are left out. */
  public static List<Entry> entries() {
    try {
      List<Entry> entries = new ArrayList<>();
      for (String name : aliases().childrenNames()) {
        Preferences entry = aliases().node(name);
        String alias = entry.get(ALIAS_KEY, "");
        if (!alias.isEmpty()) {
          entries.add(new Entry(new File(entry.get(FILE_KEY, "")), alias));
        }
      }
      entries.sort(Comparator.comparing(entry -> entry.alias().toLowerCase()));
      return List.copyOf(entries);
    }
    catch (BackingStoreException e) {
      throw new IllegalStateException("Link aliases can't be read", e);
    }
  }

  private static Preferences aliases() {
    Preferences root = node != null ? node : Preferences.userNodeForPackage(Specman.class);
    return root.node(ALIASES_NODE);
  }

  /** Preferences keys and node names are limited to 80 characters, which paths exceed. */
  private static String entryName(File file) {
    try {
      byte[] hash = MessageDigest.getInstance("SHA-256").digest(absolutePath(file).getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    }
    catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  /** The identity of a file, which is what makes the same file recognizable however its path was written. */
  public static String absolutePath(File file) {
    return file.toPath().toAbsolutePath().normalize().toString();
  }
}
