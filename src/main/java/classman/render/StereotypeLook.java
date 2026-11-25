package classman.render;

/** What the host knows about a stereotype which a diagram refers to by name, and the diagram doesn't carry itself. */
public final class StereotypeLook {
  /** The text shown between the angle brackets, empty if the stereotype shows no text. */
  public final String label;

  public StereotypeLook(String label) {
    this.label = label;
  }
}
