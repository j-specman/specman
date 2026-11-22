package classman.editor;

import classman.model.v001.AbstractDiagramElementModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.model.v001.NoteModel_V001;
import classman.model.v001.PositionModel_V001;
import classman.render.Scene;
import classman.render.SceneElementArea;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Fixes the places of the boxes of a diagram while one of them is changed by dragging. A box without a position
 * stands where the layout puts it, depending on the other boxes without one: so on the first change all of them get
 * the position they have, and only the dragged box changes after that. Everything can be put back. */
class PositionFreezer {
  private final Scene startScene;
  private final List<AbstractDiagramElementModel_V001> placedBoxes;
  private final List<ClassModel_V001> associationClasses;
  private Map<AbstractDiagramElementModel_V001, PositionModel_V001> positionsBefore;

  /** @param startScene the diagram as it is before the first change
   * @param placedBoxes the classes and notes of the diagram, which the layout stacks if they have no position
   * @param associationClasses the association classes, which the layout puts beside their relation if they have none */
  PositionFreezer(Scene startScene, List<AbstractDiagramElementModel_V001> placedBoxes,
                  List<ClassModel_V001> associationClasses) {
    this.startScene = startScene;
    this.placedBoxes = placedBoxes;
    this.associationClasses = associationClasses;
  }

  boolean isFrozen() { return positionsBefore != null; }

  /** Remembers all positions and gives each box without one the position it has in the start scene. Does nothing if
   * that was done already. */
  void freeze() {
    if (positionsBefore != null) {
      return;
    }
    positionsBefore = new LinkedHashMap<>();
    for (AbstractDiagramElementModel_V001 placed : placedBoxes) {
      positionsBefore.put(placed, positionOf(placed));
    }
    for (ClassModel_V001 associationClass : associationClasses) {
      positionsBefore.put(associationClass, associationClass.position);
    }
    for (AbstractDiagramElementModel_V001 placed : placedBoxes) {
      SceneElementArea area = startScene.elementAreaOf(placed);
      if (positionOf(placed) == null && area != null) {
        setPosition(placed, new PositionModel_V001((int) Math.round(area.area.getX()),
          (int) Math.round(area.area.getY())));
      }
    }
  }

  /** Puts all positions back as they were before {@link #freeze}. */
  void restore() {
    if (positionsBefore != null) {
      positionsBefore.forEach(PositionFreezer::setPosition);
      positionsBefore = null;
    }
  }

  static PositionModel_V001 positionOf(AbstractDiagramElementModel_V001 element) {
    return element instanceof ClassModel_V001 classModel ? classModel.position : ((NoteModel_V001) element).position;
  }

  static void setPosition(AbstractDiagramElementModel_V001 element, PositionModel_V001 position) {
    if (element instanceof ClassModel_V001 classModel) {
      classModel.position = position;
    }
    else {
      ((NoteModel_V001) element).position = position;
    }
  }
}
