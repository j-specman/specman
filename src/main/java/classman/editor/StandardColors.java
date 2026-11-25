package classman.editor;

import classman.render.DiagramStyle;
import classman.render.StereotypeLook;

/** The colors which an element has without a color of its own: what the style dialog shows for "default". A class whose
 * stereotype refers to a configuration of the host has the colors of the configuration where it has some. */
final class StandardColors {
  static final StandardColors NOTE = new StandardColors(DiagramStyle.NOTE_FILL, DiagramStyle.TEXT_COLOR,
    DiagramStyle.STROKE);
  static final StandardColors RELATION = new StandardColors(null, DiagramStyle.TEXT_COLOR, DiagramStyle.STROKE);

  /** The background, null for an element without one. */
  final String fill;
  final String text;
  final String line;

  private StandardColors(String fill, String text, String line) {
    this.fill = fill;
    this.text = text;
    this.line = line;
  }

  /** @param look what the host says about the configuration which the stereotype of the class refers to, null if the
   * class has no such stereotype */
  static StandardColors forClass(StereotypeLook look) {
    if (look == null) {
      return new StandardColors(DiagramStyle.CLASS_FILL, DiagramStyle.TEXT_COLOR, DiagramStyle.STROKE);
    }
    return new StandardColors(
      look.background != null ? look.background : DiagramStyle.CLASS_FILL,
      look.foreground != null ? look.foreground : DiagramStyle.TEXT_COLOR,
      look.border != null ? look.border : DiagramStyle.STROKE);
  }
}
