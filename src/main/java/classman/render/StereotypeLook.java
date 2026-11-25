package classman.render;

/** What the host knows about a stereotype which a diagram refers to by name, and the diagram doesn't carry itself. */
public final class StereotypeLook {
  /** The text shown between the angle brackets, empty if the stereotype shows no text. */
  public final String label;
  /** The icon which stands right of the class name, null if the stereotype has none. */
  public final VectorImage icon;
  /** The colors which a class with the stereotype has, as #rrggbb or #rrggbbaa, each null for the default of the class.
   * The own style of a class wins over them. */
  public final String background;
  public final String border;
  public final String foreground;

  public StereotypeLook(String label, VectorImage icon) {
    this(label, icon, null, null, null);
  }

  private StereotypeLook(String label, VectorImage icon, String background, String border, String foreground) {
    this.label = label;
    this.icon = icon;
    this.background = background;
    this.border = border;
    this.foreground = foreground;
  }

  /** @param background the fill of the box, null for the default of the class
   * @param border the color of the lines of the box, null for the default
   * @param foreground the color of all texts of the box, null for the default
   * @return the same look with these colors */
  public StereotypeLook withColors(String background, String border, String foreground) {
    return new StereotypeLook(label, icon, background, border, foreground);
  }
}
