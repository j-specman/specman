package specman.visitor;

import specman.editarea.image.AbstractImageEditArea;

import java.util.HashSet;
import java.util.Set;

/** Collects the names of the meta tag configurations which diagrams in the traversed part of the document use, e.g.
 * as the stereotype of a class. The tags at steps are not part of this, they belong to the steps. */
public class MetaTagConfigUseCollector implements DiagramVisitor {
  private final Set<String> configNames = new HashSet<>();

  @Override
  public void visitImageArea(AbstractImageEditArea imageArea) {
    configNames.addAll(imageArea.metaTagConfigNamesInUse());
  }

  public Set<String> configNames() {
    return configNames;
  }
}
