package specman.visitor;

import org.jetbrains.annotations.NotNull;
import specman.ChangeSet;
import specman.editarea.AbstractListItemEditArea;
import specman.editarea.ImageEditArea;
import specman.editarea.TableEditArea;
import specman.editarea.TextEditArea;
import specman.view.AbstractSchrittView;
import specman.view.SchrittSequenzView;

/**
 * Reassigns everything changed by the source change set to the target change set in the traversed part of the
 * diagram: steps, sequences, catch headings, edit areas and, if requested, the change markups in the texts.
 * Each node merges only itself, descending into the diagram is the traversal's job.
 */
public class ChangeSetMerger implements DiagramVisitor {

  private final ChangeSet target;
  private final ChangeSet source;
  private final boolean withMarkups;

  public ChangeSetMerger(@NotNull ChangeSet target, @NotNull ChangeSet source, boolean withMarkups) {
    this.target = target;
    this.source = source;
    this.withMarkups = withMarkups;
  }

  @Override
  public void visitStep(AbstractSchrittView step) {
    step.mergeChangeSetUDBL(target, source);
  }

  @Override
  public void visitSequence(SchrittSequenzView sequence) {
    sequence.mergeChangeSetUDBL(target, source);
  }

  @Override
  public void visitTextArea(TextEditArea textArea) {
    textArea.mergeChangeSetUDBL(target, source, withMarkups);
  }

  @Override
  public void visitImageArea(ImageEditArea imageArea) {
    imageArea.mergeChangeSetUDBL(target, source, withMarkups);
  }

  @Override
  public void visitTable(TableEditArea table) {
    table.mergeChangeSetUDBL(target, source, withMarkups);
  }

  @Override
  public void visitListItem(AbstractListItemEditArea listItem) {
    listItem.mergeChangeSetUDBL(target, source, withMarkups);
  }
}
