package classman.editor;

/** The tools of the palette which are used on the diagram after the button was pressed, one at a time. */
enum Tool {
  NONE,
  /** Draws an association from a class to a class. */
  ASSOCIATION,
  /** Draws the anchor of a note to the element which the note is attached to. */
  ANCHOR
}
