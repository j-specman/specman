package classman.editor;

import java.awt.geom.Point2D;

/** What the mouse does in the editor: a press selects the box below it, and dragging moves the selected box. */
class PointerControl implements PointerListener {
  private final DiagramEditing editing;
  private final Runnable showDiagram;

  /** @param showDiagram shows the diagram and the selection as the editing has them now */
  PointerControl(DiagramEditing editing, Runnable showDiagram) {
    this.editing = editing;
    this.showDiagram = showDiagram;
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
}
