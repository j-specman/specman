package specman.link;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * How a link names the file it points to, which is the part of the URL between the scheme and the fragment, see
 * {@link LinkUrl}. There are four ways, each written differently, so that one can't be taken for another:
 * <ul>
 *   <li>The <em>alias</em> which the user has for the file: {@code uc720}. An alias consists of letters, digits,
 *   underscores and hyphens, so it never contains a dot or a slash.</li>
 *   <li>The <em>absolute path</em> of the file: {@code /C:/project/UC-720.nsd}. It starts with a slash.</li>
 *   <li>A path <em>relative</em> to the directory of the document which contains the link: {@code ./UC-720.nsd},
 *   {@code ../other/UC-720.nsd}. It starts with a dot, which is what makes it a relative path, as in a shell.</li>
 *   <li>Nothing at all, <em>local</em>: the link points into the document which contains it, e.g. from a diagram to a step
 *   of the same specification. It has no file, so it is no use for a link to a file, see
 *   {@link specman.link.classman.ClassmanLink}.</li>
 * </ul>
 * A path is URL-encoded as usual, e.g. blanks become {@code %20}, and so is a comma, {@code %2C}, which a URI would leave
 * as it is. The comma doesn't have to be encoded in a URL which has a fragment. It is done so that a URL without fragment
 * can't be mistaken for a link of the old form of Specman, see {@link SpecmanLink}.
 * <p>
 * A relative path needs a directory to start from, which is the one of the document that contains the link. Where there
 * is no document, e.g. for a link in the clipboard or on the command line without a document open, it is the
 * working directory, see {@link #baseDirectoryOf(File)}.
 */
public final class LinkLocation {
  private static final LinkLocation LOCAL = new LinkLocation(Kind.LOCAL, null, null, "");
  private static final String COMMA = ",";
  private static final String ESCAPED_COMMA = "%2C";
  private static final String ABSOLUTE_PREFIX = "/";
  private static final String SAME_DIRECTORY_PREFIX = "./";
  private static final String PARENT_DIRECTORY = "..";
  private static final String PARENT_DIRECTORY_PREFIX = "../";

  private enum Kind { ALIAS, ABSOLUTE, RELATIVE, LOCAL }

  private final Kind kind;
  private final String alias;
  private final File absoluteFile;
  /** The location as written in the URL. */
  private final String text;

  private LinkLocation(Kind kind, String alias, File absoluteFile, String text) {
    this.kind = kind;
    this.alias = alias;
    this.absoluteFile = absoluteFile;
    this.text = text;
  }

  public static LinkLocation alias(String alias) {
    return new LinkLocation(Kind.ALIAS, alias, null, alias);
  }

  public static LinkLocation absolute(File file) {
    return new LinkLocation(Kind.ABSOLUTE, null, file, file.toURI().getRawPath().replace(COMMA, ESCAPED_COMMA));
  }

  public static LinkLocation local() {
    return LOCAL;
  }

  /** @return the alias if there is one, otherwise the absolute path of the file */
  public static LinkLocation of(File file, String alias) {
    return alias.isEmpty() ? absolute(file) : alias(alias);
  }

  /** @param baseDirectory the directory of the document which contains the link
   * @return the path of the file relative to the directory, null if there is none: on Windows, a file on another drive
   * can't be reached by a relative path */
  public static LinkLocation relative(File file, File baseDirectory) {
    Path base = baseDirectory.toPath().toAbsolutePath().normalize();
    Path target = file.toPath().toAbsolutePath().normalize();
    try {
      String path = base.relativize(target).toString().replace(File.separatorChar, '/');
      boolean leavesDirectory = path.equals(PARENT_DIRECTORY) || path.startsWith(PARENT_DIRECTORY_PREFIX);
      return new LinkLocation(Kind.RELATIVE, null, null, encoded(leavesDirectory ? path : SAME_DIRECTORY_PREFIX + path));
    }
    catch (IllegalArgumentException differentRoots) {
      return null;
    }
  }

  /** @param text the location as written in a URL
   * @throws IllegalArgumentException if the text is a path which isn't valid, or neither a path nor an alias */
  public static LinkLocation parse(String text) {
    if (text.isEmpty()) {
      return LOCAL;
    }
    if (text.startsWith(ABSOLUTE_PREFIX)) {
      return new LinkLocation(Kind.ABSOLUTE, null, fileOf(text), text);
    }
    if (text.startsWith(SAME_DIRECTORY_PREFIX) || text.startsWith(PARENT_DIRECTORY_PREFIX)) {
      pathOf(text);
      return new LinkLocation(Kind.RELATIVE, null, null, text);
    }
    if (!LinkAliases.keepAllowedCharacters(text).equals(text)) {
      throw new IllegalArgumentException("Neither an alias nor a path: " + text);
    }
    return alias(text);
  }

  /** @return the directory which a relative path starts from for a link in the document: the one the file is in, or the
   * working directory if there is no file, e.g. because the document hasn't been saved yet */
  public static File baseDirectoryOf(File documentFile) {
    File directory = documentFile == null ? null : documentFile.getAbsoluteFile().getParentFile();
    return directory != null ? directory : new File(".").getAbsoluteFile();
  }

  /** @return the location as written in the URL */
  public String text() { return text; }

  public boolean isAlias() { return kind == Kind.ALIAS; }

  public boolean isRelative() { return kind == Kind.RELATIVE; }

  public boolean isLocal() { return kind == Kind.LOCAL; }

  /** @return the alias, null if the location isn't one */
  public String alias() { return alias; }

  /** @param baseDirectory where a relative path starts, see {@link #baseDirectoryOf(File)}
   * @return the file the location names, null if there is none: for an alias nobody knows, and for a local location */
  public File resolve(File baseDirectory) {
    return switch (kind) {
      case ALIAS -> LinkAliases.fileOf(alias);
      case ABSOLUTE -> absoluteFile;
      case RELATIVE -> new File(baseDirectory.getAbsoluteFile(), pathOf(text)).toPath().normalize().toFile();
      case LOCAL -> null;
    };
  }

  private static String encoded(String path) {
    try {
      return new URI(null, null, path, null).getRawPath().replace(COMMA, ESCAPED_COMMA);
    }
    catch (URISyntaxException e) {
      throw new IllegalArgumentException("Not a valid path in a link: " + path, e);
    }
  }

  private static String pathOf(String relativeText) {
    try {
      return new URI(relativeText).getPath();
    }
    catch (URISyntaxException e) {
      throw new IllegalArgumentException("Not a valid path in a link: " + relativeText, e);
    }
  }

  private static File fileOf(String encodedPath) {
    try {
      return new File(URI.create("file://" + encodedPath));
    }
    catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Not a valid file path in a link: " + encodedPath, e);
    }
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof LinkLocation location && kind == location.kind && text.equals(location.text);
  }

  @Override
  public int hashCode() {
    return Objects.hash(kind, text);
  }

  @Override
  public String toString() {
    return text;
  }
}
