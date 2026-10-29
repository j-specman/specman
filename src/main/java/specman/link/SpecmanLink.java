package specman.link;

import specman.view.AbstractSchrittView;

import java.io.File;

/** A global link to a Specman file and optionally a step in it, e.g. {@code specman:///C:/project/UC-720.nsd,2d34f85e,1.3}.
 * With an alias for the file, the alias replaces the path: {@code specman://uc720,2d34f85e,1.3}. */
public final class SpecmanLink {

  public static final String SCHEME = "specman://";
  public static final String SEPARATOR = ",";

  private SpecmanLink() {}

  /** The link to the passed step in the passed file, which is identified by the alias if there is one, otherwise
   * by its path. The step number is merely informative. */
  public static String of(File file, String alias, AbstractSchrittView step) {
    String location = alias.isEmpty() ? pathOf(file) : SCHEME + alias;
    return location + SEPARATOR + step.getId() + SEPARATOR + step.getNumber();
  }

  /** The path is URL-encoded as usual, e.g. blanks become %20. On top of that, commas are encoded, which URIs
   * leave as they are, because the comma separates the file from the step ID and number. */
  private static String pathOf(File file) {
    return SCHEME + file.toURI().getRawPath().replace(SEPARATOR, "%2C");
  }
}
