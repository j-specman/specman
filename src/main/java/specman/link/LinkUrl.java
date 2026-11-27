package specman.link;

/**
 * The structure which all link URLs of Specman and Classman share: {@code scheme://location#fragment}, e.g.
 * {@code specman://uc720#2d34f85e,1.3} or {@code classman:///C:/project/hilfe.cld#0a1b2c3d,Hilfe}.
 * <ul>
 *   <li>The <em>location</em> names a file, by alias, by absolute or relative path, or not at all, see
 *   {@link LinkLocation}.</li>
 *   <li>The <em>fragment</em> names a part inside the file, the same way the fragment of a web address names a part of
 *   the page. The kind of link tells what it consists of. It is separated from the location by a hash sign, which
 *   can't be part of the location, as a hash sign in a path is encoded as {@code %23}.</li>
 * </ul>
 * <p>
 * Whoever pastes a URL somewhere often doesn't get back what was put in. Tools which don't know the scheme tend to treat
 * it like a web address, for which an empty path is written as a slash: {@code specman://uc720/#2d34f85e,1.3} instead
 * of {@code specman://uc720#2d34f85e,1.3}. A slash at the end of the URL is a common leftover too. Both are ignored when
 * a URL is read. Neither can be part of what is meant: an alias has no slash, and the path of a file is not
 * a directory path. For the same reason {@code specman:///#2d34f85e,1.3} is the same link as {@code specman://#2d34f85e,1.3}.
 */
public final class LinkUrl {
  private static final String FRAGMENT_SEPARATOR = "#";
  private static final String TRAILING_SLASHES = "/+$";

  private final LinkLocation location;
  private final String fragment;

  private LinkUrl(LinkLocation location, String fragment) {
    this.location = location;
    this.fragment = fragment;
  }

  /** @param fragment what the URL names in the file, null if it names the file only
   * @return the URL */
  public static String of(String scheme, LinkLocation location, String fragment) {
    return scheme + location.text() + (fragment == null ? "" : FRAGMENT_SEPARATOR + fragment);
  }

  /** Reads a URL, which may carry surrounding whitespace and slashes where tools put them, see above. Whether a URL
   * without location, which is a local link, makes sense is up to the kind of link.
   * @throws IllegalArgumentException if the text isn't a URL of the scheme, or has a location which isn't valid */
  public static LinkUrl parse(String text, String scheme) {
    String url = text.strip().replaceAll(TRAILING_SLASHES, "");
    if (!url.startsWith(scheme)) {
      throw new IllegalArgumentException("Not a link of " + scheme + ": " + text);
    }
    String rest = url.substring(scheme.length());
    int fragmentStart = rest.indexOf(FRAGMENT_SEPARATOR);
    String fragment = fragmentStart < 0 ? null : rest.substring(fragmentStart + 1);
    String location = (fragmentStart < 0 ? rest : rest.substring(0, fragmentStart)).replaceAll(TRAILING_SLASHES, "");
    return new LinkUrl(LinkLocation.parse(location), fragment);
  }

  public LinkLocation location() { return location; }

  /** @return what the URL names in the file, null if there is no fragment */
  public String fragment() { return fragment; }
}
