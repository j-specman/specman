package specman;

/**
 * Base class for functionality extracted from the {@link Specman} monolith.
 * <p>
 * Specman has grown as a JFrame monolith. To reduce it incrementally, well-isolated
 * code blocks are moved into subclasses of this base class. Access to Specman internals
 * is intentionally done via package visibility rather than EditorI, to avoid bloating
 * the EditorI interface with implementation details.
 */
abstract class AbstractSpecmanOp {

  protected final Specman specman;

  AbstractSpecmanOp(Specman specman) {
    this.specman = specman;
  }

}
