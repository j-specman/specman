package specman.link;

import specman.view.AbstractSchrittView;

import java.io.File;
import java.net.URI;

/** A global link to a Specman file and optionally a step in it, e.g. {@code specman:///C:/project/UC-720.nsd,2d34f85e,1.3}.
 * With an alias for the file, the alias replaces the path: {@code specman://uc720,2d34f85e,1.3}. */
public final class SpecmanLink {

  public static final String SCHEME = "specman://";
  public static final String SEPARATOR = ",";

  /** What a link points to. Exactly one of {@code file} and {@code alias} is set. Step ID and number are null if the
   * link doesn't address a step, the number is null as well if the link doesn't tell it. */
  public record Reference(File file, String alias, String stepId, String stepNumber) {
    public boolean hasAlias() {
      return alias != null;
    }

    public boolean hasStep() {
      return stepId != null;
    }
  }

  private SpecmanLink() {}

  /** The link to the passed step in the passed file, which is identified by the alias if there is one, otherwise
   * by its path. The step number is merely informative. */
  public static String of(File file, String alias, AbstractSchrittView step) {
    String location = alias.isEmpty() ? pathOf(file) : SCHEME + alias;
    return location + SEPARATOR + step.getId() + SEPARATOR + step.getNumber();
  }

  /** Reads the link text, which may carry surrounding whitespace and a slash at the end, as other tools tend to
   * add when copying a link. The step ID and number are optional.
   * @throws IllegalArgumentException if the text isn't a link */
  public static Reference parse(String text) {
    String link = text.strip().replaceAll("/+$", "");
    if (!link.startsWith(SCHEME)) {
      throw new IllegalArgumentException("Not a Specman link: " + text);
    }
    String[] parts = link.substring(SCHEME.length()).split(SEPARATOR, -1);
    String location = parts[0];
    if (location.isEmpty() || parts.length > 3) {
      throw new IllegalArgumentException("Not a Specman link: " + text);
    }
    File file = location.startsWith("/") ? fileOf(location) : null;
    String alias = file == null ? location : null;
    return new Reference(file, alias, partOrNull(parts, 1), partOrNull(parts, 2));
  }

  private static String partOrNull(String[] parts, int index) {
    return index < parts.length && !parts[index].isBlank() ? parts[index].strip() : null;
  }

  private static File fileOf(String encodedPath) {
    try {
      return new File(URI.create("file://" + encodedPath));
    }
    catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Not a valid file path in a Specman link: " + encodedPath, e);
    }
  }

  /** The path is URL-encoded as usual, e.g. blanks become %20. On top of that, commas are encoded, which URIs
   * leave as they are, because the comma separates the file from the step ID and number. */
  private static String pathOf(File file) {
    return SCHEME + file.toURI().getRawPath().replace(SEPARATOR, "%2C");
  }
}
