package specman.editarea.image.classman;

import specman.link.classman.ClassmanLinkResolution;

import java.io.File;

/** What a linked diagram points to, and what the file had when it was last read or written, which tells whether the
 * user changed the diagram since. */
final class ClassmanLinkTarget {
  private final String link;
  private final File file;
  private final String savedSource;

  private ClassmanLinkTarget(String link, File file, String savedSource) {
    this.link = link;
    this.file = file;
    this.savedSource = savedSource;
  }

  /** @param link the link text which the model keeps */
  static ClassmanLinkTarget of(String link, ClassmanLinkResolution resolution) {
    return new ClassmanLinkTarget(link, resolution.isOk() ? resolution.file() : null,
      resolution.source() != null ? resolution.source() : "");
  }

  /** @return the same target, whose file now has the source */
  ClassmanLinkTarget saved(String source) {
    return new ClassmanLinkTarget(link, file, source);
  }

  String link() { return link; }

  /** @return the file the diagram is written to, null if the link doesn't lead to a class diagram: nothing is written
   * then, so that a file which is no diagram is never overwritten */
  File file() { return file; }

  /** @return the text the file had when it was last read or written */
  String savedSource() { return savedSource; }
}
