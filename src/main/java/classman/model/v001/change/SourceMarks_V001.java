package classman.model.v001.change;

import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import classman.model.v001.io.ClassDiagramSerializer_V001;

import java.util.function.Consumer;

/** Accepts, rejects and renames the change marks, change(...) and was(...), in the source text of a Classman diagram. */
public class SourceMarks_V001 {
  private final ClassDiagramParser_V001 parser = new ClassDiagramParser_V001();
  private final ClassDiagramSerializer_V001 serializer = new ClassDiagramSerializer_V001();
  private final ChangeResolver_V001 resolver = new ChangeResolver_V001();
  private final ChangeSetRenamer_V001 renamer = new ChangeSetRenamer_V001();

  /** @return the source with the marks of the change set accepted, the given source itself if there is nothing to accept */
  public String accepted(String source, String changeSet) {
    return changed(source, model -> resolver.accept(model, changeSet));
  }

  /** @return the source with the marks of the change set rejected, the given source itself if there is nothing to reject */
  public String rejected(String source, String changeSet) {
    return changed(source, model -> resolver.reject(model, changeSet));
  }

  /** @return the source with the marks of one change set given to another, the given source itself if there are none */
  public String renamed(String source, String from, String to) {
    return changed(source, model -> renamer.rename(model, from, to));
  }

  /** The source is only rewritten, in canonical form, if the change actually changes the diagram, so a diagram
   * without marks keeps the formatting of the user. A source which can't be read stays as it is: the editor only
   * commits sources which were validated. */
  private String changed(String source, Consumer<ClassDiagramModel_V001> change) {
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
