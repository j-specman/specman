package specman.visitor;

/**
 * Thrown by a {@link DiagramVisitor} from a {@code visit...} method to tell the traversal not to descend into
 * the children of the node just reported. The traversal carries on with the node's siblings. Only the
 * {@code visit...} methods for nodes having children declare it.
 * <p>
 * Control flow only, so it doesn't capture a stack trace.
 */
public class IgnoreChildrenException extends Exception {

  public IgnoreChildrenException() {
    super(null, null, false, false);
  }
}
