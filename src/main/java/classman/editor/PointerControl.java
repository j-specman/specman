package classman.editor;

import classman.render.SceneTextArea;

import java.awt.geom.Point2D;
import java.util.function.Consumer;

/** What the mouse does in the editor: a press selects the box below it, dragging moves the selected box, and a
 * double click on one of its texts lets the user change the text. */
class PointerControl implements PointerListener {
  private final DiagramEditing editing;
  private final Runnable showDiagram;
  private final Consumer<SceneTextArea> editText;

  /** @param showDiagram shows the diagram and the selection as the editing has them now
   * @param editText lets the user change a text of the diagram */
  PointerControl(DiagramEditing editing, Runnable showDiagram, Consumer<SceneTextArea> editText) {
    this.editing = editing;
    this.showDiagram = showDiagram;
    this.editText = editText;
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

  /** A double click on a text of the box it selects lets the user change the text. */
  @Override
  public void doubleClicked(Point2D at) {
    editing.selectAt(at);
    showDiagram.run();
    SceneTextArea text = editing.selectedTextAt(at);
    if (text != null) {
      editText.accept(text);
    }
  }
}
