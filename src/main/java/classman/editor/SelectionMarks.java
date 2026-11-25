package classman.editor;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

/** What the canvas shows to mark the selection, apart from the diagram itself. */
class SelectionMarks {
  /** The areas of the diagram which get a frame: the selected box, and the class which the end of a relation is
   * going to be hung onto or ended at. */
  final List<Rectangle2D> frames;
  /** The points of a selected relation which are marked with small circles. */
  final List<Point2D> handles;
  /** The places of the texts which a selected relation could get, marked by dashed rectangles. */
  final List<Rectangle2D> placeholders;
  /** The thin dashed lines which join the texts of a selected relation which the user has moved far away to its line. */
  final List<Line2D> leaders;
  /** The fill of the places while changes are tracked, the pale color of the change set, null otherwise. */
  final Color placeholderFill;
  /** The line of a relation which is being drawn, null if none is. */
  final Line2D connectionLine;
  /** The color of the frames if the usual one can't be told from what is selected, null otherwise. */
  final Color specialColor;
  /** The rectangle which the user pulls open to select boxes, null if none is. */
  final Rectangle2D rubberBand;
  /** The row of the selected member, across the width of its class, null if no row is selected. */
  final Rectangle2D memberRow;

  SelectionMarks(List<Rectangle2D> frames, List<Point2D> handles, List<Rectangle2D> placeholders, List<Line2D> leaders,
                 Color placeholderFill, Line2D connectionLine, Color specialColor, Rectangle2D rubberBand,
                 Rectangle2D memberRow) {
    this.frames = List.copyOf(frames);
    this.handles = List.copyOf(handles);
    this.placeholders = List.copyOf(placeholders);
    this.leaders = List.copyOf(leaders);
    this.placeholderFill = placeholderFill;
    this.connectionLine = connectionLine;
    this.specialColor = specialColor;
    this.rubberBand = rubberBand;
    this.memberRow = memberRow;
  }

  static SelectionMarks none() {
    return new SelectionMarks(List.of(), List.of(), List.of(), List.of(), null, null, null, null, null);
  }
}
