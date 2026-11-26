package specman.link.classman;

import java.io.File;

/** The result of following a Classman link: the file and its text, and what is wrong if the link doesn't lead to a
 * class diagram. */
public final class ClassmanLinkResolution {
  private final File file;
  private final String source;
  private final ClassmanLinkProblem problem;
  private final String problemDetail;
  private final String diagramId;
  private final String diagramName;

  private ClassmanLinkResolution(File file, String source, ClassmanLinkProblem problem, String problemDetail,
                                 String diagramId, String diagramName) {
    this.file = file;
    this.source = source;
    this.problem = problem;
    this.problemDetail = problemDetail;
    this.diagramId = diagramId;
    this.diagramName = diagramName;
  }

  static ClassmanLinkResolution ok(File file, String source, String diagramId, String diagramName) {
    return new ClassmanLinkResolution(file, source, null, null, diagramId, diagramName);
  }

  static ClassmanLinkResolution broken(File file, ClassmanLinkProblem problem, String problemDetail) {
    return new ClassmanLinkResolution(file, null, problem, problemDetail, null, null);
  }

  static ClassmanLinkResolution invalid(File file, String source, String parserMessage) {
    return new ClassmanLinkResolution(file, source, ClassmanLinkProblem.NOT_A_DIAGRAM, parserMessage, null, null);
  }

  /** @return true if the link leads to a file with a class diagram */
  public boolean isOk() { return problem == null; }

  /** @return the file the link leads to, null if there is none, e.g. for an alias nobody knows */
  public File file() { return file; }

  /** @return the text of the file, null if it wasn't read */
  public String source() { return source; }

  /** @return what is wrong, null if the link leads to a class diagram */
  public ClassmanLinkProblem problem() { return problem; }

  /** @return what the problem is about, see {@link ClassmanLinkProblem}, null if there is none */
  public String problemDetail() { return problemDetail; }

  /** @return the ID of the diagram in the file, null if it has none or the link doesn't lead to a diagram */
  public String diagramId() { return diagramId; }

  /** @return the name of the diagram in the file, null if it has none or the link doesn't lead to a diagram */
  public String diagramName() { return diagramName; }
}
