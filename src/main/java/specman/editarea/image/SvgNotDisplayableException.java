package specman.editarea.image;

/** Thrown for bytes which are no SVG graphic Specman can show. */
public class SvgNotDisplayableException extends RuntimeException {
  /** @param problem why the graphic can't be shown, null if the SVG loader didn't say */
  public SvgNotDisplayableException(String problem) {
    super(problem);
  }
}
