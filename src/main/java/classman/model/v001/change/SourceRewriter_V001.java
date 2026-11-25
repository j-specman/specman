package classman.model.v001.change;

import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import classman.model.v001.io.ClassDiagramSerializer_V001;

import java.util.function.Consumer;

/** Changes the model of a diagram source and writes it back, for the operations which the host does on a source
 * without the editor. */
public class SourceRewriter_V001 {
  private final ClassDiagramParser_V001 parser = new ClassDiagramParser_V001();
  private final ClassDiagramSerializer_V001 serializer = new ClassDiagramSerializer_V001();

  /** The source is only rewritten, in canonical form, if the change actually changes the diagram, so a diagram
   * without such a change keeps the formatting of the user. A source which can't be read stays as it is: the editor
   * only commits sources which were validated.
   * @return the changed source, the given source itself if there is nothing to change */
  public String rewritten(String source, Consumer<ClassDiagramModel_V001> change) {
    try {
      ClassDiagramModel_V001 model = parser.parse(source);
      String before = serializer.serialize(model);
      change.accept(model);
      String after = serializer.serialize(model);
      return before.equals(after) ? source : after;
    }
    catch (ClassDiagramParseException e) {
      return source;
    }
  }
}
