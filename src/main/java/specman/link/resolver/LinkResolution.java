package specman.link.resolver;

import specman.link.LinkAliases;

import java.io.File;

/** What a link stands for. The file is null if the link doesn't even lead to one. The detail depends on the status: For
 * {@link ResolutionStatus#OK} and {@link ResolutionStatus#SANITIZED} it is the complete link with the step's current number,
 * for {@link ResolutionStatus#ERROR} it is the explanation. */
public class LinkResolution {
  private final ResolutionStatus status;
  private final File file;
  private final String detail;

  private LinkResolution(ResolutionStatus status, File file, String detail) {
    this.status = status;
    this.file = file;
    this.detail = detail;
  }

  static LinkResolution ok(File file, String link) {
    return new LinkResolution(ResolutionStatus.OK, file, link);
  }

  static LinkResolution sanitized(File file, String correctedLink) {
    return new LinkResolution(ResolutionStatus.SANITIZED, file, correctedLink);
  }

  static LinkResolution error(File file, String explanation) {
    return new LinkResolution(ResolutionStatus.ERROR, file, explanation);
  }

  public ResolutionStatus status() {
    return status;
  }

  public File file() {
    return file;
  }

  public String detail() {
    return detail;
  }

  public boolean isError() {
    return status == ResolutionStatus.ERROR;
  }

  /** @return status, file and detail separated by the separator in one line of text, with an empty field for a missing file,
   * whatever the detail contains. */
  public String toLine(String separator) {
    String filePath = file == null ? "" : LinkAliases.absolutePath(file);
    return status + separator + filePath + separator + detail.replaceAll("\\s+", " ");
  }
}
