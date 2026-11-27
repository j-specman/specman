package specman.link.classman;

import specman.link.LinkLocation;
import specman.link.LinkUrl;

import java.io.File;

/** A link to the file which has the source of a Classman class diagram, e.g. {@code classman:///C:/project/hilfe.cld}.
 * With an alias for the file, the alias replaces the path: {@code classman://hilfe}. The fragment behind the hash sign
 * has the ID and the name of the diagram, {@code classman://hilfe#0a1b2c3d,Hilfe Versionierung}, which makes the link
 * readable for people. They are informative, the file is what the link points to. The name is the last part and may
 * contain commas and blanks. See {@link specman.link.LinkUrl} for what the form of the URL means. */
public final class ClassmanLink {
  public static final String SCHEME = "classman://";
  /** Separates the ID and the name of the diagram in the fragment. */
  public static final String SEPARATOR = ",";
  public static final String FILE_EXTENSION = "cld";

  private static final int MAX_FRAGMENT_PARTS = 2;

  private static final String PERCENT = "%";
  private static final String BACKTICK = "`";
  private static final String LINE_FEED = "\n";
  private static final String CARRIAGE_RETURN = "\r";
  private static final String ESCAPED_PERCENT = "%25";
  private static final String ESCAPED_BACKTICK = "%60";
  private static final String ESCAPED_LINE_FEED = "%0A";
  private static final String ESCAPED_CARRIAGE_RETURN = "%0D";

  private ClassmanLink() {}

  /** The link to the passed file, which is identified by the alias if there is one (an empty alias means none),
   * otherwise by its path. The ID and the name of the diagram may be null, the ID even if the name isn't. */
  public static String of(File file, String alias, String diagramId, String diagramName) {
    return of(LinkLocation.of(file, alias), diagramId, diagramName);
  }

  /** The link to the file which the location names. The ID and the name of the diagram may be null, the ID even if the
   * name isn't. */
  public static String of(LinkLocation location, String diagramId, String diagramName) {
    if (diagramId == null && diagramName == null) {
      return LinkUrl.of(SCHEME, location, null);
    }
    String fragment = diagramId == null ? "" : diagramId;
    if (diagramName != null) {
      fragment += SEPARATOR + escapeName(diagramName);
    }
    return LinkUrl.of(SCHEME, location, fragment);
  }

  /** @throws IllegalArgumentException if the text isn't a Classman link. A link without file isn't one, because it would
   * point into the document it is in, which has no Classman diagram files. */
  public static ClassmanLinkReference parse(String text) {
    LinkUrl url = LinkUrl.parse(text, SCHEME);
    if (url.location().isLocal()) {
      throw new IllegalArgumentException("Link without a file: " + text);
    }
    String[] parts = url.fragment() == null ? new String[0] : url.fragment().split(SEPARATOR, MAX_FRAGMENT_PARTS);
    String name = partOrNull(parts, 1);
    return new ClassmanLinkReference(url.location(), partOrNull(parts, 0), name == null ? null : unescapeName(name));
  }

  private static String partOrNull(String[] parts, int index) {
    return index < parts.length && !parts[index].isBlank() ? parts[index].strip() : null;
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
