package specman.visitor;

import specman.editarea.image.AbstractImageEditArea;

import java.util.LinkedHashSet;
import java.util.Set;

/** Takes away what diagrams in the traversed part of the document use of meta tag configurations which the document
 * doesn't have, for diagrams which have just come in from outside. */
public class UnknownMetaTagConfigDropper implements DiagramVisitor {
  private final Set<String> droppedConfigNames = new LinkedHashSet<>();

  @Override
  public void visitImageArea(AbstractImageEditArea imageArea) {
    imageArea.dropUnknownMetaTagConfigs(droppedConfigNames);
  }

  /** @return the names of the configurations which were missing */
  public Set<String> droppedConfigNames() {
    return droppedConfigNames;
  }
}
