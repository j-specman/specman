package specman.editarea.image.classman;

import specman.link.classman.ClassmanLink;
import specman.link.classman.ClassmanLinkProblem;
import specman.link.classman.ClassmanLinkReference;
import specman.link.classman.ClassmanLinkResolution;

import java.io.File;

import static specman.i18n.Messages.i18n;

/** What a linked diagram points to, and what the file had when it was last read or written, which tells whether the
 * user changed the diagram since. */
final class ClassmanLinkTarget {
  private final String link;
  private final File file;
  private final String savedSource;
  private final boolean duplicate;
  private final String placeholderProblem;

  private ClassmanLinkTarget(String link, File file, String savedSource, boolean duplicate, String placeholderProblem) {
    this.link = link;
    this.file = file;
    this.savedSource = savedSource;
    this.duplicate = duplicate;
    this.placeholderProblem = placeholderProblem;
  }

  /** @param link the link text which the model keeps */
  static ClassmanLinkTarget of(String link, ClassmanLinkResolution resolution) {
    return new ClassmanLinkTarget(link, resolution.isOk() ? resolution.file() : null,
      resolution.source() != null ? resolution.source() : "", false, placeholderProblemOf(resolution));
  }

  /** A file which was read but has no diagram is not a placeholder: its content is shown as the problem of the diagram,
   * like for an embedded one. */
  private static String placeholderProblemOf(ClassmanLinkResolution resolution) {
    boolean fileHasNoDiagram = resolution.problem() == ClassmanLinkProblem.NOT_A_DIAGRAM;
    return resolution.isOk() || fileHasNoDiagram ? null : resolution.problemText();
  }

  /** @return the same target, whose file now has the source */
  ClassmanLinkTarget saved(String source) {
    return new ClassmanLinkTarget(link, file, source, duplicate, placeholderProblem);
  }

  /** @return the same target as the one of a diagram which is not the first to link the file: it has no file to write */
  ClassmanLinkTarget asDuplicate() {
    return new ClassmanLinkTarget(link, null, savedSource, true, i18n("msg.classman.link.problem.duplicate"));
  }

  String link() { return link; }

  /** @return the alias the link names its file by, null if it names it otherwise or isn't a link at all */
  String alias() {
    try {
      ClassmanLinkReference reference = ClassmanLink.parse(link);
      return reference.hasAlias() ? reference.alias() : null;
    }
    catch (IllegalArgumentException notALink) {
      return null;
    }
  }

  /** @return the file the diagram is written to, null if the link doesn't lead to a class diagram or if another diagram
   * links the file as well: nothing is written then, so that a file which is no diagram is never overwritten and two
   * diagrams never overwrite each other */
  File file() { return file; }

  boolean isDuplicate() { return duplicate; }

  /** @return what keeps the diagram from being shown, in the language of the user, null if it is shown from its source.
   * A placeholder with the link is shown instead. */
  String placeholderProblem() { return placeholderProblem; }

  /** @return the text the file had when it was last read or written */
  String savedSource() { return savedSource; }
}
