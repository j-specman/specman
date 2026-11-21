package classman.editor;

import classman.model.v001.AbstractDiagramElementModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.NoteModel_V001;
import classman.model.v001.PositionModel_V001;
import classman.render.Scene;
import classman.render.SceneElementArea;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The dragging of one box over the diagram. It changes the positions in the model as the mouse moves and can put
 * them back. A box without a position stands where the layout puts it, depending on the other boxes without one:
 * so on the first move all of them get the position they have, and only the dragged box changes. */
class BoxDrag extends Drag {
  private final AbstractDiagramElementModel_V001 box;
  private final Point2D pressedAt;
  private final Rectangle2D startArea;
  private final List<AbstractDiagramElementModel_V001> placedBoxes;
  private final List<ClassModel_V001> associationClasses;
  private final Scene startScene;
  private Map<AbstractDiagramElementModel_V001, PositionModel_V001> positionsBefore;

  /** @param placedBoxes the classes and notes of the diagram, which the layout stacks if they have no position
   * @param associationClasses the association classes, which the layout puts beside their relation if they have none */
  BoxDrag(AbstractDiagramElementModel_V001 box, Point2D pressedAt, Scene startScene,
          List<AbstractDiagramElementModel_V001> placedBoxes, List<ClassModel_V001> associationClasses) {
    this.box = box;
    this.pressedAt = pressedAt;
    this.startScene = startScene;
    this.startArea = startScene.elementAreaOf(box).area;
    this.placedBoxes = placedBoxes;
    this.associationClasses = associationClasses;
  }

  @Override
  boolean hasMoved() { return positionsBefore != null; }

  /** Puts the box where it is when the mouse is at the given point. A box doesn't snap to anything. */
  @Override
  void to(Point2D at, double snapTolerance) {
    if (positionsBefore == null) {
      rememberPositions();
      freezePlacedBoxes();
    }
    setPosition(box, new PositionModel_V001(
      (int) Math.round(startArea.getX() + at.getX() - pressedAt.getX()),
      (int) Math.round(startArea.getY() + at.getY() - pressedAt.getY())));
  }

  /** Puts everything back where it was before the first move. */
  @Override
  void cancel() {
    if (positionsBefore != null) {
      positionsBefore.forEach(BoxDrag::setPosition);
      positionsBefore = null;
    }
  }

  private void rememberPositions() {
    positionsBefore = new LinkedHashMap<>();
    for (AbstractDiagramElementModel_V001 placed : placedBoxes) {
      positionsBefore.put(placed, positionOf(placed));
    }
    for (ClassModel_V001 associationClass : associationClasses) {
      positionsBefore.put(associationClass, associationClass.position);
    }
  }

  private void freezePlacedBoxes() {
    for (AbstractDiagramElementModel_V001 placed : placedBoxes) {
      SceneElementArea area = startScene.elementAreaOf(placed);
      if (positionOf(placed) == null && area != null) {
        setPosition(placed, new PositionModel_V001((int) Math.round(area.area.getX()),
          (int) Math.round(area.area.getY())));
      }
    }
  }

  private static PositionModel_V001 positionOf(AbstractDiagramElementModel_V001 element) {
    return element instanceof ClassModel_V001 classModel ? classModel.position : ((NoteModel_V001) element).position;
  }

  private static void setPosition(AbstractDiagramElementModel_V001 element, PositionModel_V001 position) {
    if (element instanceof ClassModel_V001 classModel) {
      classModel.position = position;
    }
    else {
      ((NoteModel_V001) element).position = position;
    }
  }
}
