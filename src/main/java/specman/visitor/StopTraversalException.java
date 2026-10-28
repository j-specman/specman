package specman.visitor;

/**
 * Thrown by a {@link DiagramVisitor} from any {@code visit...} method to stop the whole traversal, e.g.
 * once it has found what it is looking for. It unwinds all the way to the place the traversal was started,
 * which ends the traversal normally. Nodes not yet visited stay unvisited.
 * <p>
 * Control flow only, so it doesn't capture a stack trace.
 */
public class StopTraversalException extends Exception {

  public StopTraversalException() {
    super(null, null, false, false);
  }
}
