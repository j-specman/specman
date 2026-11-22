package classman.editor;

/** The tools of the palette which are used on the diagram after the button was pressed, one at a time. */
enum Tool {
  NONE,
  /** Draws a relation of the chosen type from a class to a class. */
  RELATION,
  /** Draws the anchor of a note to the element which the note is attached to. */
  ANCHOR
}
