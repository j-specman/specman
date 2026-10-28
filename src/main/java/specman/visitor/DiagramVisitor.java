package specman.visitor;

import specman.editarea.AbstractListItemEditArea;
import specman.editarea.EditContainer;
import specman.editarea.ImageEditArea;
import specman.editarea.TableEditArea;
import specman.editarea.TextEditArea;
import specman.view.AbstractSchrittView;
import specman.view.CatchBereich;
import specman.view.SchrittSequenzView;

/**
 * Classic Visitor pattern for the recursive descent through the diagram: sequences, steps, catch areas,
 * edit containers and edit areas. Every node implements {@code accept(DiagramVisitor)}, which reports the node
 * to the visitor and passes the visitor on to the node's own children, so the descent is implemented once, not
 * once per query. A visitor only decides what to do with the nodes it is interested in, i.e. it overrides the
 * methods it needs.
 * <p>
 * Nodes are visited in document order: Every node is reported before its children, e.g. a step before its sub
 * sequences and a table before the edit areas in its cells. A branch heading comes before the branch's steps,
 * and the Else branch of a case step comes first, as it is the leftmost column on screen.
 * <p>
 * A visitor which wants to steer the traversal throws one of two checked exceptions, so all other visitors don't
 * have to care. They don't need to declare them, as overriding methods may omit the {@code throws} clause.
 * <ul>
 *   <li>{@link IgnoreChildrenException} skips everything inside the node just reported. It is declared by the
 *   methods for nodes having children only.</li>
 *   <li>{@link StopTraversalException} ends the whole traversal. It is declared by all methods.</li>
 * </ul>
 */
public interface DiagramVisitor {

  /** Called for each sequence of steps, including the branches of if, if-else and case steps and the catch sequences. */
  default void visitSequence(SchrittSequenzView sequence) throws IgnoreChildrenException, StopTraversalException {}

  /** Called for each step, before the visitor gets to see anything inside the step. */
  default void visitStep(AbstractSchrittView step) throws IgnoreChildrenException, StopTraversalException {}

  /**
   * Called for the catch area of each sequence, also if it contains no catch sequences. Catch sequences have
   * no catch area themselves. Throw {@link IgnoreChildrenException} to keep out of the catch area,
   * e.g. when only looking for break steps.
   */
  default void visitCatchArea(CatchBereich catchArea) throws IgnoreChildrenException, StopTraversalException {}

  /** Called for each edit container: of a step, a branch heading, a table cell, a list item, the intro and the outro. */
  default void visitEditContainer(EditContainer editContainer) throws IgnoreChildrenException, StopTraversalException {}

  default void visitTextArea(TextEditArea textArea) throws StopTraversalException {}

  default void visitImageArea(ImageEditArea imageArea) throws StopTraversalException {}

  default void visitTable(TableEditArea table) throws IgnoreChildrenException, StopTraversalException {}

  default void visitListItem(AbstractListItemEditArea listItem) throws IgnoreChildrenException, StopTraversalException {}

  /** One call of a {@code visit...} method for a node having children. */
  @FunctionalInterface
  interface VisitCall {
    void run() throws IgnoreChildrenException, StopTraversalException;
  }

  /**
   * Makes the call and tells whether the traversal is supposed to descend into the children of the reported node.
   * All {@code accept} methods of nodes having children report themselves this way, so this is the one place
   * to deal with the {@link IgnoreChildrenException}.
   */
  static boolean mayDescend(VisitCall visitCall) throws StopTraversalException {
    try {
      visitCall.run();
      return true;
    }
    catch (IgnoreChildrenException x) {
      return false;
    }
  }
}
