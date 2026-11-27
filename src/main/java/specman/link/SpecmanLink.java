package specman.link;

import specman.view.AbstractSchrittView;

import java.io.File;

/**
 * A global link to a Specman file and optionally a step in it, e.g. {@code specman:///C:/project/UC-720.nsd#2d34f85e,1.3}.
 * With an alias for the file, the alias replaces the path: {@code specman://uc720#2d34f85e,1.3}. The fragment behind the
 * hash sign is the ID of the step, and the number which the step had when the link was made, separated by a comma.
 * The number is merely informative. See {@link LinkUrl} for what the form of the URL means, and {@link LinkLocation} for
 * the ways to name the file. A link without file, {@code specman://#2d34f85e,1.3}, points to a step of the document which
 * contains the link, and has to name the step.
 * <p>
 * <b>The old form</b> of the link had no hash sign, but only commas: {@code specman://uc720,2d34f85e,1.3}. Such links
 * are part of documents which were written before the fragment was introduced, and they are still read, but never
 * written. The first comma after the location is taken for the hash sign. That is unambiguous, because a location has no
 * comma: an alias can't contain one, and the commas of a path are written as {@code %2C}.
 */
public final class SpecmanLink {

  public static final String SCHEME = "specman://";
  /** Separates the step ID and the step number in the fragment. */
  public static final String SEPARATOR = ",";
  /** Separates the file from the step in the URL. */
  public static final String FRAGMENT_SEPARATOR = "#";

  private static final int MAX_FRAGMENT_PARTS = 2;

  /** What a link points to. The location says which file, or that it is the document the link is in. Step ID and number
   * are null if the link doesn't address a step, the number is null as well if the link doesn't tell it. A local link
   * always addresses a step. */
  public record Reference(LinkLocation location, String stepId, String stepNumber) {
    public boolean hasAlias() {
      return location.isAlias();
    }

    public String alias() {
      return location.alias();
    }

    public boolean isLocal() {
      return location.isLocal();
    }

    public boolean hasStep() {
      return stepId != null;
    }
  }

  private SpecmanLink() {}

  /** The link to the passed step in the passed file, which is identified by the alias if there is one, otherwise
   * by its path. The step number is merely informative. */
  public static String of(File file, String alias, AbstractSchrittView step) {
    return of(LinkLocation.of(file, alias), step);
  }

  /** The link to the passed step in the file which the location names. The step number is merely informative. */
  public static String of(LinkLocation location, AbstractSchrittView step) {
    return of(location, step.getId(), String.valueOf(step.getNumber()));
  }

  /** The link to the passed file, which is identified by the alias if there is one (an empty alias means none), otherwise
   * by its path. The step ID and number may be null, the number even if the ID isn't. A link without ID names the file only. */
  public static String of(File file, String alias, String stepId, String stepNumber) {
    return of(LinkLocation.of(file, alias), stepId, stepNumber);
  }

  /** The link to the step in the file which the location names. The step ID and number may be null, the number even if
   * the ID isn't. A link without ID names the file only. */
  public static String of(LinkLocation location, String stepId, String stepNumber) {
    if (stepId == null) {
      return LinkUrl.of(SCHEME, location, null);
    }
    return LinkUrl.of(SCHEME, location, stepNumber == null ? stepId : stepId + SEPARATOR + stepNumber);
  }

  /** Reads the link text, which may carry surrounding whitespace and slashes at the end or in front of the hash sign, as
   * other tools tend to add when copying a link, see {@link LinkUrl}. The step ID and number are optional. A link of the
   * old form is read as well, see above.
   * @throws IllegalArgumentException if the text isn't a link */
  public static Reference parse(String text) {
    LinkUrl url = LinkUrl.parse(withFragmentSeparator(text.strip()), SCHEME);
    String[] parts = url.fragment() == null ? new String[0] : url.fragment().split(SEPARATOR, -1);
    Reference reference = new Reference(url.location(), partOrNull(parts, 0), partOrNull(parts, 1));
    if (parts.length > MAX_FRAGMENT_PARTS || reference.isLocal() && !reference.hasStep()) {
      throw new IllegalArgumentException("Not a Specman link: " + text);
    }
    return reference;
  }

  /** @return what the link points to, null if the text isn't a link, see {@link #parse(String)} */
  public static Reference parseOrNull(String text) {
    if (text == null) {
      return null;
    }
    try {
      return parse(text);
    }
    catch (IllegalArgumentException e) {
      return null;
    }
  }

  /** @return the link with the hash sign where the old form of the link had its first comma. A link of the old form had a
   * file, so a comma right behind the scheme is no such link. */
  private static String withFragmentSeparator(String link) {
    int comma = link.indexOf(SEPARATOR);
    if (link.contains(FRAGMENT_SEPARATOR) || comma <= SCHEME.length()) {
      return link;
    }
    return link.substring(0, comma) + FRAGMENT_SEPARATOR + link.substring(comma + 1);
  }

  private static String partOrNull(String[] parts, int index) {
    return index < parts.length && !parts[index].isBlank() ? parts[index].strip() : null;
  }
}
