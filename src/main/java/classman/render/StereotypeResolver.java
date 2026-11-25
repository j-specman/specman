package classman.render;

/** Gives a diagram what a stereotype looks like which it refers to by the name of a configuration of the host. */
@FunctionalInterface
public interface StereotypeResolver {
  /** A host without configurations: every reference stays unknown. */
  StereotypeResolver NONE = name -> null;

  /** @return the look of the stereotype, null if the host doesn't know a configuration of that name, in which case
   * the diagram shows the name itself */
  StereotypeLook lookOf(String name);
}
