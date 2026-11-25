package specman.visitor;

import specman.editarea.image.AbstractImageEditArea;

/** Takes away what diagrams in the traversed part of the document use of a meta tag configuration, as undoable edits.
 * The tags at steps are not part of this, they belong to the steps. */
public class MetaTagConfigUseRemover implements DiagramVisitor {
  private final String configName;

  public MetaTagConfigUseRemover(String configName) {
    this.configName = configName;
  }

  @Override
  public void visitImageArea(AbstractImageEditArea imageArea) {
    imageArea.removeMetaTagConfigUseUDBL(configName);
  }
}
