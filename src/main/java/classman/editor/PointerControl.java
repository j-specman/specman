package classman.editor;

import classman.render.SceneTextArea;

import java.awt.geom.Point2D;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

/** What the mouse does in the editor: a press selects the box or relation below it, dragging moves the selected box,
 * and a double click on one of its texts lets the user change the text. */
class PointerControl implements PointerListener {
  private final DiagramEditing editing;
  private final Runnable showDiagram;
  private final Consumer<SceneTextArea> editText;
  private final DoubleSupplier hitTolerance;

  /** @param showDiagram shows the diagram and the selection as the editing has them now
   * @param editText lets the user change a text of the diagram
   * @param hitTolerance how far, in units of the diagram, a press may be from the line of a relation or from one of
   * its handles and still hit it */
  PointerControl(DiagramEditing editing, Runnable showDiagram, Consumer<SceneTextArea> editText,
                 DoubleSupplier hitTolerance) {
    this.editing = editing;
    this.showDiagram = showDiagram;
    this.editText = editText;
    this.hitTolerance = hitTolerance;
  }

  @Override
  public void pressed(Point2D at) {
    editing.selectAt(at, hitTolerance.getAsDouble());
    editing.startDrag(at);
    showDiagram.run();
  }

  @Override
  public void dragged(Point2D at) {
    editing.dragTo(at);
    showDiagram.run();
  }

  @Override
  public void released(Point2D at) {
    editing.endDrag();
    showDiagram.run();
  }

  /** A double click on a waypoint of the selected relation deletes it. A double click on a text of the box it selects,
   * or on a text of a relation, lets the user change the text. */
  @Override
  public void doubleClicked(Point2D at) {
    if (editing.deleteWaypointAt(at, hitTolerance.getAsDouble())) {
      showDiagram.run();
      return;
    }
    editing.selectAt(at, hitTolerance.getAsDouble());
    showDiagram.run();
    SceneTextArea text = editing.editableTextAt(at);
    if (text != null) {
      editText.accept(text);
    }
  }
}
