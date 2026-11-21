package classman.editor;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/** What the mouse does in the editor: a press selects the box below it, dragging moves the selected box, and a
 * double click on a class lets the user rename it. */
class PointerControl implements PointerListener {
  private final DiagramEditing editing;
  private final Runnable showDiagram;
  private final Runnable editName;

  /** @param showDiagram shows the diagram and the selection as the editing has them now
   * @param editName lets the user edit the name of the selected class, if a class is selected */
  PointerControl(DiagramEditing editing, Runnable showDiagram, Runnable editName) {
    this.editing = editing;
    this.showDiagram = showDiagram;
    this.editName = editName;
  }

  @Override
  public void pressed(Point2D at) {
    editing.selectAt(at);
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

  /** Only a double click on the name opens the name editor, not one on the stereotype or the attributes. */
  @Override
  public void doubleClicked(Point2D at) {
    editing.selectAt(at);
    showDiagram.run();
    Rectangle2D nameArea = editing.selectedNameArea();
    if (nameArea != null && nameArea.contains(at)) {
      editName.run();
    }
  }
}
