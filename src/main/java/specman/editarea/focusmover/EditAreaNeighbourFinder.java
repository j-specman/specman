package specman.editarea.focusmover;

import specman.EditorI;
import specman.editarea.EditArea;
import specman.editarea.focusmover.FocusCandidateCollector.Candidate;

import java.awt.Container;
import java.awt.Rectangle;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Finds the edit area graphically next to a given one, no matter which step or table cell it belongs to.
 * All geometry is expressed in the coordinates of the working area, which all edit areas live in.
 * <p>
 * Of the candidates above / below the source area, the ones overlapping the source horizontally are preferred,
 * because these are what the user sees when looking straight up or down. Among them the vertically nearest wins.
 * If several candidates are equally near (e.g. the branches of an if-else step), the one horizontally closest to the
 * desired x position wins. Only if nothing overlaps, the candidate with the smallest distance wins, however far
 * beside it is.
 */
class EditAreaNeighbourFinder {

  enum Direction { UP, DOWN }

  /** Neighbouring areas may overlap by the width of shared border lines, they still count as being above / below. */
  private static final int BORDER_SLACK = 4;
  /** Candidates differing by less than this in their vertical distance are considered to be in the same row. */
  private static final int ROW_TOLERANCE = 4;

  private final EditorI editor;
  private final Container workingArea;

  EditAreaNeighbourFinder(EditorI editor) {
    this.editor = editor;
    this.workingArea = editor.getArbeitsbereich();
  }

  Optional<EditArea> find(EditArea source, int desiredX, Direction direction) {
    Rectangle sourceBounds = FocusCandidateCollector.boundsInWorkingArea(workingArea, source.asComponent());
    FocusCandidateCollector focusCandidateCollector = new FocusCandidateCollector(workingArea, source);
    editor.traverseDiagram(focusCandidateCollector);
    List<Candidate> inDirection = focusCandidateCollector.candidates().stream()
      .filter(candidate -> gap(sourceBounds, candidate.bounds(), direction) >= -BORDER_SLACK)
      .toList();
    List<Candidate> overlapping = inDirection.stream()
      .filter(candidate -> overlapsHorizontally(sourceBounds, candidate.bounds()))
      .toList();
    Optional<Candidate> neighbour = overlapping.isEmpty()
      ? nearest(inDirection, sourceBounds, desiredX, direction)
      : nearestInRow(overlapping, sourceBounds, desiredX, direction);
    return neighbour.map(Candidate::area);
  }

  private Optional<Candidate> nearestInRow(List<Candidate> candidates, Rectangle sourceBounds, int desiredX, Direction direction) {
    int smallestGap = candidates.stream().mapToInt(candidate -> gap(sourceBounds, candidate.bounds(), direction)).min().orElseThrow();
    return candidates.stream()
      .filter(candidate -> gap(sourceBounds, candidate.bounds(), direction) <= smallestGap + ROW_TOLERANCE)
      .min(Comparator.comparingInt(candidate -> horizontalDistance(candidate.bounds(), desiredX)));
  }

  private Optional<Candidate> nearest(List<Candidate> candidates, Rectangle sourceBounds, int desiredX, Direction direction) {
    return candidates.stream().min(Comparator.comparingDouble(candidate -> {
      int dx = horizontalDistance(candidate.bounds(), desiredX);
      int dy = Math.max(0, gap(sourceBounds, candidate.bounds(), direction));
      return Math.hypot(dx, dy);
    }));
  }

  /** The vertical distance between the source and a candidate in the given direction. Negative if they overlap. */
  private static int gap(Rectangle source, Rectangle candidate, Direction direction) {
    return direction == Direction.DOWN
      ? candidate.y - (source.y + source.height)
      : source.y - (candidate.y + candidate.height);
  }

  private static boolean overlapsHorizontally(Rectangle a, Rectangle b) {
    return Math.min(a.x + a.width, b.x + b.width) > Math.max(a.x, b.x);
  }

  /** 0 if the x position is within the bounds' horizontal extent. */
  private static int horizontalDistance(Rectangle bounds, int x) {
    if (x < bounds.x) {
      return bounds.x - x;
    }
    return Math.max(0, x - (bounds.x + bounds.width));
  }
}
