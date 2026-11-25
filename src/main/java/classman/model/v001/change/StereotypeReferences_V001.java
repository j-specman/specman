package classman.model.v001.change;

import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.StereotypeReference_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** The configurations of the host which the stereotypes of a diagram source refer to, for the host which protects
 * them against removal and takes the stereotypes away when a configuration goes. Only the stereotype itself counts,
 * not the old text of a change mark: its reference may be dangling, which a rejected change copes with. */
public class StereotypeReferences_V001 {
  private final ClassDiagramParser_V001 parser = new ClassDiagramParser_V001();
  private final SourceRewriter_V001 rewriter = new SourceRewriter_V001();

  /** @return the names of the configurations which the stereotypes refer to, none for a source which can't be read */
  public Set<String> configsUsedIn(String source) {
    Set<String> names = new LinkedHashSet<>();
    try {
      for (ClassModel_V001 box : parser.parse(source).classes()) {
        if (StereotypeReference_V001.isReference(box.stereotype)) {
          names.add(StereotypeReference_V001.nameOf(box.stereotype));
        }
      }
    }
    catch (ClassDiagramParseException unreadable) {
      return Set.of();
    }
    return names;
  }

  /** @return the names of the classes, interfaces and enums whose stereotype refers to the configuration, in the order of
   * the source, none for a source which can't be read */
  public List<String> classesUsing(String source, String configName) {
    List<String> names = new ArrayList<>();
    try {
      for (ClassModel_V001 box : parser.parse(source).classes()) {
        if (StereotypeReference_V001.isReference(box.stereotype)
            && StereotypeReference_V001.nameOf(box.stereotype).equals(configName)) {
          names.add(box.name);
        }
      }
    }
    catch (ClassDiagramParseException unreadable) {
      return List.of();
    }
    return names;
  }

  /** Takes the stereotype away from every class which refers to one of the configurations, with its marks: a
   * configuration which goes is not a change of the content, so it leaves no change mark.
   * @return the source without them, the given source itself if no class refers to one of them */
  public String withoutConfigs(String source, Collection<String> configNames) {
    return rewriter.rewritten(source, model -> removeStereotypes(model, configNames));
  }

  private static void removeStereotypes(ClassDiagramModel_V001 model, Collection<String> configNames) {
    for (ClassModel_V001 box : model.classes()) {
      if (StereotypeReference_V001.isReference(box.stereotype)
          && configNames.contains(StereotypeReference_V001.nameOf(box.stereotype))) {
        StereotypeChanger_V001.clear(box);
      }
    }
  }
}
