package specman.link;

import specman.Specman;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
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

  /** The text without the characters an alias must not contain. */
  public static String keepAllowedCharacters(String text) {
    return text.replaceAll("[^" + ALLOWED_CHARACTERS + "]", "");
  }

  /** The message key of the reason why the alias can't be assigned to the file, null if it can. */
  public static String problemKeyFor(File file, String alias) {
    if (!VALID_ALIAS.matcher(alias).matches()) {
      return PROBLEM_INVALID;
    }
    if (!alias.isEmpty() && isUsedForOtherFile(file, alias)) {
      return PROBLEM_TAKEN;
    }
    return null;
  }

  private static boolean isUsedForOtherFile(File file, String alias) {
    try {
      String ownPath = absolutePath(file);
      for (String name : aliases().childrenNames()) {
        Preferences entry = aliases().node(name);
        if (!ownPath.equals(entry.get(FILE_KEY, "")) && alias.equalsIgnoreCase(entry.get(ALIAS_KEY, ""))) {
          return true;
        }
      }
      return false;
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

  private static String absolutePath(File file) {
    return file.toPath().toAbsolutePath().normalize().toString();
  }
}
