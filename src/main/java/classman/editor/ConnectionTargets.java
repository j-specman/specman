package classman.editor;

import classman.model.v001.AbstractRelationModel_V001;
import classman.model.v001.ChangeType_V001;
import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.ClassModel_V001;
import classman.render.Scene;
import classman.render.SceneRelationLine;
import classman.render.SceneTextArea;
import classman.render.TextKind;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/** The things a connection can end at in a diagram as it is shown now. */
class ConnectionTargets {
  private final ClassDiagramModel_V001 model;
  private final Scene scene;

  ConnectionTargets(ClassDiagramModel_V001 model, Scene scene) {
    this.model = model;
    this.scene = scene;
  }

  /** @return the class which the point is on or at most the tolerance away from, null if there is none */
  ClassTarget closestClass(Point2D point, double tolerance) {
    return closestClass(point, tolerance, box -> true);
  }

  /** @param allowed tells which of the classes count, the others are not there for this search
   * @return the class which the point is on or at most the tolerance away from, null if there is none */
  ClassTarget closestClass(Point2D point, double tolerance, Predicate<ClassModel_V001> allowed) {
    Map<ClassModel_V001, Rectangle2D> areas = new LinkedHashMap<>();
    for (ClassModel_V001 box : model.classes()) {
      if (allowed.test(box)) {
        areas.put(box, scene.elementAreaOf(box).area);
      }
    }
    ClassModel_V001 closest = ClassPicker.closest(areas, point, tolerance);
    return closest != null ? new ClassTarget(closest, areas.get(closest)) : null;
  }

  /** @return what the anchor of a note can end at below the point: the attribute whose line the point is on, else a
   * relation whose line is at most the tolerance away, else a class like {@link #closestClass} finds it. The attribute
   * wins over its class, so the user takes the class by its name or by the space around the attributes. */
  ConnectionTarget closestForAnchor(Point2D point, double tolerance) {
    AttributeTarget attribute = attributeAt(point);
    if (attribute != null) {
      return attribute;
    }
    RelationTarget relation = closestRelation(point, tolerance, candidate -> true);
    if (relation != null) {
      return relation;
    }
    return closestClass(point, tolerance);
  }

  /** Only the attributes of the classes of the diagram, which are not removed, count: a note can't be attached to the
   * others. */
  private AttributeTarget attributeAt(Point2D point) {
    for (int i = scene.textAreas.size() - 1; i >= 0; i--) {
      SceneTextArea text = scene.textAreas.get(i);
      if (isAttributeLineOfClass(text) && text.area.contains(point)
        && (text.attribute.change == null || text.attribute.change.type != ChangeType_V001.REMOVED)) {
        return new AttributeTarget(text.attribute, text.area);
      }
    }
    return null;
  }

  private boolean isAttributeLineOfClass(SceneTextArea text) {
    return text.kind == TextKind.ATTRIBUTE_LINE && text.attribute != null
      && text.element instanceof ClassModel_V001 owner && model.classes().contains(owner);
  }

  /** @param allowed tells which of the relations count, the others are not there for this search. A relation which a
   * change set has removed never counts.
   * @return the relation whose line is at most the tolerance away from the point, the closest one, null if there is
   * none */
  RelationTarget closestRelation(Point2D point, double tolerance, Predicate<AbstractRelationModel_V001> allowed) {
    SceneRelationLine closest = null;
    double closestDistance = tolerance;
    for (SceneRelationLine candidate : scene.relationLines) {
      boolean removed = candidate.relation.change != null && candidate.relation.change.type == ChangeType_V001.REMOVED;
      double distance = candidate.distanceTo(point);
      if (!removed && allowed.test(candidate.relation) && distance <= closestDistance) {
        closest = candidate;
        closestDistance = distance;
      }
    }
    return closest != null ? new RelationTarget(closest) : null;
  }
}
