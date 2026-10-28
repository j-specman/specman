package specman.visitor;

import specman.ChangeInfo;
import specman.ChangeSet;
import specman.editarea.AbstractListItemEditArea;
import specman.editarea.ImageEditArea;
import specman.editarea.TextEditArea;

/**
 * Finds the change set of the first change mark in the traversed part of the diagram, i.e. a text with change
 * markups, a changed image, or a changed list item. The traversal is stopped as soon as there is one.
 * Catch areas and the headings of branches and catch sequences are part of what is searched.
 */
public class FirstChangeSetFinder implements DiagramVisitor {

  private ChangeSet firstChangeSet;

  @Override
  public void visitTextArea(TextEditArea textArea) throws StopTraversalException {
    found(textArea.firstChangeSet());
  }

  @Override
  public void visitImageArea(ImageEditArea imageArea) throws StopTraversalException {
    found(imageArea.firstChangeSet());
  }

  @Override
  public void visitListItem(AbstractListItemEditArea listItem) throws StopTraversalException {
    ChangeInfo changeInfo = listItem.getChangeInfo();
    if (changeInfo.isChange()) {
      found(changeInfo.changeSet());
    }
  }

  private void found(ChangeSet changeSet) throws StopTraversalException {
    if (changeSet != null) {
      firstChangeSet = changeSet;
      throw new StopTraversalException();
    }
  }

  /** @return the change set of the first change mark found, or null if there is none */
  public ChangeSet changeSet() {
    return firstChangeSet;
  }
}
