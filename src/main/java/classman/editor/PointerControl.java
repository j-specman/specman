package classman.editor;

import classman.render.SceneTextArea;

import java.awt.Cursor;
import java.awt.geom.Point2D;
import java.util.function.Consumer;
import java.util.function.DoubleUnaryOperator;

/** What the mouse does in the editor: a press selects the box or relation below it, dragging moves the selected box
 * or a handle of the relation, a double click on one of its texts lets the user change the text, and the context menu
 * of a class lets the user add parts to it. A class changes its width where the mouse is at its left or right side,
 * which the cursor shows. Distances which the user judges on the screen are given in pixels and
 * turned into units of the diagram, so they feel the same at every zoom. */
class PointerControl implements PointerListener {
  static final double HIT_PIXELS = 4;
  private static final double SNAP_PIXELS = 6;

  private final DiagramEditing editing;
  private final Runnable showDiagram;
  private final Consumer<SceneTextArea> editText;
  private final Consumer<Point2D> showContextMenu;
  private final DoubleUnaryOperator toDiagramLength;

  /** @param showDiagram shows the diagram and the selection as the editing has them now
   * @param editText lets the user change a text of the diagram
   * @param showContextMenu shows the menu of the selected class or note at the point of the diagram
   * @param toDiagramLength turns a length in pixels of the screen into a length in units of the diagram */
  PointerControl(DiagramEditing editing, Runnable showDiagram, Consumer<SceneTextArea> editText,
                 Consumer<Point2D> showContextMenu, DoubleUnaryOperator toDiagramLength) {
    this.editing = editing;
    this.showDiagram = showDiagram;
    this.editText = editText;
    this.showContextMenu = showContextMenu;
    this.toDiagramLength = toDiagramLength;
  }

  /** The cursor shows what a press would do: it draws a relation while the tool for relations is armed, it changes
   * the width at the left or right side of a class or note, except where a handle of the selected relation is: a drag there
   * moves the handle. */
  @Override
  public Cursor cursorAt(Point2D at) {
    if (editing.isFormatPainterArmed()) {
      return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    }
    if (editing.isToolArmed()) {
      return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }
    double tolerance = toDiagramLength.applyAsDouble(HIT_PIXELS);
    ResizeTarget edge = editing.resizeTargetAt(at, tolerance);
    if (edge == null || editing.isOnHandle(at, tolerance)) {
      return Cursor.getDefaultCursor();
    }
    return Cursor.getPredefinedCursor(edge.side == ResizeSide.LEFT ? Cursor.W_RESIZE_CURSOR : Cursor.E_RESIZE_CURSOR);
  }

  /** A press selects what is below it. Dragging moves a handle of the selected relation if the press was on one,
   * a side of a class changes its width, otherwise the selected box is moved. While a tool is armed, a
   * press on a class (or a note, for the anchor) starts a connection instead, which is drawn by dragging to a class, and
   * nothing is selected. A press while the format painter is armed gives the element below it the style which was
   * picked up. */
  @Override
  public void pressed(Point2D at) {
    double tolerance = toDiagramLength.applyAsDouble(HIT_PIXELS);
    if (editing.isFormatPainterArmed()) {
      editing.paintFormatAt(at, tolerance);
      showDiagram.run();
      return;
    }
    if (editing.isToolArmed()) {
      editing.startDrawing(at, tolerance);
      showDiagram.run();
      return;
    }
    editing.selectAt(at, tolerance);
    editing.startDrag(at, tolerance);
    showDiagram.run();
  }

  @Override
  public void dragged(Point2D at) {
    editing.dragTo(at, toDiagramLength.applyAsDouble(SNAP_PIXELS));
    showDiagram.run();
  }

  @Override
  public void released(Point2D at) {
    editing.endDrag();
    showDiagram.run();
  }

  /** A double click on a waypoint of the selected relation deletes it, one on its line elsewhere adds a waypoint. A
   * double click on a text of the box it selects, or on a text of a relation, lets the user change the text. Nothing
   * of this happens while a tool is armed. */
  @Override
  public void doubleClicked(Point2D at) {
    if (editing.isToolArmed()) {
      return;
    }
    double tolerance = toDiagramLength.applyAsDouble(HIT_PIXELS);
    if (editing.deleteWaypointAt(at, tolerance) || editing.insertWaypointAt(at, tolerance)) {
      showDiagram.run();
      return;
    }
    editing.selectAt(at, tolerance);
    showDiagram.run();
    SceneTextArea text = editing.editableTextAt(at);
    if (text != null) {
      editText.accept(text);
    }
  }

  /** The context menu selects what is below the point, like a press, and is shown if that is a class or a note, or an
   * end handle of the selected relation (the press doesn't select anything else there). Nothing else has a menu yet, and
   * nothing happens while a tool is armed. */
  @Override
  public void contextMenuRequested(Point2D at) {
    if (editing.isToolArmed()) {
      return;
    }
    editing.selectAt(at, toDiagramLength.applyAsDouble(HIT_PIXELS));
    showDiagram.run();
    boolean onEndHandle = editing.endHandleAt(at, toDiagramLength.applyAsDouble(HIT_PIXELS)) != null;
    boolean onMovableText = editing.movableTextAt(at) != null;
    if (editing.selectedClass() != null || editing.selectedNote() != null || onEndHandle || onMovableText) {
      showContextMenu.accept(at);
    }
  }
}
