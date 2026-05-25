package specman.ops;

import specman.SpecmanOpContext;
import specman.SpecmanOpContextMixin;

/**
 * Base class for functionality extracted from the {@link specman.Specman} monolith.
 * <p>
 * Specman has grown as a JFrame monolith. To reduce it incrementally, well-isolated
 * code blocks are moved into subclasses of this base class. Access to Specman internals
 * is intentionally done via package visibility rather than EditorI, to avoid bloating
 * the EditorI interface with implementation details.
 */
public abstract class AbstractSpecmanOp implements SpecmanOpContextMixin {

  protected final SpecmanOpContext context;

  protected AbstractSpecmanOp(SpecmanOpContext context) {
    this.context = context;
  }

  @Override
  public SpecmanOpContext context() { return context; }

}
