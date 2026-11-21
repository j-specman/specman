package classman.editor;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

/** What the canvas shows to mark the selection, apart from the diagram itself. */
class SelectionMarks {
  /** The areas of the diagram which get a frame: the selected box, and the class which the end of a relation is
   * going to be hung onto. */
  final List<Rectangle2D> frames;
  /** The points of a selected relation which are marked with small circles. */
  final List<Point2D> handles;
  /** The places of the texts which a selected relation could get, marked by dashed rectangles. */
  final List<Rectangle2D> placeholders;
  /** The fill of the places while changes are tracked, the pale color of the change set, null otherwise. */
  final Color placeholderFill;
  /** The color of the frames if the usual one can't be told from what is selected, null otherwise. */
  final Color specialColor;

  SelectionMarks(List<Rectangle2D> frames, List<Point2D> handles, List<Rectangle2D> placeholders,
                 Color placeholderFill, Color specialColor) {
    this.frames = List.copyOf(frames);
    this.handles = List.copyOf(handles);
    this.placeholders = List.copyOf(placeholders);
    this.placeholderFill = placeholderFill;
    this.specialColor = specialColor;
  }

  static SelectionMarks none() {
    return new SelectionMarks(List.of(), List.of(), List.of(), null, null);
  }
}
