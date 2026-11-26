package specman.link.classman;

import java.io.File;
import java.net.URI;

/** A link to the file which has the source of a Classman class diagram, e.g. {@code classman:///C:/project/hilfe.cld}.
 * With an alias for the file, the alias replaces the path: {@code classman://hilfe}. The ID and the name of the diagram
 * can follow, {@code classman://hilfe,0a1b2c3d,Hilfe Versionierung}, which makes the link readable for people. The name
 * is the last part and may contain commas. */
public final class ClassmanLink {
  public static final String SCHEME = "classman://";
  public static final String SEPARATOR = ",";
  public static final String FILE_EXTENSION = "cld";

  private static final int MAX_PARTS = 3;

  private static final String PERCENT = "%";
  private static final String BACKTICK = "`";
  private static final String LINE_FEED = "\n";
  private static final String CARRIAGE_RETURN = "\r";
  private static final String ESCAPED_PERCENT = "%25";
  private static final String ESCAPED_BACKTICK = "%60";
  private static final String ESCAPED_LINE_FEED = "%0A";
  private static final String ESCAPED_CARRIAGE_RETURN = "%0D";
  private static final String ESCAPED_SEPARATOR = "%2C";

  private ClassmanLink() {}

  /** The link to the passed file, which is identified by the alias if there is one (an empty alias means none),
   * otherwise by its path. The ID and the name of the diagram may be null, the ID even if the name isn't. */
  public static String of(File file, String alias, String diagramId, String diagramName) {
    String link = alias.isEmpty() ? pathOf(file) : SCHEME + alias;
    if (diagramId == null && diagramName == null) {
      return link;
    }
    link += SEPARATOR + (diagramId == null ? "" : diagramId);
    return diagramName == null ? link : link + SEPARATOR + escapeName(diagramName);
  }

  /** @throws IllegalArgumentException if the text isn't a Classman link */
  public static ClassmanLinkReference parse(String text) {
    String link = text.strip();
    if (!link.startsWith(SCHEME)) {
      throw new IllegalArgumentException("Not a Classman link: " + text);
    }
    String[] parts = link.substring(SCHEME.length()).split(SEPARATOR, MAX_PARTS);
    String location = parts[0];
    if (location.isEmpty()) {
      throw new IllegalArgumentException("Not a Classman link: " + text);
    }
    File file = location.startsWith("/") ? fileOf(location) : null;
    String alias = file == null ? location : null;
    String name = partOrNull(parts, 2);
    return new ClassmanLinkReference(file, alias, partOrNull(parts, 1), name == null ? null : unescapeName(name));
  }

  private static String partOrNull(String[] parts, int index) {
    return index < parts.length && !parts[index].isBlank() ? parts[index].strip() : null;
  }

  private static File fileOf(String encodedPath) {
    try {
      return new File(URI.create("file://" + encodedPath));
    }
    catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Not a valid file path in a Classman link: " + encodedPath, e);
    }
  }

  /** The path is URL-encoded as usual, e.g. blanks become %20. On top of that, commas are encoded, which URIs leave as
   * they are, because the comma separates the file from the ID and the name. */
  private static String pathOf(File file) {
    return SCHEME + file.toURI().getRawPath().replace(SEPARATOR, ESCAPED_SEPARATOR);
  }

  /** The link is written between backticks in the model file and in a single line, so these have to go. The percent
   * sign is escaped first, and unescaped last, so that a text which looks like an escape survives. */
  private static String escapeName(String name) {
    return name.replace(PERCENT, ESCAPED_PERCENT)
      .replace(BACKTICK, ESCAPED_BACKTICK)
      .replace(LINE_FEED, ESCAPED_LINE_FEED)
      .replace(CARRIAGE_RETURN, ESCAPED_CARRIAGE_RETURN);
  }

  private static String unescapeName(String name) {
    return name.replace(ESCAPED_BACKTICK, BACKTICK)
      .replace(ESCAPED_LINE_FEED, LINE_FEED)
      .replace(ESCAPED_CARRIAGE_RETURN, CARRIAGE_RETURN)
      .replace(ESCAPED_PERCENT, PERCENT);
  }
}
